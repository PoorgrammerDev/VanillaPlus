package io.github.poorgrammerdev.ominouswither.internal.events;

import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.HandlerList;

/**
 * Event for when a player spawns an Ominous Wither
 * @author Thomas Tran
 */
public class OminousWitherSpawnEvent extends AbstractOminousWitherEvent {

    public enum SpawnReason {
        BUILD,
        COMMAND,
    }

    private final Player spawner;
    private final int level;
    private final SpawnReason spawnReason;

    public OminousWitherSpawnEvent(final Wither wither, final Player spawner, final int level, final SpawnReason spawnReason) {
        super(wither);

        this.spawner = spawner;
        this.level = level;
        this.spawnReason = spawnReason;
    }

    /**
     * @return the player that most likely spawned the Ominous Wither
     */
    public Player getSpawner() {
        return this.spawner;
    }

    /**
     * @return the level of Bad Omen (and thus level of Ominous Wither); value should be in range [1,5]
     */
    public int getLevel() {
        return this.level;
    }

    /**
     * @return why/how this wither was spawned (was it built, or were commands used?)
     */
    public SpawnReason getSpawnReason() {
        return this.spawnReason;
    }
    
    /*
     * Required methods for Spigot Event system
     */

    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
 
    public static HandlerList getHandlerList() {
        return handlers;
    } 
    
}
