package io.github.poorgrammerdev.ominouswither;

import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

/**
 * The Ominous Wither is resistant to explosion damage
 * @author Thomas Tran
 */
public class ExplosionResistance implements Listener {
    // Default values: 50% resistance to normal explosions, 80% resistance to end crystals

    private final OminousWither plugin;

    public ExplosionResistance(OminousWither plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(final EntityDamageEvent event) {
        //Must be an explosion
        final DamageCause cause = event.getCause();
        if (cause != DamageCause.BLOCK_EXPLOSION && cause != DamageCause.ENTITY_EXPLOSION) return;

        //Entity must be an ominous wither
        if (event.getEntityType() != EntityType.WITHER || !(event.getEntity() instanceof Wither)) return;

        final Wither wither = (Wither) event.getEntity();
        if (!this.plugin.isOminous(wither)) return;

        //Apply damage reduction
        final double scalar = (this.isCrystal(event.getDamageSource()) ? 0.2D : 0.5D);
        event.setDamage(event.getDamage() * scalar);
    }

    private boolean isCrystal(final DamageSource source) {
        if (source == null) return false;

        final Entity entity = source.getDirectEntity();
        return (entity != null && entity.getType() == EntityType.END_CRYSTAL);
    }


}
