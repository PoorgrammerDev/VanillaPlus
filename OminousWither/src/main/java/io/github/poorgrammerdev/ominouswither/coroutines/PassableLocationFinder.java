package io.github.poorgrammerdev.ominouswither.coroutines;

import java.util.Random;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.internal.ICoroutine;
import io.github.poorgrammerdev.ominouswither.utils.Utils;

/**
 * Finds passable locations using the Coroutine system (is non-blocking and not instant)
 * @author Thomas Tran
 */
public class PassableLocationFinder implements ICoroutine {
    private static final int MAX_CONSECUTIVE_FAILS = 100;

    private final Random random;

    private final Location center;
    private final Vector maxSpread;
    private final int heightSpaceRequired;
    private final boolean requireSight; 
    private final boolean requireImmediateGround;
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
     * @param requireImmediateGround Does the found location need to be directly on solid ground? If false, will still search for some solid block between here and the bottom of the world to prevent falling into the Void
     * @param amount How many locations should be found
     * @param consumer What to do with the locations as they're found
     * @param callback Called after task is finished; integer is how many locations were found
     */
    public PassableLocationFinder(Location center, Vector maxSpread, int heightSpaceRequired, boolean requireSight,
            boolean requireImmediateGround, int amount, Consumer<Location> consumer, Consumer<Integer> callback) {
        this.center = center;
        this.maxSpread = maxSpread;
        this.heightSpaceRequired = heightSpaceRequired;
        this.requireSight = requireSight;
        this.requireImmediateGround = requireImmediateGround;
        this.amount = amount;
        this.consumer = consumer;
        this.callback = callback;

        this.random = new Random();
    }

    @Override
    public boolean tick() {
        if (!this.shouldBeRescheduled()) return false;

        final World world = this.center.getWorld();
        if (world == null) return this.shouldBeRescheduled();

        //Get a random location within range
        Location location = this.center.clone().add(
            this.maxSpread.getX() > 0.0D ? random.nextDouble(this.maxSpread.getX() * 2) - this.maxSpread.getX() : 0.0D,
            this.maxSpread.getY() > 0.0D ? random.nextDouble(this.maxSpread.getY() * 2) - this.maxSpread.getY() : 0.0D,
            this.maxSpread.getZ() > 0.0D ? random.nextDouble(this.maxSpread.getZ() * 2) - this.maxSpread.getZ() : 0.0D
        );

        //Make sure Y coordinate is not out of the world
        location.setY(Utils.clamp(location.getY(), world.getMinHeight(), world.getMaxHeight()));

        //Passable check
        if (!Utils.isLocationPassable(location, this.heightSpaceRequired)) {
            //Failed
            this.consecutiveFails++;
            return this.shouldBeRescheduled();
        }

        //If requires ground, tries to get a valid ground location that's in range
        if (this.requireImmediateGround) {
            //Get the lowest Y coordinate we can search at
            final int minY = Math.max(this.center.getBlockY() - this.maxSpread.getBlockY(), world.getMinHeight());
            final Location ground = Utils.tryGetGround(location, minY);

            if (ground == null) {
                //Failed
                this.consecutiveFails++;
                return this.shouldBeRescheduled();
            }
    
            //Found a location in range that's on the ground, use that instead
            location = ground;
        }
        //Otherwise, at least check if there is *some* ground between here and the void
        else {
            if (Utils.tryGetGround(location, world.getMinHeight()) == null) {
                //Failed
                this.consecutiveFails++;
                return this.shouldBeRescheduled();
            }
        }

        //Line of sight check if required
        if (this.requireSight && !Utils.hasLineOfSight(this.center, location)) {
            //Failed
            this.consecutiveFails++;
            return this.shouldBeRescheduled();
        }

        //Location is valid
        this.successCount++;
        this.consecutiveFails = 0;
        if (this.consumer != null) this.consumer.accept(location);
        return this.shouldBeRescheduled();
    }

    private boolean shouldBeRescheduled() {
        // Check if stop condition reached (pass or fail) and run callback
        if (successCount >= amount || this.consecutiveFails >= MAX_CONSECUTIVE_FAILS) {
            if (this.callback != null) this.callback.accept(this.successCount);
            return false;
        }

        // Otherwise continue running
        return true;
    }

}
