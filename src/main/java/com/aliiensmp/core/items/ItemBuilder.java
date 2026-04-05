package com.aliiensmp.core.items;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {

    /**
     * The physical ItemStack being constructed.
     */
    private final ItemStack item;

    /**
     * The ItemMeta containing the item's display and behavior data.
     */
    private final ItemMeta meta;

    /**
     * Constructs a new ItemBuilder with the specified material.
     *
     * @param material The Material of the item.
     */
    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    /**
     * Constructs a new ItemBuilder with the specified material and stack size.
     *
     * @param material The Material of the item.
     * @param amount   The amount of items in this stack.
     */
    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
    }

    /**
     * Sets the display name of the item.
     *
     * @param name The Component representing the new display name.
     * @return The current ItemBuilder instance for chaining.
     */
    public ItemBuilder name(Component name) {
        if (meta != null) meta.displayName(name);
        return this;
    }

    /**
     * Replaces the entire lore of the item with a new list.
     *
     * @param lore The list of Components to set as the item's lore.
     * @return The current ItemBuilder instance for chaining.
     */
    public ItemBuilder lore(List<Component> lore) {
        if (meta != null) meta.lore(lore);
        return this;
    }

    /**
     * Appends a single new line to the bottom of the item's existing lore.
     *
     * @param line The Component to append.
     * @return The current ItemBuilder instance for chaining.
     */
    public ItemBuilder addLoreLine(Component line) {
        if (meta != null) {
            List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
            if (lore != null) {
                lore.add(line);
                meta.lore(lore);
            }
        }
        return this;
    }

    /**
     * Sets the custom model data for the item (used for resource packs).
     * If the ID is invalid (<=0), no changes are applied.
     *
     * @param modelData The custom model ID to apply.
     * @return The current ItemBuilder instance for chaining.
     */
    @SuppressWarnings("deprecation")
    public ItemBuilder customModelData(int modelData) {
        if (meta != null && modelData > 0) meta.setCustomModelData(modelData);
        return this;
    }

    /**
     * Adds one or more ItemFlags to the item (e.g., HIDE_ENCHANTS).
     *
     * @param flags The ItemFlags to apply.
     * @return The current ItemBuilder instance for chaining.
     */
    public ItemBuilder addFlags(ItemFlag... flags) {
        if (meta != null) meta.addItemFlags(flags);
        return this;
    }

    /**
     * Toggles a glowing effect on the item.
     * If true, it applies an Unbreaking I enchantment and hides the enchantment flags.
     *
     * @param glow True to make the item glow, false to do nothing.
     * @return The current ItemBuilder instance for chaining.
     */
    public ItemBuilder glow(boolean glow) {
        if (glow && meta != null) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    /**
     * Attaches hidden string data to the item using the PersistentDataContainer.
     * This acts as a secure "storage locker" for the item, allowing you to hide data
     * (like GUI actions or rule IDs) that cannot be modified by players.
     *
     * @param plugin The plugin instance. This generates the "Namespace" (the owner of the locker),
     * ensuring your data never conflicts with other plugins.
     * @param key    The identifier or label for this specific piece of data (e.g., "gui_action").
     * @param value  The actual data string you want to store (e.g., "ban_player").
     * @return       The current ItemBuilder instance for chaining.
     */
    public ItemBuilder addStringTag(Plugin plugin, String key, String value) {
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            meta.getPersistentDataContainer().set(namespacedKey, org.bukkit.persistence.PersistentDataType.STRING, value);
        }
        return this;
    }

    /**
     * Finalizes the construction by applying the ItemMeta to the ItemStack.
     *
     * @return The fully constructed ItemStack.
     */
    public ItemStack build() {
        if (meta != null) item.setItemMeta(meta);
        return item;
    }
}