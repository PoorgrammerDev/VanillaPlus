package io.github.poorgrammerdev.ominouswither.mechanics.customskulls;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Wither;
import org.bukkit.event.entity.ProjectileHitEvent;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.config.BossStat;
import io.github.poorgrammerdev.ominouswither.utils.ParticleInfo;

/**
 * Skull that creates a large fiery explosion on hit
 * @author Thomas Tran
 */
public class ExplosiveSkull extends AbstractSkullHandler {

    public ExplosiveSkull(OminousWither plugin) {
        super(plugin, BossStat.EXPLOSIVE_SKULL_SPEED, new ParticleInfo(Particle.FLAME, 1, 0, 0, 0, 0, null));
    }

    @Override
    public void onHit(final ProjectileHitEvent event, final Wither wither) {
        final Location location = wither.getLocation();
        final World world = location.getWorld();
        if (world == null) return;

        // Boom
        final float explosionPower = (float) this.plugin.getBossStatsManager().getStat(BossStat.EXPLOSIVE_SKULL_POWER, wither);
        world.createExplosion(location, explosionPower, true, true, wither);
    }

    @Override
    public String getSkullTag() {
        return "explosive";
    }
    
}
