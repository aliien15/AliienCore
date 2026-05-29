package com.aliiensmp.core.utils;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.Positive;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void formattedShortStyleShouldAlwaysParseBackToOriginal(@ForAll @Positive @LongRange(max = 31536000L) long seconds) {
        Duration original = Duration.ofSeconds(seconds);
        String formatted = DurationUtils.format(original, DurationUtils.Style.SHORT);
        Duration parsed = DurationUtils.parse(formatted);

        assertEquals(original, parsed);
    }

    @Property
    void ticksAlwaysMultiplyBy20(@ForAll @Positive long seconds) {
        Duration randomDuration = Duration.ofSeconds(seconds);

        long expectedTicks = seconds * 20;
        assertEquals(expectedTicks, DurationUtils.toTicks(randomDuration));
    }

    // Testing up to 99 hours
    @Property
    void clockStyleAlwaysFollowsFormat(@ForAll @Positive @LongRange(max = 359999L) long seconds) {
        Duration randomDuration = Duration.ofSeconds(seconds);
        String clockFormat = DurationUtils.format(randomDuration, DurationUtils.Style.CLOCK);

        assertTrue(clockFormat.matches("^[0-9:]+$"));
    }
}