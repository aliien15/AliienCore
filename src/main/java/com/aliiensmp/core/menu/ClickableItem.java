package com.aliiensmp.core.menu;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import java.util.function.Consumer;

/**
 * Represents an immutable wrapper for an {@link ItemStack} and its
 * associated click logic.
 *
 * @param itemStack The visual item to display in the GUI.
 * @param action The logic to execute when a player clicks this item.
 */
public record ClickableItem(ItemStack itemStack, Consumer<InventoryClickEvent> action) {

    /**
     * Creates a new ClickableItem with a specific action.
     *
     * @param item The item to display.
     * @param action The consumer to run on click.
     * @return A new ClickableItem instance.
     */
    public static ClickableItem of(ItemStack item, Consumer<InventoryClickEvent> action) {
        return new ClickableItem(item, action);
    }

    /**
     * Creates a ClickableItem that has no action (e.g., background glass).
     *
     * @param item The item to display.
     * @return A new ClickableItem instance with a null-safe empty action.
     */
    public static ClickableItem empty(ItemStack item) {
        return new ClickableItem(item, event -> {});
    }
}