package io.github.poorgrammerdev.ominouswither.backend;

import org.bukkit.entity.Wither;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event for when an unloaded Ominous Wither is loaded back in
 * @author Thomas Tran
 */
public class OminousWitherLoadEvent extends Event {
    private final Wither wither;

    public OminousWitherLoadEvent(final Wither wither) {
        this.wither = wither;
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
