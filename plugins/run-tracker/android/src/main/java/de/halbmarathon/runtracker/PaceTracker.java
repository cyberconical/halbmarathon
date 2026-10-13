package de.halbmarathon.runtracker;

/**
 * Counts distance while the app is in the background and reports every time a mark (for example every
 * 500 m) is crossed, with the time for the last piece and the total active time.
 *
 * This is the same distance logic as {@code analyze()} and {@code splits()} in the engine of index.html,
 * step by step, so the voice and the screen agree: fixes that are too inaccurate are dropped, outliers
 * (jumps) are skipped, the speed measured by the GPS chip (Doppler) is preferred, small movements are noise,
 * and test mode (a larger accepted error) takes the expected error off each step.
 * If you change one of the two, change the other one too.
 *
 * One exception, for trying out the voice: a step below 100 m (the 10 m step of test mode) counts the raw
 * movement between fixes. It ignores the GPS speed, the noise limit and the error subtraction, so a mark
 * is reached after a few steps, or just from GPS jitter, and the voice can be heard without running. The
 * distances it announces are then not real. It speaks at most every {@link #RAW_GAP_SEC} seconds, because
 * a new announcement cuts off the one before.
 *
 * Only plain Java, no Android classes, so it can be tested on a computer.
 * Fixes must come in order and only while the recording is running (not while paused). After a pause call
 * {@link #resumed()}.
 */
final class PaceTracker {

    /** One crossed mark. */
    static final class Mark {
        /** Distance of the mark in metres, for example 500, 1000, 1500. */
        final int meters;
        /** Seconds of active time for the piece since the previous mark. */
        final double splitSec;
        /** Seconds of active time since the start. */
        final double totalSec;

        Mark(int meters, double splitSec, double totalSec) {
            this.meters = meters;
            this.splitSec = splitSec;
            this.totalSec = totalSec;
        }
    }

    private static final double ER = 6371000.0;
    /** Shortest time between two announcements in the raw test mode, in active seconds. */
    static final double RAW_GAP_SEC = 12;

    private final double maxAcc;
    private final boolean loose;
    private final int stepM;
    private final boolean raw;
    private final long startMs;

    private long lastT = Long.MIN_VALUE;
    private boolean hasAnchor = false;
    private long anchorT;
    private double anchorLat, anchorLon, anchorAcc;
    private boolean hasLast = false;
    private long lastFixT;
    private double lastSp;
    private int drops = 0;
    private double dist = 0;

    private boolean hasPt = false;
    private double ptA, ptD;
    private int nextMark = 1;
    private boolean marked = false;
    private double lastSayA = 0;
    private double prevMarkA = 0;

    /**
     * @param maxAccM accepted error of a fix in metres (35, or more in test mode)
     * @param stepM   distance between two marks in metres (500 or 1000, 10 in test mode)
     * @param startMs start of the recording (wall clock, like the time of the fixes)
     */
    PaceTracker(double maxAccM, int stepM, long startMs) {
        this.maxAcc = maxAccM > 35 ? maxAccM : 35;
        this.loose = this.maxAcc > 35;
        this.stepM = stepM;
        this.raw = stepM < 100;
        this.startMs = startMs;
    }

    /** Call when a pause ends: the next fix starts a new piece of track, nothing is counted in between. */
    void resumed() {
        hasAnchor = false;
        hasLast = false;
    }

    /** Distance counted so far in metres. */
    double distance() {
        return dist;
    }

    private static double hav(double lat1, double lon1, double lat2, double lon2) {
        double rad = Math.PI / 180;
        double dLa = (lat2 - lat1) * rad, dLo = (lon2 - lon1) * rad;
        double s = Math.pow(Math.sin(dLa / 2), 2) + Math.cos(lat1 * rad) * Math.cos(lat2 * rad) * Math.pow(Math.sin(dLo / 2), 2);
        return 2 * ER * Math.asin(Math.min(1, Math.sqrt(s)));
    }

    /**
     * @param timeMs   time of the fix (wall clock)
     * @param acc      reported error in metres, -1 if unknown
     * @param speed    speed in m/s measured by the GPS chip, -1 if unknown
     * @param activeSec seconds of active time at this fix (without pauses)
     * @return the last mark crossed by this fix, or null
     */
    Mark onFix(long timeMs, double lat, double lon, double acc, double speed, double activeSec) {
        if (!(timeMs > lastT) || timeMs < startMs - 3000) return null;     // out of order or from before the start
        if (acc > maxAcc) return null;                                      // too inaccurate
        boolean noisy = loose && acc > 35 && !raw;
        double minStep = noisy ? acc : 3;                                   // with a big error, movement smaller than the error is noise
        lastT = timeMs;
        double a = Math.max(0, activeSec);
        if (!hasAnchor) {
            setAnchor(timeMs, lat, lon, acc);
            hasLast = true;
            lastFixT = timeMs;
            lastSp = speed;
            drops = 0;
            return addPoint(a, dist);
        }
        double step = hav(anchorLat, anchorLon, lat, lon);
        double dt = (timeMs - anchorT) / 1000.0;
        if (dt > 0 && step / dt > 9 && !(loose && step <= acc + anchorAcc) && drops < 5) {   // outlier (jump)
            drops++;
            return null;
        }
        drops = 0;
        boolean hasSp = !raw && speed >= 0 && lastSp >= 0;
        double gap = (timeMs - lastFixT) / 1000.0;
        if (hasSp && gap <= 5) {
            double v0 = lastSp < 0.5 ? 0 : lastSp, v1 = speed < 0.5 ? 0 : speed;
            dist += (v0 + v1) / 2 * gap;
            lastFixT = timeMs;
            lastSp = speed;
            if (step >= minStep || dt >= 8) {
                setAnchor(timeMs, lat, lon, acc);
                return addPoint(a, dist);
            }
            return null;
        }
        lastFixT = timeMs;
        lastSp = speed;
        if (step < minStep && (dt < 8 || noisy)) return null;               // standing still or noise
        dist += noisy ? Math.max(0, step - acc) : step;
        setAnchor(timeMs, lat, lon, acc);
        return addPoint(a, dist);
    }

    private void setAnchor(long t, double lat, double lon, double acc) {
        hasAnchor = true;
        anchorT = t;
        anchorLat = lat;
        anchorLon = lon;
        anchorAcc = acc;
    }

    /** A new point of the cleaned track. Looks for marks between the previous point and this one. */
    private Mark addPoint(double a, double d) {
        Mark last = null;
        if (hasPt) {
            double span = d - ptD;
            while ((double) nextMark * stepM <= d) {
                if (raw && marked && a - lastSayA < RAW_GAP_SEC) break;    // too soon after the last one, wait
                double mark = (double) nextMark * stepM;
                double f = (mark - ptD) / (span == 0 ? 1 : span);
                double aCross = ptA + (a - ptA) * f;
                last = new Mark(nextMark * stepM, aCross - prevMarkA, aCross);
                prevMarkA = aCross;
                marked = true;
                lastSayA = a;
                nextMark = raw ? (int) (d / stepM) + 1 : nextMark + 1;      // raw: skip the marks that were passed
            }
        }
        hasPt = true;
        ptA = a;
        ptD = d;
        return last;
    }
}
