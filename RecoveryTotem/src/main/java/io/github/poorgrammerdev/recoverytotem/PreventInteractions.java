package io.github.poorgrammerdev.recoverytotem;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents unwanted interactions associated with the base item.
 * @author Thomas Tran
 */
public class PreventInteractions implements Listener {
    private final RecoveryTotem plugin;

    public PreventInteractions(final RecoveryTotem plugin) {
        this.plugin = plugin;
    }

    /**
     * Prevent any possible anvil interactions
     */
    @EventHandler(ignoreCancelled = true)
    public void onAnvilUse(final PrepareAnvilEvent event) {
        // Item cannot be used in an anvil
        for (final ItemStack item : event.getInventory().getStorageContents()) {
            if (this.plugin.isTotem(item)) {
                event.setResult(null);
            }
        }
    }

    /**
     * Prevent any possible grindstone interactions
     */
    @EventHandler(ignoreCancelled = true)
    public void onGrindstoneUse(final PrepareGrindstoneEvent event) {
        // Item cannot be used in a grindstone
        for (final ItemStack item : event.getInventory().getStorageContents()) {
            if (this.plugin.isTotem(item)) {
                event.setResult(null);
            }
        }
    }

    /**
     * Prevent right-click interaction on a Chiseled Bookshelf
     */
    @EventHandler(ignoreCancelled = true)
    public void onInteract(final PlayerInteractEvent event) {
        // Must be a right click on a Chiseled Bookshelf
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.CHISELED_BOOKSHELF) return;

        final ItemStack item = event.getItem();
        if (!this.plugin.isTotem(item)) return;

        event.setUseInteractedBlock(Result.DENY);
        event.setUseItemInHand(Result.DEFAULT);
    }

}
