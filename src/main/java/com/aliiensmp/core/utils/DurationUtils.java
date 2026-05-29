package com.aliiensmp.core.utils;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationUtils {

    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([dhms])");

    /**
     * Converts a String input of a time (such as, for example, "3d 12h 7m 4s") into a Duration object
     *
     * @param input the String input to convert
     * @return the input converted
     */
    public static Duration parse(String input) {
        if (input == null || input.isBlank()) {
            return Duration.ZERO;
        }

        // Clean up the input string (lowercase and remove spaces)
        String cleanInput = input.toLowerCase().replaceAll("\\s+", "");
        Matcher matcher = DURATION_PATTERN.matcher(cleanInput);

        Duration totalDuration = Duration.ZERO;
        boolean foundMatches = false;

        while (matcher.find()) {
            foundMatches = true;
            long amount = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "d" -> totalDuration = totalDuration.plusDays(amount);
                case "h" -> totalDuration = totalDuration.plusHours(amount);
                case "m" -> totalDuration = totalDuration.plusMinutes(amount);
                case "s" -> totalDuration = totalDuration.plusSeconds(amount);
            }
        }

        return foundMatches ? totalDuration : Duration.ZERO;
    }

    /**
     * Enum to choose what type of style you want when formatting a duration into a
     * String using the {@code format(Duration duration)} method.
     *
     * Examples with 2 hours and 30 minutes:
     * SHORT - 2h 30m
     * LONG - 2 Hours 30 Minutes
     * CLOCK - 02:30
     */
    public enum Style {
        SHORT, LONG, CLOCK
    }

    /**
     * Turns a duration object into a String input (such as, for example, "3d 12h 7m 4s")
     * This method uses the {@code Style.SHORT} style. You can set whatever style you want
     * by using the method {@code format(Duration duration, Style style)}
     *
     * @param duration the duration to be converted
     * @return a duration object, or "Permanent" if the duration is null, zero, or negative.
     */
    public static String format(Duration duration) {
        return format(duration, Style.SHORT);
    }

    /**
     * Turns a duration object into a String input (such as, for example, "3d 12h 7m 4s")
     *
     * @param duration the duration to be converted
     * @param style the format of the output
     * @return a duration object, or "Permanent" if the duration is null, zero, or negative.
     */
    public static String format(Duration duration, Style style) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return "Permanent";
        }

        long days = duration.toDays();
        int hours = duration.toHoursPart();
        int minutes = duration.toMinutesPart();
        int seconds = duration.toSecondsPart();

        StringBuilder builder = new StringBuilder();

        switch (style) {
            case SHORT -> {
                if (days > 0) builder.append(days).append("d ");
                if (hours > 0) builder.append(hours).append("h ");
                if (minutes > 0) builder.append(minutes).append("m ");
                if (seconds > 0) builder.append(seconds).append("s ");
            }
            case LONG -> {
                if (days > 0) builder.append(days).append(days == 1 ? " Day " : " Days ");
                if (hours > 0) builder.append(hours).append(hours == 1 ? " Hour " : " Hours ");
                if (minutes > 0) builder.append(minutes).append(minutes == 1 ? " Minute " : " Minutes ");
                if (seconds > 0) builder.append(seconds).append(seconds == 1 ? " Second " : " Seconds ");
            }
            case CLOCK -> {
                if (days > 0) builder.append(clockTimeConvert(days, String.valueOf(builder)));
                if (hours > 0) builder.append(clockTimeConvert(hours, String.valueOf(builder)));
                if (minutes > 0) builder.append(clockTimeConvert(minutes, String.valueOf(builder)));
                if (seconds > 0) builder.append(clockTimeConvert(seconds, String.valueOf(builder)));
            }
        }

        return builder.toString().trim();
    }

    /**
     * Convert a time into a string that can be used under the {@code CLOCK} format
     * If the number of the time unit is greater than 9 it will just append it normally,
     * otherwise it will appeand a 0 to the left of the number
     *
     * @param time Time to be converted
     * @return the converted time with the conditions mentioned above
     * @requires {@code time >= 0}
     */
    private static String clockTimeConvert(long time, String currentFormat) {
        StringBuilder sb = new StringBuilder();

        if (!currentFormat.isEmpty()) sb.append(":");
        sb.append("%02d".formatted(time));

        return sb.toString();
    }

    /**
     * Converts a Duration object into Minecraft server ticks.
     * (20 ticks = 1 second)
     *
     * @param duration the duration to be converted
     * @return the total amount of ticks, or 0 if the duration is null/negative.
     */
    public static long toTicks(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return 0L;
        }

        return duration.getSeconds() * 20;
    }

    /**
     * Parses a String input (e.g., "1h 30m") directly into Minecraft server ticks.
     *
     * @param input the string format to parse
     * @return the total amount of ticks.
     */
    public static long toTicks(String input) {
        return toTicks(parse(input));
    }
}