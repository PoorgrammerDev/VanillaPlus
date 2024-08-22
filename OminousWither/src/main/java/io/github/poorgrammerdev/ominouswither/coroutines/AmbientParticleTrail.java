package io.github.poorgrammerdev.ominouswither.coroutines;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.internal.ICoroutine;
import io.github.poorgrammerdev.ominouswither.utils.ParticleInfo;

/**
 * Represents an ambient particle trail
 * @author Thomas Tran
 */
public class AmbientParticleTrail implements ICoroutine {
    private final World world;
    private final Vector velocity;
    private final ParticleInfo particle;
    private final int lifespan;

    private Location location;
    private int i;

    public AmbientParticleTrail(World world, Location location, Vector velocity, ParticleInfo particle, int lifespan) {
        this.world = world;
        this.velocity = velocity;
        this.particle = particle;
        this.lifespan = lifespan;

        this.location = location;
        this.i = 0;
    }

    @Override
    public boolean tick() {
        //Reached end of life or impassable block, cancel
        if (i >= lifespan || !location.getBlock().isPassable()) return false;

        //Otherwise keep moving and display particle trail
        particle.spawnParticle(world, location);
        this.location.add(this.velocity);

        //Increment ticker
        ++i;
        return true;
    }
    
}
