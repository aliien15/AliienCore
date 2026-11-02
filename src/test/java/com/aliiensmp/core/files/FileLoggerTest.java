package com.aliiensmp.core.files;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.*;

class FileLoggerTest {
    @TempDir Path directory;

    private FileLogger logger() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        return new FileLogger(plugin);
    }

    @Test
    void concurrentLoggersAppendCompleteUtf8LinesWithoutLosingMessages() throws Exception {
        var first = logger().setFileName("events.log");
        var second = logger().setFileName("events.log");
        CompletableFuture<?>[] calls = new CompletableFuture<?>[40];
        for (int i = 0; i < calls.length; i++) {
            final int n = i;
            calls[i] =
                    CompletableFuture.runAsync(
                            () -> (n % 2 == 0 ? first : second).logAsync("message-" + n + " ☃"));
        }
        CompletableFuture.allOf(calls).get(5, TimeUnit.SECONDS);
        Path file = directory.resolve("events.log");
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            var lines = Files.readAllLines(file);
                            assertEquals(40, lines.size());
                            for (int n = 0; n < 40; n++) {
                                final int number = n;
                                assertEquals(
                                        1,
                                        lines.stream()
                                                .filter(l -> l.endsWith("message-" + number + " ☃"))
                                                .count());
                            }
                        });
    }

    @Test
    void disabledLoggingDoesNotCreateFilesAndCanBeReenabled() {
        var logger = logger().setEnabled(false);
        logger.logAsync("ignored");
        assertFalse(Files.exists(directory.resolve("logs.log")));
        assertTrue(logger.toggleEnabled());
        logger.logAsync("written");
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () ->
                                assertTrue(
                                        Files.readString(directory.resolve("logs.log"))
                                                .endsWith("written" + System.lineSeparator())));
        assertThrows(IllegalArgumentException.class, () -> logger.setFileName("  "));
    }
}
