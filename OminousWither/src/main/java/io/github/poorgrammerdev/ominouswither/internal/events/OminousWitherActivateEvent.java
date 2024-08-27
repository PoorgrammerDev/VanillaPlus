package io.github.poorgrammerdev.ominouswither.internal.events;

import org.bukkit.entity.Wither;
import org.bukkit.event.HandlerList;

/**
 * Event for when a spawned Ominous Wither reaches full health and explodes
 * @author Thomas Tran
 */
public class OminousWitherActivateEvent extends AbstractOminousWitherEvent {

    public OminousWitherActivateEvent(Wither wither) {
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
