package io.github.poorgrammerdev.ominouswither.mechanics;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherActivateEvent;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherLoadEvent;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherPhaseChangeEndEvent;
import io.github.poorgrammerdev.ominouswither.utils.Utils;
import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.CoroutineManager;
import io.github.poorgrammerdev.ominouswither.internal.ICoroutine;
import io.github.poorgrammerdev.ominouswither.internal.config.BossStat;

/**
 * Handles the mechanic where the Ominous Wither rapidly accelerates towards its target
 * @author Thomas Tran
 */
public class FlightAcceleration implements Listener {
    private final OminousWither plugin;

    public FlightAcceleration(final OminousWither plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    private void onActivate(final OminousWitherActivateEvent event) {
        this.flightBehavior(event.getWither());
    }

    @EventHandler(ignoreCancelled = true)
    private void onSecondPhaseEnter(final OminousWitherPhaseChangeEndEvent event) {
        this.flightBehavior(event.getWither());
    }

    @EventHandler(ignoreCancelled = true)
    private void onLoad(final OminousWitherLoadEvent event) {
        final Wither wither = event.getWither();

        //Applies to both first and second phases
        //If the Wither has already been activated, then just enable the flight behavior
        if (wither.getInvulnerabilityTicks() <= 0) {
            this.flightBehavior(wither);
            return;
        }

        //Otherwise, wait until the Wither activates and then do it
        final UUID witherID = wither.getUniqueId();
        CoroutineManager.getInstance().enqueue(new ICoroutine() {
            @Override
            public boolean tick() {
                final Entity entity = plugin.getServer().getEntity(witherID);
                //Wither has despawned or unloaded again -> cancel
                if (!(entity instanceof Wither)) return false;
                final Wither wither = (Wither) entity;
                if (wither.isDead() || !wither.isInWorld()) return false;

                //Wither has activated -> turn on flight behavior and cancel
                if (wither.getInvulnerabilityTicks() <= 0) {
                    flightBehavior((Wither) entity);
                    return false;
                }

                //Wither is here but hasn't activated yet
                return true;
            }
        });

    }

    /**
     * Activates flight acceleration mechanic on Wither, respects phases
     */
    private void flightBehavior(final Wither wither) {
        final double distanceThresholdSq = Math.pow(this.plugin.getBossStatsManager().getStat(BossStat.FLIGHT_ACCELERATION_DISTANCE_THRESHOLD, wither), 2);
        final double flightSpeed = this.plugin.getBossStatsManager().getStat(BossStat.FLIGHT_SPEED, wither);

        //If flight speed is not positive then this Wither does not have the flight system enabled
        if (flightSpeed <= 0.0D) return;

        //Check phase of Wither and begin respective flight patterns
        if (!wither.getPersistentDataContainer().getOrDefault(plugin.getSecondPhaseKey(), PersistentDataType.BOOLEAN, false)) {
            //First phase
            this.phaseOneFlight(wither, distanceThresholdSq, flightSpeed);
        }
        else {
            //Second phase
            this.phaseTwoFlight(wither, distanceThresholdSq, flightSpeed);
        }
    }

    /**
     * Accelerate towards a target if present, is far enough, and has line of sight
     */
    private void phaseOneFlight(final Wither wither, final double distanceThresholdSq, final double flightSpeed) {
        final UUID witherID = wither.getUniqueId();

        CoroutineManager.getInstance().enqueue(new ICoroutine() {
            @Override
            public boolean tick() {
                final Entity entity = plugin.getServer().getEntity(witherID);
                if (!(entity instanceof Wither)) return false;

                //If entered second phase -> cancel
                final Wither wither = (Wither) entity;
                if (wither.getPersistentDataContainer().getOrDefault(plugin.getSecondPhaseKey(), PersistentDataType.BOOLEAN, false)) return false;

                //Must have a target and have line of sight to it
                final LivingEntity target = wither.getTarget();
                if (target == null || !wither.hasLineOfSight(target)) return true;

                //Wither cannot be close to its target -- this would likely mess up its AI
                final Location witherLoc = wither.getLocation();
                final Location targetLoc = target.getLocation();
                final double distance = witherLoc.distanceSquared(targetLoc);
                if (distance < distanceThresholdSq) return true;

                //Sets velocity to face towards the target and multiplies it by the Wither's flight speed (determined by level)
                entity.setVelocity(targetLoc.subtract(witherLoc).toVector().normalize().multiply(flightSpeed));
                return true;
            }
        });
    }

    /**
     * Accelerate towards a target if present, is far enough, and has no wither-immune blocks occluding them
     * Break all nearby non-wither-immune blocks periodically during acceleration
     */
    private void phaseTwoFlight(final Wither wither, final double distanceThresholdSq, final double flightSpeed) {
        final int range = (int) this.plugin.getBossStatsManager().getStat(BossStat.ENHANCED_BREAK_RANGE, wither);
        final int height = (int) this.plugin.getBossStatsManager().getStat(BossStat.ENHANCED_BREAK_HEIGHT, wither);
        final int interval = (int) this.plugin.getBossStatsManager().getStat(BossStat.ENHANCED_BREAK_INTERVAL, wither);

        final UUID witherID = wither.getUniqueId();
        CoroutineManager.getInstance().enqueue(new ICoroutine() {
            @Override
            public boolean tick() {
                final Entity entity = plugin.getServer().getEntity(witherID);
                if (!(entity instanceof Wither)) return false;

                final Wither wither = (Wither) entity;

                //Must have a target that's in breakable line of sight
                final LivingEntity target = wither.getTarget();
                if (target == null || !Utils.hasBreakableLineOfSight(wither.getEyeLocation(), target.getEyeLocation())) return true;

                //Wither cannot be close to its target if there is direct line of sight
                final Location witherLoc = wither.getLocation();
                final Location targetLoc = target.getLocation();
                final double distance = witherLoc.distanceSquared(targetLoc);
                if (wither.hasLineOfSight(target) && distance < distanceThresholdSq) return true;

                //Wither flies at constant speed towards target
                entity.setVelocity(targetLoc.subtract(witherLoc).toVector().normalize().multiply(flightSpeed));

                //Wither smashes through all breakable blocks in the way every so often
                //Not a cooldown-based system per se, just an interval based system (i.e. still goes "cooldown" if not used)
                //Non-positive interval indicates deactivation of system
                if (interval > 0 && wither.getTicksLived() % interval == 0) {
                    enhancedBlockBreaking(wither, target, range, height);
                }
                return true;
            }
        });
    }

    /**
     * Break all nearby non-wither-immune blocks
     */
    private void enhancedBlockBreaking(final Wither wither, final LivingEntity target, final int range, final int height) {
        final Location witherLoc = wither.getLocation();
        
        //Get middle location of Wither
        witherLoc.add(0, wither.getHeight() / 2, 0); 

        //Move center breakage point slightly towards the target
        final Vector direction = target.getLocation().subtract(witherLoc).toVector();
        witherLoc.add(direction.normalize());

        //Break blocks in range
        boolean brokeAnyBlocks = false;
        for (int x = -range; x <= range; ++x) {
            for (int y = -height; y <= height; ++y) {
                for (int z = -range; z <= range; ++z) {
                    //Add offset to get the desired location
                    witherLoc.add(x, y, z);
    
                    //Break block if valid
                    final Block block = witherLoc.getBlock();
                    if (block != null && !block.getType().isAir() && !block.isLiquid() && !Tag.WITHER_IMMUNE.isTagged(block.getType())) {
                        block.breakNaturally();
                        if (!brokeAnyBlocks) brokeAnyBlocks = true;
                    }
    
                    //Subtract offset to reset the location for the next iteration
                    witherLoc.subtract(x, y, z);
                }
            }
        }

        //Play sfx if any blocks were actually broken
        if (brokeAnyBlocks) {
            wither.getWorld().playSound(wither, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.HOSTILE, 1.0f, 1.0f);
        }
    }

}
