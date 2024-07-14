package io.github.poorgrammerdev.ominouswither.backend;

import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event for when a player spawns an Ominous Wither
 * @author Thomas Tran
 */
public class OminousWitherSpawnEvent extends Event implements Cancellable {

    private boolean cancelled;
    private final Wither wither;
    private final Player spawner;
    private final int level;

    public OminousWitherSpawnEvent(final Wither wither, final Player spawner, final int level) {
        this.cancelled = false;
        this.wither = wither;
        this.spawner = spawner;
        this.level = level;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /**
     * @return the ominous wither entity
     */
    public Wither getWither() {
        return this.wither;
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
