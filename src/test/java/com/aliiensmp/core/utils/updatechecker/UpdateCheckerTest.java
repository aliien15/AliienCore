package com.aliiensmp.core.utils.updatechecker;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aliiensmp.core.support.HttpTestServer;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

class UpdateCheckerTest {
    private Plugin plugin() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("TestPlugin");
        when(plugin.getDescription())
                .thenReturn(new PluginDescriptionFile("TestPlugin", "1.0", "test.Main"));
        when(plugin.getLogger()).thenReturn(Logger.getLogger("update-test"));

        return plugin;
    }

    @ParameterizedTest
    @CsvSource({"200, 2.0, 2.0", "200, '   ', EMPTY", "404, missing, EMPTY", "500, error, EMPTY"})
    void parsesVersionsAndRejectsHttpErrors(int status, String body, String expected)
            throws Exception {
        AtomicReference<String> agent = new AtomicReference<>();

        try (var http =
                new HttpTestServer(
                        exchange -> {
                            agent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
                            HttpTestServer.respond(exchange, status, body);
                        })) {
            assertEquals(
                    expected.equals("EMPTY") ? Optional.empty() : Optional.of(expected),
                    new UpdateChecker(plugin(), http.url())
                            .fetchVersion()
                            .get(10, TimeUnit.SECONDS));
            assertEquals("TestPlugin/1.0", agent.get());
        }
    }

    @Test
    void readsOnlyFirstTrimmedLine() throws Exception {
        try (var http =
                new HttpTestServer(e -> HttpTestServer.respond(e, 200, " 2.1 \r\nignored"))) {
            assertEquals(
                    Optional.of("2.1"),
                    new UpdateChecker(plugin(), http.url())
                            .fetchVersion()
                            .get(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void invalidUriReturnsEmptyRatherThanThrowing() throws Exception {
        assertEquals(
                Optional.empty(),
                new UpdateChecker(plugin(), "not a valid URI").fetchVersion().get());
        assertEquals(
                Optional.empty(),
                new UpdateChecker(plugin(), "ftp://localhost/test").fetchVersion().get());
    }
}
