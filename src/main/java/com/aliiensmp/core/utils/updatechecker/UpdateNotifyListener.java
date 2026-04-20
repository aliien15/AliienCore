package com.aliiensmp.core.utils.updatechecker;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class UpdateNotifyListener implements Listener {

    private final Plugin plugin;
    private final String permissionNode;
    private final Supplier<Component> messageSupplier;
    private final UpdateChecker updateChecker;
    private final AtomicBoolean versionCheckInFlight = new AtomicBoolean(false);
    private volatile String latestVersion;

    /**
     * Creates a generic Update Notify Listener for any plugin.
     *
     * @param plugin          The plugin instance.
     * @param gistUrl         The raw URL to the version string.
     * @param permissionNode  The permission required to see the update message.
     * @param messageSupplier A supplier that fetches the update message (so it updates on config reloads).
     */
    public UpdateNotifyListener(Plugin plugin, String gistUrl, String permissionNode, Supplier<Component> messageSupplier) {
        this.plugin = plugin;
        this.permissionNode = permissionNode;
        this.messageSupplier = messageSupplier;
        this.updateChecker = new UpdateChecker(plugin, gistUrl);
        refreshLatestVersion();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPermission(permissionNode)) {
            return;
        }

        String cachedLatestVersion = latestVersion;
        if (cachedLatestVersion == null) {
            refreshLatestVersion();
            return;
        }

        if (!plugin.getDescription().getVersion().equals(cachedLatestVersion)) {
            player.sendMessage(messageSupplier.get());
        }
    }

    public void refreshLatestVersion() {
        if (!versionCheckInFlight.compareAndSet(false, true)) {
            return;
        }

        updateChecker.fetchVersion().whenComplete((version, throwable) -> {
            if (version != null) {
                version.ifPresent(value -> latestVersion = value);
            }
            versionCheckInFlight.set(false);
        });
    }
}
