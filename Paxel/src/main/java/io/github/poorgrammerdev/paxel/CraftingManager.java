package io.github.poorgrammerdev.paxel;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;

/**
 * Handles the crafting of the different Paxel items.
 * @author Thomas Tran
 */
public class CraftingManager implements Listener {
    private final Paxel plugin;
    private final ToolMapper toolMapper;
    private final ExternalItemManager externalItemManager;

    public CraftingManager(Paxel plugin, ToolMapper toolMapper, ExternalItemManager externalItemManager) {
        this.plugin = plugin;
        this.toolMapper = toolMapper;
        this.externalItemManager = externalItemManager;
    }

    /**
     * Registers all craftable paxels
     */
    public HashMap<String, NamespacedKey> registerAllRecipes() {
        final HashMap<String, NamespacedKey> ret = new HashMap<String, NamespacedKey>();

        //Loops through every defined tool mineral tier and creates a paxel for each tier
        for (final String tier : toolMapper.getAllTiers()) {
            final NamespacedKey key = new NamespacedKey(plugin, tier.toLowerCase() + "_paxel");
            final Material[] toolSet = toolMapper.getToolSet(tier);

            //Error handling
            if (toolSet == null || toolSet.length == 0) {
                plugin.getLogger().warning("Crafting Manager received invalid tool set for mineral type " + tier);
                continue;
            }

            final ItemStack paxel = plugin.createPaxel(tier);
            final ShapedRecipe recipe = new ShapedRecipe(key, paxel);
            recipe.shape(
                "@#*",
                " | ",
                " | "
            );

            recipe.setIngredient('|', Material.STICK);
            recipe.setIngredient('@', toolSet[ToolMapper.AXE_INDEX]);
            recipe.setIngredient('#', toolSet[ToolMapper.PICKAXE_INDEX]);
            recipe.setIngredient('*', toolSet[ToolMapper.SHOVEL_INDEX]);

            recipe.setCategory(CraftingBookCategory.EQUIPMENT);
            plugin.getServer().addRecipe(recipe);
            ret.put(tier, key);
        }

        return ret;
    }

    /**
     * Paxels crafted from broken base tools will have the average durability of all three items
     */
    @EventHandler
    public void handleDurability(PrepareItemCraftEvent event) {
        // Prevent conflict with paxel repair
        if (event.isRepair()) return;

        final ItemStack result = event.getInventory().getResult();

        //Make sure the item to be crafted is a paxel
        if (!plugin.isPaxel(result)) return;
        if (!(result.getItemMeta() instanceof Damageable)) return;

        final Damageable meta = (Damageable) result.getItemMeta();
        final int[] damages = new int[3];
        final ItemStack[] matrix = event.getInventory().getMatrix();

        //For the three tool ingredients at the top, iterate through them and collect their damages
        for (int i = 0; i < 3 && i < matrix.length; i++) {
            if (matrix[i] == null) continue;

            if (matrix[i].getItemMeta() instanceof Damageable) {
                final Damageable damageable = (Damageable) matrix[i].getItemMeta();
                damages[i] = damageable.getDamage();
            }
        }

        //Average the collected damages and apply them to the resulting paxel
        final int averageDamage = (damages[0] + damages[1] + damages[2]) / 3;
        meta.setDamage(averageDamage);
        result.setItemMeta(meta);
    }

    /**
     * Paxels cannot be used in the base recipe above to craft new paxels
     */
    @EventHandler
    public void preventPaxelRecrafting(PrepareItemCraftEvent event) {
        // Prevent conflict with paxel repair
        if (event.isRepair()) return;

        final ItemStack result = event.getInventory().getResult();

        //Make sure the item to be crafted is a paxel
        if (!plugin.isPaxel(result)) return;

        //Loop through the ingredients and make sure none of the tools are paxels or some conflicting external tool
        final ItemStack[] matrix = event.getInventory().getMatrix();
        for (int i = 0; i < matrix.length; ++i) {
            if (plugin.isPaxel(matrix[i]) || this.externalItemManager.isExternalItem(matrix[i])) {
                event.getInventory().setResult(null);
                return;
            }            
        }
    }

}
