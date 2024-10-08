package io.github.poorgrammerdev.ominouswither.mechanics;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.config.BossStat;

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
    private void onExplode(final EntityDamageEvent event) {
        //Must be an explosion
        final DamageCause cause = event.getCause();
        if (cause != DamageCause.BLOCK_EXPLOSION && cause != DamageCause.ENTITY_EXPLOSION) return;

        //Entity must be an ominous wither
        if (event.getEntityType() != EntityType.WITHER || !(event.getEntity() instanceof Wither)) return;

        final Wither wither = (Wither) event.getEntity();
        if (!this.plugin.isOminous(wither)) return;

        final double reduction = this.plugin.getBossStatsManager().getStat((this.isCrystal(event.getDamageSource()) ? BossStat.END_CRYSTAL_RESISTANCE : BossStat.GENERAL_EXPLOSION_RESISTANCE), wither);
        if (reduction < 0.0D) return;
        
        //Apply damage reduction and play indication sound
        final double remainingDamage = event.getDamage() * (1.0D - reduction);
        event.setDamage(Math.max(remainingDamage, 0.0D));
        wither.getWorld().playSound(wither, Sound.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 2.0f, 1.0f);
    }

    private boolean isCrystal(final DamageSource source) {
        if (source == null) return false;

        final Entity entity = source.getDirectEntity();
        return (entity != null && entity.getType() == EntityType.END_CRYSTAL);
    }


}
