package io.github.poorgrammerdev.ominouswither;

import java.util.UUID;

import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherLoadEvent;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherPhaseChangeEndEvent;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherSpawnEvent;
import io.github.poorgrammerdev.ominouswither.coroutines.PersistentTrackingParticle;
import io.github.poorgrammerdev.ominouswither.internal.CoroutineManager;

/**
 * Displays the particle aura around the Ominous Wither
 * @author Thomas Tran
 */
public class OminousAura implements Listener {
    private final OminousWither plugin;

    public OminousAura(final OminousWither plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onOminousSpawn(final OminousWitherSpawnEvent event) {
        this.runPhaseOneParticle(event.getWither().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    private void onSecondPhaseActivate(final OminousWitherPhaseChangeEndEvent event) {
        this.runPhaseTwoParticle(event.getWither().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onLoad(final OminousWitherLoadEvent event) {
        //If second phase began already, run second phase particle
        final Wither wither = event.getWither();
        if (wither.getPersistentDataContainer().getOrDefault(this.plugin.getSecondPhaseKey(), PersistentDataType.BOOLEAN, false)) {
            //Phase change animation must be complete
            if (wither.getInvulnerabilityTicks() <= 0) {
                this.runPhaseTwoParticle(wither.getUniqueId());
            }

            //If the animation isn't complete, then it will simply fire the End event when it is
            //And then the regular handler above will begin the particle
            return;
        }

        //Otherwise run Phase 1 particle
        this.runPhaseOneParticle(wither.getUniqueId());
    }

    private void runPhaseOneParticle(final UUID witherID) {
        //Constant ominous particle
        CoroutineManager.getInstance().enqueue(new PersistentTrackingParticle(
            this.plugin,
            ((entityID) -> {
                //If cannot find entity -> cancel
                final Entity entity = plugin.getServer().getEntity(entityID);
                if (entity == null) return true;
                if (!(entity instanceof Wither)) return false;

                //If wither is dead or entered second phase -> cancel
                final Wither wither = (Wither) entity;
                return wither.isDead() || wither.getPersistentDataContainer().getOrDefault(plugin.getSecondPhaseKey(), PersistentDataType.BOOLEAN, false);
            }),
            witherID,
            new Vector(0, 1.5, 0),
            new ParticleInfo(Particle.RAID_OMEN, 3, 0.75, 1, 0.75, 0, null)
        ));
    }

    private void runPhaseTwoParticle(final UUID witherID) {
        //Constant ominous particle
        CoroutineManager.getInstance().enqueue(new PersistentTrackingParticle(
            this.plugin,
            ((entityID) -> {
                //If cannot find entity -> cancel
                final Entity entity = plugin.getServer().getEntity(entityID);
                if (entity == null) return true;
                if (!(entity instanceof Wither)) return false;

                //If wither is dead -> cancel
                final Wither wither = (Wither) entity;
                return wither.isDead();
            }),
            witherID,
            new Vector(0, 1.5, 0),
            new ParticleInfo(Particle.TRIAL_OMEN, 3, 0.75, 1, 0.75, 0, null)
        ));
    }
}
