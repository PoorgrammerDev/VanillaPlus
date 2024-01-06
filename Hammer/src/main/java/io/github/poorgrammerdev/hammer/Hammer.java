package io.github.poorgrammerdev.hammer;

import java.util.HashMap;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;

public class Hammer extends JavaPlugin {
    private final NamespacedKey hammerKey;
    
    public Hammer() {
        this.hammerKey = new NamespacedKey(this, "is_hammer");
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();

        final Random random = new Random();
        final HashMap<Material, NamespacedKey> recipeKeyMap = new CraftingManager(this).registerAllRecipes();

        final FauxBlockDamage fauxBlockDamage = new FauxBlockDamage(this, random);
        if (fauxBlockDamage.isEnabled()) {
            fauxBlockDamage.runTaskTimer(this, 0, 0);
            this.getServer().getPluginManager().registerEvents(fauxBlockDamage, this);
        }

        this.getServer().getPluginManager().registerEvents(new HammerMechanism(this, random, fauxBlockDamage), this);
        this.getServer().getPluginManager().registerEvents(new RepairingManager(this), this);
        this.getServer().getPluginManager().registerEvents(new RecipeManager(recipeKeyMap), this);

        GiveCommand giveCommand = new GiveCommand(this, recipeKeyMap);
        this.getCommand("givehammer").setExecutor(giveCommand);
        this.getCommand("givehammer").setTabCompleter(giveCommand);
    }

    @Override
    public void onDisable() {
    }
    
    /**
     * @return the NamespacedKey for validating that an item is a hammer
     */
    public NamespacedKey getHammerKey() {
        return this.hammerKey;
    }

    /**
     * Checks if an item is a hammer or not
     * @return if the item is a hammer or not
     */
    public boolean isHammer(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return (
            meta.getPersistentDataContainer().getOrDefault(this.getHammerKey(), PersistentDataType.BOOLEAN, false)
        );
    }

    /**
     * Creates a new hammer item from the base pickaxe material
     * @param pickaxe Base item to use for the hammer
     * @return Hammer item
     */
    public ItemStack createHammer(Material pickaxe) {
        if (!Tag.ITEMS_PICKAXES.isTagged(pickaxe)) return null;
        final String displayName = getHammerName(pickaxe);

        final ItemBuilder builder = new ItemBuilder(pickaxe)
            .setCustomModelData(this.getConfig().getInt("custom_model_data", 101))
            .setName(ChatColor.RESET + displayName)
            .setPersistentData(this.getHammerKey(), PersistentDataType.BOOLEAN, true);
        
        if (this.getConfig().getBoolean("write_description", true)) {
            //The reason for setting the lore too is because the player can rename the tool
            builder.setLore(ChatColor.GRAY + displayName);
        }

        return builder.build();
    }

    /**
     * Get the name of a hammer for a given tier
     * @param type The pickaxe base-item type for this tool
     * @return name of this tier's hammer
     */
    public String getHammerName(Material type) {
        String tier = type.toString().split("_")[0];

        return tier.charAt(0) + tier.substring(1).toLowerCase() + " Hammer";
    }
}
