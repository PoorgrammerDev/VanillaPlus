package io.github.poorgrammerdev.ominouswither.internal;

import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherActivateEvent;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherPhaseChangeBeginEvent;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherSpawnEvent;

/**
 * <p>Tags Ominous Withers with important PDC tags</p>
 * @author Thomas Tran
 */
public class WitherPDCTagger implements Listener {
    private final OminousWither plugin;

    public WitherPDCTagger(OminousWither plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    private void onSpawn(final OminousWitherSpawnEvent event) {
        final Wither wither = event.getWither();

        //Tag Wither entity as Ominous and other important info
        wither.getPersistentDataContainer().set(this.plugin.getOminousWitherKey(), PersistentDataType.BOOLEAN, true);
        wither.getPersistentDataContainer().set(this.plugin.getLevelKey(), PersistentDataType.INTEGER, event.getLevel());
        wither.getPersistentDataContainer().set(this.plugin.getSpawnerKey(), PersistentDataType.STRING, event.getSpawner().getUniqueId().toString());
    }

    @EventHandler(ignoreCancelled = true)
    private void onActivate(final OminousWitherActivateEvent event) {
        //Mark as fully spawned
        event.getWither().getPersistentDataContainer().set(this.plugin.getIsFullySpawnedKey(), PersistentDataType.BOOLEAN, true);
    }

    @EventHandler(ignoreCancelled = true)
    private void onPhaseChangeBegin(final OminousWitherPhaseChangeBeginEvent event) {
        //Tag the wither with the second phase key
        event.getWither().getPersistentDataContainer().set(this.plugin.getSecondPhaseKey(), PersistentDataType.BOOLEAN, true);
    }
    
}
