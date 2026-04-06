package com.aliiensmp.core.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * A custom {@link InventoryHolder} used to identify and track
 * {@link AliienGUI} instances across the server.
 */
public class MenuHolder implements InventoryHolder {
    private final AliienGUI gui;
    private final int page;
    private Inventory inventory;

    /**
     * Constructs a new MenuHolder for a specific GUI and page.
     *
     * @param gui The parent GUI instance.
     * @param page The current page number being displayed.
     */
    public MenuHolder(AliienGUI gui, int page) {
        this.gui = gui;
        this.page = page;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    /**
     * Sets the actual Bukkit inventory associated with this holder.
     *
     * @param inventory The inventory instance.
     */
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    /**
     * Retrieves the parent GUI configuration.
     *
     * @return The {@link AliienGUI} instance.
     */
    public AliienGUI getGui() {
        return gui;
    }

    /**
     * Retrieves the page number stored in this holder.
     *
     * @return The current page integer.
     */
    public int getPage() {
        return page;
    }
}