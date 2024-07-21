package io.github.poorgrammerdev.ominouswither.backend;

import org.bukkit.entity.Wither;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event for when a spawned Ominous Wither reaches full health and explodes
 * @author Thomas Tran
 */
public class OminousWitherActivateEvent extends Event implements Cancellable {

    private boolean cancelled;
    private final Wither wither;

    public OminousWitherActivateEvent(final Wither wither) {
        this.cancelled = false;
        this.wither = wither;
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
