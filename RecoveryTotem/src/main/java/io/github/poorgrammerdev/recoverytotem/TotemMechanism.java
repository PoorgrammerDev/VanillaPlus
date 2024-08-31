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
    
    private final boolean dropOtherRecoveryTotemsOnDeath;
    
    public TotemMechanism(RecoveryTotem plugin) {
        this.plugin = plugin;

        this.dropOtherRecoveryTotemsOnDeath = plugin.getConfig().getBoolean("drop_other_recovery_totems_on_death", false);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(final PlayerDeathEvent event) {
        if (this.dropOtherRecoveryTotemsOnDeath) {
            this.mechanismWithDuplicateDrop(event);
        }
        else {
            this.mechanismWithoutDuplicateDrop(event);
        }
    }

    /**
     * Default behavior: simply destroy one Totem and keep inventory
     * @param event
     */
    private void mechanismWithoutDuplicateDrop(final PlayerDeathEvent event) {
        final Player player = event.getEntity();
        
        for (final ItemStack item : player.getInventory()) {
            if (this.plugin.isTotem(item)) {
                this.onFindRecoveryTotem(event);

                //Remove the used totem
                item.setAmount(item.getAmount() - 1);
                return;
            }
        }
    }

    /**
     * Modified behavior: destroy one Totem, drop other Totems, and keep other items
     */
    private void mechanismWithDuplicateDrop(final PlayerDeathEvent event) {
        final Player player = event.getEntity();

        boolean encounteredRecoveryTotem = false;
        for (final ItemStack item : player.getInventory()) {
            if (this.plugin.isTotem(item)) {
                //First totem encountered
                if (!encounteredRecoveryTotem) {
                    this.onFindRecoveryTotem(event);

                    //Remove the used totem
                    final int amountToDrop = item.getAmount() - 1;
                    
                    //If totem was somehow a stack, drop the rest of the stack
                    if (amountToDrop > 0) {
                        final ItemStack droppedTotem = item.clone();
                        droppedTotem.setAmount(amountToDrop);
                        event.getDrops().add(droppedTotem);
                    }
                    
                    item.setAmount(0);
                    encounteredRecoveryTotem = true;
                }
                
                //Drop all other Recovery Totems
                else {
                    event.getDrops().add(item.clone());
                    item.setAmount(0);
                }

            }
        }
    }

    /**
     * Handles keeping inventory and effects on finding a Recovery Totem on death
     */
    private void onFindRecoveryTotem(final PlayerDeathEvent event) {
        final Player player = event.getEntity();

        //Keep items
        event.setKeepInventory(true);
        event.getDrops().clear();
    
        //Keep EXP
        event.setKeepLevel(true);
        event.setDroppedExp(0);

        //Play effects and return
        final World world = player.getWorld();
        final Location location = player.getEyeLocation();
        world.playSound(location, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1, 0.75F);
        world.spawnParticle(Particle.BLOCK, location, 50, 0.5, 0.5, 0.5, 0, Material.SCULK.createBlockData());
        world.spawnParticle(Particle.BLOCK, location, 50, 0.5, 0.5, 0.5, 0, Material.SOUL_FIRE.createBlockData());
    }

}