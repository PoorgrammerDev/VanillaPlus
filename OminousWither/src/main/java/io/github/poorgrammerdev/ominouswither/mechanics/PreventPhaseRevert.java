package io.github.poorgrammerdev.ominouswither.mechanics;

import java.util.UUID;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.persistence.PersistentDataType;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.ICoroutine;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherLoadEvent;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherPhaseChangeEndEvent;

/**
 * Prevents a Second Phase Ominous Wither from regenerating above half health
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

        //Wither must fully be in the second phase
        if (wither.getInvulnerabilityTicks() > 0 || !wither.getPersistentDataContainer().getOrDefault(this.plugin.getSecondPhaseKey(), PersistentDataType.BOOLEAN, false)) return;

        //Calculate how much the Wither can heal
        final double maxHealth = wither.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        final double halfHealth = maxHealth / 2.0D;
        final double maxHealAmount = halfHealth - wither.getHealth();

        //Cannot heal any -> simply cancel
        if (maxHealAmount <= 0.0D) {
            event.setAmount(0.0D);
            event.setCancelled(true);
            return;
        }

        //Allow heal with limiter
        event.setAmount(Math.min(event.getAmount(), maxHealAmount));
    }
    
    @EventHandler(ignoreCancelled = true)
    private void onPhaseEnter(final OminousWitherPhaseChangeEndEvent event) {
        this.forceHealthBoundary(event.getWither());
    }

    @EventHandler(ignoreCancelled = true)
    private void onLoad(final OminousWitherLoadEvent event) {
        final Wither wither = event.getWither();
        
        //Wither must fully be in the second phase
        if (wither.getInvulnerabilityTicks() > 0 || !wither.getPersistentDataContainer().getOrDefault(this.plugin.getSecondPhaseKey(), PersistentDataType.BOOLEAN, false)) return;

        this.forceHealthBoundary(wither);
    }

    /**
     * Uses a coroutine to force Wither health below half
     * @param witherParam Ominous Wither
     */
    private void forceHealthBoundary(final Wither witherParam) {
        final UUID witherID = witherParam.getUniqueId();
        final double healthBoundary = witherParam.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2.0D;

        this.plugin.getCoroutineManager().enqueue(new ICoroutine() {
            @Override
            public boolean tick() {
                final Entity entity = plugin.getServer().getEntity(witherID);
                if (!(entity instanceof Wither) || entity.isDead() || !entity.isInWorld()) return false;

                //If health is above max allowed set to max
                final Wither wither = (Wither) entity;
                if (wither.getHealth() > healthBoundary) {
                    wither.setHealth(healthBoundary);
                }

                return true;
            }
            
        });
    }

    
}
