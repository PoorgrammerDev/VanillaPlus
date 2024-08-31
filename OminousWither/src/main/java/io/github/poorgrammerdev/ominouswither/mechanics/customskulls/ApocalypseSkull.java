package io.github.poorgrammerdev.ominouswither.mechanics.customskulls;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wither;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.config.BossStat;
import io.github.poorgrammerdev.ominouswither.mechanics.ApocalypseHorsemen;
import io.github.poorgrammerdev.ominouswither.utils.ParticleInfo;

/**
 * Skull that spawns Skeleton Horsemen on hit
 * @author Thomas Tran
 */
public class ApocalypseSkull extends AbstractHomingSkull {
    private final ApocalypseHorsemen apocalypseHorsemen;

    public ApocalypseSkull(final OminousWither plugin, final ApocalypseHorsemen apocalypseHorsemen) {
        super(plugin, BossStat.APOCALYPSE_SKULL_SPEED, new ParticleInfo(Particle.CLOUD, 1), BossStat.APOCALYPSE_HOMING_LIFESPAN, 5, 10, false);

        this.apocalypseHorsemen = apocalypseHorsemen;
    }

    @Override
    public void onHit(final ProjectileHitEvent event, final Wither wither) {
        //Projectile must have hit a living entity straight-on to activate
        if (!(event.getHitEntity() instanceof LivingEntity)) return;

        //Hit entity cannot be undead ("wither friend") or a minion
        final LivingEntity hitEntity = (LivingEntity) event.getHitEntity();
        if (Tag.ENTITY_TYPES_WITHER_FRIENDS.isTagged(hitEntity.getType()) || this.plugin.isMinion(hitEntity)) return;

        //Delay by one tick to get correct Last Damage Cause
        final Projectile skull = event.getEntity();
        new BukkitRunnable() {
            @Override
            public void run() {
                //Last damage cause must be this skull and it must've done damage after negation
                //Unfortunately the Absorption potion effect seems to count as damage negation and the only method of detecting that is deprecated
                //So Absorption is a counter to the Apocalypse skull for the time being 
                final EntityDamageEvent lastDamageCause = hitEntity.getLastDamageCause();
                if (
                    lastDamageCause == null ||
                    lastDamageCause.getDamageSource() == null ||
                    lastDamageCause.getDamageSource().getDirectEntity() == null ||
                    !lastDamageCause.getDamageSource().getDirectEntity().equals(skull) ||
                    lastDamageCause.getFinalDamage() <= 0.0D
                ) return;

                final Location center = skull.getLocation();

                //VFX + SFX
                final World centerWorld = center.getWorld();
                if (centerWorld != null) {
                    centerWorld.strikeLightningEffect(center);
                    centerWorld.playSound(center, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.HOSTILE, 5, 0.875f);
                }

                //Start the apocalypse! o_O
                apocalypseHorsemen.startApocalypse(wither, hitEntity);
            }
        }.runTaskLater(plugin, 1L);
    }

    @Override
    public String getSkullTag() {
        return "apocalypse";
    }
    
}
