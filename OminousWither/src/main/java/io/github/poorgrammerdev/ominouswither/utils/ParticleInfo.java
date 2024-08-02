package io.github.poorgrammerdev.ominouswither.utils;

import org.bukkit.Particle;

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

    public ParticleInfo(Particle particle, int count, double offsetX, double offsetY, double offsetZ, double extra, Object data) {
        this.particle = particle;
        this.count = count;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.extra = extra;

        if (data == null || data.getClass().equals(particle.getDataType())) {
            this.data = data;
        }
        else throw new IllegalArgumentException();
    }
}
