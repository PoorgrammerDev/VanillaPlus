package io.github.poorgrammerdev.xpcontrol;

import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import io.github.poorgrammerdev.xpcontrol.external.Experience;

/**
 * Controls how much experience the player keeps and drops on death
 * @author Thomas Tran
 */
public class DeathXPControl extends AbstractModule {
    private final XPVoucher xpVoucherManager;

    private final int percentKeep;
    private final int percentDrop;
    private final int levelLimit;
    private final boolean allowWorkarounds;

    public DeathXPControl(final XPControl plugin, final XPVoucher xpVoucherManager) {
        super(plugin);
        this.xpVoucherManager = xpVoucherManager;

        //Reading from config into temp int variables
        int percentKeep = this.plugin.getConfig().getInt("death_xp_control.percent_keep", 25);
        int percentDrop = this.plugin.getConfig().getInt("death_xp_control.percent_drop", 50);

        //Make sure the values are within range/valid
        if (percentKeep + percentDrop > 100 || percentKeep < 0 || percentDrop < 0) {
            //If they are not, force clamp them to be. (Keep takes precedence over Drop)
            percentKeep = Math.max(Math.min(percentKeep, 100), 0);
            percentDrop = Math.max(Math.min(percentDrop, 100 - percentKeep), 0);

            plugin.getLogger().log(Level.WARNING, "Invalid values for percent_keep and percent_drop! Using clamped values " + percentKeep + " and " + percentDrop + " instead.");
        }
        
        this.percentKeep = percentKeep;
        this.percentDrop = percentDrop;
        this.levelLimit = this.plugin.getConfig().getInt("death_xp_control.max_level", -1);
        this.allowWorkarounds = this.plugin.getConfig().getBoolean("death_xp_control.allow_experimental_workarounds", false);
    }

    @Override
    protected String getModuleConfigPath() {
        return "death_xp_control.enabled";
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(final PlayerDeathEvent event) {
        if (event.getKeepLevel()) return;

        final Player player = event.getEntity();

        //Get the level while respecting the limit if it is set
        int level = player.getLevel();
        if (this.levelLimit >= 0 && level > this.levelLimit) {
            level = this.levelLimit;
        }

        //I'm using doubles here as well as modifying the external class to use doubles everywhere
        //because the total exp of a player that has the max exp you can have can only be represented in doubles
        //(integers and even longs overflow) thus all calculations are done in doubles first
        final double originalXP = getExp(level, player.getExp());

        //Sanity check -- exp cannot be negative for whatever reason
        if (originalXP < 0.0D) {
            this.plugin.getLogger().log(Level.SEVERE, "Death EXP calculation yielded negative EXP value " + originalXP);
            event.setDroppedExp(0);
            event.setNewExp(0);
            return;
        }
        
        //Division calculation -- clamped, just in case
        final double keptXP = Math.min(originalXP * (this.percentKeep / 100.0), originalXP);
        final double dropXP = Math.min(originalXP * (this.percentDrop / 100.0), originalXP - keptXP);

        //Handle these values with respect to potential integer overflow
        handleKeepingEXP(event, keptXP);
        handleDroppingEXP(event, dropXP);
    }

    private void handleKeepingEXP(final PlayerDeathEvent event, final double keptXP) {
        //Special case for integer overflow -- calculate levels to keep and assign that instead
        //(circumvents overflow but forfeits progression towards next level)
        if (keptXP > (double) Integer.MAX_VALUE) {
            //Use workaround or limit to max int value
            if (this.allowWorkarounds) {
                event.setNewLevel((int) Experience.getLevelFromExp(keptXP));
            }
            else {
                event.setNewExp(Integer.MAX_VALUE);
            }
        }
        //general case
        else {
            event.setNewExp((int) keptXP);
        }
    }

    private void handleDroppingEXP(final PlayerDeathEvent event, final double dropXP) {
        //Special case for overflow 
        if (dropXP > (double) Integer.MAX_VALUE) {
            //Use workaround or limit to max int value
            if (this.allowWorkarounds) {
                //Spawning in the experience orbs will be a bit more of a workaround.
                //The orb itself won't be spawned since this value is too large to fit into one orb,
                //and spawning in extra orbs can lag down the server.
                //So instead I'll be using a "voucher" system -- a placeholder item is dropped in place of the orbs.
                //When this item is picked up, whatever player gets it will be given the levels and exp stored in it and it will disappear from existence.
                final ItemStack voucher = this.xpVoucherManager.createVoucher(dropXP);
                event.getDrops().add(voucher);
                event.setDroppedExp(0);
            }
            else {
                event.setDroppedExp(Integer.MAX_VALUE);
            }
        }
        //general case
        else {
            event.setDroppedExp((int) dropXP);
        }
    }

    /**
     * Gets the total experience for some level and some exp (not necessarily a player's current level or exp)
     * @param level EXP level
     * @param exp level progression in current level
     * @return total experience points
     */
    private double getExp(int level, float exp) {
      return Experience.getExpFromLevel(level) + Math.round(Experience.getExpToNext(level) * exp);
    }

}
