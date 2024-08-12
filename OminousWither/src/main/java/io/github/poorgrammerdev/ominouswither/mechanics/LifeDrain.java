package io.github.poorgrammerdev.ominouswither.mechanics;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

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
import org.bukkit.scheduler.BukkitRunnable;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.CoroutineManager;
import io.github.poorgrammerdev.ominouswither.internal.ICoroutine;
import io.github.poorgrammerdev.ominouswither.internal.config.BossStat;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherLoadEvent;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherPhaseChangeEndEvent;
import io.github.poorgrammerdev.ominouswither.utils.ParticleInfo;
import io.github.poorgrammerdev.ominouswither.utils.ParticleShapes;
import io.github.poorgrammerdev.ominouswither.utils.Utils;

public class LifeDrain implements Listener {
    private final OminousWither plugin;

    private final HashMap<UUID, Integer> lastUsed;

    public LifeDrain(OminousWither plugin) {
        this.plugin = plugin;
        this.lastUsed = new HashMap<>();
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
        //TODO: do we need to wait until phase change ends otherwise? see similar issue in FlightAcceleration
    }

    private void enableAttackMechanism(final Wither wither) {
        final UUID witherID = wither.getUniqueId();

        final int cooldown = (int) this.plugin.getBossStatsManager().getStat(BossStat.LIFE_DRAIN_COOLDOWN, wither);
        final double rangeSq = Math.pow(this.plugin.getBossStatsManager().getStat(BossStat.LIFE_DRAIN_RANGE, wither), 2);
        final double maxSpeedSq = Math.pow(this.plugin.getBossStatsManager().getStat(BossStat.LIFE_DRAIN_SPEED_THRESHOLD, wither), 2);

        CoroutineManager.getInstance().enqueue(new ICoroutine() {
            @Override
            public boolean tick() {
                //If wither no longer exists -> cancel
                final Entity entity = plugin.getServer().getEntity(witherID);
                if (!(entity instanceof Wither) || entity.isDead() || !entity.isInWorld()) return false;

                //Check cooldown
                //If never used, then cannot be in cooldown. Otherwise check as normal
                final Wither wither = (Wither) entity;
                if (lastUsed.containsKey(witherID) && wither.getTicksLived() <= lastUsed.get(witherID) + cooldown) return true;

                //Must have a target
                final LivingEntity target = wither.getTarget();
                if (target == null || target.isDead() || !target.isInWorld()) return true;

                //Target must be in range and not moving too fast
                if (target.getVelocity().lengthSquared() > maxSpeedSq || wither.getLocation().distanceSquared(target.getLocation()) > rangeSq) return true;

                //If more than double cooldown time has passed or a random chance weighted on HP ratios, then activate move
                //This will give the illusion that the Wither becomes more desperate to use the life drain move as it loses more HP, without actually lessening its cooldown
                // TODO: do random chance here

                // Summon life drain circle
                final Location location = target.getLocation().add(0, target.getHeight() / 2.0, 0).add(target.getVelocity().normalize());
                final boolean success = createLifeDrainCircle(location, wither);

                //If activated properly, set on cooldown
                if (success) lastUsed.put(witherID, wither.getTicksLived());
                return true;
            }
        });
    }
    
    private boolean createLifeDrainCircle(final Location location, final Wither wither) {
        final World world = wither.getWorld();
        if (world == null) return false;

        final int activationTime = (int) this.plugin.getBossStatsManager().getStat(BossStat.LIFE_DRAIN_STARTUP_TIME, wither);
        final int activeTime = (int) this.plugin.getBossStatsManager().getStat(BossStat.LIFE_DRAIN_LIFESPAN, wither);
        final int drainInterval = (int) this.plugin.getBossStatsManager().getStat(BossStat.LIFE_DRAIN_ATTACK_INTERVAL, wither);
        final double radiusH = this.plugin.getBossStatsManager().getStat(BossStat.LIFE_DRAIN_HORIZONTAL_RADIUS, wither);
        final double radiusV = this.plugin.getBossStatsManager().getStat(BossStat.LIFE_DRAIN_VERTICAL_RADIUS, wither);

        final int totalDuration = activationTime + activeTime;

        final ParticleInfo circleParticle = new ParticleInfo(Particle.SOUL_FIRE_FLAME, 1, 0, 0, 0, 0, null, true);
        final ParticleInfo centerParticle = new ParticleInfo(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, 2, 0.125, 0.125, 0.125, 0.1);
        final ParticleInfo hitParticle = new ParticleInfo(Particle.RAID_OMEN, 3, 0.125, 0.125, 0.125, 1.5);

        final Location bottomLoc = location.clone().subtract(0, radiusV, 0);
        final Location topLoc = location.clone().add(0, radiusV, 0);

        //Must be tick-accurate now, using runnable instead of Coroutine system
        new BukkitRunnable() {
            private final HashSet<UUID> players = new HashSet<>();
            private int i = 0;

            @Override
            public void run() {
                if (i >= totalDuration) {
                    this.cancel();
                    return;
                }
                
                //Display particle circle
                ParticleShapes.partialCircle(circleParticle, radiusH, 10, location, Utils.lerp(0, 2*Math.PI, ((double) i / activationTime)));
                
                //If fully activated...
                if (i >= activationTime) {
                    //Display activation indicator
                    ParticleShapes.line(centerParticle, bottomLoc, topLoc, 10);

                    //Drain HP periodically
                    if (i % drainInterval == 0) {
                        //Get target entities
                        final Collection<Entity> entities = world.getNearbyEntities(location, radiusH, radiusV, radiusH,
                            (entity -> (
                                entity instanceof LivingEntity &&
                                !entity.isDead() &&
                                entity.isInWorld() &&
                                !Tag.ENTITY_TYPES_WITHER_FRIENDS.isTagged(entity.getType()) &&
                                !plugin.isMinion(entity) &&
                                !(entity instanceof Wither)
                            ))
                        );

                        double hpDrained = 0.0D;
                        for (final Entity e : entities) {
                            final LivingEntity entity = (LivingEntity) e;

                            final double currentHealth = entity.getHealth();
                            if (currentHealth <= 1.0D) continue;

                            //If player, check if targetable and also play SFX
                            if (entity instanceof Player) {
                                final Player player = (Player) entity;
                                if (!Utils.isTargetable(player)) continue;

                                //Attention-getting warning sound to alert the player
                                //Only happens once per life drain instance to not spam the player with an annoying sound
                                if (!this.players.contains(player.getUniqueId())) {
                                    player.playSound(player, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 3.0f, 1.0f);
                                    this.players.add(player.getUniqueId());
                                }
                                
                            }

                            //Drain HP
                            entity.setHealth(currentHealth - 1.0D);
                            hpDrained += 1.0D;

                            //Display hit particle and play regular drain sound
                            hitParticle.spawnParticle(world, entity.getLocation().add(0, entity.getHeight() / 2.0, 0));
                            world.playSound(entity, Sound.ENTITY_ITEM_BREAK, SoundCategory.HOSTILE, 3.0f, 1.0f);
                        }

                        //Heal the Wither with drained HP
                        if (wither != null && !wither.isDead() && wither.isInWorld()) {
                            final double hpBound = wither.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2.0;

                            wither.setHealth(Math.min(wither.getHealth() + hpDrained, hpBound));
                        }
                    }
                }

                ++i;
            }
        }.runTaskTimer(plugin, 0L, 0L);

        return true;
    }

}
