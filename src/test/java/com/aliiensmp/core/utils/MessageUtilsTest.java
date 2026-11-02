package com.aliiensmp.core.utils;

import static org.junit.jupiter.api.Assertions.*;

import com.aliiensmp.core.support.BukkitTest;

import org.junit.jupiter.api.Test;

import java.util.UUID;

class MessageUtilsTest extends BukkitTest {
    @Test
    void messagesReplacePairsKeepUnmatchedTokensAndApplyPrefix() {
        var player = server.addPlayer();
        MessageUtils.send(
                player, "[Core] ", "Hello %name% %unmatched%", "%name%", "Alex", "%unmatched%");
        assertEquals("[Core] Hello Alex %unmatched%", player.nextMessage());
        MessageUtils.send(player, null, null);
        MessageUtils.send(player, null, "");
        assertNull(player.nextMessage());
    }

    @Test
    void broadcastsRespectFilterAndStillReachConsole() {
        var selected = server.addPlayer();
        var excluded = server.addPlayer();
        MessageUtils.broadcast(null, "Update %v%", p -> p.equals(selected), "%v%", "2");
        assertEquals("Update 2", selected.nextMessage());
        assertNull(excluded.nextMessage());
        assertEquals("Update 2", server.getConsoleSender().nextMessage());
    }

    @Test
    void onlineDispatchIgnoresMissingPlayers() {
        var player = server.addPlayer();
        MessageUtils.sendIfOnline(player.getUniqueId(), null, "one");
        MessageUtils.sendIfOnline(player.getName(), null, "two");
        assertEquals("one", player.nextMessage());
        assertEquals("two", player.nextMessage());
        assertDoesNotThrow(() -> MessageUtils.sendIfOnline(UUID.randomUUID(), null, "missing"));
        assertDoesNotThrow(() -> MessageUtils.sendIfOnline("absent", null, "missing"));
    }

    @Test
    void actionBarsAndTitlesResolvePlaceholdersAndDefaultTimes() {
        var player = org.mockito.Mockito.mock(org.bukkit.entity.Player.class);
        MessageUtils.sendActionBar(player, "Hello %name%", "%name%", "Alex");
        org.mockito.Mockito.verify(player).sendActionBar(ColorUtils.color("Hello Alex"));
        MessageUtils.sendTitle(player, "Hi %name%", null, "%name%", "Alex");
        var title = org.mockito.ArgumentCaptor.forClass(net.kyori.adventure.title.Title.class);
        org.mockito.Mockito.verify(player).showTitle(title.capture());
        assertEquals(ColorUtils.color("Hi Alex"), title.getValue().title());
        assertEquals(net.kyori.adventure.text.Component.empty(), title.getValue().subtitle());
        assertEquals(java.time.Duration.ofMillis(3500), title.getValue().times().stay());
        MessageUtils.sendActionBar(player, null);
        MessageUtils.sendActionBar(player, "");
        org.mockito.Mockito.verifyNoMoreInteractions(player);
    }

    @Test
    void offlineActionBarsAndTitlesAreSafeThroughBothLookupMethods() {
        UUID missing = UUID.randomUUID();
        assertDoesNotThrow(
                () -> {
                    MessageUtils.sendActionBarIfOnline(missing, "ignored");
                    MessageUtils.sendActionBarIfOnline("absent", "ignored");
                    MessageUtils.sendTitleIfOnline(missing, "ignored", "ignored");
                    MessageUtils.sendTitleIfOnline("absent", "ignored", "ignored");
                });
    }
}
