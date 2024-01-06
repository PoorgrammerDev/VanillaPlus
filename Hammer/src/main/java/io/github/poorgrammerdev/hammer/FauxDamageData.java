package io.github.poorgrammerdev.hammer;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.block.Block;

/**
 * [STRUCT-LIKE CLASS]
 * Used to hold data pertaining to block damage displaying
 */
public class FauxDamageData {
    private static final int MAX_ADJACENT_LENGTH = 8;

    /**
     * IDs to use for the block damagers (randomly generated once)
     */
    public final int[] ids;

    /**
     * Effect is active for player
     */
    public boolean active;

    /**
     * Center or target block the player is using the hammer on
     */
    public Block centerBlock;
    
    /**
     * Array of locations surrounding block in the plane
     */
    public Location[] adjacentBlocks;

    /**
     * How many locations are valid in the adjacentBlocks array
     * The valid range is [0, adjacentCount - 1]
     * The remaining values are either null or garbage
     */
    public int adjacentCount;

    /**
     * Progression of effect, measured in elapsed ticks
     */
    public int ticks;

    public FauxDamageData(final Block centerBlock, final Random random) {
        this.centerBlock = centerBlock;
        this.ticks = 0;
        this.active = false;

        //Empty locations array
        this.adjacentBlocks = new Location[MAX_ADJACENT_LENGTH];
        this.adjacentCount = 0;
        
        //Generate random id values (always max length so we don't have to regenerate)
        this.ids = new int[MAX_ADJACENT_LENGTH];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = random.nextInt();
        }
    }
}