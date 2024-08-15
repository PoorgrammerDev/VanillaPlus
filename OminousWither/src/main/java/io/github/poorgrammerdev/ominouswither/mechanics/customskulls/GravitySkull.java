package io.github.poorgrammerdev.ominouswither.mechanics.customskulls;

import org.bukkit.Color;
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
import io.github.poorgrammerdev.ominouswither.internal.config.BossStat;
import io.github.poorgrammerdev.ominouswither.utils.ParticleInfo;
import io.github.poorgrammerdev.ominouswither.utils.ParticleShapes;
import io.github.poorgrammerdev.ominouswither.utils.Utils;

/**
 * Skull that pulls in enemies 
 */
public class GravitySkull extends AbstractSkullHandler {

    public GravitySkull(OminousWither plugin) {
        super(plugin, BossStat.GRAVITY_SKULL_SPEED, new ParticleInfo(Particle.DUST, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.FUCHSIA, 1.5f)));
    }

    @Override
    public void onHit(final ProjectileHitEvent event, final Wither wither) {
        final Location location = event.getEntity().getLocation().add(0, 0.5, 0);
        final World world = location.getWorld();
        if (world == null) return;

        final double radius = this.plugin.getBossStatsManager().getStat(BossStat.GRAVITY_RADIUS, wither);
        final double forceIntensity = this.plugin.getBossStatsManager().getStat(BossStat.GRAVITY_FORCE_INTENSITY, wither);

        //Particle circle
        ParticleShapes.circle(this.trackingParticle, radius, 4, location);

        //Gravity mechanic
        world.getNearbyEntities(location, radius, radius, radius, (entity) -> (!Tag.ENTITY_TYPES_WITHER_FRIENDS.isTagged(entity.getType())))
            .stream()
            
            .filter((entity) -> (
                //Entity cannot be invulnerable
                !entity.isInvulnerable() &&

                //Friendly cases should all be handled already up top but as a double check:
                !this.plugin.isMinion(entity) &&
                !(entity instanceof Wither) &&

                //Cannot affect other wither skulls
                !(entity instanceof WitherSkull) &&

                //If player -> must be targetable
                (!(entity instanceof Player) || (Utils.isTargetable((Player) entity))) &&

                //Must be in radius
                location.distanceSquared(entity.getLocation()) <= (radius * radius)
            ))

            //NOTE: currently does not do a visibility check, meaning this can hit through walls | TODO: should this behaviour be changed?

            .forEach((entity) -> {
                //Special case for players
                if (entity instanceof Player) {
                    final Player player = (Player) entity;

                    //Play SFX only to affected players
                    player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_DEACTIVATE, SoundCategory.HOSTILE, 3, 1);
                }

                //Draw a particle line to the pulled entity
                ParticleShapes.line(this.trackingParticle, location, entity.getLocation().add(0, 0.5, 0), 2);

                //Gravity effect
                final double forceLimit = Math.pow(entity.getLocation().distanceSquared(location), 0.125);
                final Vector velocity = entity.getLocation().subtract(location).toVector().normalize().multiply(-1 * Math.min(forceIntensity, forceLimit));
                entity.setVelocity(velocity);
            })
        ;
    }

    @Override
    public String getSkullTag() {
        return "gravity";
    }

    
}
