package io.github.poorgrammerdev.nightmare.utils;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Represents a listener module of the plugin that can be enabled/disabled by config
 * @author Thomas Tran
 */
public abstract class AbstractModule implements Listener {
    protected final JavaPlugin plugin;
    private boolean registered;

    public AbstractModule(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.registered = false;
    }

    /**
     * Checks the config to see if this module should be registered/enabled
     * @return success
     */
    public boolean register() {
        if (registered || !plugin.getConfig().getBoolean(getModuleConfigPath())) return false;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        registered = true;
        return true;
    }

    /**
     * @return if the module is registered/enabled
     */
    public boolean isRegistered() {
        return this.registered;
    }

    /**
     * Gets the config path for the activation status of this module
     */
    protected abstract String getModuleConfigPath();
}