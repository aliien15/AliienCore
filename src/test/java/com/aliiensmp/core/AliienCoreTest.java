package com.aliiensmp.core;

import static org.junit.jupiter.api.Assertions.*;

import com.aliiensmp.core.support.BukkitTest;

import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.Test;

class AliienCoreTest extends BukkitTest {
    @Test
    void initializationIsIdempotentAndShutdownAllowsReinitialization() {
        AliienCore.init(plugin);
        int count = HandlerList.getRegisteredListeners(plugin).size();
        assertTrue(count > 0);
        var database = AliienCore.getDatabase();
        AliienCore.init(plugin);
        assertEquals(count, HandlerList.getRegisteredListeners(plugin).size());
        assertSame(database, AliienCore.getDatabase());
        AliienCore.shutdown();
        assertNotSame(database, AliienCore.getDatabase());
        HandlerList.unregisterAll(plugin);
        AliienCore.init(plugin, false, false, false);
        assertEquals(0, HandlerList.getRegisteredListeners(plugin).size());
    }

    @Test
    void initializationRejectsNullPlugin() {
        assertThrows(NullPointerException.class, () -> AliienCore.init(null));
    }
}
