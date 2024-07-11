package io.github.poorgrammerdev.recoverytotem;

import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class RecoveryTotem extends JavaPlugin {
    private final NamespacedKey recoveryTotemKey;

    public RecoveryTotem() {
        super();

        this.recoveryTotemKey = new NamespacedKey(this, "is_recovery_totem");
    }

    @Override
    public void onEnable() {
        super.onEnable();

        this.saveDefaultConfig();
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();

        final ExternalItemManager externalItemManager = new ExternalItemManager(this);

        final CraftingManager craftingManager = new CraftingManager(this, externalItemManager);
        final NamespacedKey recipeKey = craftingManager.registerRecipe();
        this.getServer().getPluginManager().registerEvents(craftingManager, this);

        final RecipeManager recipeManager = new RecipeManager(recipeKey);
        this.getServer().getPluginManager().registerEvents(recipeManager, this);

        final TotemMechanism totemMechanism = new TotemMechanism(this);
        this.getServer().getPluginManager().registerEvents(totemMechanism, this);

        final PreventInteractions preventInteractions = new PreventInteractions(this);
        this.getServer().getPluginManager().registerEvents(preventInteractions, this);

        final GiveCommand giveCommand = new GiveCommand(this);
        this.getCommand("giverecoverytotem").setExecutor(giveCommand);
        this.getCommand("giverecoverytotem").setTabCompleter(giveCommand);
    }

    /**
     * @return the NamespacedKey for validating that an item is a recovery totem
     */
    public NamespacedKey getRecoveryTotemKey() {
        return recoveryTotemKey;
    }

    /**
     * Checks if an item is a Totem of Recovery or not
     * @return if the item is a Totem of Recovery or not
     */
    public boolean isTotem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return meta.getPersistentDataContainer().getOrDefault(this.recoveryTotemKey, PersistentDataType.BOOLEAN, false);
    }

    /**
     * Creates a Totem of Recovery item
     * @return Totem of Recovery itemstack
     */
    public ItemStack createTotem() {
        final ItemStack totem = new ItemStack(Material.ENCHANTED_BOOK);
        
        final ItemMeta meta = totem.getItemMeta();
        if (meta == null) return null;

        // TODO: find a way to remove the enchantment storage component on the item
        meta.setItemName("Totem of Recovery");
        meta.setCustomModelData(this.getConfig().getInt("custom_model_data", 100));
        meta.getPersistentDataContainer().set(this.recoveryTotemKey, PersistentDataType.BOOLEAN, true);

        if (this.getConfig().getBoolean("write_description", true)) {
            meta.setLore(Arrays.asList(
                ChatColor.GRAY.toString() + "Upon a single death, if this",
                ChatColor.GRAY.toString() + "totem is present anywhere",
                ChatColor.GRAY.toString() + "within your inventory, your",
                ChatColor.GRAY.toString() + "items shall follow you",
                ChatColor.GRAY.toString() + "into the next life."
            ));
        }

        totem.setItemMeta(meta);
        return totem;
    }
}
