package io.github.poorgrammerdev.ominouswither;

import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.backend.CoroutineManager;
import io.github.poorgrammerdev.ominouswither.backend.OminousWitherLoadEvent;
import io.github.poorgrammerdev.ominouswither.backend.OminousWitherSpawnEvent;
import io.github.poorgrammerdev.ominouswither.coroutines.PersistentTrackingParticle;

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
        this.runParticle(event.getWither());
    }

    @EventHandler(ignoreCancelled = true)
    public void onLoad(final OminousWitherLoadEvent event) {
        this.runParticle(event.getWither());
    }

    private void runParticle(final Wither wither) {
        //Constant ominous particle
        CoroutineManager.getInstance().enqueue(new PersistentTrackingParticle(
            this.plugin,
            ((entityID) -> {
                final Entity entity = plugin.getServer().getEntity(entityID);
                if (entity == null) return true;
                if (!(entity instanceof LivingEntity)) return false;

                final LivingEntity livingEntity = (LivingEntity) entity;
                return livingEntity.isDead();
            }),
            wither.getUniqueId(),
            new Vector(0, 1, 0),
            new ParticleInfo(Particle.RAID_OMEN, 3, 0.75, 1, 0.75, 0, null)
        ));
    }
}
