package io.github.poorgrammerdev.recoverytotem;

import java.util.HashSet;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Implements the Totem's mechanism: one-time KeepInventory on death
 * @author Thomas Tran
 */
public class TotemMechanism implements Listener {
    private final RecoveryTotem plugin;
    private final HashSet<UUID> respawningPlayers;
    
    public TotemMechanism(RecoveryTotem plugin) {
        this.plugin = plugin;
        this.respawningPlayers = new HashSet<UUID>();
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(final PlayerDeathEvent event) {
        final Player player = event.getEntity();
        
        for (final ItemStack item : player.getInventory()) {
            if (this.plugin.isTotem(item)) {
                //Keep items
                event.setKeepInventory(true);
                event.getDrops().clear();

                //Keep EXP
                event.setKeepLevel(true);
                event.setDroppedExp(0);
                
                //Remove the used totem, add to hashset for vfx/sfx on respawn, and return
                item.setAmount(item.getAmount() - 1);
                this.respawningPlayers.add(player.getUniqueId());
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRespawn(final PlayerRespawnEvent event) {
        // Must be a player respawning after having used a Totem of Recovery
        final Player player = event.getPlayer();
        if (!this.respawningPlayers.contains(player.getUniqueId())) return;

        //Remove from set
        this.respawningPlayers.remove(player.getUniqueId());

        //Play effects delayed by a tick (TODO: why?)
        final World world = player.getWorld();
        final Location location = player.getEyeLocation();
        new BukkitRunnable() {

            @Override
            public void run() {
                world.playSound(location, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1, 0.8F);
                player.spawnParticle(Particle.BLOCK, location, 25, 0.5, 1, 0.5, 1, Material.SCULK.createBlockData());
                player.spawnParticle(Particle.BLOCK, location, 25, 0.5, 1, 0.5, 1, Material.SOUL_FIRE.createBlockData());
            }
            
        }.runTaskLater(this.plugin, 1L);
    }




}