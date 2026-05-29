package com.aliiensmp.core.utils.sounds;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SoundUtilsTest {

    @Test
    void testValidSoundParsing() {
        CustomSound sound = SoundUtils.parse("ENTITY_EXPERIENCE_ORB_PICKUP:1.5:0.5");

        assertNotNull(sound);
        assertEquals("ENTITY_EXPERIENCE_ORB_PICKUP", sound.soundKey());
        assertEquals(1.5 , sound.volume());
        assertEquals(0.5 , sound.pitch());
    }

    @Property
    void parserNeverCrashesOnGarbageStrings(@ForAll String garbageInput) {
        assertDoesNotThrow(() -> SoundUtils.parse(garbageInput));
    }
}