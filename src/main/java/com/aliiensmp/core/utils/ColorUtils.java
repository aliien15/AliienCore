package com.aliiensmp.core.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for handling text colorization and formatting.
 * Utilizes Adventure's MiniMessage API while maintaining fallback support
 * for legacy ampersand (&) color codes and hex codes (&#ffffff).
 */
public final class ColorUtils {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final char SECTION_CHAR = '\u00A7';
    private static final String DEFAULT_STYLE_PREFIX = "<!italic>";

    private ColorUtils() {
    }

    /**
     * Converts a raw string containing MiniMessage tags, legacy color codes,
     * or hex codes into the mm format.
     *
     * Supported formats:
     * - MiniMessage: {@code <red><bold>Text} or {@code <#ffffff>Text}
     * - Legacy Hex: {@code &#ffffffText}
     * - Legacy Ampersand: {@code &c&lText}
     *
     * @param text The raw string to colorize.
     * @return A fully serialized Component, or an empty Component if the input is null.
     */
    public static Component color(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        return MM.deserialize(DEFAULT_STYLE_PREFIX + translateLegacyFormatting(text));
    }

    /**
     * Converts a list of raw strings into a list of formatted Adventure Components.
     * Perfect for formatting configuration lore lists.
     *
     * @param lore The list of raw strings to colorize.
     * @return A list of formatted Components.
     */
    public static List<Component> color(List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return List.of();
        }

        List<Component> coloredLore = new ArrayList<>(lore.size());
        for (String line : lore) {
            coloredLore.add(color(line));
        }
        return List.copyOf(coloredLore);
    }

    private static String translateLegacyFormatting(String text) {
        if (text.indexOf('&') < 0 && text.indexOf(SECTION_CHAR) < 0) {
            return text;
        }

        StringBuilder builder = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);

            if ((current == '&' || current == SECTION_CHAR) && i + 1 < text.length()) {
                if (text.charAt(i + 1) == '#' && i + 7 < text.length() && isHexColor(text, i + 2)) {
                    builder.append("<#").append(text, i + 2, i + 8).append('>');
                    i += 7;
                    continue;
                }

                String replacement = legacyReplacement(text.charAt(i + 1));
                if (replacement != null) {
                    builder.append(replacement);
                    i++;
                    continue;
                }
            }

            builder.append(current);
        }

        return builder.toString();
    }

    private static boolean isHexColor(String text, int startIndex) {
        for (int i = startIndex; i < startIndex + 6; i++) {
            if (!isHexDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHexDigit(char character) {
        return (character >= '0' && character <= '9')
                || (character >= 'a' && character <= 'f')
                || (character >= 'A' && character <= 'F');
    }

    private static String legacyReplacement(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset><!italic>";
            default -> null;
        };
    }
}
