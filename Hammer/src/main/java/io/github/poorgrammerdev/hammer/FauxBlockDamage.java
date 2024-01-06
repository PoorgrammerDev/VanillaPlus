package io.github.poorgrammerdev.hammer;

import java.util.HashMap;
import java.util.Random;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Displays block damaging visual effect for adjacent blocks during hammer area-mining
 */
public class FauxBlockDamage extends BukkitRunnable implements Listener {
    private final Random random;
    private final boolean enabled;

    private final HashMap<Player, FauxDamageData> playerData;

    public FauxBlockDamage(final Hammer plugin, final Random random) {
        this.random = random;
        this.enabled = plugin.getConfig().getBoolean("show_adjacent_breaking", true);

        this.playerData = new HashMap<>();
    }

    /**
     * Registers player into the system with the block they're mining
     * If the player is not already in the system, it allocates a new data object
     * If the player is already in the system, it re-uses the old data object
     * 
     * To minimize constant heap allocations, the adjacentBlocks array is not reassigned
     * The existing values must be edited externally from the returned data object
     * NOTE: The behaviour may be undefined if this is not done
     * 
     * @param player the player using the hammer
     * @param centerBlock the center/target block the hammer is being used on
     * @return new or reused data object to assign adjacent blocks to, or null if the system is disabled
     */
    public FauxDamageData register(final Player player, Block centerBlock) {
        //System must be enabled
        if (!this.enabled) return null;

        //TODO: This is most likely bad code architecture, find a way to clean this up without constant memory allocation
        FauxDamageData data = this.playerData.getOrDefault(player, null);

        //First time this player's been registered -- allocate new data objects
        if (data == null) {
            data = new FauxDamageData(centerBlock, random);
            data.active = true;

            this.playerData.put(player, data);
            return data;
        }
        
        //Returning player -----

        //Check if they are active; if so, deactivate first
        if (data.active) {
            deactivate(player);
        }

        //Update data values and re-activate
        data.centerBlock = centerBlock;
        data.ticks = 0;
        data.active = true;
        return data;
    }
    
    /**
     * Stops the system from acting upon the player unconditionally
     * @param player player to stop
     */
    public void deactivate(final Player player) {
        //System must be enabled
        if (!this.enabled) return;

        deactivate(player, null);
    }

    /**
     * Stops the system from acting upon the player if the center block matches
     * @param player player to stop
     * @param block center block to check against
     */
    public void deactivate(final Player player, final Block block) {
        //System must be enabled
        if (!this.enabled) return;

        //Ensure player exists and is active
        final FauxDamageData data = this.playerData.getOrDefault(player, null);
        if (data == null || !data.active) return;

        //Ensure block is valid
        if (block != null && !block.equals(data.centerBlock)) return;

        //Reset block damage animation
        for (int i = 0; i < data.adjacentCount; i++) {
            player.sendBlockDamage(data.adjacentBlocks[i], 0.0f, data.ids[i]);
        }
            
        //Deactivate
        data.active = false;
    }

    /**
     * Core mechanism of this class
     * Displays the progressive breaking animation at the estimated speed
     * until it reaches completion or is cancelled externally
     */
    @Override
    public void run() {
        this.playerData.keySet().forEach((final Player player) -> {
            final FauxDamageData data = this.playerData.getOrDefault(player, null);
            if (!data.active) return;

            //Calculate the estimated progress % and display to player
            final float progress = data.ticks * data.centerBlock.getBreakSpeed(player);
            for (int i = 0; i < data.adjacentCount; i++) {
                player.sendBlockDamage(data.adjacentBlocks[i], Math.max(Math.min(progress, 1.0f), 0.0f), data.ids[i]);
            }

            //If reached full progress, deactivate this player
            if (progress >= 1.0f) {
                deactivate(player);
            }

            data.ticks++;
        });
    }
    
    /**
     * Removes references to offline players to prevent memory leaks
     */
    @EventHandler(ignoreCancelled = true)
    public void removeLeavingPlayers(final PlayerQuitEvent event) {
        this.playerData.remove(event.getPlayer());
    }

    /**
     * Checks if this system is enabled
     * @return if the system is enabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

}