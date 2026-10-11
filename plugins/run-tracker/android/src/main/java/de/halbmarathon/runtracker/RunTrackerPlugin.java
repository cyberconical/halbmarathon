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
 * Bridge between the app (JavaScript) and the recording service.
 *
 * The app fetches fixes with getFixes(). So no points are lost while the screen is off: the service
 * keeps collecting and the app catches up later.
 * Permissions use checkPermissions() and requestPermissions() of the base class.
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

    @Override
    public void load() {
        RunLog.init(getContext());
        RunLog.installCrashHandler();
        RunLog.add("plugin:load");
    }

    @PluginMethod
    public void start(PluginCall call) {
        if (getPermissionState("location") != PermissionState.GRANTED) {
            RunLog.add("start:reject PERMISSION");
            call.reject("Precise location is not allowed.", "PERMISSION");
            return;
        }
        LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        if (lm == null || !lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            RunLog.add("start:reject GPS_OFF");
            call.reject("GPS is turned off.", "GPS_OFF");
            return;
        }
        if (RunTrackerService.running) {
            RunLog.add("start:reject RUNNING");
            call.reject("A recording is already running.", "RUNNING");
            return;
        }
        RunTrackerService.prepare(call.getString("cues", "[]"), call.getString("meta", ""), call.getString("lang", "en"));
        Intent intent = new Intent(getContext(), RunTrackerService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                getContext().startForegroundService(intent);
            } else {
                getContext().startService(intent);
            }
        } catch (Exception e) {
            RunTrackerService.running = false;
            RunLog.add("start:fail " + RunLog.describe(e));
            call.reject("The recording service could not be started.", "START_FAILED");
            return;
        }
        RunLog.add("start:ok");
        JSObject ret = new JSObject();
        ret.put("startedAt", RunTrackerService.startedAt);
        call.resolve(ret);
    }

    /** The UI stores its small state here (pauses, laps), see getStatus(). */
    @PluginMethod
    public void saveState(PluginCall call) {
        String s = call.getString("state", "");
        RunTrackerService.appState = s == null ? "" : s;
        call.resolve();
    }

    /** Write a UI event to the anonymous log. RunLog only lets harmless characters through. */
    @PluginMethod
    public void diagLog(PluginCall call) {
        String e = call.getString("e", "");
        if (e != null && e.length() > 0) RunLog.add("js:" + e);
        call.resolve();
    }

    @PluginMethod
    public void getLog(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("text", RunLog.read());
        ret.put("enabled", RunLog.isEnabled());
        call.resolve(ret);
    }

    @PluginMethod
    public void clearLog(PluginCall call) {
        RunLog.clear();
        call.resolve();
    }

    @PluginMethod
    public void setLogEnabled(PluginCall call) {
        Boolean on = call.getBoolean("on", true);
        RunLog.setEnabled(on == null || on);
        call.resolve();
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

    /** Returns fixes from position "from". After stop() they stay available until the next start(). */
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

    /** State of the recording, so the app can reattach after its UI was restarted. */
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
        ret.put("state", RunTrackerService.appState);
        call.resolve(ret);
    }
}
