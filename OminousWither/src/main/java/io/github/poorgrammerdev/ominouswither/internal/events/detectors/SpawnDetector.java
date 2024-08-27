package io.github.poorgrammerdev.ominouswither.internal.events.detectors;

import java.util.HashSet;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherSpawnEvent;
import io.github.poorgrammerdev.ominouswither.mechanics.SpawnCooldown;
import io.github.poorgrammerdev.ominouswither.utils.Utils;

/**
 * <p>Detects if the Wither Boss was spawned by a Player with Bad Omen</p>
 * <p>Fires event {@link OminousWitherSpawnEvent}</p>
 * @author Thomas Tran
 */
public class SpawnDetector implements Listener {
    private static final int MAX_LEVEL = 5;

    /*
     * IMPLEMENTATION:
     * Since there's no trivial way to get who/what spawned a Wither, the system will work as follows:
     * - Every time a Player places a Wither Skull block down, they will be added to some set C.
     * - When a Wither is spawned, it will search for all Players within a radius. It will check the closest player in C for Bad Omen.
     * - Regardless of if a Wither is even spawned, the Player will be removed from C the following game tick.
     */
    
    /**
     * Instance of main plugin class
     */
    private final OminousWither plugin;
    private final SpawnCooldown cooldownManager;
    
    private final double spawnerSearchRadius;
    private final boolean produceRegularWitherOnFailedSpawn;

    /**
     * List of players that may have spawned a Wither in the last game tick
     */
    private final HashSet<UUID> candidateSpawners;

    public SpawnDetector(final OminousWither plugin, final SpawnCooldown cooldownManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;

        this.spawnerSearchRadius = Math.max(plugin.getConfig().getDouble("spawner_search_radius", 10), 1);
        this.produceRegularWitherOnFailedSpawn = plugin.getConfig().getBoolean("spawn_cooldown.produce_regular_wither_on_failed_spawn", false);
        this.candidateSpawners = new HashSet<UUID>();
    }

    /**
     * Detect when a player places a Wither skull to add to the candidate collection.
     * Using lowest priority for this event to run first (I find this a bit confusing but this description is from the Spigot API documentation)
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onSkullPlace(final BlockPlaceEvent event) {
        final Block block = event.getBlockPlaced();
        if (block == null || (
            block.getType() != Material.WITHER_SKELETON_SKULL &&
            block.getType() != Material.WITHER_SKELETON_WALL_SKULL
        )) return;

        //Add to candidate set
        final UUID playerID = event.getPlayer().getUniqueId();
        this.candidateSpawners.add(playerID);

        //Remove the next game tick
        new BukkitRunnable() {
            @Override
            public void run() {
                candidateSpawners.remove(playerID);
            }
        }.runTaskLater(this.plugin, 1L);
    }

    /**
     * Checks if candidate spawner has Bad Omen and fires an event if so
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onWitherSpawn(final CreatureSpawnEvent event) {
        // Spawned entity must be a Wither
        if (event.getSpawnReason() != SpawnReason.BUILD_WITHER || event.getEntityType() != EntityType.WITHER) return;
        if (!(event.getEntity() instanceof Wither)) return;

        final Wither wither = (Wither) event.getEntity();
        
        //Find most likely spawner
        final Player spawner = this.getCandidateSpawner(wither);
        if (spawner == null) return;

        //Test for Bad Omen
        final PotionEffect badOmen = spawner.getPotionEffect(PotionEffectType.BAD_OMEN);
        if (badOmen == null) return;

        //Check cooldown
        if (!this.cooldownManager.handleCooldownOnSpawn(spawner, wither.getLocation())) {
            if (!this.produceRegularWitherOnFailedSpawn) event.setCancelled(true);
            return;
        }

        final int level = Utils.clamp(badOmen.getAmplifier() + 1, 0, MAX_LEVEL);

        // *** Fire Ominous Wither Spawn event ***
        this.plugin.getServer().getPluginManager().callEvent(new OminousWitherSpawnEvent(wither, spawner, level, OminousWitherSpawnEvent.SpawnReason.BUILD));
    }

    /**
     * Find the closest player that could've summoned the Wither
     * @param wither the summoned Wither
     * @return a Player if it exists, or null if no candidates were found
     */
    private Player getCandidateSpawner(final Wither wither) {
        final Location witherLocation = wither.getLocation();

        return wither.getNearbyEntities(this.spawnerSearchRadius, this.spawnerSearchRadius, this.spawnerSearchRadius).stream()
            //Only consider players
            .filter(entity -> (entity instanceof Player))
            .map(entity -> (Player) entity)

            //Only consider players that have placed a Wither Skull in the last tick
            .filter(player -> this.candidateSpawners.contains(player.getUniqueId()))

            //Get the closest player
            .min((player1, player2) -> (
                Double.valueOf(witherLocation.distanceSquared(player1.getLocation()))
                .compareTo(
                    Double.valueOf(witherLocation.distanceSquared(player2.getLocation()))
                )
            ))
            .orElse(null);
    }
}
