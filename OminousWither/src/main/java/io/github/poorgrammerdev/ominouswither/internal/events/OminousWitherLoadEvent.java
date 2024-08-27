package io.github.poorgrammerdev.ominouswither.internal.events;

import org.bukkit.entity.Wither;
import org.bukkit.event.HandlerList;

/**
 * Event for when an unloaded Ominous Wither is loaded back in
 * @author Thomas Tran
 */
public class OminousWitherLoadEvent extends AbstractOminousWitherEvent {

    public OminousWitherLoadEvent(Wither wither) {
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
