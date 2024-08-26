package io.github.poorgrammerdev.ominouswither.mechanics;

import java.time.Duration;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.CooldownManager;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherSpawnEvent;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherSpawnEvent.SpawnReason;
import io.github.poorgrammerdev.ominouswither.utils.ParticleInfo;
import io.github.poorgrammerdev.ominouswither.utils.Utils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Handles cooldown for spawning Ominous Withers
 * @author Thomas Tran
 */
public class SpawnCooldown extends CooldownManager implements Listener {
    private final OminousWither plugin;

    private final int cooldownDuration;
    private final boolean globalCreativeBypass;
    private final boolean sendChatMessageOnFailedSpawn;
    private final boolean sendActionBarMessageOnFailedSpawn;

    private final String failedSpawnChatText;
    private final String failedSpawnActionbarText;

    private final ParticleInfo failedToSpawnParticle;

    public SpawnCooldown(final OminousWither plugin) {
        this.plugin = plugin;

        this.cooldownDuration = plugin.getConfig().getInt("spawn_cooldown.cooldown_duration", 0);
        this.globalCreativeBypass = plugin.getConfig().getBoolean("spawn_cooldown.global_creative_bypass", true);
        this.sendChatMessageOnFailedSpawn = plugin.getConfig().getBoolean("spawn_cooldown.send_message_on_failed_spawn.chat_message", true);
        this.sendActionBarMessageOnFailedSpawn = plugin.getConfig().getBoolean("spawn_cooldown.send_message_on_failed_spawn.actionbar", false);

        this.failedSpawnChatText = plugin.getConfig().getString("messages.attempted_build_on_cooldown_chat", "");
        this.failedSpawnActionbarText = plugin.getConfig().getString("messages.attempted_build_on_cooldown_actionbar", "");

        this.failedToSpawnParticle = new ParticleInfo(Particle.SMOKE,50,1.25,1,1.25);
    }
    
    /**
     * Handles checking the cooldown state of a player attempting to spawn an Ominous Wither
     * @param player player spawning the Wither
     * @param spawnLocation location of Wither to be spawned
     * @return if the player should be allowed to spawn the Ominous Wither
     */
    public boolean handleCooldownOnSpawn(final Player player, final Location spawnLocation) {
        if (this.isAffectedByCooldowns(player) && this.isOnCooldown(player)) {
            final World world = player.getWorld();

            //Play effects
            this.failedToSpawnParticle.spawnParticle(world, spawnLocation.add(0,1,0));
            world.playSound(spawnLocation, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 1.0f, 1.0f);

            //Send messages if enabled
            if (this.sendChatMessageOnFailedSpawn) {
                final String message = Utils.formatMessage(this.failedSpawnChatText, this.getRemainingCooldown(player).toSeconds());
                player.sendMessage(message);
            }
            if (this.sendActionBarMessageOnFailedSpawn) {
                final String message = Utils.formatMessage(this.failedSpawnActionbarText, this.getRemainingCooldown(player).toSeconds());
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message));
            }

            return false;
        }
        return true;
    }

    /**
     * Applies cooldown to players that have successfully spawned an Ominous Wither
     */
    @EventHandler(ignoreCancelled = true)
    private void onOminousSpawn(final OminousWitherSpawnEvent event) {
        if (event.getSpawnReason() == SpawnReason.COMMAND) return;

        final Player player = event.getSpawner();
        
        //Apply cooldown
        if (this.cooldownDuration > 0 && this.isAffectedByCooldowns(player)) {
            this.setCooldown(player, Duration.ofSeconds(this.cooldownDuration));
        }
    }

    /**
     * Resets the spawner's cooldown on Ominous Wither death
     */
    @EventHandler(ignoreCancelled = true)
    private void onDeath(final EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Wither)) return;

        final Wither wither = (Wither) event.getEntity();
        if (!this.plugin.isOminous(wither)) return;
    
        final String idStr = wither.getPersistentDataContainer().getOrDefault(this.plugin.getSpawnerKey(), PersistentDataType.STRING, null);
        if (idStr == null) return;

        UUID id;
        try {
            id = UUID.fromString(idStr);
        }
        catch (final IllegalArgumentException exception) {
            this.plugin.getLogger().warning("Malformed player UUID stored in spawner field of Ominous Wither");
            return;
        }

        // No longer on cooldown -> don't need to do anything
        if (!this.isOnCooldown(id)) return;

        //Clear cooldown regardless of if player is online or not
        this.removeCooldown(id);

        //If player is online, play an effect
        final Player player = this.plugin.getServer().getPlayer(id);
        if (player == null) return;

        player.playSound(player, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    /**
     * <p>Checks if a player should be affected by cooldowns</p>
     * <p>If they are in Survival or Adventure mode, they're always affected</p>
     * <p>Otherwise, if they are in Creative mode, check if global bypass setting is enabled or if they have permission to bypass</p>
     */
    private boolean isAffectedByCooldowns(final Player player) {
        return Utils.isTargetable(player) || (!this.globalCreativeBypass && !player.hasPermission("ominouswither.creative_bypass_spawn_cooldown"));
    }
}
