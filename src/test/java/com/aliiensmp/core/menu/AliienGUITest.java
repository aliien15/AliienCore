package com.aliiensmp.core.menu;

import static org.junit.jupiter.api.Assertions.*;

import com.aliiensmp.core.support.BukkitTest;

import org.bukkit.Material;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class AliienGUITest extends BukkitTest {
    @Test
    void openingClonesAndMarksItemsAndRetainsPage() {
        var player = server.addPlayer();
        var original = new ItemStack(Material.DIAMOND);
        var gui = new AliienGUI("Page %page%", 2);
        gui.setItem(0, ClickableItem.empty(original));
        gui.open(player, 3);
        var inventory = player.getOpenInventory().getTopInventory();
        assertEquals(18, inventory.getSize());
        var holder = assertInstanceOf(MenuHolder.class, inventory.getHolder());
        assertEquals(3, holder.getPage());
        assertSame(gui, holder.getGui());
        assertSame(inventory, holder.getInventory());
        assertTrue(
                inventory
                        .getItem(0)
                        .getItemMeta()
                        .getPersistentDataContainer()
                        .has(AliienGUI.GUI_MARKER, PersistentDataType.BYTE));
        assertFalse(
                original.getItemMeta()
                        .getPersistentDataContainer()
                        .has(AliienGUI.GUI_MARKER, PersistentDataType.BYTE));
    }

    @Test
    void clicksRouteOnlyTopInventoryActionsAndDragsCannotTakeMenuItems() {
        var player = server.addPlayer();
        var gui = new AliienGUI("Menu", 1);
        AtomicInteger clicks = new AtomicInteger();
        gui.setItem(
                0, ClickableItem.of(new ItemStack(Material.STONE), e -> clicks.incrementAndGet()));
        gui.open(player);
        var listener = new MenuListener(plugin);
        for (int slot : new int[] {0, 9, -999}) {
            var event =
                    new InventoryClickEvent(
                            player.getOpenInventory(),
                            InventoryType.SlotType.CONTAINER,
                            slot,
                            ClickType.LEFT,
                            InventoryAction.PICKUP_ALL);
            listener.onInventoryClick(event);
            assertTrue(event.isCancelled());
        }
        assertEquals(1, clicks.get());
        var drag =
                new InventoryDragEvent(
                        player.getOpenInventory(),
                        new ItemStack(Material.STONE),
                        new ItemStack(Material.STONE),
                        false,
                        Map.of(0, new ItemStack(Material.STONE)));
        listener.onInventoryDrag(drag);
        assertTrue(drag.isCancelled());
        var bottom =
                new InventoryDragEvent(
                        player.getOpenInventory(),
                        new ItemStack(Material.STONE),
                        new ItemStack(Material.STONE),
                        false,
                        Map.of(10, new ItemStack(Material.STONE)));
        listener.onInventoryDrag(bottom);
        assertFalse(bottom.isCancelled());
    }

    @Test
    void paginationClampsAndFillPreservesExistingItems() {
        var gui = new AliienGUI("Menu", 1);
        gui.setItems(
                List.of(0, 1),
                List.of(Material.STONE, Material.DIRT, Material.DIAMOND),
                99,
                m -> ClickableItem.empty(new ItemStack(m)));
        assertEquals(Material.DIAMOND, gui.getItems().get(0).itemStack().getType());
        gui.setItem(-1, ClickableItem.empty(new ItemStack(Material.STONE)));
        gui.setItem(9, ClickableItem.empty(new ItemStack(Material.STONE)));
        gui.fillAllEmptySlots(new ItemStack(Material.GLASS));
        assertEquals(9, gui.getItems().size());
        assertEquals(Material.DIAMOND, gui.getItems().get(0).itemStack().getType());
    }
}
