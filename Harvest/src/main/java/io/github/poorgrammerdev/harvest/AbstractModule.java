package io.github.poorgrammerdev.harvest;

import org.bukkit.event.Listener;

/**
 * Represents a listener module of the plugin that can be enabled/disabled by config
 */
public abstract class AbstractModule implements Listener {
    protected final Harvest plugin;
    private boolean registered;

    public AbstractModule(final Harvest plugin) {
        this.plugin = plugin;
        this.registered = false;
    }

    /**
     * Checks the config to see if this module should be registered/enabled
     * @param configPath path to the boolean
     * @return success
     */
    protected boolean register(String configPath) {
        if (registered || !plugin.getConfig().getBoolean(configPath)) return false;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        registered = true;
        return true;
    }

    /**
     * Public facing method to register this module.
     * @return success
     */
    public abstract boolean register(); //Should be implemented as call to register(String) with a literal path
}
