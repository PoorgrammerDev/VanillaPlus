package io.github.poorgrammerdev.ominouswither.backend;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataType;

import io.github.poorgrammerdev.ominouswither.OminousWither;

/**
 * Detects when an Ominous Wither is fully spawned in and can begin moving (activates)
 */
public class ActivationDetector implements Listener {
    private final OminousWither plugin;

    public ActivationDetector(OminousWither plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles events that occur when the Ominous Wither is fully spawned
     * (i.e. when it blows up)
     * @param event
     */
    @EventHandler(ignoreCancelled = true)
    public void onFullySpawned(final EntityExplodeEvent event) {
        //Must be a Wither
        if (event.getEntityType() != EntityType.WITHER || !(event.getEntity() instanceof Wither)) return;

        //Must be Ominous
        final Wither wither = (Wither) event.getEntity();
        if (!this.plugin.isOminous(wither)) return;

        //Wither can cause explosions after it's spawned, we only want this to trigger once
        //Check if it's already fully spawned
        if (wither.getPersistentDataContainer().getOrDefault(this.plugin.getIsFullySpawnedKey(), PersistentDataType.BOOLEAN, false)) return;

        //*** Fire Event ***
        this.plugin.getServer().getPluginManager().callEvent(new OminousWitherActivateEvent(wither));
    }
    
}
