package de.halbmarathon.runtracker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Schnittstelle zwischen der App (JavaScript) und dem Aufzeichnungsdienst.
 *
 * Die App holt die Messpunkte mit getFixes() ab. So gehen auch bei ausgeschaltetem Bildschirm
 * keine Punkte verloren, denn der Dienst sammelt weiter und die App liest später nach.
 * Die Berechtigungen laufen über checkPermissions() und requestPermissions() der Basisklasse.
 */
@CapacitorPlugin(
    name = "RunTracker",
    permissions = {
        @Permission(strings = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION }, alias = "location"),
        @Permission(strings = { "android.permission.POST_NOTIFICATIONS" }, alias = "notifications")
    }
)
public class RunTrackerPlugin extends Plugin {

    private static final int MAX_PER_CALL = 2000;

    @PluginMethod
    public void start(PluginCall call) {
        if (getPermissionState("location") != PermissionState.GRANTED) {
            call.reject("Der genaue Standort ist nicht erlaubt.", "PERMISSION");
            return;
        }
        LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        if (lm == null || !lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            call.reject("GPS ist ausgeschaltet.", "GPS_OFF");
            return;
        }
        if (RunTrackerService.running) {
            call.reject("Es läuft bereits eine Aufzeichnung.", "RUNNING");
            return;
        }
        RunTrackerService.prepare(call.getString("cues", "[]"), call.getString("meta", ""));
        Intent intent = new Intent(getContext(), RunTrackerService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                getContext().startForegroundService(intent);
            } else {
                getContext().startService(intent);
            }
        } catch (Exception e) {
            RunTrackerService.running = false;
            call.reject("Der Aufzeichnungsdienst konnte nicht gestartet werden.", "START_FAILED");
            return;
        }
        JSObject ret = new JSObject();
        ret.put("startedAt", RunTrackerService.startedAt);
        call.resolve(ret);
    }

    @PluginMethod
    public void pause(PluginCall call) {
        RunTrackerService.requestPause();
        JSObject ret = new JSObject();
        ret.put("t", System.currentTimeMillis());
        call.resolve(ret);
    }

    @PluginMethod
    public void resume(PluginCall call) {
        RunTrackerService.requestResume();
        JSObject ret = new JSObject();
        ret.put("t", System.currentTimeMillis());
        call.resolve(ret);
    }

    @PluginMethod
    public void stop(PluginCall call) {
        int count;
        synchronized (RunTrackerService.LOCK) {
            count = RunTrackerService.FIXES.size();
        }
        RunTrackerService.requestStop();
        JSObject ret = new JSObject();
        ret.put("count", count);
        call.resolve(ret);
    }

    /** Liefert Messpunkte ab Position "from". Nach stop() bleiben sie bis zum nächsten start() abrufbar. */
    @PluginMethod
    public void getFixes(PluginCall call) {
        int from = Math.max(0, call.getInt("from", 0));
        JSArray fixes = new JSArray();
        int next;
        synchronized (RunTrackerService.LOCK) {
            int size = RunTrackerService.FIXES.size();
            int to = Math.min(size, from + MAX_PER_CALL);
            for (int k = from; k < to; k++) {
                double[] f = RunTrackerService.FIXES.get(k);
                JSONArray row = new JSONArray();
                try {
                    for (double d : f) row.put(d);
                } catch (JSONException ignored) {
                }
                fixes.put(row);
            }
            next = Math.max(from, to);
        }
        JSObject ret = new JSObject();
        ret.put("fixes", fixes);
        ret.put("next", next);
        ret.put("running", RunTrackerService.running);
        ret.put("paused", RunTrackerService.paused);
        ret.put("gps", RunTrackerService.gpsOn);
        call.resolve(ret);
    }

    /** Zustand der Aufzeichnung, damit die App nach einem Neustart der Oberfläche wieder anknüpfen kann. */
    @PluginMethod
    public void getStatus(PluginCall call) {
        int count;
        synchronized (RunTrackerService.LOCK) {
            count = RunTrackerService.FIXES.size();
        }
        LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        JSObject ret = new JSObject();
        ret.put("running", RunTrackerService.running);
        ret.put("paused", RunTrackerService.paused);
        ret.put("gps", RunTrackerService.gpsOn);
        ret.put("gpsEnabled", gpsEnabled);
        ret.put("count", count);
        ret.put("startedAt", RunTrackerService.startedAt);
        ret.put("meta", RunTrackerService.meta);
        ret.put("cues", RunTrackerService.cuesJson);
        call.resolve(ret);
    }
}
