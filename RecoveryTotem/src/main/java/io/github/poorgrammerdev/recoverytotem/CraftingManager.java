package io.github.poorgrammerdev.recoverytotem;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;

/**
 * Handles the crafting of the Totem of Recovery.
 * @author Thomas Tran
 */
public class CraftingManager implements Listener {
    private final RecoveryTotem plugin;
    private final ExternalItemManager externalItemManager;

    public CraftingManager(RecoveryTotem plugin, ExternalItemManager externalItemManager) {
        this.plugin = plugin;
        this.externalItemManager = externalItemManager;
    }

    /**
     * Registers the crafting recipe
     * @return The key that the recipe is registered under
     */
    public NamespacedKey registerRecipe() {
        final NamespacedKey key = new NamespacedKey(this.plugin, "totem_of_recovery");
        final ShapedRecipe recipe = new ShapedRecipe(key, this.plugin.createTotem());

        //The recipe is a Totem of Undying surrounded in 8 Echo Shards
        recipe.shape(
            "eee",
            "eTe",
            "eee"
        );

        recipe.setIngredient('e', Material.ECHO_SHARD);
        recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);
        
        recipe.setCategory(CraftingBookCategory.MISC);
        this.plugin.getServer().addRecipe(recipe);
        return key;
    }

    /**
     * Implements External Item blocklist
     */
    @EventHandler
    public void preventExternalItems(PrepareItemCraftEvent event) {
        final ItemStack result = event.getInventory().getResult();

        //Make sure the item to be crafted is a Totem of Recovery
        if (!this.plugin.isTotem(result)) return;

        //Loop through the ingredients and make sure none of the items are any conflicting external item
        final ItemStack[] matrix = event.getInventory().getMatrix();
        for (int i = 0; i < matrix.length; ++i) {
            if (this.externalItemManager.isExternalItem(matrix[i])) {
                event.getInventory().setResult(null);
                return;
            }            
        }
    }

}
