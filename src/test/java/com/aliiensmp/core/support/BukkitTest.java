package com.aliiensmp.core.support;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import com.aliiensmp.core.AliienCore;
import com.aliiensmp.core.utils.DebugUtils;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

@org.junit.jupiter.api.extension.ExtendWith(BukkitTest.FailOnUnsupportedOperation.class)
public abstract class BukkitTest {
    public static class FailOnUnsupportedOperation
            implements org.junit.jupiter.api.extension.TestExecutionExceptionHandler {
        @Override
        public void handleTestExecutionException(
                org.junit.jupiter.api.extension.ExtensionContext context, Throwable throwable)
                throws Throwable {
            if (throwable instanceof org.opentest4j.TestAbortedException) {
                throw new AssertionError(
                        "MockBukkit must execute the test, not skip it", throwable);
            }
            throw throwable;
        }
    }

    protected ServerMock server;
    protected JavaPlugin plugin;

    @BeforeEach
    void startServer() {
        // MockBukkit 3 does not implement Paper's Component inventory overload.
        // Bridge only inventory construction; items, views, and events remain MockBukkit's.
        server =
                MockBukkit.mock(
                        new ServerMock() {
                            @Override
                            public be.seeseemelk.mockbukkit.inventory.InventoryMock createInventory(
                                    org.bukkit.inventory.InventoryHolder holder,
                                    int size,
                                    net.kyori.adventure.text.Component title) {
                                return new be.seeseemelk.mockbukkit.inventory.ChestInventoryMock(
                                        holder, size);
                            }
                        });
        plugin = MockBukkit.createMockPlugin();
        DebugUtils.setDebug(false);
    }

    @AfterEach
    void stopServer() {
        AliienCore.shutdown();
        DebugUtils.setDebug(false);
        MockBukkit.unmock();
    }
}
