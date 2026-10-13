package de.halbmarathon.runtracker;

import java.util.Locale;

/**
 * The words that are spoken at a mark. Always English, whatever language the app or the phone uses.
 * Only plain Java, so it can be tested on a computer.
 *
 * Paces are written out in words ("six twelve") so every speech engine reads them the same way. A digit
 * string such as "6:12" is read as a time of day by some engines.
 *
 * Examples:
 *   first mark:   "500 metres. Pace six twelve per kilometre. Average six twelve."
 *   later marks:  "1 kilometre. Pace six ten. Average six eleven."
 *                 "1.5 kilometres. Pace six oh five. Average six ten."
 */
final class PaceSpeech {

    private PaceSpeech() {}

    private static final String[] WORDS = {
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
        "twenty", "twenty one", "twenty two", "twenty three", "twenty four", "twenty five", "twenty six", "twenty seven", "twenty eight", "twenty nine",
        "thirty", "thirty one", "thirty two", "thirty three", "thirty four", "thirty five", "thirty six", "thirty seven", "thirty eight", "thirty nine",
        "forty", "forty one", "forty two", "forty three", "forty four", "forty five", "forty six", "forty seven", "forty eight", "forty nine",
        "fifty", "fifty one", "fifty two", "fifty three", "fifty four", "fifty five", "fifty six", "fifty seven", "fifty eight", "fifty nine"
    };

    /** Pace in seconds per kilometre, rounded to whole seconds, or -1 if it makes no sense (not moving). */
    static int paceSec(double seconds, double meters) {
        if (!(seconds > 0) || !(meters > 0)) return -1;
        long p = Math.round(seconds / (meters / 1000.0));
        return p >= 10 && p < 3600 ? (int) p : -1;                       // faster than 0:10 or slower than 60:00 per km is not a pace
    }

    /** "six twelve", "six oh five", "six minutes". Input 10 to 3599 seconds per kilometre. */
    static String paceWords(int sec) {
        int m = sec / 60, s = sec % 60;
        if (s == 0) return WORDS[m] + (m == 1 ? " minute" : " minutes");
        if (s < 10) return WORDS[m] + " oh " + WORDS[s];
        return WORDS[m] + " " + WORDS[s];
    }

    /** "500 metres", "1 kilometre", "1.5 kilometres" */
    static String distanceWords(int meters) {
        if (meters < 1000) return meters + " metres";
        if (meters % 1000 == 0) {
            int km = meters / 1000;
            return km + (km == 1 ? " kilometre" : " kilometres");
        }
        return String.format(Locale.US, "%.1f", meters / 1000.0) + " kilometres";
    }

    /**
     * @param splitSec  active seconds for the piece since the previous mark
     * @param stepM     length of that piece in metres
     * @param totalSec  active seconds since the start
     * @param first     true for the first announcement of a run: it adds the unit "per kilometre" once
     */
    static String text(int meters, double splitSec, int stepM, double totalSec, boolean first) {
        int split = paceSec(splitSec, stepM);
        int avg = paceSec(totalSec, meters);
        StringBuilder b = new StringBuilder(distanceWords(meters)).append('.');
        if (split > 0) b.append(" Pace ").append(paceWords(split)).append(first ? " per kilometre" : "").append('.');
        if (avg > 0) b.append(" Average ").append(paceWords(avg)).append(first && split <= 0 ? " per kilometre" : "").append('.');
        return b.toString();
    }

    /** What the "Test voice" button says. */
    static String sample() {
        return "500 metres. Pace six twelve per kilometre. Average six twenty five.";
    }
}
