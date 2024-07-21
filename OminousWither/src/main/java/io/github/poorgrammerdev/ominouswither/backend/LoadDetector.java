package io.github.poorgrammerdev.ominouswither.backend;

import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

import io.github.poorgrammerdev.ominouswither.OminousWither;

public class LoadDetector implements Listener {
    private final OminousWither plugin;

    public LoadDetector(OminousWither plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onLoad(EntitiesLoadEvent event) {
        event.getEntities()
            .stream()
            .filter((entity) -> (entity instanceof Wither))
            .map((entity) -> (Wither) entity)
            .filter((wither) -> (this.plugin.isOminous(wither)))
            .forEach((wither) -> {this.plugin.getServer().getPluginManager().callEvent(new OminousWitherLoadEvent(wither));});
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onUnload(EntitiesUnloadEvent event) {
        event.getEntities()
            .stream()
            .filter((entity) -> (entity instanceof Wither))
            .map((entity) -> (Wither) entity)
            .filter((wither) -> (this.plugin.isOminous(wither)))
            .forEach((wither) -> {this.plugin.getServer().getPluginManager().callEvent(new OminousWitherUnloadEvent(wither));});
    }
}
