package io.github.poorgrammerdev.hammer;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;

/**
 * Handles unlocking the crafting recipes for Hammers
 * @author Thomas Tran
 */
public class RecipeManager implements Listener {
    /**
     * Maps vanilla pickaxe recipe key to corresponding hammer recipe key of the same type
     * e.g. minecraft:iron_pickaxe -> hammer:iron_hammer
     */
    private final HashMap<NamespacedKey, NamespacedKey> recipeMapper;

    /*
     * Behaviour:
     * The recipe for a given hammer is unlocked at the same time as
     * when the pickaxe of the same type is unlocked.
     * 
     * So when a player unlocks the recipe for an iron pickaxe, they also unlock for iron hammer.
     */

    public RecipeManager(HashMap<Material, NamespacedKey> hammerRecipeMap) {
        this.recipeMapper = new HashMap<>();

        //Creates mapping from pick -> hammer
        for (Material pick : Tag.ITEMS_PICKAXES.getValues()) {
            this.recipeMapper.put(
                pick.getKey(),
                hammerRecipeMap.get(pick)
            );
        }
    }

    /**
     * Listens for correspnding pickaxe recipe unlock, and unlocks hammer of that tier
     */
    @EventHandler(ignoreCancelled = true)
    public void unlockToolRecipe(PlayerRecipeDiscoverEvent event) {
        if (!recipeMapper.containsKey(event.getRecipe())) return;

        event.getPlayer().discoverRecipe(recipeMapper.get(event.getRecipe()));
    }
    
}
