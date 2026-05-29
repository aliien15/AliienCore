package com.aliiensmp.core.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorUtilsTest {

    @Test
    void testStandardColorConversion() {
        assertEquals(ColorUtils.color((String) null), Component.empty());
        assertEquals(ColorUtils.color((List<String>) null), List.of());

        Component component = ColorUtils.color("&cHello");
        String serializedBack = MiniMessage.miniMessage().serialize(component);

        assertTrue(serializedBack.contains("<!italic>"));
        assertTrue(serializedBack.contains("<red>"));
    }

    @Property
    void parserNeverCrashesOnRandomStrings(@ForAll String input) {
        assertDoesNotThrow(() -> ColorUtils.color(input));
    }
}