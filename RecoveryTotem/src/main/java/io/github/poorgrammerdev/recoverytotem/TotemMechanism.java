package io.github.poorgrammerdev.recoverytotem;

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
import org.bukkit.inventory.ItemStack;

/**
 * Implements the Totem's mechanism: one-time KeepInventory on death
 * @author Thomas Tran
 */
public class TotemMechanism implements Listener {
    private final RecoveryTotem plugin;
    
    public TotemMechanism(RecoveryTotem plugin) {
        this.plugin = plugin;
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
                
                //Remove the used totem
                item.setAmount(item.getAmount() - 1);

                //Play effects and return
                final World world = player.getWorld();
                final Location location = player.getEyeLocation();
                world.playSound(location, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1, 0.75F);
                world.spawnParticle(Particle.BLOCK, location, 50, 0.5, 0.5, 0.5, 0, Material.SCULK.createBlockData());
                world.spawnParticle(Particle.BLOCK, location, 50, 0.5, 0.5, 0.5, 0, Material.SOUL_FIRE.createBlockData());
                return;
            }
        }
    }





}