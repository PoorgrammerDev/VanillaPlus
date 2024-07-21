package io.github.poorgrammerdev.ominouswither;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;

import io.github.poorgrammerdev.ominouswither.backend.CoroutineManager;
import io.github.poorgrammerdev.ominouswither.backend.ICoroutine;
import io.github.poorgrammerdev.ominouswither.backend.OminousWitherActivateEvent;
import io.github.poorgrammerdev.ominouswither.backend.OminousWitherLoadEvent;

public class FlightSpeed implements Listener {
    private static final int DISTANCE_THRESHOLD = 15;
    private static final int DISTANCE_THRESHOLD_SQUARED = DISTANCE_THRESHOLD * DISTANCE_THRESHOLD;

    private final OminousWither plugin;

    public FlightSpeed(final OminousWither plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onActivate(final OminousWitherActivateEvent event) {
        final Wither wither = event.getWither();
        final int level = wither.getPersistentDataContainer().getOrDefault(this.plugin.getLevelKey(), PersistentDataType.INTEGER, 1);

        this.flightBehavior(wither, level);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLoad(final OminousWitherLoadEvent event) {
        final Wither wither = event.getWither();
        final int level = wither.getPersistentDataContainer().getOrDefault(this.plugin.getLevelKey(), PersistentDataType.INTEGER, 1);

        //If the Wither has already been activated, then just enable the flight behavior
        final boolean isActivated = wither.getPersistentDataContainer().getOrDefault(this.plugin.getIsFullySpawnedKey(), PersistentDataType.BOOLEAN, false);
        if (isActivated) {
            this.flightBehavior(wither, level);
            return;
        }

        //Otherwise, wait until the Wither activates and then do it
        final UUID witherID = wither.getUniqueId();
        CoroutineManager.getInstance().enqueue(new ICoroutine() {
            @Override
            public void tick() {
                //TODO: sign of a bad architecture? should tick itself return if it should be rescheduled or not instead of having two diff. methods?
                return;
            }

            @Override
            public boolean shouldBeRescheduled() {
                final Entity entity = plugin.getServer().getEntity(witherID);
                //Wither has despawned or unloaded again -> cancel
                if (!(entity instanceof Wither)) return false;

                //Wither has activated -> turn on flight behavior and cancel
                if (entity.getPersistentDataContainer().getOrDefault(plugin.getIsFullySpawnedKey(), PersistentDataType.BOOLEAN, false)) {
                    flightBehavior((Wither) entity, level);
                    return false;
                }

                //Wither is here but hasn't activated yet
                return true;
            }
        });

    }

    public void flightBehavior(final Wither wither, final int level) {
        //Only applies to Withers level 3 and up
        if (level < 3) return;

        final UUID witherID = wither.getUniqueId();
        CoroutineManager.getInstance().enqueue(new ICoroutine() {
            @Override
            public void tick() {
                final Entity entity = plugin.getServer().getEntity(witherID);
                if (!(entity instanceof Wither)) return;

                //Must have a target and have line of sight to it
                final Wither wither = (Wither) entity;
                final LivingEntity target = wither.getTarget();
                if (target == null || !wither.hasLineOfSight(target)) return;

                //Wither cannot be close to its target -- this would likely mess up its AI
                final Location witherLoc = wither.getLocation();
                final Location targetLoc = target.getLocation();
                final double distance = witherLoc.distanceSquared(targetLoc);
                if (distance < DISTANCE_THRESHOLD_SQUARED) return;

                //Sets velocity to face towards the target and multiplies it by the Wither's flight speed (determined by level)
                entity.setVelocity(targetLoc.subtract(witherLoc).toVector().normalize().multiply(getFlightSpeed(level)));
            }

            @Override
            public boolean shouldBeRescheduled() {
                return (plugin.getServer().getEntity(witherID) != null);
            }
        });
    }

    private double getFlightSpeed(final int level) {
        switch (level) {
            case 3:
                return 0.75;
            case 4:
                return 1.5;
            case 5:
                return 10.0;
            default:
                return 0.5;
        }
    }

}
