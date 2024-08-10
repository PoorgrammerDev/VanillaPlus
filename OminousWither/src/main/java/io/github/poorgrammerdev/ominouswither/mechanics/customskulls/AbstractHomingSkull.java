package io.github.poorgrammerdev.ominouswither.mechanics.customskulls;

import java.util.Arrays;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.Wither.Head;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.config.BossStat;
import io.github.poorgrammerdev.ominouswither.utils.ParticleInfo;
import io.github.poorgrammerdev.ominouswither.utils.Utils;

/**
 * Represents any dangerous skull that homes in on its target and follows it
 * @author Thomas Tran
 */
public abstract class AbstractHomingSkull extends AbstractSkullHandler {
    protected static final double DEACTIVATE_DISTANCE_SQ = 1.0D;

    protected final BossStat homingLifespanSetting;
    protected final double searchRange;
    protected final int searchInterval;
    protected final boolean canChangeTarget;

    protected final Random random;

    /**
     * Constructor with target switching options
     * @param plugin instance of main plugin class
     * @param initialSpeedSetting setting to look up for speed multiplier on launch
     * @param trackingParticle skull particle trail
     * @param homingLifespan how long the projectile can continue homing for. set to SKULL_LIFESPAN to have the effect active for the entire lifespan of the skull
     * @param searchRange how far to search for a target
     * @param searchInterval how often to search for a target
     * @param canChangeTarget if the skull can search for a new target after one has already been found
     */
    public AbstractHomingSkull(OminousWither plugin, BossStat initialSpeedSetting, ParticleInfo trackingParticle, BossStat homingLifespanSetting, double searchRange, int searchInterval, boolean canChangeTarget) {
        super(plugin, initialSpeedSetting, trackingParticle);

        this.homingLifespanSetting = homingLifespanSetting;
        this.searchRange = searchRange;
        this.canChangeTarget = canChangeTarget;
        this.searchInterval = searchInterval;
        this.random = new Random();
    }

    @Override
    public void onSpawn(WitherSkull skull, Wither shooter) {
        super.onSpawn(skull, shooter);
        
        final LivingEntity initialTarget = this.getInitialTarget(skull);
        final double speed = skull.getVelocity().length();
        final double homingLifespan = this.plugin.getBossStatsManager().getStat(this.homingLifespanSetting, shooter);

        new BukkitRunnable() {
            private LivingEntity target = initialTarget;
            private int t = 0;

            @Override
            public void run() {
                //Skull must be still alive
                if (skull == null || skull.isDead() || skull.getTicksLived() >= homingLifespan || !skull.isInWorld()) {
                    this.cancel();
                    return;
                }

                //Condition to search for a new target
                if (this.target == null || (canChangeTarget && t >= searchInterval)) {
                    this.target = getNearestTarget(skull);
                    t = 0;
                }

                //If target is still invalid, don't do anything this tick
                if (this.target == null || this.target.isDead() || !this.target.isInWorld()) return;

                final Location skullLocation = skull.getLocation();
                final Location targetLocation = this.target.getEyeLocation();
                final Vector direction = targetLocation.clone().subtract(skullLocation).toVector();

                //Move towards the target at a constant speed
                skull.setVelocity(direction.normalize().multiply(speed));

                //Prevent strange collision glitch
                //Glitch: skull reaches target but does not collide, causing glitchy spazzing movement as it continues to try to set its velocity towards the target
                //Currrent patchwork solution: disable homing once distance threshold reached
                if (skullLocation.distanceSquared(targetLocation) <= DEACTIVATE_DISTANCE_SQ) {
                    this.cancel();
                    return;
                }

                ++t;
            }

        }.runTaskTimer(plugin, 0L, 0L);

        

    }

    /**
     * Gets an initial target for the WitherSkull if available
     * Searches through the Wither's three heads' targets for the closest entity
     * @param skull homing skull
     * @return a living target, or null if not found
     */
    protected LivingEntity getInitialTarget(final WitherSkull skull) {
        if (!(skull.getShooter() instanceof Wither)) return null;

        final Wither wither = (Wither) skull.getShooter();
        final Location skullLocation = skull.getLocation();
        
        //Return the closest Wither target to the skull
        return Arrays.asList(Head.values())
            .stream()

            //Get this head's target
            .map(wither::getTarget)

            //Entity must exist
            .filter((entity) -> (entity != null && !entity.isDead()))

            //Get the closest entity
            .min((entity1, entity2) -> (
                Double.valueOf(skullLocation.distanceSquared(entity1.getLocation()))
                .compareTo(
                    Double.valueOf(skullLocation.distanceSquared(entity2.getLocation()))
                )
            ))
            .orElse(null)
        ;
    }

    /**
     * Gets the closest non-wither-friend living entity to the skull
     * @param skull homing skull
     * @return a living target, or null if not found
     */
    protected LivingEntity getNearestTarget(final WitherSkull skull) {
        final Location skullLocation = skull.getLocation();

        return skull.getNearbyEntities(this.searchRange, this.searchRange, this.searchRange)
            .stream()
            .filter((entity) -> (
                //Entity must exist and be living
                (entity instanceof LivingEntity) &&

                //Cannot be a friendly
                !Tag.ENTITY_TYPES_WITHER_FRIENDS.isTagged(entity.getType()) &&

                //Cannot be invulnerable
                !entity.isInvulnerable() &&

                //Friendly cases should all be handled already up top but as a double check
                !this.plugin.isMinion(entity) &&
                !(entity instanceof Wither) &&

                //If it is a player, it must be targetable
                (!(entity instanceof Player) || (Utils.isTargetable((Player) entity)))
            ))
            .map((entity) -> ((LivingEntity) entity))
            
            //Get the closest entity
            .min((entity1, entity2) -> (
                Double.valueOf(skullLocation.distanceSquared(entity1.getLocation()))
                .compareTo(
                    Double.valueOf(skullLocation.distanceSquared(entity2.getLocation()))
                )
            ))
            .orElse(null)
        ;
    }
    
}
