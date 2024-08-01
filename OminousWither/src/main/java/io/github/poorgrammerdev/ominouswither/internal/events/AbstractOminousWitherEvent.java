package io.github.poorgrammerdev.ominouswither.internal.events;

import org.bukkit.entity.Wither;
import org.bukkit.event.Event;

/**
 * Base class for all Ominous Wither events
 * @author Thomas Tran
 */
public abstract class AbstractOminousWitherEvent extends Event {
    protected final Wither wither;

    public AbstractOminousWitherEvent(final Wither wither) {
        this.wither = wither;
    }

    /**
     * @return the ominous wither entity
     */
    public Wither getWither() {
        return this.wither;
    }
    
}
