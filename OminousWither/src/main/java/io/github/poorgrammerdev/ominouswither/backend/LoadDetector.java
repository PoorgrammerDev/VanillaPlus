package io.github.poorgrammerdev.ominouswither.backend;

import org.bukkit.World;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

import io.github.poorgrammerdev.ominouswither.OminousWither;

/**
 * Handles firing events for Ominous Withers loading and unloading
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
    public void onLoad(EntitiesLoadEvent event) {
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
    public void onUnload(EntitiesUnloadEvent event) {
        event.getEntities()
            .stream()
            .filter((entity) -> (entity instanceof Wither))
            .map((entity) -> (Wither) entity)
            .filter((wither) -> (this.plugin.isOminous(wither)))
            .forEach((wither) -> {this.plugin.getServer().getPluginManager().callEvent(new OminousWitherUnloadEvent(wither));});
    }

    /**
     * In case the plugin was /reloaded (which is currently the only known case that would require this)
     * existing already-loaded Ominous Withers must be recognized by the system
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
