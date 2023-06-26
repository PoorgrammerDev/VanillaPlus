package io.github.poorgrammerdev.paxel;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;

/**
 * Handles unlocking the crafting recipes for Paxels
 * @author Thomas Tran
 */
public class RecipeManager implements Listener {
    private final ToolMapper toolMapper;
    private final HashMap<String, NamespacedKey> paxelRecipeMap;
    private final HashMap<NamespacedKey, Material> toolKeys;

    /*
     * Behaviour:
     * The recipe for a given paxel is unlocked at the same time as
     * when any of its consittuent tools are unlocked.
     * 
     * So when a player unlocks the recipe for an iron pickaxe, they also unlock for iron paxel.
     */

    public RecipeManager(ToolMapper toolMapper, HashMap<String, NamespacedKey> paxelRecipeMap) {
        this.toolMapper = toolMapper;
        this.paxelRecipeMap = paxelRecipeMap;
        this.toolKeys = new HashMap<>();

        //Populates map all constituent tools
        Tag.ITEMS_AXES.getValues().forEach(tool -> {toolKeys.put(tool.getKey(), tool);});
        Tag.ITEMS_PICKAXES.getValues().forEach(tool -> {toolKeys.put(tool.getKey(), tool);});
        Tag.ITEMS_SHOVELS.getValues().forEach(tool -> {toolKeys.put(tool.getKey(), tool);});
    }

    /**
     * Listens for constituent tool recipe unlock, and unlocks paxel of that tier
     */
    @EventHandler
    public void unlockToolRecipe(PlayerRecipeDiscoverEvent event) {
        if (event.isCancelled()) return;
        if (!toolKeys.containsKey(event.getRecipe())) return;

        final String tier = toolMapper.getToolTier(toolKeys.get(event.getRecipe()));
        if (tier == null || !paxelRecipeMap.containsKey(tier)) return;

        event.getPlayer().discoverRecipe(paxelRecipeMap.get(tier));
    }
    
}
