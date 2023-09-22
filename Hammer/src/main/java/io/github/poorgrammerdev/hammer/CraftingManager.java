package io.github.poorgrammerdev.hammer;

import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import org.bukkit.event.Listener;

/**
 * Handles the crafting of the different Hammer items.
 * @author Thomas Tran
 */
public class CraftingManager implements Listener {
    private final Hammer plugin;

    public CraftingManager(Hammer plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers all craftable hammers
     */
    public HashMap<Material, NamespacedKey> registerAllRecipes() {
        final HashMap<Material, NamespacedKey> ret = new HashMap<Material, NamespacedKey>();

        //Get the mapping from the material to the pickaxe
        final HashMap<RecipeChoice, Material> craftMap = getMineralsToToolsMap();

        //Loops through every defined tool mineral tier and creates a hammer for each tier
        for (final RecipeChoice choice : craftMap.keySet()) {
            final Material resultType = craftMap.get(choice);

            final String tier = resultType.name().split("_")[0];
            final NamespacedKey key = new NamespacedKey(plugin, tier.toLowerCase() + "_hammer");

            final ItemStack hammer = plugin.createHammer(resultType);
            final ShapedRecipe recipe = new ShapedRecipe(key, hammer);
            recipe.shape(
                "***",
                "*|*",
                " | "
            );

            recipe.setIngredient('*', choice);
            recipe.setIngredient('|', Material.STICK);

            recipe.setCategory(CraftingBookCategory.EQUIPMENT);
            plugin.getServer().addRecipe(recipe);
            ret.put(resultType, key);
        }

        return ret;
    }

    /**
     * Gets a map of material choices to craft a pickaxe to the pickaxe it crafts
     */
    private HashMap<RecipeChoice, Material> getMineralsToToolsMap() {
        final HashMap<RecipeChoice, Material> craftMap = new HashMap<>();

        //This iterates through all of the registered recipes in the server and finds only the pickaxe recipes
        final Iterator<Recipe> iterator = this.plugin.getServer().recipeIterator();
        while (iterator.hasNext()) {
            final Recipe recipe = iterator.next();

            //Filtering out by pickaxe recipes
            if (recipe.getResult() == null || !Tag.ITEMS_PICKAXES.isTagged(recipe.getResult().getType()) || !(recipe instanceof ShapedRecipe)) continue;

            //Only default recipes are read, ignoring custom plugin recipes
            final ShapedRecipe shaped = (ShapedRecipe) recipe;
            if (!shaped.getKey().getNamespace().equals(NamespacedKey.MINECRAFT)) continue;
                
            craftMap.put(
                //Here we're getting the first item in the recipe (top left corner), which by the pickaxe recipe is the material comprising the head of the pickaxe
                //Since ShapedRecipes' shapes are denoted by character, we first get the char and then lookup the RecipeChoice
                shaped.getChoiceMap().get(shaped.getShape()[0].charAt(0)),

                //Then we pair the mineral with the tool of the same type in the returned map
                shaped.getResult().getType()
            );
        }

        return craftMap;
    }

}
