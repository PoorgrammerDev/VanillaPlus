package io.github.poorgrammerdev.ominouswither.internal.events;

import org.bukkit.entity.Wither;
import org.bukkit.event.HandlerList;

/**
 * Event for when a loaded Ominous Wither is unloaded
 * @author Thomas Tran
 */
public class OminousWitherUnloadEvent extends AbstractOminousWitherEvent {

    public OminousWitherUnloadEvent(final Wither wither) {
        super(wither);
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
