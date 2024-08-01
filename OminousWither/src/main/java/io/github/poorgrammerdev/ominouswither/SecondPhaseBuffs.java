package io.github.poorgrammerdev.ominouswither;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherPhaseChangeBeginEvent;

/**
 * This handles the stat buffs gained upon entering the second phase
 * @author Thomas Tran
 */
public class SecondPhaseBuffs implements Listener {
    private final OminousWither plugin;

    public SecondPhaseBuffs(OminousWither plugin) {
        this.plugin = plugin;
    } 

    @EventHandler(ignoreCancelled = true)
    public void onPhaseChange(final OminousWitherPhaseChangeBeginEvent event) {
        final Wither wither = event.getWither();
        final int level = this.plugin.getLevel(wither, 1);
        
        wither.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(7.0 + (level * 3));
        wither.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).setBaseValue(level * 4);
    }


}
