package io.github.poorgrammerdev.paxel;

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
        craftingManager.registerAllRecipes();
        this.getServer().getPluginManager().registerEvents(craftingManager, this);

        final PaxelMechanism paxelMechanism = new PaxelMechanism(this, this.toolMapper);
        this.getServer().getPluginManager().registerEvents(paxelMechanism, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public NamespacedKey getPaxelKey() {
        return paxelKey;
    }

    public boolean isPaxel(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return (
            meta.getPersistentDataContainer().has(this.getPaxelKey(), PersistentDataType.BOOLEAN) &&
            meta.getPersistentDataContainer().get(this.getPaxelKey(), PersistentDataType.BOOLEAN)
        );
    }

    public ItemStack createPaxel(String tier) {
        final Material[] toolSet = this.toolMapper.getToolSet(tier);
        if (toolSet == null) return null;

        final String displayName = tier.charAt(0) + tier.substring(1).toLowerCase() + " Paxel";
        return new ItemBuilder(toolSet[ToolMapper.PICKAXE_INDEX])
            .setCustomModelData(111) //TODO: set in config
            .setName(ChatColor.WHITE + displayName)
            .setLore(ChatColor.GRAY + displayName) //The reason for setting the lore too is because the player can rename the tool
            .setPersistentData(this.getPaxelKey(), PersistentDataType.BOOLEAN, true)
            .build();
    }
}
