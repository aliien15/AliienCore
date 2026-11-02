package com.aliiensmp.core.utils;

import static org.junit.jupiter.api.Assertions.*;

import com.aliiensmp.core.support.BukkitTest;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class PlayerUtilsTest extends BukkitTest {
    @Test
    void itemsGoIntoInventoryAndOverflowIsDropped() {
        var player = server.addPlayer();
        var world = player.getWorld();
        PlayerUtils.giveItem(player, new ItemStack(Material.DIAMOND, 3));
        assertEquals(3, player.getInventory().getItem(0).getAmount());
        for (int slot = 0; slot < player.getInventory().getSize(); slot++)
            player.getInventory().setItem(slot, new ItemStack(Material.STONE, 64));
        PlayerUtils.giveItem(player, new ItemStack(Material.DIAMOND, 5));
        assertTrue(
                world.getEntities().stream()
                        .filter(Item.class::isInstance)
                        .map(Item.class::cast)
                        .anyMatch(
                                i ->
                                        i.getItemStack().getType() == Material.DIAMOND
                                                && i.getItemStack().getAmount() == 5));
    }

    @Test
    void nullAndAirInputsAreIgnored() {
        var player = server.addPlayer();
        PlayerUtils.giveItem(null, new ItemStack(Material.STONE));
        PlayerUtils.giveItem(player, null);
        PlayerUtils.giveItem(player, new ItemStack(Material.AIR));
        assertTrue(player.getInventory().isEmpty());
    }
}
