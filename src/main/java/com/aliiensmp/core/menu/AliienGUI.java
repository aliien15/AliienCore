package com.aliiensmp.core.menu;

import com.aliiensmp.core.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * The primary builder for creating interactive menus within the AliienCore framework.
 */
public class AliienGUI {

    // Modern String-based NamespacedKey creation to avoid deprecation warnings
    protected static final NamespacedKey GUI_MARKER = NamespacedKey.fromString("aliiencore:gui_item");

    private final String title;
    private final int rows;
    private final Map<Integer, ClickableItem> items = new HashMap<>();

    /**
     * Initializes a new GUI configuration.
     *
     * @param title The title of the menu (Supports MiniMessage and Hex).
     * @param rows The number of rows (1-6).
     */
    public AliienGUI(String title, int rows) {
        this.title = title;
        this.rows = (rows < 1 || rows > 6) ? 6 : rows;
    }

    /**
     * Maps a {@link ClickableItem} to a specific inventory slot.
     *
     * @param slot The slot index (0 to rows*9 - 1).
     * @param item The clickable item to place.
     */
    public void setItem(int slot, ClickableItem item) {
        items.put(slot, item);
    }

    /**
     * Generates the inventory and opens it for the specified player.
     * Automatically handles color parsing, page-placeholder replacement, and item marking.
     *
     * @param player The player to open the menu for.
     * @param page The page number to display (replaces %page% in title).
     */
    public void open(Player player, int page) {
        MenuHolder holder = new com.aliiensmp.core.menu.MenuHolder(this, page);
        String finalTitle = title.replace("%page%", String.valueOf(page));
        Component coloredTitle = ColorUtils.color(finalTitle);

        Inventory inventory = Bukkit.createInventory(holder, rows * 9, coloredTitle);
        holder.setInventory(inventory);

        for (Map.Entry<Integer, ClickableItem> entry : items.entrySet()) {
            int slot = entry.getKey();
            ItemStack item = entry.getValue().itemStack();

            // Anti-dupe prevention
            markItem(item);

            if (slot >= 0 && slot < (rows * 9)) {
                inventory.setItem(slot, item);
            }
        }

        player.openInventory(inventory);
    }

    /**
     * Tags an item stack with the internal AliienCore GUI marker.
     *
     * @param item The item to protect.
     */
    private void markItem(ItemStack item) {
        if (item == null || item.getType().isAir() || GUI_MARKER == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().set(GUI_MARKER, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    /**
     * Returns the internal map of all items registered to this GUI.
     *
     * @return A map of slot indices to {@link ClickableItem}s.
     */
    public Map<Integer, ClickableItem> getItems() {
        return items;
    }
}