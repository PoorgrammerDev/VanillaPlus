package io.github.poorgrammerdev.ominouswither.internal.events;

import org.bukkit.entity.Wither;
import org.bukkit.event.HandlerList;

/**
 * Event for when an Ominous Wither begins entering the second phase by reaching half health
 * @author Thomas Tran
 */
public class OminousWitherPhaseChangeBeginEvent extends AbstractOminousWitherEvent {

    public OminousWitherPhaseChangeBeginEvent(final Wither wither) {
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
