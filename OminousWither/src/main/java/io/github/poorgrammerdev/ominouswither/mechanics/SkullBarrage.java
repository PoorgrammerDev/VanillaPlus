package io.github.poorgrammerdev.ominouswither.mechanics;

import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.config.BossStat;

public class SkullBarrage implements Listener {
    private final OminousWither plugin;

    /**
     * Each skull before the final one in the barrage will not produce iframes on the hit target
     * This is so the next skull can hit the target
     */
    private final NamespacedKey isInvulnCancelling;

    public SkullBarrage(final OminousWither plugin) {
        this.plugin = plugin;
        this.isInvulnCancelling = new NamespacedKey(plugin, "is_invulnerability_frame_cancelling");
    }

    /**
     * Each normal skull (black skull) an Ominous Wither fires will be a barrage 
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onSkullLaunch(final ProjectileLaunchEvent event) {
        //Must be a skull
        if (event.getEntityType() != EntityType.WITHER_SKULL || !(event.getEntity() instanceof WitherSkull)) return;

        //Must be a black skull
        final WitherSkull skull = (WitherSkull) event.getEntity();
        if (skull.isCharged()) return;

        //Shooter of skull must be a Wither
        if (!(skull.getShooter() instanceof Wither)) return;

        //Wither must be Ominous
        final Wither wither = (Wither) skull.getShooter();
        if (!this.plugin.isOminous(wither)) return;

        final World witherWorld = wither.getWorld();
        if (witherWorld == null) return;

        //Get setting context
        final int level = this.plugin.getLevel(wither, 1);
        final Difficulty difficulty = witherWorld.getDifficulty();
        
        //Velocity buff of black skulls based on level
        final double speed = this.plugin.getBossStatsManager().getStat(BossStat.NORMAL_SKULL_SPEED, level, difficulty);
        final Vector velocity = skull.getVelocity().multiply(speed);
        skull.setVelocity(velocity);
        skull.getPersistentDataContainer().set(this.isInvulnCancelling, PersistentDataType.BOOLEAN, true);

        //Summon duplicate skulls in the barrage
        final World world = skull.getWorld();
        final Location location = skull.getLocation();
        final Vector acceleration = skull.getAcceleration();
        final int amount = (int) this.plugin.getBossStatsManager().getStat(BossStat.SKULL_BARRAGE_AMOUNT, level, difficulty) - 1; //Subtracting one to account for the already shot skull
        new BukkitRunnable() {
            private int i = 0;

            @Override
            public void run() {
                //Stop runnable once done
                if (i > amount) {
                    this.cancel();
                    return;
                }

                final Entity entity = world.spawnEntity(location, EntityType.WITHER_SKULL);
                if (entity instanceof WitherSkull) {
                    //Set values accordingly
                    final WitherSkull duplicate = (WitherSkull) entity;
                    duplicate.setVelocity(velocity);
                    duplicate.setShooter(wither);
                    duplicate.setAcceleration(acceleration);
                    duplicate.setCharged(false);

                    //Tag all but last as i-frame cancelling
                    if (i != (amount - 1)) {
                        duplicate.getPersistentDataContainer().set(isInvulnCancelling, PersistentDataType.BOOLEAN, true);
                    }

                }

                //Increment ticker
                ++i;
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
    }

    /**
     * Removes i-frames from hit entity if applicable
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onSkullHit(final ProjectileHitEvent event) {
        //Must be a skull
        if (event.getEntityType() != EntityType.WITHER_SKULL || !(event.getEntity() instanceof WitherSkull)) return;

        //Must have hit an entity
        final Entity entity = event.getHitEntity();
        if (!(entity instanceof LivingEntity)) return;

        //Must be marked as non-iframe producing
        final WitherSkull skull = (WitherSkull) event.getEntity();
        if (!skull.getPersistentDataContainer().getOrDefault(this.isInvulnCancelling, PersistentDataType.BOOLEAN, false)) return;

        //Clear iframes
        ((LivingEntity) entity).setNoDamageTicks(0);
    }
}
