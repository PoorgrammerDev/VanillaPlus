package io.github.poorgrammerdev.paxel;


import java.util.ArrayList;

import org.bukkit.Bukkit;
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
        }
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
     * Paxels cannot be used in the recipe above to craft new paxels
     */
    @EventHandler
    public void preventPaxelRecrafting(PrepareItemCraftEvent event) {
        // Prevent conflict with paxel repair
        if (event.isRepair()) return;

        final ItemStack result = event.getInventory().getResult();

        //Make sure the item to be crafted is a paxel
        if (!plugin.isPaxel(result)) return;

        //Loop through the ingredients and make sure none of the tools are paxels
        final ItemStack[] matrix = event.getInventory().getMatrix();
        for (int i = 0; i < matrix.length; ++i) {
            if (plugin.isPaxel(matrix[i])) {
                event.getInventory().setResult(null);
                return;
            }            
        }
    }

    /**
     * Paxels can be combined with each other to repair, like regular items
     * Paxels cannot be combined with regular tools to repair the regular tool
     */
    @EventHandler
    public void paxelRepairing(PrepareItemCraftEvent event) {
        if (!event.isRepair()) return;

        //Find the two tools being repaired together
        final ArrayList<ItemStack> tools = new ArrayList<>();
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient != null) {
                tools.add(ingredient);
            }
        }

        if (tools.size() != 2) return;

        final boolean isPaxel1 = plugin.isPaxel(tools.get(0));
        final boolean isPaxel2 = plugin.isPaxel(tools.get(1));

        //Both tools are paxels -> perform paxel repair
        if (isPaxel1 && isPaxel2) {
            //Convert the final result to a paxel but collect the damage first
            final ItemStack result = event.getInventory().getResult();
            if (result != null && result.getItemMeta() instanceof Damageable) {
                Damageable meta = (Damageable) result.getItemMeta();

                //Create a new paxel based on the item tier
                final String tier = this.toolMapper.getToolTier(result.getType());
                if (tier != null) {
                    final ItemStack newPaxel = plugin.createPaxel(tier);

                    //Set the damage accordingly
                    if (newPaxel != null && newPaxel.getItemMeta() instanceof Damageable) {
                        Damageable paxelMeta = (Damageable) newPaxel.getItemMeta();

                        paxelMeta.setDamage(meta.getDamage());
                        newPaxel.setItemMeta(paxelMeta);
                        event.getInventory().setResult(newPaxel);
                        return;
                    }
                }
            }

            //Something went wrong -> disallow the repair
            event.getInventory().setResult(null);
        }

        //Mismatching types, cancel the operation
        else if (isPaxel1 ^ isPaxel2) {
            event.getInventory().setResult(null);
        } 

        //Otherwise repairing regular tools, continue as normal
    }


    //TODO: Grindstone and Anvil enforce no paxel-nonpaxel repairs
    //TODO: smithing table upgrade works but rename to Netherite Paxel


}
