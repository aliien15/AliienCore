package com.aliiensmp.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.Positive;

import org.junit.jupiter.api.Test;

import java.time.Duration;

class DurationUtilsTest {

    @Test
    void testSafetyBarriers() {
        assertEquals(Duration.ZERO, DurationUtils.parse(null));
        assertEquals(Duration.ZERO, DurationUtils.parse("   "));
        assertEquals("Permanent", DurationUtils.format(Duration.ZERO));
        assertEquals("Permanent", DurationUtils.format(null));

        assertEquals(Duration.ofDays(1).plusSeconds(15), DurationUtils.parse(" 1d   15s "));
    }

    // Testing up to 1 year
    @Property
    void formattedShortStyleShouldAlwaysParseBackToOriginal(
            @ForAll @Positive @LongRange(max = 31536000L) long seconds) {
        Duration original = Duration.ofSeconds(seconds);
        String formatted = DurationUtils.format(original, DurationUtils.Style.SHORT);
        Duration parsed = DurationUtils.parse(formatted);

        assertEquals(original, parsed);
    }

    @Property
    void ticksAlwaysMultiplyBy20(
            @ForAll @LongRange(min = 0, max = 461168601842738790L) long seconds) {
        Duration randomDuration = Duration.ofSeconds(seconds);

        long expectedTicks = seconds * 20;
        assertEquals(expectedTicks, DurationUtils.toTicks(randomDuration));
    }

    @Test
    void clockStyleKeepsZeroPlaceholdersAfterLargestUnit() {
        assertEquals(
                "04:12",
                DurationUtils.format(
                        Duration.ofMinutes(4).plusSeconds(12), DurationUtils.Style.CLOCK));
        assertEquals(
                "02:30:00",
                DurationUtils.format(
                        Duration.ofHours(2).plusMinutes(30), DurationUtils.Style.CLOCK));
        assertEquals(
                "02:00:30:00",
                DurationUtils.format(
                        Duration.ofDays(2).plusMinutes(30), DurationUtils.Style.CLOCK));
    }

    // Testing up to 99 hours
    @Property
    void clockStyleAlwaysFollowsFormat(@ForAll @Positive @LongRange(max = 359999L) long seconds) {
        Duration randomDuration = Duration.ofSeconds(seconds);
        String clockFormat = DurationUtils.format(randomDuration, DurationUtils.Style.CLOCK);

        assertTrue(clockFormat.matches("^[0-9:]+$"));
    }

    @Property
    void clockStylePreservesEverySecond(@ForAll @LongRange(min = 1, max = 31536000L) long seconds) {
        String[] units =
                DurationUtils.format(Duration.ofSeconds(seconds), DurationUtils.Style.CLOCK)
                        .split(":");
        long reconstructed = 0;
        long[] multipliers = {1, 60, 3600, 86400};
        for (int i = 0; i < units.length; i++) {
            reconstructed += Long.parseLong(units[units.length - 1 - i]) * multipliers[i];
        }
        assertEquals(seconds, reconstructed);
    }

    @Test
    void longStyleUsesSingularAndPluralUnitsAndNonpositiveDurationsArePermanent() {
        assertEquals(
                "1 Day 1 Hour 1 Minute 1 Second",
                DurationUtils.format(Duration.ofSeconds(90061), DurationUtils.Style.LONG));
        assertEquals(
                "2 Days 2 Hours 2 Minutes 2 Seconds",
                DurationUtils.format(Duration.ofSeconds(180122), DurationUtils.Style.LONG));
        assertEquals("Permanent", DurationUtils.format(Duration.ofSeconds(-1)));
        assertEquals(0, DurationUtils.toTicks((Duration) null));
        assertEquals(0, DurationUtils.toTicks(Duration.ofSeconds(-1)));
        assertEquals(1200, DurationUtils.toTicks("1m"));
    }
}
