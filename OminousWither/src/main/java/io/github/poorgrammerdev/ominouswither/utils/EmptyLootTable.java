package io.github.poorgrammerdev.ominouswither.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;

import io.github.poorgrammerdev.ominouswither.OminousWither;

/**
 * Empty loot table
 */
public class EmptyLootTable implements LootTable {
    private final NamespacedKey key;

    public EmptyLootTable(final OminousWither plugin) {
        this.key = new NamespacedKey(plugin, "empty");
    }

    @Override
    public NamespacedKey getKey() {
        return this.key;
    }

    @Override
    public void fillInventory(Inventory arg0, Random arg1, LootContext arg2) {
        return;
    }

    @Override
    public Collection<ItemStack> populateLoot(Random arg0, LootContext arg1) {
        return new ArrayList<ItemStack>();
    }
    
}
