package io.github.poorgrammerdev.ominouswither.mechanics;

import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.config.BossStat;
import io.github.poorgrammerdev.ominouswither.utils.Utils;

/**
 * Manages the loot that an Ominous Wither drops
 */
public class Loot implements Listener {
    private final OminousWither plugin;

    public Loot(OminousWither plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    private void onDeath(final EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Wither)) return;

        final Wither wither = (Wither) event.getEntity();
        if (!this.plugin.isOminous(wither)) return;

        //Experience multiplier
        event.setDroppedExp((int) (event.getDroppedExp() * this.plugin.getBossStatsManager().getStat(BossStat.LOOT_EXP_MULTIPLIER, wither)));

        //Loot multiplier
        //This system should be replaced with something more complex if custom loot is added
        final int itemMultiplier = (int) this.plugin.getBossStatsManager().getStat(BossStat.LOOT_ITEM_MULTIPLIER, wither);
        for (final ItemStack drops : event.getDrops()) {
            drops.setAmount(Utils.clamp(drops.getAmount() * itemMultiplier, 1, drops.getMaxStackSize()));
        }
    }
    
}
