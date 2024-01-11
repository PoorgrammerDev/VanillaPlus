package io.github.poorgrammerdev.nightmare.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

/**
 * Utility class for easily creating custom items.
 * @author Thomas Tran
 */
public class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;
    private final PersistentDataContainer container;

    public ItemBuilder (ItemStack item) throws IllegalArgumentException {
        this.item = item;
        if (item.getItemMeta() == null) throw new IllegalArgumentException("ItemMeta cannot be a null value.");

        meta = item.getItemMeta();
        container = meta.getPersistentDataContainer();
    }

    public ItemBuilder(Material material, int count) throws IllegalArgumentException {
        this(new ItemStack(material, count));
    }

    public ItemBuilder(Material material) throws IllegalArgumentException {
        this(material, 1);
    }

    public ItemBuilder setName(String name) {
        meta.setDisplayName(name);
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        meta.setLore(Arrays.asList(lore));
        return this;
    }

    public ItemBuilder addItemFlags (ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder setCustomModelData (int data) {
        meta.setCustomModelData(data);
        return this;
    }

    public ItemBuilder addEnchant (Enchantment enchantment, int level, boolean ignoreLevelRestriction) {
        meta.addEnchant(enchantment, level, ignoreLevelRestriction);
        return this;
    }

    public ItemBuilder setDamage(int damage) throws IllegalArgumentException {
        if (meta instanceof Damageable) {
            final Damageable damageable = (Damageable) meta;
            damageable.setDamage(damage);
            return this;
        }
        throw new IllegalArgumentException("Item does not have durability");
    }

    public ItemBuilder setRepairCost (int repairCost) throws IllegalArgumentException {
        if (meta instanceof Repairable) {
            final Repairable repairable = (Repairable) meta;
            repairable.setRepairCost(repairCost);
            return this;
        }
        throw new IllegalArgumentException("Item is not repairable");
    }

    public <T,Z> ItemBuilder setPersistentData(NamespacedKey key, PersistentDataType<T, Z> type, Z data) throws IllegalArgumentException {
        if (container != null) {
            container.set(key, type, data);
            return this;
        }
        throw new IllegalArgumentException("Item does not have PersistentDataContainer");
    }

    public ItemBuilder addAttributeModifier(Attribute attribute, AttributeModifier modifier) {
        meta.addAttributeModifier(attribute, modifier);
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}
