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

    @Property
    void legacyConversionNeverCrashesOnRandomStrings(@ForAll String input) {
        assertDoesNotThrow(() -> ColorUtils.translateLegacyFormatting(input));
    }

    @Test
    void testAmpersandLegacyCodes() {
        String input = "&cThis is red and &lbold";
        String expected = "<red>This is red and <bold>bold";

        assertEquals(expected, ColorUtils.translateLegacyFormatting(input));
    }

    @Test
    void testSectionCharLegacyCodes() {
        String input = "§aThis is green and §oitalic";
        String expected = "<green>This is green and <italic>italic";

        assertEquals(expected, ColorUtils.translateLegacyFormatting(input));
    }

    @Test
    void testLegacyHexCodes() {
        String input = "&#FF0000Red &#00ff00Green &#0000ffBlue";
        String expected = "<#FF0000>Red <#00ff00>Green <#0000ff>Blue";

        assertEquals(expected, ColorUtils.translateLegacyFormatting(input));
    }

    @Test
    void testSectionCharHexCodes() {
        String input = "§#123456Custom Hex Color";
        String expected = "<#123456>Custom Hex Color";

        assertEquals(expected, ColorUtils.translateLegacyFormatting(input));
    }

    @Test
    void testConsecutiveCodes() {
        String input = "&4&lWARNING: &#123456Custom color!";
        String expected = "<dark_red><bold>WARNING: <#123456>Custom color!";

        assertEquals(expected, ColorUtils.translateLegacyFormatting(input));
    }

    @Test
    void testIgnoresInvalidCodesAndTrailingCharacters() {
        String input = "&zThis is invalid, this is just an & and this is a short hex &#123";
        String expected = "&zThis is invalid, this is just an & and this is a short hex &#123";

        assertEquals(expected, ColorUtils.translateLegacyFormatting(input));
    }

    @Test
    void testLeavesExistingMiniMessageIntact() {
        String input = "&cLegacy with <blue>MiniMessage</blue>";
        String expected = "<red>Legacy with <blue>MiniMessage</blue>";

        assertEquals(expected, ColorUtils.translateLegacyFormatting(input));
    }

    @Test
    void testEmptyAndNoFormatStrings() {
        assertEquals("", ColorUtils.translateLegacyFormatting(""));
        assertEquals("Just plain text", ColorUtils.translateLegacyFormatting("Just plain text"));
    }
}