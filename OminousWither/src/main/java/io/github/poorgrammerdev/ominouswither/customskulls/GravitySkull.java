package io.github.poorgrammerdev.ominouswither.customskulls;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.ParticleInfo;
import io.github.poorgrammerdev.ominouswither.Utils;

/**
 * Skull that pulls in enemies 
 */
public class GravitySkull extends AbstractSkullHandler {
    private static final double RADIUS = 6.25;
    private static final double FORCE_INTENSITY = 2.0D;

    public GravitySkull(OminousWither plugin) {
        super(plugin, 5.0D, new ParticleInfo(Particle.DUST, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.FUCHSIA, 1.5f)));
    }

    @Override
    public void onHit(final ProjectileHitEvent event, final Wither wither) {
        final Location location = event.getEntity().getLocation().add(0, 0.5, 0);
        final World world = location.getWorld();
        if (world == null) return;

        //Particle circle
        Utils.particleCircle(this.trackingParticle, RADIUS, 20, location);

        //Gravity mechanic
        world.getNearbyEntities(location, RADIUS, RADIUS, RADIUS, (entity) -> (!Tag.ENTITY_TYPES_WITHER_FRIENDS.isTagged(entity.getType())))
            .stream()
            
            //Entity cannot be invulnerable
            .filter((entity) -> (!entity.isInvulnerable()))

            //Friendly cases should all be handled already up top but as a double check:
            .filter((entity) -> (!this.plugin.isMinion(entity))) //Cannot be a minion
            .filter((entity) -> (!(entity instanceof Wither) || this.plugin.isOminous((Wither) entity))) //If it is a Wither, cannot be an Ominous Wither 

            //Cannot affect other wither skulls
            .filter((entity) -> (!(entity instanceof WitherSkull)))

            //Must be in radius
            .filter((entity) -> (location.distanceSquared(entity.getLocation()) <= (RADIUS * RADIUS)))

            //NOTE: currently does not do a visibility check, meaning this can hit through walls | TODO: should this behaviour be changed?

            .forEach((entity) -> {
                //Special case for players
                if (entity instanceof Player) {
                    final Player player = (Player) entity;

                    //Do not affect creative or spectator players
                    if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

                    //Play SFX only to affected players
                    player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_DEACTIVATE, SoundCategory.HOSTILE, 3, 1);
                }

                //Draw a particle line to the pulled entity
                Utils.particleLine(this.trackingParticle, location, entity.getLocation().add(0, 0.5, 0), 10);

                //Gravity effect
                final double forceLimit = Math.pow(entity.getLocation().distanceSquared(location), 0.125);
                final Vector velocity = entity.getLocation().subtract(location).toVector().normalize().multiply(-1 * Math.min(FORCE_INTENSITY, forceLimit));
                entity.setVelocity(velocity);
            })
        ;
    }

    @Override
    public String getSkullTag() {
        return "gravity";
    }

    
}
