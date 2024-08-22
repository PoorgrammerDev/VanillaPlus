package io.github.poorgrammerdev.ominouswither.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Dataclass/struct for holding a particle and info pertaining to its spawning
 * @author Thomas Tran
 */
public class ParticleInfo {
    public final Particle particle;
    public final int count;
    public final double offsetX;
    public final double offsetY;
    public final double offsetZ;
    public final double extra;
    public final Object data;
    public final boolean force;

    public ParticleInfo(Particle particle, int count, double offsetX, double offsetY, double offsetZ, double extra, Object data, boolean force) {
        this.particle = particle;
        this.count = count;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.extra = extra;

        if (data == null || particle.getDataType().isInstance(data)) {
            this.data = data;
        }
        else throw new IllegalArgumentException("Unexpected type for particle data");

        this.force = force;
    }

    public ParticleInfo(Particle particle, int count, double offsetX, double offsetY, double offsetZ, double extra, Object data) {
        this(particle, count, offsetX, offsetY, offsetZ, extra, data, false);
    }

    public ParticleInfo(Particle particle, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        this(particle, count, offsetX, offsetY, offsetZ, extra, null, false);
    }

    public ParticleInfo(Particle particle, int count, double offsetX, double offsetY, double offsetZ) {
        this(particle, count, offsetX, offsetY, offsetZ, 0, null, false);
    }

    public ParticleInfo(Particle particle, int count) {
        this(particle, count, 0, 0, 0, 0, null, false);
    }

    /**
     * Convenience method for spawning this particle somewhere
     * @param world world to spawn in
     * @param location location to spawn at
     */
    public void spawnParticle(final World world, final Location location) {
        world.spawnParticle(
            this.particle,
            location,
            this.count,
            this.offsetX,
            this.offsetY,
            this.offsetZ,
            this.extra,
            this.data,
            this.force
        );
    }

    /**
     * Convenience method for spawning this particle somewhere to a single recipient
     * @param player player to show particle
     * @param location location to spawn at
     */
    public void spawnParticle(final Player player, final Location location) {
        player.spawnParticle(
            this.particle,
            location,
            this.count,
            this.offsetX,
            this.offsetY,
            this.offsetZ,
            this.extra,
            this.data,
            this.force
        );
    }
}
