package de.halbmarathon.runtracker;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Anonymes Diagnose-Protokoll.
 *
 * Es hält nur technische Ereignisse fest ("Dienst gestartet", "Wake-Lock fehlgeschlagen",
 * "Android hat den Prozess beendet: Speicher"). Nie enthalten sind Orte, Strecken, Uhrzeiten,
 * Datum, Namen oder Gerätekennungen. Die Zeit ist nur die Sekunden seit dem Start des Prozesses.
 * Die Datei liegt im privaten Speicher der App und verlässt das Gerät nur, wenn die Person sie teilt.
 *
 * Damit nichts Persönliches hineingelangt, lässt add() nur eine kleine Zeichenmenge zu und
 * ersetzt alles, was wie eine Koordinate aussieht.
 */
final class RunLog {

    private static final String FILE = "diagnose.log";
    private static final String PREFS = "halbmarathon_diag";
    private static final int MAX_LINES = 200;
    private static final int KEEP_LINES = 150;
    private static final int MAX_LEN = 100;
    private static final Object LOCK = new Object();
    private static final long PROC_START = SystemClock.elapsedRealtime();

    private static Context appCtx;
    private static volatile boolean enabled = true;
    private static boolean started = false;
    private static boolean crashHandlerInstalled = false;
    private static int lines = -1;

    private RunLog() {}

    /** Idempotent. Beim ersten Aufruf je Prozess wird vermerkt, warum der vorherige Prozess endete. */
    static void init(Context c) {
        if (c == null) return;
        boolean first;
        synchronized (LOCK) {
            if (appCtx == null) appCtx = c.getApplicationContext();
            first = !started;
            started = true;
        }
        try {
            enabled = prefs().getBoolean("on", true);
        } catch (Throwable ignored) {
        }
        if (first) {
            add("proc:start api=" + Build.VERSION.SDK_INT);
            logPreviousExit();
        }
    }

    static boolean isEnabled() {
        return enabled;
    }

    static void setEnabled(boolean on) {
        enabled = on;
        try {
            prefs().edit().putBoolean("on", on).apply();
        } catch (Throwable ignored) {
        }
        if (!on) clear();
    }

    static void add(String code) {
        if (!enabled || appCtx == null || code == null) return;
        long s = (SystemClock.elapsedRealtime() - PROC_START) / 1000;
        String line = "+" + s + "s " + sanitize(code) + "\n";
        synchronized (LOCK) {
            try {
                File f = file();
                if (lines < 0) lines = countLines(f);
                if (lines >= MAX_LINES) trim(f);
                FileOutputStream o = new FileOutputStream(f, true);
                try {
                    o.write(line.getBytes("UTF-8"));
                } finally {
                    o.close();
                }
                lines++;
            } catch (Throwable ignored) {
            }
        }
    }

    static String read() {
        if (appCtx == null) return "";
        synchronized (LOCK) {
            try {
                File f = file();
                if (!f.exists()) return "";
                return new String(readAll(f), "UTF-8");
            } catch (Throwable t) {
                return "";
            }
        }
    }

    static void clear() {
        if (appCtx == null) return;
        synchronized (LOCK) {
            try {
                file().delete();
            } catch (Throwable ignored) {
            }
            lines = 0;
        }
    }

    /**
     * Schreibt bei einem Absturz Klasse und Fundstelle der Ausnahme ins Protokoll, ohne die Meldung.
     * Danach läuft der bisherige Handler weiter, das Verhalten der App ändert sich nicht.
     */
    static void installCrashHandler() {
        synchronized (LOCK) {
            if (crashHandlerInstalled) return;
            crashHandlerInstalled = true;
        }
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                try {
                    add("crash:" + describe(e));
                } catch (Throwable ignored) {
                }
                if (prev != null) prev.uncaughtException(t, e);
            }
        });
    }

    /** Kurzbeschreibung einer Ausnahme: Klassenname und Codestelle, nie die Meldung. */
    static String describe(Throwable e) {
        if (e == null) return "null";
        StringBuilder b = new StringBuilder(e.getClass().getSimpleName());
        StackTraceElement[] st = e.getStackTrace();
        if (st != null && st.length > 0) {
            b.append(" top=").append(frame(st[0]));
            for (StackTraceElement el : st) {
                if (el.getClassName().startsWith("de.halbmarathon.")) {
                    b.append(" app=").append(frame(el));
                    break;
                }
            }
        }
        Throwable c = e.getCause();
        if (c != null && c != e) b.append(" cause=").append(c.getClass().getSimpleName());
        return b.toString();
    }

    private static String frame(StackTraceElement el) {
        String cn = el.getClassName();
        int dot = cn.lastIndexOf('.');
        return (dot >= 0 ? cn.substring(dot + 1) : cn) + "." + el.getMethodName() + ":" + el.getLineNumber();
    }

    private static String sanitize(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length() && b.length() < MAX_LEN; i++) {
            char ch = s.charAt(i);
            boolean ok = (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')
                || ch == ' ' || ch == '_' || ch == ':' || ch == '.' || ch == '=' || ch == '+' || ch == '-'
                || ch == '@' || ch == '$' || ch == '(' || ch == ')';
            b.append(ok ? ch : '?');
        }
        // Alles, was wie eine Koordinate aussieht (Zahl mit drei oder mehr Nachkommastellen), fällt weg.
        return b.toString().replaceAll("[0-9]+\\.[0-9]{3,}", "#");
    }

    private static void logPreviousExit() {
        if (Build.VERSION.SDK_INT < 30 || appCtx == null) return;
        try {
            ActivityManager am = (ActivityManager) appCtx.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return;
            SharedPreferences p = prefs();
            long seen = p.getLong("exitSeen", 0);
            long newest = seen;
            List<ApplicationExitInfo> list = am.getHistoricalProcessExitReasons(appCtx.getPackageName(), 0, 5);
            for (int i = list.size() - 1; i >= 0; i--) { // alt nach neu
                ApplicationExitInfo e = list.get(i);
                long ts = e.getTimestamp();
                if (ts <= seen) continue;
                long agoMin = Math.max(0, (System.currentTimeMillis() - ts) / 60000);
                add("exit:" + reasonName(e.getReason()) + " imp=" + importanceName(e.getImportance()) + " vor=" + agoMin + "min");
                if (ts > newest) newest = ts;
            }
            if (newest > seen) p.edit().putLong("exitSeen", newest).apply();
        } catch (Throwable ignored) {
        }
    }

    private static String reasonName(int r) {
        switch (r) {
            case 1: return "EXIT_SELF";
            case 2: return "SIGNALED";
            case 3: return "LOW_MEMORY";
            case 4: return "CRASH";
            case 5: return "CRASH_NATIVE";
            case 6: return "ANR";
            case 7: return "INIT_FAILURE";
            case 8: return "PERMISSION_CHANGE";
            case 9: return "EXCESSIVE_RESOURCE";
            case 10: return "USER_REQUESTED";
            case 11: return "USER_STOPPED";
            case 12: return "DEPENDENCY_DIED";
            case 13: return "OTHER";
            case 14: return "FREEZER";
            case 15: return "PACKAGE_STATE_CHANGE";
            case 16: return "PACKAGE_UPDATED";
            default: return "UNKNOWN(" + r + ")";
        }
    }

    private static String importanceName(int i) {
        if (i <= 100) return "foreground";
        if (i <= 125) return "fgservice";
        if (i <= 200) return "visible";
        if (i <= 230) return "perceptible";
        if (i <= 300) return "service";
        if (i <= 400) return "cached";
        return String.valueOf(i);
    }

    private static SharedPreferences prefs() {
        return appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static File file() {
        return new File(appCtx.getFilesDir(), FILE);
    }

    private static int countLines(File f) {
        if (!f.exists()) return 0;
        try {
            byte[] b = readAll(f);
            int n = 0;
            for (byte x : b) if (x == '\n') n++;
            return n;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static void trim(File f) {
        try {
            String[] all = new String(readAll(f), "UTF-8").split("\n");
            List<String> keep = new ArrayList<>();
            for (int i = Math.max(0, all.length - KEEP_LINES); i < all.length; i++) {
                if (all[i].length() > 0) keep.add(all[i]);
            }
            StringBuilder sb = new StringBuilder();
            for (String l : keep) sb.append(l).append('\n');
            FileOutputStream o = new FileOutputStream(f, false);
            try {
                o.write(sb.toString().getBytes("UTF-8"));
            } finally {
                o.close();
            }
            lines = keep.size();
        } catch (Throwable t) {
            f.delete();
            lines = 0;
        }
    }

    private static byte[] readAll(File f) throws java.io.IOException {
        FileInputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
