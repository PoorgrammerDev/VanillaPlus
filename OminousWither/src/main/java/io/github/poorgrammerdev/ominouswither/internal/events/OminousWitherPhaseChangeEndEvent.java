package io.github.poorgrammerdev.ominouswither.internal.events;

import org.bukkit.entity.Wither;
import org.bukkit.event.HandlerList;

/**
 * Event for when an Ominous Wither finishes the Phase Change animation, entering Second Phase fully
 * @author Thomas Tran
 */
public class OminousWitherPhaseChangeEndEvent extends AbstractOminousWitherEvent {

    public OminousWitherPhaseChangeEndEvent(final Wither wither) {
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
