package io.github.poorgrammerdev.ominouswither.mechanics;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.UUID;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.ICoroutine;
import io.github.poorgrammerdev.ominouswither.internal.config.BossStat;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherLoadEvent;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherPhaseChangeEndEvent;
import io.github.poorgrammerdev.ominouswither.utils.ParticleInfo;
import io.github.poorgrammerdev.ominouswither.utils.ParticleShapes;
import io.github.poorgrammerdev.ominouswither.utils.Utils;

/**
 * Second Phase attack to directly drain HP from targets, bypassing armor
 * @author Thomas Tran
 */
public class LifeDrain implements Listener {
    /**
     * When velocity of target cannot be directly retrieved without accuracy loss, infer velocity by recording movement over a span of ticks
     * Larger values will delay the attack longer but likely give more accurate aim
     */
    private static final int VELOCITY_INFERENCE_DURATION = 10;

    private final OminousWither plugin;
    private final Random random;

    /**
     * Holds when a Wither last used the Life Drain attack, measured using Spigot's ticks lived metric
     */
    //TODO: should this use Instant/Duration instead?
    private final HashMap<UUID, Integer> lastUsed;

    public LifeDrain(OminousWither plugin) {
        this.plugin = plugin;
        this.lastUsed = new HashMap<>();
        this.random = new Random();
    }

    @EventHandler(ignoreCancelled = true)
    private void onSecondPhaseActivate(final OminousWitherPhaseChangeEndEvent event) {
        this.enableAttackMechanism(event.getWither());
    }

    @EventHandler(ignoreCancelled = true)
    private void onLoad(final OminousWitherLoadEvent event) {
        final Wither wither = event.getWither();
        //If fully in second phase -> activate
        if (wither.getInvulnerabilityTicks() <= 0 && wither.getPersistentDataContainer().getOrDefault(plugin.getSecondPhaseKey(), PersistentDataType.BOOLEAN, false)) {
            this.enableAttackMechanism(wither);
        }

        //NOTE: Does not need to wait until invuln is over here: that will be handled in the PhaseChangeEndEvent handler.
    }

    /**
     * Allows this wither to use the Life Drain attacks when appropriate
     */
    private void enableAttackMechanism(final Wither wither) {
        final UUID witherID = wither.getUniqueId();

        final int activationTime = (int) this.plugin.getBossStatsManager().getStat(BossStat.LIFE_DRAIN_STARTUP_TIME, wither);
        final int cooldown = (int) this.plugin.getBossStatsManager().getStat(BossStat.LIFE_DRAIN_COOLDOWN, wither);
        final double rangeSq = Math.pow(this.plugin.getBossStatsManager().getStat(BossStat.LIFE_DRAIN_RANGE, wither), 2);

        this.plugin.getCoroutineManager().enqueue(new ICoroutine() {
            @Override
            public boolean tick() {
                //If wither no longer exists -> cancel
                final Entity entity = plugin.getServer().getEntity(witherID);
                if (!(entity instanceof Wither) || entity.isDead() || !entity.isInWorld()) {
                    LifeDrain.this.lastUsed.remove(witherID);
                    return false;
                }

                //Check cooldown
                //If never used, then cannot be in cooldown. Otherwise check as normal
                final Wither wither = (Wither) entity;
                if (lastUsed.containsKey(witherID) && wither.getTicksLived() <= lastUsed.get(witherID) + cooldown) return true;

                //Must have a target that's in range
                final LivingEntity target = wither.getTarget();
                if (target == null || target.isDead() || !target.isInWorld() || wither.getLocation().distanceSquared(target.getLocation()) > rangeSq) return true;

                //Special case for if the target is on ground - cannot get their velocity, must infer from movement
                if (Utils.isOnGround(target.getLocation(), 3.5D)) {
                    final Location[] locations = new Location[VELOCITY_INFERENCE_DURATION];

                    new BukkitRunnable() {
                        private int i = 0;
                        @Override
                        public void run() {
                            if (i >= VELOCITY_INFERENCE_DURATION) {
                                //Infer velocity from recorded movement and use to get target location
                                Location targetLocation = getTargetLocation(target, calculateVelocity(locations), activationTime);

                                //Attempt to get a spot on the ground within reasonable range
                                final Location groundLocation = Utils.tryGetGround(targetLocation, Math.max(targetLocation.getBlockY() - 10, target.getWorld().getMinHeight()));
                                if (groundLocation != null) {
                                    targetLocation = groundLocation.add(0, 0.5, 0);
                                }

                                //Summon life drain circle
                                new LifeDrainConstruct(plugin, targetLocation, wither).runTaskTimer(plugin, 0L, 0L);

                                this.cancel();
                                return;
                            }

                            //Record locations
                            locations[i] = target.getLocation();
                            ++i;
                        }
                    }.runTaskTimer(plugin, 0L, 0L);
                }
                //Target's velocity can be directly retrieved - continue like normal
                else {
                    //Get target location and summon life drain circle
                    final Location targetLocation = getTargetLocation(target, target.getVelocity(), activationTime);
                    new LifeDrainConstruct(plugin, targetLocation, wither).runTaskTimer(plugin, 0L, 0L);
                }

                //Set on cooldown for either case
                lastUsed.put(witherID, wither.getTicksLived());
                return true;
            }
        });
    }
    
    /**
     * Aims the Life Drain construct 
     * @param target target entity
     * @param velocity inferred velocity of target
     * @param activationTime how long the construct will take to begin dealing damage
     * @return location to spawn construct at
     */
    private Location getTargetLocation(final LivingEntity target, final Vector velocity, final int activationTime) {
        if (velocity.lengthSquared() <= 0.0D) return target.getEyeLocation();

        final double lengthVariance = (random.nextDouble() * 0.75D) + 0.75D;
        final Location location = target.getEyeLocation();
        final World world = target.getWorld();

        final RayTraceResult result = world.rayTraceBlocks(location, velocity, velocity.length() * activationTime * lengthVariance, FluidCollisionMode.ALWAYS, true);
        if (result == null || result.getHitPosition() == null) {
            return location.add(velocity.clone().multiply(activationTime * lengthVariance));
        }

        return result.getHitPosition().toLocation(world).add(0, 0.25, 0);
    }

    private Vector calculateVelocity(final Location[] locations) {
        final int n = locations.length - 1;

        final Vector velocity = new Vector(0.0D, 0.0D, 0.0D);
        for (int i = 0; i < n; ++i) {
            //Add the difference between each location
            velocity.add(locations[i + 1].toVector().subtract(locations[i].toVector()));
        }

        //Scale down
        velocity.multiply(1.0D / n);

        return velocity;
    }

    /**
     * Represents the actual particle construct attack
     * @author Thomas Tran
     */
    private class LifeDrainConstruct extends BukkitRunnable {

        private final Wither wither;
        private final Location location;
        private final World world;

        private final int activationTime;
        private final int activeTime;
        private final int drainInterval;
        private final double healMultiplier;
        private final double curseProbability;
        private final double radiusH;
        private final double radiusV;

        private final ParticleInfo circleParticle;
        private final ParticleInfo centerParticle;
        private final ParticleInfo hitParticle;

        private final int totalDuration;
        private final Location bottomLoc;
        private final Location topLoc;

        private final HashSet<UUID> previousTargets;

        private boolean hasPlayedActivationSound;
        private int i;

        /**
         * Summons a Life Drain particle construct at the desired location
         * @param location location to spawn the construct
         * @param wither wither that summoned it (drained HP will heal this wither)
         * @return if the construct was created properly
         */
        public LifeDrainConstruct(final OminousWither plugin, final Location location, final Wither wither) {
            this.wither = wither;
            this.location = location;
            this.world = wither.getWorld();

            this.activationTime = (int) plugin.getBossStatsManager().getStat(BossStat.LIFE_DRAIN_STARTUP_TIME, wither);
            this.activeTime = (int) plugin.getBossStatsManager().getStat(BossStat.LIFE_DRAIN_LIFESPAN, wither);
            this.drainInterval = (int) plugin.getBossStatsManager().getStat(BossStat.LIFE_DRAIN_ATTACK_INTERVAL, wither);
            this.healMultiplier = plugin.getBossStatsManager().getStat(BossStat.LIFE_DRAIN_HEAL_MULTIPLIER, wither);
            this.curseProbability = plugin.getBossStatsManager().getStat(BossStat.LIFE_DRAIN_CURSE_PROBABILITY, wither);
            this.radiusH = plugin.getBossStatsManager().getStat(BossStat.LIFE_DRAIN_HORIZONTAL_RADIUS, wither);
            this.radiusV = plugin.getBossStatsManager().getStat(BossStat.LIFE_DRAIN_VERTICAL_RADIUS, wither);
    
            this.circleParticle = new ParticleInfo(Particle.SOUL_FIRE_FLAME, 1, 0, 0, 0, 0, null, true);
            this.centerParticle = new ParticleInfo(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, 2, 0.125, 0.125, 0.125, 0.1);
            this.hitParticle = new ParticleInfo(Particle.RAID_OMEN, 3, 0.125, 0.125, 0.125, 1.5);
    
            this.totalDuration = activationTime + activeTime;
            this.bottomLoc = location.clone().subtract(0, radiusV, 0);
            this.topLoc = location.clone().add(0, radiusV, 0);
        
            this.previousTargets = new HashSet<>();
            this.hasPlayedActivationSound = false;

            this.i = 0;
        }

            @Override
            public void run() {
                if (i >= totalDuration) {
                    //Play despawn sound
                    world.playSound(location, Sound.BLOCK_SHULKER_BOX_CLOSE, SoundCategory.HOSTILE, 3.0f, 1.0f);

                    this.cancel();
                    return;
                }
                
                //Display particle circle
                ParticleShapes.partialCircle(circleParticle, radiusH, 8, location, Utils.lerp(0, 2*Math.PI, ((double) i / activationTime)));
                
                //If fully activated...
                if (i >= activationTime) {
                    //Display active indicator
                    ParticleShapes.line(centerParticle, bottomLoc, topLoc, 8);

                    //One-time activation sound
                    if (!this.hasPlayedActivationSound) {
                        world.playSound(location, Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.HOSTILE, 3.0f, 1.0f);

                        this.hasPlayedActivationSound = true;
                    }

                    //Drain HP periodically
                    if (i % drainInterval == 0) {
                        //Get target entities
                        final Collection<Entity> entities = world.getNearbyEntities(location, radiusH, radiusV, radiusH,
                            (entity -> (
                                //Must be a LivingEntity that's alive
                                entity instanceof LivingEntity &&
                                !entity.isDead() &&
                                entity.isInWorld() &&

                                //Cannot be a friendly
                                !Tag.ENTITY_TYPES_WITHER_FRIENDS.isTagged(entity.getType()) &&
                                !plugin.isMinion(entity) &&
                                !(entity instanceof Wither)
                            ))
                        );

                        double hpDrained = 0.0D;
                        for (final Entity e : entities) {
                            //Already checked that e instanceof LivingEntity above, can cast safely here without check
                            final LivingEntity entity = (LivingEntity) e;
                            if (entity.isInvulnerable()) continue;

                            //Cannot kill entities
                            final double currentHealth = entity.getHealth();
                            if (currentHealth <= 1.0D) continue;

                            //Determine if first hit and should apply curse
                            //Not directly performing curse/register here since untargetable players may still exist here
                            final boolean isNewTarget = !this.previousTargets.contains(entity.getUniqueId());
                            final boolean applyCurse = isNewTarget && (random.nextDouble() < this.curseProbability);

                            //If player, check if targetable and also play SFX
                            if (entity instanceof Player) {
                                final Player player = (Player) entity;
                                if (!Utils.isTargetable(player)) continue;

                                //Attention-getting warning sound to alert the player
                                //If player is to have their Absorption hearts removed -> play special curse sound to indicate this
                                //Otherwise, play some other loud sound to still get their attention
                                if (isNewTarget) {
                                    player.playSound(player, applyCurse ? Sound.ENTITY_ELDER_GUARDIAN_CURSE : Sound.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, 3.0f, 1.0f);
                                }
                            }

                            //Apply first hit mechanism
                            if (isNewTarget) {
                                if (applyCurse) {
                                    entity.removePotionEffect(PotionEffectType.ABSORPTION);
                                }

                                this.previousTargets.add(entity.getUniqueId());
                            }

                            //Drain HP
                            entity.setHealth(currentHealth - 1.0D);
                            hpDrained += 1.0D;

                            //Display hit particle
                            hitParticle.spawnParticle(world, entity.getLocation().add(0, entity.getHeight() / 2.0, 0));
                        }

                        //If any entity was hit:
                        if (hpDrained > 0.0D) {
                            //Play regular drain sound
                            world.playSound(location, Sound.ENTITY_ITEM_BREAK, SoundCategory.HOSTILE, 1.0f, 1.0f);

                            //Heal the Wither with drained HP
                            if (wither != null && !wither.isDead() && wither.isInWorld()) {
                                final double hpBound = wither.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2.0;

                                wither.setHealth(Math.min(wither.getHealth() + (hpDrained * this.healMultiplier), hpBound));
                            }
                        }

                    }
                }

                ++i;
            }
        }

}
