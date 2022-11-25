package com.jetbrains.JBRperf;

enum Level {
    SEVERE,
    WARNING,
    INFO,
    CONFIG,
    FINE,
    FINER,
    FINEST
}
public class Logger {
    public void log(String message) {
        System.out.println(message);
    }
    public void logf(String format, Object ... args) {
        System.out.printf(format, args);
    }
    public void logTC(String message) {
        if (ScoresComparator.DO_TC_STATISTIC) log(message);
    }
    public void logTCf(String format, Object ... args) {
        if (ScoresComparator.DO_TC_STATISTIC) logf(format, args);
    }
    public void log(Level level, String message) {
        log(message);
    }
}
