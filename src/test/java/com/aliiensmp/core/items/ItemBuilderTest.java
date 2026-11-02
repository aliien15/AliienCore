package com.aliiensmp.core.items;

import static org.junit.jupiter.api.Assertions.*;

import com.aliiensmp.core.support.BukkitTest;

import net.kyori.adventure.text.Component;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

class ItemBuilderTest extends BukkitTest {
    @Test
    void builderPersistsMetadataLoreGlowAndTags() {
        var item =
                new ItemBuilder(Material.DIAMOND_SWORD, 2)
                        .name(Component.text("Sword"))
                        .lore(List.of(Component.text("first")))
                        .addLoreLines(Arrays.asList(null, Component.text("second")))
                        .customModelData(42)
                        .glow(true)
                        .addStringTag(plugin, "owner", "test")
                        .build();
        assertEquals(2, item.getAmount());
        var meta = item.getItemMeta();
        assertEquals(Component.text("Sword"), meta.displayName());
        assertEquals(List.of(Component.text("first"), Component.text("second")), meta.lore());
        assertEquals(42, meta.getCustomModelData());
        assertTrue(meta.hasEnchant(Enchantment.UNBREAKING));
        assertTrue(meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS));
        assertEquals(
                "test",
                meta.getPersistentDataContainer()
                        .get(new NamespacedKey(plugin, "owner"), PersistentDataType.STRING));
    }

    @Test
    void nullLoreAndInvalidModelDoNotEraseExistingMetadata() {
        var item =
                new ItemBuilder(Material.STONE)
                        .customModelData(5)
                        .customModelData(0)
                        .addLoreLine((Component) null)
                        .addLoreLines(null)
                        .stringLore(null)
                        .build();
        assertEquals(5, item.getItemMeta().getCustomModelData());
        assertNull(item.getItemMeta().lore());
        assertDoesNotThrow(() -> new ItemBuilder(Material.AIR).glow(true).name("air").build());
    }
}
