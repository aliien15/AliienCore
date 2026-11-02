package com.aliiensmp.core.input.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.papermc.paper.threadedregions.scheduler.*;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

class ChatPromptSchedulerTest {
    @AfterEach
    void cleanup() {
        ChatPrompt.shutdown();
    }

    @Test
    void replacementCancelsOldTaskAndTimeoutCancelsOnlyOnce() {
        var player = mock(Player.class);
        var plugin = mock(JavaPlugin.class);
        var scheduler = mock(EntityScheduler.class);
        var oldTask = mock(ScheduledTask.class);
        var newTask = mock(ScheduledTask.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getScheduler()).thenReturn(scheduler);
        when(scheduler.runDelayed(eq(plugin), any(), isNull(), eq(20L)))
                .thenReturn(oldTask, newTask);
        ChatPrompt.init(plugin);
        var oldCancelled = new AtomicInteger();
        var newCancelled = new AtomicInteger();
        ChatPrompt.startInput(player, 20, s -> fail(), oldCancelled::incrementAndGet);
        ChatPrompt.startInput(player, 20, s -> fail(), newCancelled::incrementAndGet);
        verify(oldTask).cancel();
        assertEquals(1, oldCancelled.get());
        assertTrue(ChatPrompt.hasPrompt(player.getUniqueId()));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<ScheduledTask>> callback = ArgumentCaptor.forClass(Consumer.class);
        verify(scheduler, times(2)).runDelayed(eq(plugin), callback.capture(), isNull(), eq(20L));
        callback.getAllValues().get(1).accept(newTask);
        callback.getAllValues().get(1).accept(newTask);
        assertEquals(1, newCancelled.get());
        assertFalse(ChatPrompt.hasPrompt(player.getUniqueId()));
    }
}
