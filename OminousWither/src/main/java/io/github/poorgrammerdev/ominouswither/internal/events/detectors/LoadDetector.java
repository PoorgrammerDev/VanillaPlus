package io.github.poorgrammerdev.ominouswither.internal.events.detectors;

import org.bukkit.World;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherLoadEvent;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherUnloadEvent;

/**
 * <p>Handles firing events for Ominous Withers loading and unloading</p>
 * <p>Fires events {@link OminousWitherLoadEvent} and {@link OminousWitherUnloadEvent}</p>
 * @author Thomas Tran
 */
public class LoadDetector implements Listener {
    private final OminousWither plugin;

    public LoadDetector(OminousWither plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles Ominous Withers being loaded
     */
    @EventHandler(ignoreCancelled = true)
    private void onLoad(EntitiesLoadEvent event) {
        event.getEntities()
            .stream()
            .filter((entity) -> (entity instanceof Wither))
            .map((entity) -> (Wither) entity)
            .filter((wither) -> (this.plugin.isOminous(wither)))
            .forEach((wither) -> {this.plugin.getServer().getPluginManager().callEvent(new OminousWitherLoadEvent(wither));});
    }
    
    /**
     * Handles Ominous Withers going into unloaded chunks
     */
    @EventHandler(ignoreCancelled = true)
    private void onUnload(EntitiesUnloadEvent event) {
        event.getEntities()
            .stream()
            .filter((entity) -> (entity instanceof Wither))
            .map((entity) -> (Wither) entity)
            .filter((wither) -> (this.plugin.isOminous(wither)))
            .forEach((wither) -> {this.plugin.getServer().getPluginManager().callEvent(new OminousWitherUnloadEvent(wither));});
    }

    /**
     * <p>In case the plugin was /reloaded, call the load event for existing already-loaded Ominous Withers</p>
     * <p>The /reload case is the currently only known case that requires this method</p>
     * <p><strong>IMPORTANT</strong>: Call this method AFTER all listeners have been registered in the onEnable!</p>
     */
    public void onPluginEnable() {
        for (final World world : this.plugin.getServer().getWorlds()) {
            world.getEntitiesByClass(Wither.class)
                .stream()
                .filter((entity) -> (entity instanceof Wither))
                .map((entity) -> (Wither) entity)
                .filter((wither) -> (this.plugin.isOminous(wither)))
                .forEach((wither) -> {this.plugin.getServer().getPluginManager().callEvent(new OminousWitherLoadEvent(wither));});
            ;
        }
    }
}
