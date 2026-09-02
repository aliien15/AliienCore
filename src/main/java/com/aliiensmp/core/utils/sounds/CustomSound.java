package com.aliiensmp.core.utils.sounds;

import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a cached, playable sound loaded from a configuration file.
 * Natively supports both Bukkit Vanilla Sound enums and Custom Resource Pack sound strings.
 */
public record CustomSound(String soundKey, @Nullable Sound vanillaSound, float volume, float pitch) {

    /**
     * Plays this specific sound directly to a {@link Player}.
     *
     * @param player The {@link Player} who will hear the sound.
     */
    public void play(Player player) {
        if (vanillaSound != null) {
            player.playSound(player.getLocation(), vanillaSound, volume, pitch);
        } else if (soundKey != null && !soundKey.isEmpty()) {
            player.playSound(player.getLocation(), soundKey, volume, pitch);
        }
    }

    /**
     * Safely attempts to play this sound to a {@link CommandSender}.
     *
     * @param potentialPlayer The {@link CommandSender} who might be a player.
     */
    public void play(CommandSender potentialPlayer) {
        if (potentialPlayer instanceof Player player) {
            play(player);
        }
    }

    /**
     * Conditionally plays this sound directly to a {@link Player}.
     *
     * @param player The {@link Player} who will hear the sound.
     * @param isEnabled A setting value to decide whether to actually play the sound.
     */
    public void play(Player player, boolean isEnabled) {
        if (isEnabled) {
            play(player);
        }
    }

    /**
     * Safely and conditionally attempts to play this sound to a {@link CommandSender}.
     *
     * @param potentialPlayer The {@link CommandSender} who might be a player.
     * @param isEnabled A setting value to decide whether to actually play the sound.
     */
    public void play(CommandSender potentialPlayer, boolean isEnabled) {
        if (isEnabled) {
            play(potentialPlayer);
        }
    }
}