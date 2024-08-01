package io.github.poorgrammerdev.ominouswither.coroutines;

import java.util.function.BooleanSupplier;

import org.bukkit.Location;
import org.bukkit.World;

import io.github.poorgrammerdev.ominouswither.ParticleInfo;
import io.github.poorgrammerdev.ominouswither.internal.ICoroutine;

/**
 * Continuously displays a STATIONARY particle at a location until a stop condition is reached
 * @author Thomas Tran
 */
public class PersistentParticle implements ICoroutine {
    private final BooleanSupplier stopCondition;
    private final Location location;
    private final ParticleInfo particleInfo;

    /**
     * Constructor
     * @param stopCondition when to stop spawning the particle
     * @param location where to summon the particle at
     * @param particleInfo the particle itself along with other summoning details
     */
    public PersistentParticle(BooleanSupplier stopCondition, Location location, ParticleInfo particleInfo) {
        this.stopCondition = stopCondition;
        this.location = location;
        this.particleInfo = particleInfo;
    }

    @Override
    public void tick() {
        final World world = location.getWorld();
        if (world != null) {
            world.spawnParticle(
                particleInfo.particle,
                location,
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
        return !this.stopCondition.getAsBoolean();
    }
    
}
