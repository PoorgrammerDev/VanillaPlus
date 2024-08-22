package io.github.poorgrammerdev.ominouswither.mechanics;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.coroutines.AmbientParticleTrail;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherActivateEvent;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherLoadEvent;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherPhaseChangeEndEvent;
import io.github.poorgrammerdev.ominouswither.utils.ParticleInfo;
import io.github.poorgrammerdev.ominouswither.utils.Utils;

/**
 * <p>Fun little easter egg for spawning the Wither above the Nether roof</p>
 * <p>Summons particle trails in the sky around the Wither</p>
 * <p>Only active when the Wither doesn't have a target</p>
 * <p>Doesn't do any damage, is just a visual ambient effect</p>
 * @author Thomas Tran
 */
public class ShootingStars implements Listener {
    private final int NETHER_ROOF_HEIGHT = 128;
    private final int RELATIVE_SPAWN_HEIGHT = 20;
    private final int SPAWN_RANGE = 20;
    private final int LIFESPAN = 250;

    private final OminousWither plugin;
    private final Random random;

    public ShootingStars(OminousWither plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    @EventHandler(ignoreCancelled = true)
    private void onActivate(final OminousWitherActivateEvent event) {
        this.startMechanism(event.getWither());
    }

    @EventHandler(ignoreCancelled = true)
    private void onSecondPhaseEnter(final OminousWitherPhaseChangeEndEvent event) {
        this.startMechanism(event.getWither());
    }

    @EventHandler(ignoreCancelled = true)
    private void onLoad(final OminousWitherLoadEvent event) {
        final Wither wither = event.getWither();

        //Apply mechanic to loaded wither
        if (wither.getInvulnerabilityTicks() <= 0) {
            this.startMechanism(wither);
        }
        
        //NOTE: Does not need to wait until invuln is over here: that will be handled in the two event handlers above
    }

    private void startMechanism(final Wither wither) {
        final World world = wither.getWorld();
        if (world == null || world.getEnvironment() != Environment.NETHER) return;

        final Location location = wither.getLocation();
        if (location.getY() < NETHER_ROOF_HEIGHT) return;

        //Runs every second
        new ShootingStarRunnable(wither).runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Handles spawning stars for a single Ominous Wither
     */
    private class ShootingStarRunnable extends BukkitRunnable {
        private final Wither wither;
        private final ParticleInfo particle;

        public ShootingStarRunnable(final Wither wither) {
            this.wither = wither;

            final boolean isInSecondPhase = wither.getPersistentDataContainer().getOrDefault(plugin.getSecondPhaseKey(), PersistentDataType.BOOLEAN, false);
            this.particle = !isInSecondPhase ?
                new ParticleInfo(Particle.CHERRY_LEAVES, 5, 1.5,1.5,1.5) :
                new ParticleInfo(Particle.GLOW, 10, 0.25, 0.25, 0.25, 0.125)
            ;
        }

        @Override
        public void run() {
            if (wither.isDead() || !wither.isInWorld() || wither.getInvulnerabilityTicks() > 0) {
                this.cancel();
                return;
            }
            final Location location = wither.getLocation();
            final World world = wither.getWorld();

            if (world.getEnvironment() != Environment.NETHER || location.getY() < NETHER_ROOF_HEIGHT) return;
            if (wither.getTarget() != null) return;

            //Get a random location in the air
            location.add(random.nextInt(2 * SPAWN_RANGE) - SPAWN_RANGE, RELATIVE_SPAWN_HEIGHT, random.nextInt(2 * SPAWN_RANGE) - SPAWN_RANGE);
            if (!Utils.isLocationPassable(location, 1)) return;

            //Spawn a star trail
            final Vector velocity = new Vector(random.nextDouble() - 0.5D, -0.25D, random.nextDouble() - 0.5D).normalize();
            plugin.getCoroutineManager().enqueue(new AmbientParticleTrail(world, location, velocity, particle, LIFESPAN));
        }
    }
    
}
