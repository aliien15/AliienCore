package com.aliiensmp.core.utils.sounds;

import org.bukkit.Sound;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class SoundUtils {

    private SoundUtils() {
    }

    /**
     * Parses a configuration string into a playable CustomSound.
     * Expected format: "SOUND_NAME:VOLUME:PITCH" (e.g., "BLOCK_NOTE_BLOCK_PLING:1.0:2.0" or "custom.aliien.click:1.0:1.0")
     *
     * @param format The raw string from the config file.
     * @return A parsed CustomSound, or null if the format is invalid or "none".
     */
    @Nullable
    public static CustomSound parse(String format) {
        if (format == null) {
            return null;
        }

        String trimmed = format.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("none")) {
            return null;
        }

        ParsedSound parsedSound = parseSoundDefinition(trimmed);
        String soundKey = parsedSound.soundKey().trim();
        if (soundKey.isEmpty()) {
            return null;
        }

        Sound vanillaSound = null;
        try {
            // Try to resolve it as a standard Bukkit Vanilla sound first
            vanillaSound = Sound.valueOf(soundKey.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            // If it's not a vanilla enum, keep vanillaSound as null
            // The CustomSound record will automatically fall back to using the 'soundKey' string.
        }

        return new CustomSound(soundKey, vanillaSound, parsedSound.volume(), parsedSound.pitch());
    }

    private static ParsedSound parseSoundDefinition(String soundDefinition) {
        float volume = 1.0f;
        float pitch = 1.0f;

        int lastColon = soundDefinition.lastIndexOf(':');
        if (lastColon < 0) {
            return new ParsedSound(soundDefinition, volume, pitch);
        }

        Float lastValue = tryParseFloat(soundDefinition.substring(lastColon + 1));
        if (lastValue == null) {
            return new ParsedSound(soundDefinition, volume, pitch);
        }

        String beforeLastValue = soundDefinition.substring(0, lastColon);
        int secondLastColon = beforeLastValue.lastIndexOf(':');
        if (secondLastColon < 0) {
            return new ParsedSound(beforeLastValue, lastValue, pitch);
        }

        Float volumeValue = tryParseFloat(beforeLastValue.substring(secondLastColon + 1));
        if (volumeValue == null) {
            return new ParsedSound(beforeLastValue, lastValue, pitch);
        }

        return new ParsedSound(beforeLastValue.substring(0, secondLastColon), volumeValue, lastValue);
    }

    private static Float tryParseFloat(String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private record ParsedSound(String soundKey, float volume, float pitch) {
    }
}
