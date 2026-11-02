package com.aliiensmp.core.input.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aliiensmp.core.support.BukkitTest;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import net.kyori.adventure.text.Component;

import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.*;

import java.util.concurrent.atomic.*;

class ChatPromptTest extends BukkitTest {
    @AfterEach
    void resetToggle() {
        ChatPrompt.setCancellationToggle("cancel");
    }

    @Test
    void chatConsumesPromptExactlyOnceAndTrimsInput() {
        var player = server.addPlayer();
        var task = mock(ScheduledTask.class);
        var received = new AtomicReference<String>();
        var cancelled = new AtomicInteger();
        ChatPrompt.ACTIVE_PROMPTS.put(
                player.getUniqueId(),
                new ChatPrompt.ActivePrompt(received::set, cancelled::incrementAndGet, task));
        var event = mock(AsyncChatEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.message()).thenReturn(Component.text("  hello  "));
        var listener = new ChatPromptListeners();
        listener.onChat(event);
        listener.onChat(event);
        assertEquals("hello", received.get());
        assertEquals(0, cancelled.get());
        assertFalse(ChatPrompt.hasPrompt(player.getUniqueId()));
        verify(task, times(1)).cancel();
        verify(event, times(1)).setCancelled(true);
    }

    @Test
    void cancellationCommandAndQuitRemovePrompts() {
        var player = server.addPlayer();
        var listener = new ChatPromptListeners();
        for (int mode = 0; mode < 3; mode++) {
            var task = mock(ScheduledTask.class);
            var cancel = new AtomicInteger();
            ChatPrompt.ACTIVE_PROMPTS.put(
                    player.getUniqueId(),
                    new ChatPrompt.ActivePrompt(
                            s -> fail("cancelled prompt accepted input"),
                            cancel::incrementAndGet,
                            task));
            if (mode == 0) {
                ChatPrompt.setCancellationToggle(" stop ");
                var event = mock(AsyncChatEvent.class);
                when(event.getPlayer()).thenReturn(player);
                when(event.message()).thenReturn(Component.text(" StOp "));
                listener.onChat(event);
            } else if (mode == 1)
                listener.onCommand(new PlayerCommandPreprocessEvent(player, "/spawn"));
            else listener.onQuit(new PlayerQuitEvent(player, Component.empty()));
            assertEquals(1, cancel.get());
            verify(task).cancel();
            assertFalse(ChatPrompt.hasPrompt(player.getUniqueId()));
        }
        assertThrows(IllegalArgumentException.class, () -> ChatPrompt.setCancellationToggle(" "));
    }

    @Test
    void shutdownCancelsTasksWithoutInvokingUserCancellationCallbacks() {
        var player = server.addPlayer();
        var task = mock(ScheduledTask.class);
        ChatPrompt.ACTIVE_PROMPTS.put(
                player.getUniqueId(), new ChatPrompt.ActivePrompt(s -> fail(), () -> fail(), task));
        ChatPrompt.shutdown();
        verify(task).cancel();
        assertFalse(ChatPrompt.hasPrompt(player.getUniqueId()));
    }
}
