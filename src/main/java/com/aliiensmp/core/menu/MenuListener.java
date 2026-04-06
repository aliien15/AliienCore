package com.aliiensmp.core.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Global listener that handles all inventory interactions for the AliienCore
 * GUI framework. Prevents item duplication and routes click actions.
 */
public class MenuListener implements Listener {

    private final JavaPlugin plugin;

    public MenuListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Intercepts clicks and routes them to the appropriate {@link ClickableItem} action.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof MenuHolder menuHolder) {
            event.setCancelled(true);

            int slot = event.getRawSlot();
            ClickableItem clickable = menuHolder.getGui().getItems().get(slot);

            if (clickable != null && clickable.action() != null) {
                clickable.action().accept(event);
            }
        }
    }

    /**
     * Prevents players from dropping items marked by the GUI system.
     */
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isMarked(event.getItemDrop().getItemStack())) {
            event.getItemDrop().remove();
            plugin.getLogger().warning("Removed a leaked GUI item dropped by " + event.getPlayer().getName());
        }
    }

    /**
     * Prevents players from picking up items marked by the GUI system.
     */
    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (isMarked(event.getItem().getItemStack())) {
            event.setCancelled(true);
            event.getItem().remove();
        }
    }

    /**
     * Scans a player's inventory on login to remove any "ghost" GUI items.
     * Uses Folia's Entity Scheduler for modern, region-safe threading.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 10 ticks delay to ensure the inventory is fully loaded
        player.getScheduler().runDelayed(plugin, scheduledTask -> {
            for (ItemStack item : player.getInventory().getContents()) {
                if (isMarked(item)) {
                    player.getInventory().remove(item);
                    plugin.getLogger().warning("Cleaned a leaked GUI item from " + player.getName() + "'s inventory.");
                }
            }
        }, null, 10L);
    }

    /**
     * Helper to check if an item carries the AliienCore GUI marker.
     */
    private boolean isMarked(ItemStack item) {
        if (item == null || !item.hasItemMeta() || AliienGUI.GUI_MARKER == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(AliienGUI.GUI_MARKER, PersistentDataType.BYTE);
    }
}