package io.github.poorgrammerdev.ominouswither;

import java.util.List;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * A static class with utility functions.
 */
public final class Utils {

    /**
     * Gets nearby safe locations synchronously
     * @param center the center location
     * @param maxSpread max distance in each direction the locations can be
     * @param requireSight the location must be within line of sight to the center
     * @param requireGround the location must be on the ground, it cannot be midair
     * @param amount how many locations to return
     * @return a list of locations, up to <amount> length. length can be less or even empty if the search fails
     */
    public static List<Location> getNearbySafeLocations(final Location center, final Vector maxSpread, final boolean requireSight, final boolean requireGround, final int amount) {
        return null;
    }

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
    
}
