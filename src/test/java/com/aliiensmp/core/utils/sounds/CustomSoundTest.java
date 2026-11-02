package com.aliiensmp.core.utils.sounds;

import static org.mockito.Mockito.*;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class CustomSoundTest {
    @Test
    void vanillaAndCustomSoundsUseCorrectOverloadsAndDisabledIsSilent() {
        var player = mock(Player.class);
        var location = new Location(null, 0, 0, 0);
        when(player.getLocation()).thenReturn(location);
        var vanilla = new CustomSound("ignored", Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 2f);

        vanilla.play(player, false);

        verify(player, never()).getLocation();

        vanilla.play(player);

        verify(player).playSound(location, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 2f);

        new CustomSound("custom.click", null, 1f, 1f).play((CommandSender) player, true);

        verify(player).playSound(location, "custom.click", 1f, 1f);

        var console = mock(CommandSender.class);

        vanilla.play(console);

        verifyNoInteractions(console);

        new CustomSound("", null, 1f, 1f).play(player);
    }
}
