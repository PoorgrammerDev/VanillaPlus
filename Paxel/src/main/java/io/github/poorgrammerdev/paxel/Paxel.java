package io.github.poorgrammerdev.paxel;

import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class Paxel extends JavaPlugin {
    private final NamespacedKey paxelKey;
    private final ToolMapper toolMapper;

    public Paxel() {
        this.paxelKey = new NamespacedKey(this, "is_paxel");
        this.toolMapper = new ToolMapper(this);
    }

    @Override
    public void onEnable() {
        final CraftingManager craftingManager = new CraftingManager(this, this.toolMapper);
        final HashMap<String, NamespacedKey> paxelRecipeMap = craftingManager.registerAllRecipes();
        this.getServer().getPluginManager().registerEvents(craftingManager, this);

        final RepairingManager repairingManager = new RepairingManager(this, toolMapper);
        this.getServer().getPluginManager().registerEvents(repairingManager, this);

        final PaxelMechanism paxelMechanism = new PaxelMechanism(this, this.toolMapper);
        this.getServer().getPluginManager().registerEvents(paxelMechanism, this);

        final RecipeManager recipeManager = new RecipeManager(toolMapper, paxelRecipeMap);
        this.getServer().getPluginManager().registerEvents(recipeManager, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    /**
     * @return the NamespacedKey for validating that an item is a paxel
     */
    public NamespacedKey getPaxelKey() {
        return paxelKey;
    }

    /**
     * Checks if an item is a paxel or not
     * @return if the item is a paxel or not
     */
    public boolean isPaxel(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return (
            meta.getPersistentDataContainer().has(this.getPaxelKey(), PersistentDataType.BOOLEAN) &&
            meta.getPersistentDataContainer().get(this.getPaxelKey(), PersistentDataType.BOOLEAN)
        );
    }

    /**
     * Creates a new paxel item for the given mineral tier
     * @param tier mineral tier of the item (e.g. IRON or DIAMOND)
     * @return Paxel item
     */
    public ItemStack createPaxel(String tier) {
        final Material[] toolSet = this.toolMapper.getToolSet(tier);
        if (toolSet == null) return null;

        final String displayName = getPaxelName(tier);

        final ItemBuilder builder = new ItemBuilder(toolSet[ToolMapper.PICKAXE_INDEX])
            .setCustomModelData(this.getConfig().getInt("custom_model_data", 100))
            .setName(ChatColor.RESET + displayName)
            .setPersistentData(this.getPaxelKey(), PersistentDataType.BOOLEAN, true);
        
        if (this.getConfig().getBoolean("write_description", true)) {
            //The reason for setting the lore too is because the player can rename the tool
            builder.setLore(ChatColor.GRAY + displayName);
        }

        return builder.build();
    }

    /**
     * Get the name of a paxel for a given tier
     * @param tier mineral tier of the item (e.g. IRON or DIAMOND)
     * @return name of this tier's paxel
     */
    public String getPaxelName(String tier) {
        return tier.charAt(0) + tier.substring(1).toLowerCase() + " Paxel";
    }
}
