package io.github.poorgrammerdev.ominouswither.coroutines;

import java.util.Random;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.Utils;
import io.github.poorgrammerdev.ominouswither.backend.ICoroutine;

/**
 * Finds passable locations using the Coroutine system (is non-blocking and not instant)
 * @author Thomas Tran
 */
public class PassableLocationFinder implements ICoroutine {
    private static final int MAX_CONSECUTIVE_FAILS = 100;

    private final Random random;

    private final Location center; //TODO: is it unsafe to hold this reference?
    private final Vector maxSpread;
    private final int heightSpaceRequired;
    private final boolean requireSight; 
    private final boolean requireGround;
    private final int amount;
    private final Consumer<Location> consumer;
    private final Consumer<Integer> callback;

    private int successCount;
    private int consecutiveFails;

    /**
     * Constructor
     * @param center Center of search space
     * @param maxSpread The maximum distance from the center in all directions
     * @param heightSpaceRequired how many vertical blocks of air need to be present (e.g. 2 for a Player to fit, 3 for an Enderman) 
     * @param requireSight Does the found location need to be visible from the center?
     * @param requireGround Does the found location need to be on solid ground?
     * @param amount How many locations should be found
     * @param consumer What to do with the locations as they're found
     * @param callback Called after task is finished; integer is how many locations were found
     */
    public PassableLocationFinder(Location center, Vector maxSpread, int heightSpaceRequired, boolean requireSight,
            boolean requireGround, int amount, Consumer<Location> consumer, Consumer<Integer> callback) {
        this.center = center;
        this.maxSpread = maxSpread;
        this.heightSpaceRequired = heightSpaceRequired;
        this.requireSight = requireSight;
        this.requireGround = requireGround;
        this.amount = amount;
        this.consumer = consumer;
        this.callback = callback;

        this.random = new Random();
    }

    @Override
    public void tick() {
        //Get a random location within range
        Location location = this.center.clone().add(
            random.nextDouble(this.maxSpread.getX() * 2) - this.maxSpread.getX(),
            random.nextDouble(this.maxSpread.getY() * 2) - this.maxSpread.getY(),
            random.nextDouble(this.maxSpread.getZ() * 2) - this.maxSpread.getZ()
        );

        //Passable check
        if (!Utils.isLocationPassable(location, this.heightSpaceRequired)) {
            //Failed
            this.consecutiveFails++;
            return;    
        }

        //If requires ground, tries to get a valid ground location by iterating downwards
        if (this.requireGround) {
            final Location ground = tryGetGround(location);
            if (ground == null) {
                //Failed
                this.consecutiveFails++;
                return;
            }
    
            //Found a location in range that's on the ground, use that instead
            location = ground;
        }

        //Line of sight check if required
        if (this.requireSight && !Utils.hasLineOfSight(this.center, location)) {
            //Failed
            this.consecutiveFails++;
            return; 
        }

        //Location is valid
        this.successCount++;
        this.consecutiveFails = 0;
        if (this.consumer != null) this.consumer.accept(location);
    }

    @Override
    public boolean shouldBeRescheduled() {
        // Check if stop condition reached (pass or fail) and run callback
        if (successCount >= amount || this.consecutiveFails >= MAX_CONSECUTIVE_FAILS) {
            if (this.callback != null) this.callback.accept(this.successCount);
            return false;
        }

        // Otherwise continue running
        return true;
    }

    /**
     * Iterates downwards in attempt to find solid ground within range
     * @param location source location to begin at
     * @return a location on the ground within range if found, or null if not found
     */
    private Location tryGetGround(Location location) {
        //Get world to access the minimum height
        final World world = location.getWorld();
        if (world == null) return null;

        // Get the lowest Y coordinate we can search at, and calculate how many times we can iterate
        final int minY = Math.max(this.center.getBlockY() - this.maxSpread.getBlockY(), world.getMinHeight());
        final int limit = location.getBlockY() - minY;
        
        Block block = location.getBlock();
        for (int i = 0; i < limit; i++) {
            final Block blockUnder = block.getRelative(BlockFace.DOWN);
            if (!blockUnder.isPassable()) return block.getLocation();

            block = blockUnder;
        }

        return null;
    }
    
}
