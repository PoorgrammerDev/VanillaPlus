package io.github.poorgrammerdev.paxel;

import java.util.Arrays;
import java.util.HashMap;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.ToolComponent;
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
        this.saveDefaultConfig();
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();

        final ExternalItemManager externalItemManager = new ExternalItemManager(this);

        final CraftingManager craftingManager = new CraftingManager(this, this.toolMapper, externalItemManager);
        final HashMap<String, NamespacedKey> paxelRecipeMap = craftingManager.registerAllRecipes();
        this.getServer().getPluginManager().registerEvents(craftingManager, this);

        final RepairingManager repairingManager = new RepairingManager(this, toolMapper);
        this.getServer().getPluginManager().registerEvents(repairingManager, this);

        final PaxelSpecialActions paxelSpecialActions = new PaxelSpecialActions(this, this.toolMapper);
        this.getServer().getPluginManager().registerEvents(paxelSpecialActions, this);

        final RecipeManager recipeManager = new RecipeManager(toolMapper, paxelRecipeMap);
        this.getServer().getPluginManager().registerEvents(recipeManager, this);

        final GiveCommand giveCommand = new GiveCommand(this, toolMapper);
        this.getCommand("givepaxel").setExecutor(giveCommand);
        this.getCommand("givepaxel").setTabCompleter(giveCommand);
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

        final Double speedObj = this.toolMapper.getSpeed(tier);
        if (speedObj == null) return null;
        
        final String incorrectTagStr = this.toolMapper.getIncorrectTag(tier);
        if (incorrectTagStr == null) return null;

        final Tag<Material> incorrectTag = this.getServer().getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft(incorrectTagStr), Material.class);
        if (incorrectTag == null) {
            this.getLogger().warning("Invalid tag \"" + incorrectTagStr + "\" for tier \"" + tier + "\"!");
            return null;
        }
        
        final Float speed = (float) speedObj.doubleValue();
        final String name = getPaxelName(tier);

        final ItemStack paxel = new ItemStack(toolSet[ToolMapper.PICKAXE_INDEX]);
        final ItemMeta meta = paxel.getItemMeta();
        if (meta == null) throw new IllegalStateException("ItemMeta cannot be null!");
        
        meta.setCustomModelData(this.getConfig().getInt("custom_model_data", 100));
        meta.setItemName(name);
        meta.getPersistentDataContainer().set(this.getPaxelKey(), PersistentDataType.BOOLEAN, true);

        //Component-based Paxel mechanism
        final ToolComponent tool = meta.getTool();

        tool.addRule(incorrectTag, speed, false); //NOTE: this must go before the other rules
        tool.addRule(Tag.MINEABLE_AXE, speed, true);
        tool.addRule(Tag.MINEABLE_PICKAXE, speed, true);
        tool.addRule(Tag.MINEABLE_SHOVEL, speed, true);

        tool.setDamagePerBlock(1);
        meta.setTool(tool);
        
        if (this.getConfig().getBoolean("write_description", true)) {
            //The reason for setting the lore too is because the player can rename the tool
            meta.setLore(Arrays.asList(ChatColor.GRAY + name));
        }

        paxel.setItemMeta(meta);
        return paxel;
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
