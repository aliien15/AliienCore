package com.aliiensmp.core.utils.sounds;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a cached, playable sound loaded from a configuration file.
 * Natively supports both Bukkit Vanilla Sound enums and Custom Resource Pack sound strings.
 */
public record CustomSound(String soundKey, @Nullable Sound vanillaSound, float volume, float pitch) {

    /**
     * Plays this specific sound to the designated player.
     * Automatically routes to vanilla or custom sound based on availability.
     *
     * @param player The player who will hear the sound.
     */
    public void play(Player player) {
        if (player == null) return;

        if (vanillaSound != null) {
            // It's a vanilla Bukkit sound
            player.playSound(player.getLocation(), vanillaSound, volume, pitch);
        } else if (soundKey != null && !soundKey.isEmpty()) {
            // It's a custom resource pack sound
            player.playSound(player.getLocation(), soundKey, volume, pitch);
        }
    }
}