package io.github.poorgrammerdev.ominouswither.coroutines;

import java.util.UUID;
import java.util.function.Predicate;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.ICoroutine;
import io.github.poorgrammerdev.ominouswither.utils.ParticleInfo;

/**
 * Continuously displays a TRACKING particle following an entity until a stop condition is reached
 * @author Thomas Tran
 */
public class PersistentTrackingParticle implements ICoroutine {
    private final OminousWither plugin;
    private final Predicate<UUID> stopCondition;
    private final UUID entityID;
    private final Vector offset;
    private final ParticleInfo particleInfo;

    /**
     * Constructor
     * @param stopCondition when to stop spawning the particle
     * @param entityID entity to track
     * @param particleInfo the particle itself along with other summoning details
     */
    public PersistentTrackingParticle(OminousWither plugin, Predicate<UUID> stopCondition, UUID entityID, Vector offset, ParticleInfo particleInfo) {
        this.plugin = plugin;
        this.stopCondition = stopCondition;
        this.entityID = entityID;
        this.offset = offset;
        this.particleInfo = particleInfo;
    }

    @Override
    public void tick() {
        final Entity entity = this.plugin.getServer().getEntity(this.entityID);
        if (entity == null) return;

        final World world = entity.getWorld();
        if (world != null) {
            world.spawnParticle(
                particleInfo.particle,
                entity.getLocation().add(this.offset),
                particleInfo.count,
                particleInfo.offsetX,
                particleInfo.offsetY,
                particleInfo.offsetZ,
                particleInfo.extra,
                particleInfo.data
            );
        }
    }

    @Override
    public boolean shouldBeRescheduled() {
        return !this.stopCondition.test(this.entityID);
    }
    
}
