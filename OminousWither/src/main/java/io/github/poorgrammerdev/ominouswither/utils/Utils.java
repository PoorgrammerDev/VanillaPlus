package io.github.poorgrammerdev.ominouswither.utils;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatColor;

/**
 * A static class with utility functions.
 * @author Thomas Tran
 */
public final class Utils {
    public static final ChatColor WITHER_NAME_COLOR = ChatColor.of("#8400FF");

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
     * Iterates downwards in attempt to find solid ground within range
     * @param location source location to begin at
     * @return a location on the ground within range if found, or null if not found
     */
    public static Location tryGetGround(final Location location, final int minY) {
        //Get world to access the minimum height
        final World world = location.getWorld();
        if (world == null) return null;

        //Calculate how many times we can iterate based on the minimum Y bound
        final int limit = location.getBlockY() - minY;
        
        Block block = location.getBlock();
        for (int i = 0; i < limit; i++) {
            final Block blockUnder = block.getRelative(BlockFace.DOWN);
            if (!blockUnder.isPassable()) return block.getLocation();

            block = blockUnder;
        }

        return null;
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

        final Block block = result.getHitBlock();
        return (block == null || block.isPassable());
    }

    /**
     * <p>Checks if a target location can be directly reached from the origin location, ignoring any breakable blocks</p>
     * <p>For example, if these two locations only have stone between them this will return true. If there is bedrock in the way, it will return false</p>
     * <p>It is only a simple raycast, there is no pathfinding</p>
     */
    public static boolean hasBreakableLineOfSight(Location origin, final Location target) {
        //World must exist and match
        final World world = origin.getWorld();
        final World targetWorld = target.getWorld();
        if (world == null || targetWorld == null || !world.equals(targetWorld)) return false;

        //Perform a raycast from origin facing target and check if any impassable blocks are in the way
        origin = origin.clone();
        final Vector direction = target.clone().subtract(origin).toVector();
        origin.setDirection(direction);

        //Bounds check to prevent infinite looping
        final int distance = (int) origin.distance(target);
        if (distance <= 0) return true; //In the same block, must be visible
        if (distance >= 128) return false; //Too far away

        final int MAX_ITERS = 256;
        int i = 0;
        final BlockIterator blockIterator = new BlockIterator(origin, 0.0D, distance);
        while (blockIterator.hasNext()) {
            // Safety condition to prevent infinite loop server crash
            // Shouldn't be required due to bounds check on distance, but keeping as a backup
            if (i >= MAX_ITERS) return false;
            
            final Block block = blockIterator.next();
            if (block == null) return true;

            //Cannot have passed through a Wither immune block
            if (Tag.WITHER_IMMUNE.isTagged(block.getType())) return false;

            ++i;
        }

        //Reached end -> no impassable blocks are in the way
        return true;
    }

    /**
     * Checks if a location is on the ground
     * @param location location to check
     * @param range how far to look downward for ground
     * @return if location is on the ground
     */
    public static boolean isOnGround(final Location location, final double range) {
        final World world = location.getWorld();
        if (world == null) return false;

        final RayTraceResult result = world.rayTraceBlocks(location, new Vector(0, -1, 0), range, FluidCollisionMode.NEVER, true);
        
        //From the docs: the method above returns "the ray trace hit result, or null if there is no hit"
        if (result == null) return false;

        final Block block = result.getHitBlock();
        return (block != null && !block.isPassable());
    }

    /**
     * Checks if a location is on the ground using a predefined constant range
     * @param location location to check
     * @return if location is on the ground
     */
    public static boolean isOnGround(final Location location) {
        return isOnGround(location, 0.25D);
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
     * Linearly interpolates between two scalar numbers
     * @param a first number
     * @param b second number
     * @param t value in range [0,1] that controls interpolation. 0 will give A, 1 will give B
     * @return linearly interpolated scalar
     */
    public static double lerp(final double a, final double b, double t) {
        //Clamp t to [0,1] range
        t = clamp(t, 0.0D, 1.0D);

        return ((1.0D - t) * a) + (t * b);
    }

    /**
     * Linearly interpolates between two vectors
     * @param vecA first vector
     * @param vecB second vector
     * @param t value in range [0,1] that controls interpolation. 0 will give vecA, 1 will give vecB
     * @return linearly interpolated vector
     */
    public static Vector lerp(final Vector vecA, final Vector vecB, double t) {
        //Clamp t to [0,1] range
        t = clamp(t, 0.0D, 1.0D);

        final Vector scaledA = vecA.clone().multiply(1.0D - t);
        final Vector scaledB = vecB.clone().multiply(t);

        return scaledA.add(scaledB);
    }

    /**
     * Checks if a player is in survival or adventure mode
     */
    public static boolean isTargetable(final Player player) {
        return (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE);
    }

    /**
     * Get Roman Numeral for Wither level
     */
    public static String getLevelRomanNumeral(final int level) {
        //NOTE: I am aware there is an algorithm to perform this without manually hardcoding every case
        //However, there are only 5 possible levels so this is a much simpler solution
        //If the number of levels is ever expanded I will use that algorithm instead
        switch (level) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            case 5:
                return "V";
            default:
                return null;
        }
    }

}
