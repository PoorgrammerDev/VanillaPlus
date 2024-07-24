package io.github.poorgrammerdev.ominouswither.coroutines;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.ParticleInfo;
import io.github.poorgrammerdev.ominouswither.backend.ICoroutine;

public class ScoutingParticle implements ICoroutine {
    private static final double MIN_DISTANCE = 1.0D;

    private final OminousWither plugin;
    private final UUID entityID;
    private final ParticleInfo particleInfo;
    private final double turningRadius;
    private final double speed;

    private Location currentLocation;

     /**
      * Constructor
      * @param plugin instance of main plugin class
      * @param startingLocation where the particle starts
      * @param entityID id of entity to move towards
      * @param particleInfo particle
      * @param turningRadius determines how much the particle can turn in a single tick
      * @param speed how much the particle can move in a tick
      */
    public ScoutingParticle(OminousWither plugin, Location startingLocation, UUID entityID, ParticleInfo particleInfo,
            double turningRadius, double speed) {
        this.plugin = plugin;
        this.entityID = entityID;
        this.particleInfo = particleInfo;
        this.turningRadius = turningRadius;
        this.speed = speed;

        this.currentLocation = startingLocation;
    }

    @Override
    public void tick() {
        //TODO: sign of a bad architecture? should tick itself return if it should be rescheduled or not instead of having two diff. methods?
        return;
    }

    @Override
    public boolean shouldBeRescheduled() {
        //Entity must still exist
        final Entity entity = this.plugin.getServer().getEntity(this.entityID);
        if (entity == null || entity.isDead()) return false;

        final Location targetLocation = entity.getLocation();

        //Sanity check -- worlds must exist and match
        final World currentWorld = currentLocation.getWorld();
        final World targetWorld = targetLocation.getWorld();
        if (currentWorld == null || targetWorld == null || !currentWorld.equals(targetWorld)) return false;

        //Within distance of entity -> done
        final double distance = this.currentLocation.distance(targetLocation);
        if (distance <= MIN_DISTANCE) return false;

        //Display particle
        currentWorld.spawnParticle(
            particleInfo.particle,
            this.currentLocation,
            particleInfo.count,
            particleInfo.offsetX,
            particleInfo.offsetY,
            particleInfo.offsetZ,
            particleInfo.extra,
            particleInfo.data
        );

        //Move location towards target
        final Vector direction = targetLocation.subtract(currentLocation).toVector();
        this.currentLocation.add(direction.normalize().multiply(Math.min(speed, distance)));
        return true;
    }
    
}
