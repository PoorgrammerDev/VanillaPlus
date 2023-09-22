package io.github.poorgrammerdev.paxel;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Checks if an item is a registered special item from another plugin
 * @author Thomas Tran
 */
public class ExternalItemManager {
    private final List<NamespacedKey> externalKeys;

    public ExternalItemManager(final Paxel plugin) {
        this.externalKeys =
            plugin.getConfig().getStringList("external_tool_keys")
                .stream()
                .map((str) -> (NamespacedKey.fromString(str)))
                .collect(Collectors.toList());
    }

    /**
     * Checks if an item is an external item via a key in its PersistentDataContainer
     */
    public boolean isExternalItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        for (final NamespacedKey key : this.externalKeys) {
            if (meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.BOOLEAN, false)) {
                return true;
            }
        }

        return false;
    }
    

}
