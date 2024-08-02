package io.github.poorgrammerdev.ominouswither.utils;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * A static class with utility functions.
 * @author Thomas Tran
 */
public final class Utils {

    /**
     * Checks if a location is viable for an entity to spawn at (i.e. it's not inside a wall)
     * @param location Location to check
     * @param heightSpaceRequired how many vertical blocks of air need to be present (e.g. 2 for a Player to fit, 3 for an Enderman) 
     * @return true if safe, false if not
     */
    public static boolean isLocationPassable(final Location location, final int heightSpaceRequired) {
        Block block = location.getBlock();

        for (int i = 0; i < heightSpaceRequired; ++i) {
            if (block == null || !block.isPassable()) return false;
            block = block.getRelative(BlockFace.UP);
        }

        return true;
    }

    /**
     * Checks if two locations are visible from each other
     */
    public static boolean hasLineOfSight(final Location origin, final Location target) {
        //World must exist and match
        final World world = origin.getWorld();
        final World targetWorld = target.getWorld();
        if (world == null || targetWorld == null || !world.equals(targetWorld)) return false;

        //Perform a raycast from origin facing target and check if any impassable blocks are in the way
        final double distance = origin.distance(target);
        final Vector direction = target.clone().subtract(origin).toVector();
        final RayTraceResult result = world.rayTraceBlocks(origin, direction, distance, FluidCollisionMode.NEVER, true);

        //From the docs: the method above returns "the ray trace hit result, or null if there is no hit"
        if (result == null) return true;

        //TODO: is the null check correct behavior?
        final Block block = result.getHitBlock();
        return (block == null || block.isPassable());
    }

    /**
     * Restrict an integer value to a bound
     * @return clamped value
     */
    public static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
    
    /**
     * Restrict a double value to a bound
     * @return clamped value
     */
    public static double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    /**
     * Displays a particle circle on the Y axis at the given location
     * @param particle information of particle to display
     * @param radius radius of circle
     * @param density how thick to make the line
     * @param location where to display it
     */
    public static void particleCircle(final ParticleInfo particle, final double radius, final double density, Location location) {
        final World world = location.getWorld();
        if (world == null) return;

        //Even though this method shouldn't have any effect on the location object,
        //make a clone just to be extra safe
        location = location.clone();

        //Iterates through angles in radians up to 2pi
        for (double theta = 0; theta <= 2*Math.PI; theta += Math.PI / density) {

            //Calculates offsets from the center 
            double x = radius * Math.cos(theta);
            double z = radius * Math.sin(theta);

            location.add(x, 0, z);

            world.spawnParticle(
                particle.particle,
                location,
                particle.count,
                particle.offsetX,
                particle.offsetY,
                particle.offsetZ,
                particle.extra,
                particle.data
            );

            location.subtract(x, 0, z);
        }
    }

    public static void particleLine(ParticleInfo particle, Location origin, Location target, double density) {
        final World world = origin.getWorld();
        final World targetWorld = target.getWorld();
        if (world == null || targetWorld == null || !world.equals(targetWorld)) return;

        final Vector begin = origin.toVector();
        final Vector end = target.toVector();
        for (double t = 0.0; t <= 1.0D; t += (1.0D / density)) {
            world.spawnParticle(
                particle.particle,
                lerp(begin, end, t).toLocation(world, origin.getYaw(), origin.getPitch()),
                particle.count,
                particle.offsetX,
                particle.offsetY,
                particle.offsetZ,
                particle.extra,
                particle.data
            );
        }
    }

    public static Vector lerp(final Vector vecA, final Vector vecB, double t) {
        //Clamp t to [0,1] range
        t = clamp(t, 0.0D, 1.0D);

        final Vector scaledA = vecA.clone().multiply(t);
        final Vector scaledB = vecB.clone().multiply(1.0D - t);

        return scaledA.add(scaledB);
    }

}
