package io.github.poorgrammerdev.nightmare;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.event.world.TimeSkipEvent.SkipReason;

public class NightmareEnter implements Listener {
    private final Nightmare plugin;

    public NightmareEnter(final Nightmare plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSkipNight(final TimeSkipEvent event) {
        if (event.getSkipReason() != SkipReason.NIGHT_SKIP) return;

        final NightmareManager m = new NightmareManager(plugin);
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            m.enterNightmare(player);
        });



    }
    
}
