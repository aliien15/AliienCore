package com.aliiensmp.core.utils.updatechecker;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.function.Supplier;

public class UpdateNotifyListener implements Listener {

    private final Plugin plugin;
    private final String gistUrl;
    private final String permissionNode;
    private final Supplier<Component> messageSupplier;

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
        this.gistUrl = gistUrl;
        this.permissionNode = permissionNode;
        this.messageSupplier = messageSupplier;
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission(permissionNode)) {
            new UpdateChecker(plugin, gistUrl).getVersion(version -> {
                if (!plugin.getDescription().getVersion().equals(version)) {
                    player.sendMessage(messageSupplier.get());
                }
            });
        }
    }
}