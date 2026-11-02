package com.aliiensmp.core.utils.updatechecker;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aliiensmp.core.support.HttpTestServer;

import net.kyori.adventure.text.Component;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.logging.Logger;

class UpdateNotifyListenerTest {
    @Test
    void onlyAuthorizedPlayersReceiveCachedDifferentVersion() throws Exception {
        var plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("Test");
        when(plugin.getDescription())
                .thenReturn(new PluginDescriptionFile("Test", "1.0", "test.Main"));
        when(plugin.getLogger()).thenReturn(Logger.getLogger("notify-test"));
        var allowed = mock(Player.class);
        when(allowed.hasPermission("updates")).thenReturn(true);
        var denied = mock(Player.class);
        var message = Component.text("New version");
        try (var http = new HttpTestServer(e -> HttpTestServer.respond(e, 200, "2.0"))) {
            var listener = new UpdateNotifyListener(plugin, http.url(), "updates", () -> message);
            listener.onJoin(new PlayerJoinEvent(denied, Component.empty()));
            verify(denied, never()).sendMessage(any(Component.class));
            await().atMost(Duration.ofSeconds(5))
                    .untilAsserted(
                            () -> {
                                listener.onJoin(new PlayerJoinEvent(allowed, Component.empty()));
                                verify(allowed, atLeastOnce()).sendMessage(message);
                            });
        }
    }
}
