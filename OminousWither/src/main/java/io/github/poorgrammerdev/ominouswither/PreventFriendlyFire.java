package io.github.poorgrammerdev.ominouswither;

import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Prevents the Wither from killing its own minions
 * @author Thomas Tran
 */
public class PreventFriendlyFire implements Listener {
    private final OminousWither plugin;
    //Side effect: the Wither does not damage another Wither's minions either

    public PreventFriendlyFire(OminousWither plugin) {
        this.plugin = plugin;
    }

    /**
     * Cancels any damage done to a Minion caused by an Ominous Wither
     */
    @EventHandler(ignoreCancelled = true)
    public void onDamage(final EntityDamageByEntityEvent event) {
        final DamageSource source = event.getDamageSource();
        if (source == null) return;

        //Entity responsible for damage must exist and be an Ominous Wither
        final Entity damager = source.getCausingEntity();
        if (!(damager instanceof Wither) || !this.plugin.isOminous((Wither) damager)) return;

        //Damaged entity must be a Minion
        if (!(event.getEntity() instanceof LivingEntity)) return;
        final LivingEntity entity = (LivingEntity) event.getEntity();
        if (!this.plugin.isMinion(entity)) return;

        //Cancel the event
        event.setCancelled(true);
    }
    
}
