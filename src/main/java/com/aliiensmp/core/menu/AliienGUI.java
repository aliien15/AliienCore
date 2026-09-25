package com.aliiensmp.core.menu;

import com.aliiensmp.core.utils.ColorUtils;
import com.aliiensmp.core.utils.DebugUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * The primary builder for creating interactive menus within the AliienCore framework.
 */
public class AliienGUI {

    protected static final @NotNull NamespacedKey GUI_MARKER = Objects.requireNonNull(
            NamespacedKey.fromString("aliiencore:gui_item"),
            "Unable to create the AliienCore GUI marker key."
    );

    private final @NotNull String title;
    private final int rows;
    private final @NotNull Map<Integer, ClickableItem> items = new HashMap<>();

    /**
     * Initializes a new GUI configuration.
     *
     * @param title The title of the menu (Supports MiniMessage and Hex).
     * @param rows The number of rows (1-6).
     */
    public AliienGUI(@NotNull String title, int rows) {
        this.title = title;
        this.rows = (rows < 1 || rows > 6) ? 6 : rows;
    }

    /**
     * Initializes a new GUI configuration, defaulting to 6 rows.
     *
     * @param title The title of the menu (Supports MiniMessage and Hex).
     */
    public AliienGUI(@NotNull String title) {
        this(title, 6);
    }

    /**
     * Generates the inventory and opens it for the specified player, defaulting to the first page.
     *
     * @param player The player to open the menu for.
     */
    public void open(@NotNull Player player) {
        open(player, 1);
    }

    /**
     * Generates the inventory and opens it for the specified player.
     *
     * @param player The player to open the menu for.
     * @param page The page number to display (replaces %page% in title).
     */
    public void open(@NotNull Player player, int page) {
        MenuHolder holder = new MenuHolder(this, page);
        String finalTitle = title.replace("%page%", String.valueOf(page));
        Component coloredTitle = ColorUtils.color(finalTitle);
        int size = rows * 9;

        Inventory inventory = Bukkit.createInventory(holder, size, coloredTitle);
        holder.setInventory(inventory);

        for (Map.Entry<Integer, ClickableItem> entry : items.entrySet()) {
            int slot = entry.getKey();
            ClickableItem clickableItem = entry.getValue();
            if (clickableItem == null) {
                continue;
            }

            ItemStack item = clickableItem.itemStack();
            if (item != null && slot < size) {
                inventory.setItem(slot, markItem(item));
            }
        }

        player.openInventory(inventory);
    }

    /**
     * Maps a {@link ClickableItem} to a specific inventory slot.
     *
     * @param slot The slot index (0 to rows*9 - 1).
     * @param item The clickable item to place.
     */
    public void setItem(int slot, @NotNull ClickableItem item) {
        if (isInvalidSlot(slot)) {
            DebugUtils.send(Level.WARNING, "An item was ignored due to an invalid slot: {0}", String.valueOf(slot));
            return;
        }
        items.put(slot, item);
    }

    /**
     * Sets a singular {@link ClickableItem} item across multiple slots
     *
     * @param slots slots to set the item
     * @param item the item to set
     */
    public void setItem(@NotNull Collection<Integer> slots, @NotNull ClickableItem item) {
        for (int slot : slots) {
            setItem(slot, item);
        }
    }

    /**
     * Sets a singular {@link ClickableItem} item across multiple slots
     *
     * @param slots slots to set the item
     * @param item the item to set
     * @ensures The {@link ItemStack} item will be turned into a {@link ClickableItem} that has
     *      no action. If you want this item to have any action, you should turn it into a {@link ClickableItem}
     *      before passing it through this method.
     */
    public void setItem(@NotNull Collection<Integer> slots, @NotNull ItemStack item) {
        for (int slot : slots) {
            setItem(slot, ClickableItem.empty(item));
        }
    }

    /**
     * Sets a {@link List} of {@link ClickableItem}s in a set of slots, skipping/limitting the items based on
     * the current page that we are at
     *
     * @param slots the list of slots to fill
     * @param items the list of items to fill
     * @param requestedPage the current page of the menu, which will help define how many items to skip and to limit
     * @ensures if {@code items.length() < slots.length()} then the remaining slots will just remain empty. The method will
     * also sanitize the {@code requestedPage}, ensuring {@code 1 <= requestedPage <= totalPages} via {@link #getTotalPages}.
     */
    public void setItems(@NotNull List<Integer> slots, @NotNull List<ClickableItem> items, int requestedPage) {
        setItems(slots, items, requestedPage, Function.identity());
    }

    /**
     * Sets a {@link List} of {@link ClickableItem}s in a set of slots, skipping/limitting the items based on
     * the current page that we are at. This method also received an action, which is only useful if you want the different items
     * to execute different things depending on the user's permission, status or any type of data.
     *
     * @param slots the list of slots to fill
     * @param items the list of items to fill
     * @param action the action to be executed by the item
     * @param currentPage the current page of the menu, which will help define how many items to skip and to limit
     * @ensures if {@code items.length() < slots.length()} then the remaining slots will just remain empty
     */
    public void setItems(@NotNull List<Integer> slots, @NotNull List<ItemStack> items, Consumer<InventoryClickEvent> action, int currentPage) {
        setItems(slots, items, currentPage, item -> ClickableItem.of(item, action));
    }

    /**
     * Sets a {@link List} of items in a set of slots, skipping/limitting the items based on
     * the current page that we are at. This method also receives a function, which will turns your list
     * of any type you want into a {@link ClickableItem} to place in the GUI. This is useful if you want to, for
     * example, display items differently depending on permissions or states.
     *
     * @param slots the list of slots to fill
     * @param entries the list of items to fill
     * @param requestedPage the current page of the menu, which will help define how many items to skip and to limit
     * @param itemFactory the function that will turn your list of objects into {@link ClickableItem}
     * @param <T> the type of the items you are passing
     * @ensures if {@code entries.length() < slots.length()} then the remaining slots will just remain empty. The method will
     * also sanitize the {@code requestedPage}, ensuring {@code 1 <= requestedPage <= totalPages} via {@link #getTotalPages}.
     */
    public <T> void setItems(@NotNull List<Integer> slots, @NotNull List<T> entries, int requestedPage, @NotNull Function<? super T, ClickableItem> itemFactory) {
        if (slots.isEmpty() || entries.isEmpty()) {
            DebugUtils.send(Level.WARNING, "A menu has received an empty list of either slots and/or items to set in the GUI.");
            return;
        }

        final int currentPage = sanitizePage(requestedPage, getTotalPages(slots.size(), entries.size()));
        final int itemsPerPage = slots.size();
        final int itemsToSkip = (currentPage - 1) * itemsPerPage;
        List<ClickableItem> itemsToPlace = entries.stream()
                .skip(itemsToSkip)
                .limit(itemsPerPage)
                .map(itemFactory)
                .toList();

        for (int i = 0; i < itemsToPlace.size(); i++) {
            setItem(slots.get(i), itemsToPlace.get(i));
        }
    }

    /**
     * Fills all empty slots/slots that don't have any item with a {@link ItemStack} item
     * The item will be converted to a {@link ClickableItem} with no action/empty, if you want it
     * to execute any action convert it to a {@link ClickableItem} beforehand.
     *
     * @param item item to fill the empty slots with
     */
    public void fillAllEmptySlots(@NotNull ItemStack item) {
        fillAllEmptySlots(ClickableItem.empty(item));
    }

    /**
     * Fills all empty slots/slots that don't have any item with a {@link ClickableItem} item
     *
     * @param item item to fill the empty slots with
     */
    public void fillAllEmptySlots(@NotNull ClickableItem item) {
        for (int i = 0; i < rows * 9; i++) {
            if (getItems().containsKey(i)) {
                continue;
            }

            setItem(i, item);
        }
    }

    /**
     * Calculates the highest page your menu will have to go to hold all the items you want based on
     * the amount of slots you are using for a specific list of items and the total number of items you want
     * to distribute through those slots. Ideally you should use the .length or .size of whatever data structure you
     * are using to store your slots and items.
     *
     * @param numberOfSlotsPerPage the number of slots that will be used to fill the items per page
     * @param totalNumberOfItem the total number of items
     * @return the number of requires pages to use all the slots
     * @ensures {@code \result > 0}
     */
    public int getTotalPages(int numberOfSlotsPerPage, int totalNumberOfItem) {
        return (numberOfSlotsPerPage <= 0) ? 1 : Math.max(1, (totalNumberOfItem + numberOfSlotsPerPage - 1) / numberOfSlotsPerPage);
    }

    /**
     * Fixes up the {@code requestedPage} in case of it being either less than 1 (by setting it to 1) or if it is
     * more than {@code totalPages}
     *
     * @param requestedPage the page being requested
     * @param totalPages the total number of pages being used
     * @return the {@code requestedPage} as a valid page number if needed to adjust
     * @ensures {@code 0 < \result <= totalPages}
     */
    public int sanitizePage(int requestedPage, int totalPages) {
        return Math.max(1, Math.min(requestedPage, Math.max(1, totalPages)));
    }

    /**
     * Filters out invalid slots, which means slots that are out of the menu's grid
     * @param slots all the slots to filter
     */
    public void filterInvalidSlots(@NotNull Collection<Integer> slots) {
        slots.removeIf(this::isInvalidSlot);
    }

    /**
     * Tags an item stack with the internal AliienCore GUI marker.
     *
     * @param item The item to protect.
     */
    private @NotNull ItemStack markItem(@NotNull ItemStack item) {
        if (item.getType().isAir()) {
            return item;
        }

        ItemStack markedItem = item.clone();
        ItemMeta meta = markedItem.getItemMeta();
        if (meta == null) {
            return markedItem;
        }

        meta.getPersistentDataContainer().set(GUI_MARKER, PersistentDataType.BYTE, (byte) 1);
        markedItem.setItemMeta(meta);
        return markedItem;
    }

    private boolean isInvalidSlot(int slot) {
        return slot < 0 || slot >= rows * 9;
    }

    /**
     * Returns the internal map of all items registered to this GUI.
     *
     * @return A map of slot indices to {@link ClickableItem}s.
     */
    public @NotNull Map<Integer, ClickableItem> getItems() {
        return items;
    }
}
