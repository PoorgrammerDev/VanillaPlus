package io.github.poorgrammerdev.ominouswither.mechanics;

import java.util.Random;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.persistence.PersistentDataType;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.config.BossStat;
import io.github.poorgrammerdev.ominouswither.mechanics.customskulls.AbstractSkullHandler;

/**
 * Manages custom dangerous (blue) skulls being shot from the Wither
 * @author Thomas Tran
 */
public class DangerousSkullManager implements Listener {
    private final OminousWither plugin;
    private final Random random;
    private final AbstractSkullHandler[] skullHandlers;

    public DangerousSkullManager(final OminousWither plugin, final AbstractSkullHandler[] skullHandlers) {
        this.plugin = plugin;
        this.random = new Random();

        this.skullHandlers = skullHandlers;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onSkullLaunch(final ProjectileLaunchEvent event) {
        //Must be a skull
        if (event.getEntityType() != EntityType.WITHER_SKULL || !(event.getEntity() instanceof WitherSkull)) return;

        //Must be a black skull
        final WitherSkull skull = (WitherSkull) event.getEntity();
        if (skull.isCharged()) return;

        //Shooter of skull must be a Wither
        if (!(skull.getShooter() instanceof Wither)) return;

        //Wither must be Ominous
        final Wither wither = (Wither) skull.getShooter();
        if (!this.plugin.isOminous(wither)) return;

        //Apply random chance
        if (random.nextDouble() > this.plugin.getBossStatsManager().getStat(BossStat.DANGEROUS_SKULL_CHANCE_BOOST, wither)) return;

        //Skull is set to charged and custom blue skull effects are handled after
        skull.setCharged(true);

        final AbstractSkullHandler handler = this.skullHandlers[random.nextInt(this.skullHandlers.length)];
        skull.getPersistentDataContainer().set(this.plugin.getSkullTypeKey(), PersistentDataType.STRING, handler.getSkullTag());
        handler.onSpawn(skull, wither);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onSkullHit(final ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof WitherSkull) || !(event.getEntity().getShooter() instanceof Wither)) return;
        
        final WitherSkull skull = (WitherSkull) event.getEntity();
        final Wither wither = (Wither) skull.getShooter();
        if (!this.plugin.isOminous(wither)) return;

        final String tag = skull.getPersistentDataContainer().getOrDefault(this.plugin.getSkullTypeKey(), PersistentDataType.STRING, null);
        if (tag == null) return;

        //NOTE: This is an O(N) operation but at this small value of N a hashmap probably isn't worth the overhead
        for (final AbstractSkullHandler handler : this.skullHandlers) {
            if (handler.getSkullTag().equals(tag)) {
                handler.onHit(event, wither);
                return;
            }
        }
    }

}
