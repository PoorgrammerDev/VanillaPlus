package io.github.poorgrammerdev.ominouswither.mechanics;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.persistence.PersistentDataType;

import io.github.poorgrammerdev.ominouswither.OminousWither;

/**
 * Prevents an Ominous Wither from regenerating above half health
 * @author Thomas Tran
 */
public class PreventPhaseRevert implements Listener {
    private final OminousWither plugin;

    public PreventPhaseRevert(OminousWither plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    private void onHeal(final EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Wither)) return;

        //Only affects Ominous Withers
        final Wither wither = (Wither) event.getEntity();
        if (!this.plugin.isOminous(wither)) return;

        //Wither must be in the second phase
        if (!wither.getPersistentDataContainer().getOrDefault(this.plugin.getSecondPhaseKey(), PersistentDataType.BOOLEAN, false)) return;

        //Calculate how much the Wither can heal
        final double maxHealth = wither.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        final double halfHealth = maxHealth / 2.0D;
        final double maxHealAmount = halfHealth - wither.getHealth();

        //Cannot heal any -> simply cancel
        if (maxHealAmount <= 0.0D) {
            event.setCancelled(true);
            return;
        }

        //Allow heal with limiter
        event.setAmount(Math.min(event.getAmount(), maxHealAmount));
    }
    
}
