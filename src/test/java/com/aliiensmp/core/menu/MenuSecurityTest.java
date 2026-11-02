package com.aliiensmp.core.menu;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aliiensmp.core.support.BukkitTest;

import io.papermc.paper.threadedregions.scheduler.*;

import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.function.Consumer;

class MenuSecurityTest extends BukkitTest {
    private ItemStack markedItem() {
        var player = server.addPlayer();
        var gui = new AliienGUI("Menu", 1);
        gui.setItem(0, ClickableItem.empty(new ItemStack(Material.DIAMOND)));
        gui.open(player);
        return player.getOpenInventory().getTopInventory().getItem(0).clone();
    }

    @Test
    void leakedItemsCannotBePickedUpOrDroppedWhileOrdinaryItemsRemain() {
        var listener = new MenuListener(plugin);
        var player = server.addPlayer();
        var marked = mock(Item.class);
        when(marked.getItemStack()).thenReturn(markedItem());
        var pickup = new EntityPickupItemEvent(player, marked, 0);
        listener.onPickup(pickup);
        assertTrue(pickup.isCancelled());
        verify(marked).remove();
        var dropped = mock(Item.class);
        when(dropped.getItemStack()).thenReturn(markedItem());
        listener.onDrop(new PlayerDropItemEvent(player, dropped));
        verify(dropped).remove();
        var ordinary = mock(Item.class);
        when(ordinary.getItemStack()).thenReturn(new ItemStack(Material.DIAMOND));
        var allowed = new EntityPickupItemEvent(player, ordinary, 0);
        listener.onPickup(allowed);
        listener.onDrop(new PlayerDropItemEvent(player, ordinary));
        assertFalse(allowed.isCancelled());
        verify(ordinary, never()).remove();
    }

    @Test
    void joinCleanupRunsAfterDelayAndPreservesLegitimateItems() {
        var inventory = server.addPlayer().getInventory();
        inventory.setItem(0, markedItem());
        inventory.setItem(1, new ItemStack(Material.DIAMOND));
        var player = mock(Player.class);
        var scheduler = mock(EntityScheduler.class);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getItemOnCursor()).thenReturn(markedItem());
        when(player.getScheduler()).thenReturn(scheduler);
        when(player.getName()).thenReturn("Test");
        new MenuListener(plugin).onJoin(new PlayerJoinEvent(player, Component.empty()));
        assertNotNull(inventory.getItem(0));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<ScheduledTask>> callback = ArgumentCaptor.forClass(Consumer.class);
        verify(scheduler).runDelayed(eq(plugin), callback.capture(), isNull(), eq(10L));
        callback.getValue().accept(mock(ScheduledTask.class));
        assertNull(inventory.getItem(0));
        assertEquals(Material.DIAMOND, inventory.getItem(1).getType());
        verify(player).setItemOnCursor(null);
        verify(player).updateInventory();
    }

    @Test
    void closingMenuPurgesLeakedItemsAndSchedulesInventoryRefresh() {
        var player = server.addPlayer();
        var gui = new AliienGUI("Menu", 1);
        gui.open(player);
        var inventory = player.getOpenInventory().getTopInventory();
        var fakePlayer = mock(Player.class);
        var scheduler = mock(EntityScheduler.class);
        when(fakePlayer.getInventory()).thenReturn(player.getInventory());
        when(fakePlayer.getScheduler()).thenReturn(scheduler);
        player.getInventory().setItem(0, markedItem());
        var event = mock(InventoryCloseEvent.class);
        when(event.getInventory()).thenReturn(inventory);
        when(event.getPlayer()).thenReturn(fakePlayer);
        new MenuListener(plugin).onMenuClose(event);
        assertNull(player.getInventory().getItem(0));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<ScheduledTask>> callback = ArgumentCaptor.forClass(Consumer.class);
        verify(scheduler).runDelayed(eq(plugin), callback.capture(), isNull(), eq(1L));
        callback.getValue().accept(mock(ScheduledTask.class));
        verify(fakePlayer).updateInventory();
    }
}
