package com.aliiensmp.core.utils.sounds;

import org.bukkit.Sound;
import org.jetbrains.annotations.Nullable;

public class SoundUtils {

    /**
     * Parses a configuration string into a playable CustomSound.
     * Expected format: "SOUND_NAME:VOLUME:PITCH" (e.g., "BLOCK_NOTE_BLOCK_PLING:1.0:2.0" or "custom.aliien.click:1.0:1.0")
     *
     * @param format The raw string from the config file.
     * @return A parsed CustomSound, or null if the format is invalid or "none".
     */
    @Nullable
    public static CustomSound parse(String format) {
        if (format == null || format.isEmpty() || format.equalsIgnoreCase("none")) {
            return null;
        }

        String[] parts = format.split(":");
        String soundKey = parts[0]; // The raw name provided in the config

        float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
        float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;

        Sound vanillaSound = null;
        try {
            // Try to resolve it as a standard Bukkit Vanilla sound first
            vanillaSound = Sound.valueOf(soundKey.toUpperCase());
        } catch (IllegalArgumentException e) {
            // If it's not a vanilla enum, keep vanillaSound as null!
            // The CustomSound record will automatically fall back to using the 'soundKey' string.
        }

        return new CustomSound(soundKey, vanillaSound, volume, pitch);
    }
}