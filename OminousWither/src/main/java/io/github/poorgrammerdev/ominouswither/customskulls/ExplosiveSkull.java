package io.github.poorgrammerdev.ominouswither.customskulls;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wither;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.ParticleInfo;

/**
 * Skull that creates a large fiery explosion on hit
 * @author Thomas Tran
 */
public class ExplosiveSkull extends AbstractSkullHandler {

    public ExplosiveSkull(OminousWither plugin) {
        super(plugin, 7.5D, new ParticleInfo(Particle.FLAME, 1, 0, 0, 0, 0, null));
    }

    @Override
    public void onHit(final ProjectileHitEvent event, final Wither wither) {
        final Location location = event.getEntity().getLocation();
        final World world = location.getWorld();
        if (world == null) return;

        final ProjectileSource source = event.getEntity().getShooter();
        final Entity shooter = (source instanceof Entity) ? (Entity) source : null;

        world.createExplosion(location, 3.75f, true, true, shooter);
    }

    @Override
    public String getSkullTag() {
        return "explosive";
    }
    
}
