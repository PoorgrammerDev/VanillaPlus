package io.github.poorgrammerdev.ominouswither;

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
}
