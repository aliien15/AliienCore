package com.aliiensmp.core.utils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.Map;

public class PlayerUtils {

    /**
     * Safely gives an item to a player. If their inventory is full,
     * the item is safely dropped at their current location.
     *
     * @param player The player receiving the item.
     * @param item   The ItemStack to give.
     */
    public static void giveItem(Player player, ItemStack item) {
        if (player == null || item == null) return;

        // addItem returns a map of items that didn't fit!
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);

        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }
}