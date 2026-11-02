package com.aliiensmp.core.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import java.util.logging.*;

class DebugUtilsTest {
    @Test
    void toggleControlsLoggingAndPlaceholderSubstitution() {
        var plugin = mock(JavaPlugin.class);
        var logger = mock(Logger.class);
        when(plugin.getLogger()).thenReturn(logger);
        DebugUtils.setPlugin(plugin);
        DebugUtils.setDebug(false);
        try {
            DebugUtils.send("hidden");
            verifyNoInteractions(logger);
            assertTrue(DebugUtils.toggleDebug());
            DebugUtils.send(Level.WARNING, "Hi %name% %odd%", "%name%", "Alex", "%odd%");
            verify(logger).log(Level.WARNING, "Hi Alex %odd%");
            assertFalse(DebugUtils.toggleDebug());
        } finally {
            DebugUtils.setDebug(false);
            DebugUtils.shutdown();
        }
    }
}
