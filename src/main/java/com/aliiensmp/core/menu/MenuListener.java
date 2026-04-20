package com.aliiensmp.core.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder menuHolder) {
            event.setCancelled(true);

            int rawSlot = event.getRawSlot();
            if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) {
                return;
            }

            ClickableItem clickable = menuHolder.getGui().getItems().get(rawSlot);

            if (clickable != null && clickable.action() != null) {
                clickable.action().accept(event);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder)) {
            return;
        }

        int topInventorySize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topInventorySize) {
                event.setCancelled(true);
                return;
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
            int removedItems = purgeMarkedItems(player);
            if (removedItems > 0) {
                plugin.getLogger().warning("Cleaned " + removedItems + " leaked GUI item(s) from " + player.getName() + "'s inventory.");
                player.updateInventory();
            }
        }, null, 10L);
    }

    @EventHandler
    public void onMenuClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder)) return;

        Player player = (Player) event.getPlayer();
        if (purgeMarkedItems(player) > 0) {
            player.getScheduler().runDelayed(plugin, scheduledTask -> player.updateInventory(), null, 1L);
        }
    }

    /**
     * Helper to check if an item carries the AliienCore GUI marker.
     */
    private boolean isMarked(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(AliienGUI.GUI_MARKER, PersistentDataType.BYTE);
    }

    private int purgeMarkedItems(Player player) {
        int removedItems = 0;

        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            if (isMarked(player.getInventory().getItem(slot))) {
                player.getInventory().setItem(slot, null);
                removedItems++;
            }
        }

        if (isMarked(player.getItemOnCursor())) {
            player.setItemOnCursor(null);
            removedItems++;
        }

        return removedItems;
    }
}
