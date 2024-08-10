package io.github.poorgrammerdev.ominouswither.mechanics;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.config.BossStat;
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
    private void onPhaseChange(final OminousWitherPhaseChangeBeginEvent event) {
        final Wither wither = event.getWither();
        
        wither.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(this.plugin.getBossStatsManager().getStat(BossStat.SECOND_PHASE_ARMOR, wither));
        wither.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).setBaseValue(this.plugin.getBossStatsManager().getStat(BossStat.SECOND_PHASE_ARMOR_TOUGHNESS, wither));
    }


}
