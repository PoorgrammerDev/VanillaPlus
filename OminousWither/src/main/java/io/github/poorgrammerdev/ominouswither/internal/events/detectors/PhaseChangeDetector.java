package io.github.poorgrammerdev.ominouswither.internal.events.detectors;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherLoadEvent;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherPhaseChangeBeginEvent;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherPhaseChangeEndEvent;
import io.github.poorgrammerdev.ominouswither.utils.ParticleInfo;
import io.github.poorgrammerdev.ominouswither.utils.ParticleShapes;

/**
 * <p>Detects when the Wither reaches half health and enters Second Phase</p>
 * <p>Handles Phase Change animation and all VFX/SFX associated with it</p>
 * <p>Fires events {@link OminousWitherPhaseChangeBeginEvent} and {@link OminousWitherPhaseChangeEndEvent}</p>
 * @author Thomas Tran
 */
public class PhaseChangeDetector implements Listener {
    private static final int BUFF_ANIMATION_TICKS = 120;

    private final OminousWither plugin;

    public PhaseChangeDetector(final OminousWither plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks health going below half to fire event
     */
    @EventHandler(ignoreCancelled = true)
    private void onDamage(final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Wither)) return;
        
        //Must be an Ominous Wither
        final Wither wither = (Wither) event.getEntity();
        if (!this.plugin.isOminous(wither)) return;

        //Must not already be in second phase
        if (wither.getPersistentDataContainer().getOrDefault(this.plugin.getSecondPhaseKey(), PersistentDataType.BOOLEAN, false)) return;

        //Wither must be below half health
        final double maxHealth = wither.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        final double healthAfterDamage = wither.getHealth() - event.getFinalDamage();

        //If the player somehow kills the Wither in one shot bypassing the second phase entirely, do not call event
        //If the health has not dipped past half, do not call event
        if (healthAfterDamage <= 0.0D || (healthAfterDamage / maxHealth) > 0.5D) return;

        //Tag the wither with the second phase key
        wither.getPersistentDataContainer().set(this.plugin.getSecondPhaseKey(), PersistentDataType.BOOLEAN, true);

        //Call event
        this.plugin.getServer().getPluginManager().callEvent(new OminousWitherPhaseChangeBeginEvent(wither));
    }

    /**
     * Begins phase change animation and tracking to fire End event
     */
    @EventHandler(ignoreCancelled = true)
    private void beginAnimation(final OminousWitherPhaseChangeBeginEvent event) {
        final Wither wither = event.getWither();

        //Wither grows in size
        wither.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(1.5);

        //Play starting sound
        final World world = wither.getWorld();
        if (world != null) {
            world.playSound(wither, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.HOSTILE, 5.0f, 1.0f);
        }
       
        //Start animation and track to call event when animation ends
        wither.setInvulnerabilityTicks(BUFF_ANIMATION_TICKS * 2); //Set to double the amount so it ends when HP reaches half
        new PhaseChangeAnimationTracker(wither).runTaskTimer(plugin, 0, 0);
    }

    /**
     * Restarts animation tracking on Withers that were unloaded mid-animation
     */
    @EventHandler(ignoreCancelled = true)
    private void onLoad(final OminousWitherLoadEvent event) {
        //Must be in second phase
        final Wither wither = event.getWither();
        if (!wither.getPersistentDataContainer().getOrDefault(this.plugin.getSecondPhaseKey(), PersistentDataType.BOOLEAN, false)) return;

        //Must still be in the phase change animation
        if (wither.getInvulnerabilityTicks() <= 0) return;

        //Restart tracking process
        new PhaseChangeAnimationTracker(wither).runTaskTimer(plugin, 0, 0);
    }

    /**
     * Runnable to track Wither animation and fire event when finished
     * @author Thomas Tran
     */
    private class PhaseChangeAnimationTracker extends BukkitRunnable {
        private final Wither wither;
        private final Location frozenLocation;

        private final Random random = new Random();
        private final Vector[] anchorDirections = {
            this.getRandomVector().normalize().multiply(100),
            this.getRandomVector().normalize().multiply(100),
            this.getRandomVector().normalize().multiply(100),
            this.getRandomVector().normalize().multiply(100),
        };

        public PhaseChangeAnimationTracker(final Wither wither) {
            this.wither = wither;
            this.frozenLocation = wither.getLocation();
        }

        @Override
        public void run() {
                //Wither somehow disappeared, have to cancel
                if (wither == null || wither.isDead() || !wither.isInWorld()) {
                    this.cancel();
                    return;
                }

                //Bind Wither to its current location or else it will continue following its target
                //(This is vanilla behavior, not the Flight mechanism. Withers were never meant to re-enter invulnerability)
                wither.teleport(this.frozenLocation);

                final World world = wither.getWorld();
                final int ticks = wither.getInvulnerabilityTicks();

                if (world != null) {
                    //Midway SFX
                    if (ticks != 0 && ticks % (BUFF_ANIMATION_TICKS / 4) == 0) {
                        final float pitch = (BUFF_ANIMATION_TICKS / (float) ticks);
                        world.playSound(wither, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 5.0f, pitch);
                        world.playSound(wither, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 5.0f, pitch);
                    }

                    //"Anchor" vfx
                    //TODO: EXTREMELY CURSED VALUE; IS THERE A BETTER WAY?
                    final int index = Math.min((-ticks + 2*BUFF_ANIMATION_TICKS) / (BUFF_ANIMATION_TICKS / 4), 3);
                    for (int i = 0; i <= index ; i++) {
                        ParticleShapes.line(new ParticleInfo(Particle.SOUL_FIRE_FLAME, 3, 0.125, 0.125, 0.125), wither.getEyeLocation().subtract(0,1,0), wither.getLocation().add(this.anchorDirections[i]), 1.5);
                    }

                    if (ticks <= (BUFF_ANIMATION_TICKS + 5)) {
                        //Nearing end particle effects
                        //Played a bit early to account for particle wind-up time
                        world.spawnParticle(Particle.SONIC_BOOM, wither.getEyeLocation().subtract(0,1,0), 40, 1.5, 1.5, 1.5);
                    }
                }

                //End of animation
                if (ticks <= BUFF_ANIMATION_TICKS) {
                    wither.setInvulnerabilityTicks(0);

                    //SFX
                    if (world != null) {
                        world.playSound(wither, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 5.0f, 1.0f);
                        world.playSound(wither, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.HOSTILE, 5.0f, 1.0f);
                        world.playSound(wither, Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 5.0f, 1.0f);
                    }

                    // *** FIRE EVENT ***
                    plugin.getServer().getPluginManager().callEvent(new OminousWitherPhaseChangeEndEvent(wither));

                    this.cancel();
                }
            }

        private Vector getRandomVector() {
            return new Vector(
                this.random.nextDouble(-1, 1),
                this.random.nextDouble(0.5, 1),
                this.random.nextDouble(-1, 1)
            );
        }
    }

}
