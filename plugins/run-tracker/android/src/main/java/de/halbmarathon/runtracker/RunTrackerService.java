package de.halbmarathon.runtracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Vordergrunddienst für die Laufaufzeichnung.
 *
 * Nutzt ausschließlich den Android-LocationManager mit dem GPS-Anbieter. Es werden keine
 * Google-Dienste verwendet. Die Messpunkte liegen nur im Arbeitsspeicher dieses Prozesses
 * und werden nie auf den Datenträger geschrieben.
 */
public class RunTrackerService extends Service implements LocationListener {

    static final String CHANNEL_ID = "run_tracking";
    static final int NOTIFICATION_ID = 4711;
    static final int MAX_FIXES = 30000;

    // Gemeinsamer Zustand mit dem Plugin (gleicher Prozess).
    static final Object LOCK = new Object();
    static final ArrayList<double[]> FIXES = new ArrayList<>(); // [Zeit ms, Breite, Länge, Genauigkeit m, Tempo m/s]
    static volatile boolean running = false;
    static volatile boolean paused = false;
    static volatile boolean gpsOn = true;
    static volatile long startedAt = 0;
    static volatile String cuesJson = "[]";
    static volatile String meta = "";
    static volatile RunTrackerService instance = null;

    private static class Cue {
        long atMs;
        String kind;
        String text;
    }

    private LocationManager locationManager;
    private Handler handler;
    private PowerManager.WakeLock wakeLock;
    private final List<Cue> cues = new ArrayList<>();
    private final List<Runnable> scheduled = new ArrayList<>();
    private long activeBaseMs = 0;
    private long lastResumeRt = 0;
    private String notificationText = "GPS aktiv";

    /** Vom Plugin aufgerufen, bevor der Dienst gestartet wird. */
    static void prepare(String cues, String metaData) {
        synchronized (LOCK) {
            FIXES.clear();
        }
        cuesJson = cues == null ? "[]" : cues;
        meta = metaData == null ? "" : metaData;
        startedAt = System.currentTimeMillis();
        paused = false;
        gpsOn = true;
        running = true;
    }

    static void requestPause() {
        final RunTrackerService s = instance;
        if (s == null) return;
        s.handler.post(new Runnable() {
            @Override
            public void run() {
                s.pauseTracking();
            }
        });
    }

    static void requestResume() {
        final RunTrackerService s = instance;
        if (s == null) return;
        s.handler.post(new Runnable() {
            @Override
            public void run() {
                s.resumeTracking();
            }
        });
    }

    static void requestStop() {
        final RunTrackerService s = instance;
        if (s == null) {
            running = false;
            paused = false;
            return;
        }
        s.handler.post(new Runnable() {
            @Override
            public void run() {
                s.stopTracking();
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Wurde der Prozess beendet, gibt es nichts mehr aufzuzeichnen. Dann nicht neu starten.
        if (intent == null || !running) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (instance == null) startTracking();
        return START_NOT_STICKY;
    }

    private void startTracking() {
        instance = this;
        handler = new Handler(Looper.getMainLooper());
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        activeBaseMs = 0;
        lastResumeRt = SystemClock.elapsedRealtime();
        parseCues();
        createChannel();
        try {
            Notification n = buildNotification();
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(NOTIFICATION_ID, n);
            }
        } catch (Exception e) {
            fail();
            return;
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this, Looper.getMainLooper());
            gpsOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (SecurityException | IllegalArgumentException e) {
            fail();
            return;
        }
        // Nur bei Intervallen mit Signalen muss die CPU wach bleiben, damit die Zeitpunkte stimmen.
        if (!cues.isEmpty()) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "halbmarathon:lauf");
                wakeLock.acquire(4 * 60 * 60 * 1000L);
            }
        }
        scheduleCues();
    }

    private void fail() {
        running = false;
        instance = null;
        stopSelf();
    }

    private void pauseTracking() {
        if (!running || paused) return;
        activeBaseMs += SystemClock.elapsedRealtime() - lastResumeRt;
        paused = true;
        cancelCues();
    }

    private void resumeTracking() {
        if (!running || !paused) return;
        paused = false;
        lastResumeRt = SystemClock.elapsedRealtime();
        scheduleCues();
    }

    private void stopTracking() {
        cancelCues();
        releaseResources();
        running = false;
        paused = false;
        instance = null;
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
    }

    private void releaseResources() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (Exception ignored) {
            }
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onDestroy() {
        if (handler != null) cancelCues();
        releaseResources();
        running = false;
        paused = false;
        instance = null;
        super.onDestroy();
    }

    private long activeNowMs() {
        return paused ? activeBaseMs : activeBaseMs + (SystemClock.elapsedRealtime() - lastResumeRt);
    }

    private void parseCues() {
        cues.clear();
        try {
            JSONArray a = new JSONArray(cuesJson);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Cue c = new Cue();
                c.atMs = Math.round(o.optDouble("at", 0) * 1000);
                c.kind = o.optString("v", "lap");
                c.text = o.optString("n", "");
                if (c.atMs > 0) cues.add(c);
            }
        } catch (Exception ignored) {
        }
    }

    private void scheduleCues() {
        cancelCues();
        long now = activeNowMs();
        for (final Cue c : cues) {
            long delay = c.atMs - now;
            if (delay < 0) continue;
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    fireCue(c);
                }
            };
            scheduled.add(r);
            handler.postDelayed(r, delay);
        }
    }

    private void cancelCues() {
        for (Runnable r : scheduled) handler.removeCallbacks(r);
        scheduled.clear();
    }

    private void fireCue(Cue c) {
        vibrate(c.kind);
        if (c.text != null && c.text.length() > 0) updateNotification(c.text);
    }

    private void vibrate(String kind) {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) return;
            long[] p;
            if ("work".equals(kind)) p = new long[] { 0, 250, 120, 250 };
            else if ("rest".equals(kind)) p = new long[] { 0, 700 };
            else if ("end".equals(kind)) p = new long[] { 0, 250, 120, 250, 120, 250 };
            else p = new long[] { 0, 120 };
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createWaveform(p, -1));
            } else {
                v.vibrate(p, -1);
            }
        } catch (Exception ignored) {
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Lauf-Aufzeichnung", NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("Zeigt an, dass ein Lauf per GPS aufgezeichnet wird.");
        nm.createNotificationChannel(ch);
    }

    private Notification buildNotification() {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        b.setContentTitle("Lauf wird aufgezeichnet");
        b.setContentText(notificationText);
        b.setSmallIcon(android.R.drawable.ic_menu_mylocation);
        b.setOngoing(true);
        Intent open = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (open != null) {
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
            b.setContentIntent(PendingIntent.getActivity(this, 0, open, flags));
        }
        return b.build();
    }

    private void updateNotification(String text) {
        notificationText = text;
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(NOTIFICATION_ID, buildNotification());
        } catch (Exception ignored) {
        }
    }

    // ---- LocationListener ----

    @Override
    public void onLocationChanged(Location l) {
        double[] f = new double[] {
            l.getTime(),
            l.getLatitude(),
            l.getLongitude(),
            l.hasAccuracy() ? l.getAccuracy() : -1,
            l.hasSpeed() ? l.getSpeed() : -1
        };
        synchronized (LOCK) {
            if (FIXES.size() < MAX_FIXES) FIXES.add(f);
        }
        gpsOn = true;
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider)) gpsOn = true;
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider)) gpsOn = false;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
}
