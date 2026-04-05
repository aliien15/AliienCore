package com.aliiensmp.core.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for handling text colorization and formatting.
 * Utilizes Adventure's MiniMessage API while maintaining flawless fallback support
 * for legacy ampersand (&) color codes and hex codes (&#ffffff).
 */
public class ColorUtils {

    private static final MiniMessage MM = MiniMessage.miniMessage();

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
        if (text == null) return Component.empty();

        // Convert legacy hex (&#ffffff) to mm hex (<#ffffff>)
        text = text.replaceAll("&#([A-Fa-f0-9]{6})", "<#$1>");

        // Convert standard old & codes (like &a) to § codes
        text = text.replace("&", "§");

        // Ensure any leftover § codes are properly serialized so MiniMessage doesn't break
        String parsed = LegacyComponentSerializer.legacySection().serialize(
                LegacyComponentSerializer.legacySection().deserialize(text)
        );

        return MM.deserialize(parsed);
    }

    /**
     * Converts a list of raw strings into a list of formatted Adventure Components.
     * Perfect for formatting configuration lore lists.
     *
     * @param lore The list of raw strings to colorize.
     * @return A list of formatted Components.
     */
    public static List<Component> color(List<String> lore) {
        if (lore == null) return List.of();
        return lore.stream().map(ColorUtils::color).collect(Collectors.toList());
    }
}