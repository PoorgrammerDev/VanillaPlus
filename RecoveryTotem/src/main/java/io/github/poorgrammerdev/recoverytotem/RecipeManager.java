package io.github.poorgrammerdev.recoverytotem;

import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;

/**
 * Handles unlocking the crafting recipe
 * @author Thomas Tran
 */
public class RecipeManager implements Listener {
    private final NamespacedKey totemRecipeKey;
    private final NamespacedKey compassRecipeKey;

    public RecipeManager(final NamespacedKey totemRecipeKey) {
        this.totemRecipeKey = totemRecipeKey;
        this.compassRecipeKey = NamespacedKey.minecraft("recovery_compass");
    }

    // Behaviour: The recipe is unlocked at exactly the same time the Recovery Compass recipe is unlocked. 

    /**
     * Listens for Recovery Compass unlock, and unlocks Totem of Recovery
     */
    @EventHandler(ignoreCancelled = true)
    public void unlockToolRecipe(final PlayerRecipeDiscoverEvent event) {
        if (event.getRecipe().equals(compassRecipeKey)) {
            event.getPlayer().discoverRecipe(this.totemRecipeKey);
        }
    }
    
}
