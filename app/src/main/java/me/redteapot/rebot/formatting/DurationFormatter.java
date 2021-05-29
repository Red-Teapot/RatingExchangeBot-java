package me.redteapot.rebot.formatting;

import java.time.Duration;

public class DurationFormatter implements TypeFormatter<Duration> {
    private final static long MINUTE = 60;
    private final static long HOUR = MINUTE * 60;
    private final static long DAY = HOUR * 24;

    @Override
    public String format(Duration object) {
        StringBuilder result = new StringBuilder();

        long seconds = object.getSeconds();
        if (seconds == 0) {
            return "0s";
        }

        long days = seconds >= DAY ? seconds / DAY : 0;
        seconds -= days * DAY;
        long hours = seconds >= HOUR ? seconds / HOUR : 0;
        seconds -= hours * HOUR;
        long minutes = seconds >= MINUTE ? seconds / MINUTE : 0;
        seconds -= minutes * MINUTE;

        if (days > 0) {
            result.append(days).append("d ");
        }
        if (hours > 0) {
            result.append(hours).append("h ");
        }
        if (minutes > 0) {
            result.append(minutes).append("m ");
        }
        if (seconds > 0) {
            result.append(seconds).append("s ");
        }

        return result.toString().trim();
    }
}
