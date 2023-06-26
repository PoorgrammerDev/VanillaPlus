package io.github.poorgrammerdev.paxel;


import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.Listener;

/**
 * This class handles the crafting of the different Paxel items.
 * @author Thomas Tran
 */
public class CraftingManager implements Listener {
    private final Paxel plugin;
    private final ToolMapper toolMapper;

    public CraftingManager(Paxel plugin, ToolMapper toolMapper) {
        this.plugin = plugin;
        this.toolMapper = toolMapper;
    }

    /**
     * Registers all craftable paxels
     */
    public void registerAllRecipes() {
        //Loops through every defined tool mineral tier and creates a paxel for each tier
        for (final String tier : toolMapper.getAllTiers()) {
            final NamespacedKey key = new NamespacedKey(plugin, tier.toLowerCase() + "_paxel");
            final Material[] toolSet = toolMapper.getToolSet(tier);

            //Error handling
            if (toolSet == null || toolSet.length == 0) {
                plugin.getLogger().warning("Crafting Manager received invalid tool set for mineral type " + tier);
                continue;
            }

            final String displayName = tier.charAt(0) + tier.substring(1).toLowerCase() + " Paxel";
            final ItemStack paxel =
                new ItemBuilder(toolSet[ToolMapper.PICKAXE_INDEX])
                .setCustomModelData(111) //TODO: set in config
                .setName(displayName)
                .setPersistentData(plugin.getPaxelKey(), PersistentDataType.BOOLEAN, true)
                .build();

            final ShapelessRecipe recipe = new ShapelessRecipe(key, paxel);

            for (final Material tool : toolSet) {
                recipe.addIngredient(tool);
            }

            recipe.setCategory(CraftingBookCategory.EQUIPMENT);
            plugin.getServer().addRecipe(recipe);
        }
    }

    //TODO: handle durability
}
