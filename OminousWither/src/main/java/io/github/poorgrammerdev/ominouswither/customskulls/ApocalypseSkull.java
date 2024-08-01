package io.github.poorgrammerdev.ominouswither.customskulls;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wither;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.ApocalypseHorsemen;
import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.ParticleInfo;
import io.github.poorgrammerdev.ominouswither.coroutines.PassableLocationFinder;
import io.github.poorgrammerdev.ominouswither.internal.CoroutineManager;

/**
 * Skull that spawns Skeleton Horsemen on hit
 * @author Thomas Tran
 */
public class ApocalypseSkull extends AbstractHomingSkull {
    private final ApocalypseHorsemen apocalypseHorsemen;

    public ApocalypseSkull(final OminousWither plugin, final ApocalypseHorsemen apocalypseHorsemen) {
        super(plugin, 3.0D, new ParticleInfo(Particle.CLOUD, 1, 0, 0, 0, 0, null), (SKULL_LIFESPAN / 5), 5, 10, false);

        this.apocalypseHorsemen = apocalypseHorsemen;
    }

    @Override
    public void onHit(final ProjectileHitEvent event, final Wither wither) {
        //Projectile must have hit a living entity straight-on to activate
        if (!(event.getHitEntity() instanceof LivingEntity)) return;

        //Hit entity cannot be undead ("wither friend") or a minion
        final LivingEntity hitEntity = (LivingEntity) event.getHitEntity();
        if (Tag.ENTITY_TYPES_WITHER_FRIENDS.isTagged(hitEntity.getType()) || this.plugin.isMinion(hitEntity)) return;

        //Last damage cause must be this skull and it must've done damage after negation
        //Unfortunately the Absorption potion effect seems to count as damage negation and the only method of detecting that is deprecated
        //So Absorption is a counter to the Apocalypse skull for the time being 
        final EntityDamageEvent lastDamageCause = hitEntity.getLastDamageCause();
        if (
            lastDamageCause == null ||
            lastDamageCause.getDamageSource() == null ||
            lastDamageCause.getDamageSource().getDirectEntity() == null ||
            !lastDamageCause.getDamageSource().getDirectEntity().equals(event.getEntity()) ||
            lastDamageCause.getFinalDamage() <= 0.0D
        ) return;

        final int level = this.plugin.getLevel(wither, 1);
        final Location center = event.getEntity().getLocation();

        //VFX + SFX
        final World centerWorld = center.getWorld();
        if (centerWorld != null) {
            centerWorld.strikeLightningEffect(center);
            centerWorld.playSound(center, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.HOSTILE, 5, 0.875f);
        }
        
        //Find viable locations and summon skeleton horsemen
        final UUID groupID = UUID.randomUUID();
        CoroutineManager.getInstance().enqueue(new PassableLocationFinder(
            center,
            new Vector(5, 5, 5),
            4,
            true,
            true,
            level,
            (location) -> {this.apocalypseHorsemen.spawnHorseman(location, groupID);},
            (amount) -> {this.apocalypseHorsemen.activateTimer(amount, groupID);}
        ));
    }

    @Override
    public String getSkullTag() {
        return "apocalypse";
    }
    
}
