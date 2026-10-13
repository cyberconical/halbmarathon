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
 * Foreground service that records a run.
 *
 * Uses only the Android LocationManager with the GPS provider. No Google services are used.
 * Fixes live only in this process's memory and are never written to storage.
 */
public class RunTrackerService extends Service implements LocationListener {

    static final String CHANNEL_ID = "run_tracking";
    static final int NOTIFICATION_ID = 4711;
    static final int MAX_FIXES = 30000;

    // State shared with the plugin (same process).
    static final Object LOCK = new Object();
    static final ArrayList<double[]> FIXES = new ArrayList<>(); // [time ms, lat, lon, accuracy m, speed m/s]
    static volatile boolean running = false;
    static volatile boolean paused = false;
    static volatile boolean gpsOn = true;
    static volatile long startedAt = 0;
    static volatile String cuesJson = "[]";
    static volatile String meta = "";
    // Small UI state (pauses, laps), so the UI can reattach after a restart.
    // Contains no places and, like the fixes, lives only in memory.
    static volatile String appState = "";
    // Language of the app ("en" or "de") for the notification texts.
    static volatile String lang = "en";
    // Voice announcements: distance between two announcements in metres (0 = off) and the error up to which a
    // fix counts (35, or more in test mode). The spoken text is always English.
    static volatile int voiceStepM = 0;
    static volatile double voiceMaxAcc = 35;
    // "off", or the state of the speech engine: init, ready, no_english, unavailable (see Announcer).
    static volatile String voiceStatus = "off";
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
    private String notificationText = "";
    private Announcer announcer;
    private PaceTracker pace;
    private int announceCount = 0;
    private double lastActiveSec = 0;

    static String txt(String en, String de) {
        return "de".equals(lang) ? de : en;
    }

    /** Called by the plugin before the service starts. */
    static void prepare(String cues, String metaData, String language, int voiceStep, double maxAccM) {
        synchronized (LOCK) {
            FIXES.clear();
        }
        cuesJson = cues == null ? "[]" : cues;
        meta = metaData == null ? "" : metaData;
        startedAt = System.currentTimeMillis();
        appState = "";
        lang = "de".equals(language) ? "de" : "en";
        voiceStepM = (voiceStep == 500 || voiceStep == 1000) ? voiceStep : 0;
        voiceMaxAcc = (maxAccM >= 35 && maxAccM <= 200) ? maxAccM : 35;
        voiceStatus = voiceStepM > 0 ? Announcer.INIT : "off";
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
    public void onCreate() {
        super.onCreate();
        RunLog.init(this);
        RunLog.installCrashHandler();
        RunLog.add("svc:create");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        RunLog.add("svc:cmd running=" + (running ? 1 : 0) + (intent == null ? " nullIntent" : ""));
        // If the process was ended there is nothing left to record, so do not restart.
        if (intent == null || !running) {
            leaveQuickly();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (instance == null) {
            try {
                startTracking();
            } catch (Throwable t) {
                // A failure while setting up must never take the app down.
                RunLog.add("svc:start-error " + RunLog.describe(t));
                fail();
            }
        }
        return START_NOT_STICKY;
    }

    /**
     * Whoever calls startForegroundService() must call startForeground() shortly after, otherwise
     * Android ends the app. That also applies when the service stops right away.
     */
    private void leaveQuickly() {
        try {
            createChannel();
            Notification n = buildNotification();
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(NOTIFICATION_ID, n);
            }
            if (Build.VERSION.SDK_INT >= 24) {
                stopForeground(Service.STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
        } catch (Throwable ignored) {
        }
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
            RunLog.add("svc:fg fail " + RunLog.describe(e));
            fail();
            return;
        }
        RunLog.add("svc:fg ok cues=" + cues.size());
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this, Looper.getMainLooper());
            gpsOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            RunLog.add("svc:gps ok enabled=" + (gpsOn ? 1 : 0));
        } catch (SecurityException | IllegalArgumentException e) {
            RunLog.add("svc:gps fail " + RunLog.describe(e));
            fail();
            return;
        }
        // Only intervals with cues need the CPU awake, so the cues come on time.
        // If this fails the recording continues; only the cues may be late.
        if (!cues.isEmpty()) {
            try {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "halbmarathon:lauf");
                    wakeLock.acquire(4 * 60 * 60 * 1000L);
                    RunLog.add("svc:wakelock ok");
                }
            } catch (Throwable t) {
                wakeLock = null;
                RunLog.add("svc:wakelock fail " + RunLog.describe(t));
            }
        }
        scheduleCues();
        startVoice();
    }

    /** Voice announcements are optional. If anything goes wrong here the recording continues without them. */
    private void startVoice() {
        announceCount = 0;
        lastActiveSec = 0;
        if (voiceStepM <= 0) return;
        try {
            pace = new PaceTracker(voiceMaxAcc, voiceStepM, startedAt);
            announcer = new Announcer(this, false, new Announcer.StatusListener() {
                @Override
                public void onStatus(String status) {
                    voiceStatus = status;
                }
            });
            RunLog.add("svc:voice step=" + voiceStepM + " maxacc=" + Math.round(voiceMaxAcc));
        } catch (Throwable t) {
            pace = null;
            announcer = null;
            voiceStatus = Announcer.UNAVAILABLE;
            RunLog.add("svc:voice fail " + RunLog.describe(t));
        }
    }

    private void stopVoice() {
        pace = null;
        if (announcer != null) {
            announcer.shutdown();
            announcer = null;
        }
        voiceStatus = "off";
    }

    /** Feeds one fix to the distance counter and speaks when a mark is crossed. Only while recording, not while paused. */
    private void announceFix(Location l) {
        long age = Math.max(0, SystemClock.elapsedRealtime() - l.getElapsedRealtimeNanos() / 1000000L);
        double act = Math.max(lastActiveSec, (activeNowMs() - age) / 1000.0);
        lastActiveSec = act;
        PaceTracker.Mark m = pace.onFix(
            l.getTime(), l.getLatitude(), l.getLongitude(),
            l.hasAccuracy() ? l.getAccuracy() : -1,
            l.hasSpeed() ? l.getSpeed() : -1,
            act);
        if (m == null) return;
        announceCount++;
        announcer.speak(PaceSpeech.text(m.meters, m.splitSec, voiceStepM, m.totalSec, announceCount == 1));
        RunLog.add("voice:say n=" + announceCount);
    }

    private void fail() {
        RunLog.add("svc:fail");
        running = false;
        instance = null;
        stopSelf();
    }

    private void pauseTracking() {
        if (!running || paused) return;
        activeBaseMs += SystemClock.elapsedRealtime() - lastResumeRt;
        paused = true;
        cancelCues();
        RunLog.add("svc:pause");
    }

    private void resumeTracking() {
        if (!running || !paused) return;
        paused = false;
        lastResumeRt = SystemClock.elapsedRealtime();
        if (pace != null) pace.resumed();      // nothing is counted for the time of the pause
        scheduleCues();
        RunLog.add("svc:resume");
    }

    private void stopTracking() {
        RunLog.add("svc:stop fixes=" + fixCount());
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
        stopVoice();
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
        // If "running" is still 1 here, the system ended the service, not the person.
        RunLog.add("svc:destroy running=" + (running ? 1 : 0) + " fixes=" + fixCount());
        if (handler != null) cancelCues();
        releaseResources();
        running = false;
        paused = false;
        instance = null;
        super.onDestroy();
    }

    /** The person swiped the app away from recents. The recording continues. */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        RunLog.add("svc:taskRemoved");
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onTrimMemory(int level) {
        RunLog.add("svc:trim level=" + level);
        super.onTrimMemory(level);
    }

    @Override
    public void onLowMemory() {
        RunLog.add("svc:lowMemory");
        super.onLowMemory();
    }

    private static int fixCount() {
        synchronized (LOCK) {
            return FIXES.size();
        }
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
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, txt("Run recording", "Lauf-Aufzeichnung"), NotificationManager.IMPORTANCE_LOW);
        ch.setDescription(txt("Shows that a run is being recorded with GPS.", "Zeigt an, dass ein Lauf per GPS aufgezeichnet wird."));
        nm.createNotificationChannel(ch);
    }

    private Notification buildNotification() {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        b.setContentTitle(txt("Recording your run", "Lauf wird aufgezeichnet"));
        b.setContentText(notificationText.length() > 0 ? notificationText : txt("GPS active", "GPS aktiv"));
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
        int n;
        synchronized (LOCK) {
            if (FIXES.size() < MAX_FIXES) FIXES.add(f);
            n = FIXES.size();
        }
        // Only counters and waiting times go into the log, never the measurements themselves.
        if (n == 1) RunLog.add("fix:first after=" + ((System.currentTimeMillis() - startedAt) / 1000) + "s");
        else if (n % 500 == 0) RunLog.add("fix:n=" + n);
        if (!gpsOn) RunLog.add("gps:back");
        gpsOn = true;
        if (pace != null && announcer != null && !paused) {
            try {
                announceFix(l);
            } catch (Throwable t) {
                // A problem with the voice must never stop the recording.
                RunLog.add("voice:error " + RunLog.describe(t));
                stopVoice();
            }
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider)) gpsOn = true;
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider)) {
            if (gpsOn) RunLog.add("gps:off");
            gpsOn = false;
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
}
