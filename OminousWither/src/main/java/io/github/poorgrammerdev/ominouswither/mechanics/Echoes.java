package io.github.poorgrammerdev.ominouswither.mechanics;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.entity.Wither.Head;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
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
 * <p>Second Phase attack that plummets nearby falling Player targets into the ground</p>
 * <p>Intended as a counter to the Mace weapon, but does not make it outright impossible to use</p>
 * @author Thomas Tran
 */
public class Echoes implements Listener {
    private final OminousWither plugin;

    /**
     * Holds when a Wither can use the Echoes attack again, measured using Spigot's ticks lived metric
     */
    private final HashMap<UUID, Integer> nextAvailableUse;

    private final Random random;

    public Echoes(OminousWither plugin) {
        this.plugin = plugin;
        this.nextAvailableUse = new HashMap<>();
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
     * Allows this wither to use the Echoes attack when appropriate
     */
    private void enableAttackMechanism(final Wither wither) {
        final UUID witherID = wither.getUniqueId();

        final int cooldown = (int) this.plugin.getBossStatsManager().getStat(BossStat.ECHOES_COOLDOWN, wither);
        final double range = this.plugin.getBossStatsManager().getStat(BossStat.ECHOES_RANGE, wither);
        final double maxYVelocity = this.plugin.getBossStatsManager().getStat(BossStat.ECHOES_MAX_Y_VELOCITY, wither);

        final double rangeSq = range * range;

        this.plugin.getCoroutineManager().enqueue(new ICoroutine() {
            @Override
            public boolean tick() {
                //If wither no longer exists -> cancel
                final Entity entity = plugin.getServer().getEntity(witherID);
                if (!(entity instanceof Wither) || entity.isDead() || !entity.isInWorld()) {
                    Echoes.this.nextAvailableUse.remove(witherID);
                    return false;
                }

                //Check cooldown
                //If never used, then cannot be in cooldown. Otherwise check as normal
                final Wither wither = (Wither) entity;
                if (nextAvailableUse.containsKey(witherID) && wither.getTicksLived() < nextAvailableUse.get(witherID)) return true;

                //Checks all three possible targets
                for (final Head head : Head.values()) {
                    //Target must be a player
                    final LivingEntity livingTarget = wither.getTarget(head);
                    if (!(livingTarget instanceof Player)) continue;

                    //Target must exist and be targetable
                    final Player target = (Player) livingTarget;
                    if (target.isDead() || !target.isInWorld() || Utils.isOnGround(target.getLocation()) || !Utils.isTargetable(target)) continue;

                    //Target must be in range and be falling at a high enough speed
                    if (target.getVelocity().getY() > maxYVelocity || wither.getLocation().add(0, range / 2.0, 0).distanceSquared(target.getLocation()) > rangeSq) continue;

                    // Perform Echoes attack on the target
                    final boolean success = performAttack((Player) target, wither);

                    //If activated properly, set on cooldown
                    if (success) nextAvailableUse.put(witherID, wither.getTicksLived() + cooldown);
                    return true;
                }

                return true;
            }
        });
    }
    
    /**
     * Performs the Echoes attack on the player
     * @param playerParam player to plummet into the ground
     * @param wither wither that used this attack
     * @return if Echoes was successfully activated or not
     */
    private boolean performAttack(final Player playerParam, final Wither wither) {
        final World world = wither.getWorld();
        if (world == null) return false;

        final int duration = (int) this.plugin.getBossStatsManager().getStat(BossStat.ECHOES_MAX_EFFECT_DURATION, wither);
        final double downwardForce = this.plugin.getBossStatsManager().getStat(BossStat.ECHOES_VERTICAL_FORCE, wither);

        final ParticleInfo particle = new ParticleInfo(Particle.DUST, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(0xD8FF1A), 1.5f));
        final UUID playerID = playerParam.getUniqueId();

        //Play sound
        playerParam.playSound(playerParam, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.HOSTILE, 3.0f, 1.0f);

        //Must be tick-accurate now, using runnable instead of Coroutine system
        new BukkitRunnable() {
            private int i = 0;

            @Override
            public void run() {
                //Cancel if player can't be found or attack is over
                final Player player = plugin.getServer().getPlayer(playerID);
                if (player == null || Utils.isOnGround(player.getLocation()) || player.isDead() || !player.isInWorld() || i >= duration) {
                    this.cancel();
                    return;
                }
                
                //Display particle circle
                ParticleShapes.circle(particle, player.getWidth() * 1.5D, 16, player.getLocation());
                ParticleShapes.circle(particle, player.getWidth() * 1.5D, 16, player.getLocation().add(0, player.getHeight() / 2.0D, 0));
                ParticleShapes.circle(particle, player.getWidth() * 1.5D, 16, player.getLocation().add(0, player.getHeight(), 0));

                //Apply gravity effect
                final Vector velocity = player.getVelocity();
                if (velocity.getY() > downwardForce) velocity.setY(downwardForce);
                player.setVelocity(velocity);
                
                ++i;
            }
        }.runTaskTimer(plugin, 0L, 0L);

        return true;
    }

    /**
     * Dealing heavy Mace damage to a Wither in Echoes cooldown may reduce the cooldown
     */
    @EventHandler(ignoreCancelled = true)
    private void cooldownReduction(final EntityDamageByEntityEvent event) {
        //Damage must be from a Mace Smash and be above the threshold
        final DamageSource damageSource = event.getDamageSource();
        if (event.getDamage() < 20.0 || damageSource.getDamageType() != DamageType.MACE_SMASH) return;

        //Entity must be an ominous wither
        if (event.getEntityType() != EntityType.WITHER || !(event.getEntity() instanceof Wither)) return;

        final Wither wither = (Wither) event.getEntity();
        if (!this.plugin.isOminous(wither)) return;

        //Wither's Echoes ability must be on cooldown
        final int witherTicksLived = wither.getTicksLived();
        final Integer cooldownExpiration = this.nextAvailableUse.getOrDefault(wither.getUniqueId(), null);
        if (cooldownExpiration == null || cooldownExpiration < witherTicksLived) return;

        //Roll for chance to activate
        final double chance = this.plugin.getBossStatsManager().getStat(BossStat.ECHOES_COOLDOWN_REDUCTION_PROBABILITY, wither);
        if (random.nextDouble() > chance) return;

        // --- Ability activated ---
        
        //Halve the remaining Echoes cooldown
        final int newCooldown = (witherTicksLived + cooldownExpiration) / 2;
        this.nextAvailableUse.put(wither.getUniqueId(), newCooldown);

        //VFX/SFX
        final World world = wither.getWorld();
        world.playSound(wither, Sound.BLOCK_VAULT_OPEN_SHUTTER, SoundCategory.HOSTILE, 5.0f, 1.0f);
        
        final ParticleInfo particle = new ParticleInfo(Particle.DUST, 50, 1.5, 1.5, 1.5, 0, new Particle.DustOptions(Color.fromRGB(0xD8FF1A), 3f));
        particle.spawnParticle(world, wither.getEyeLocation());
    }

}
