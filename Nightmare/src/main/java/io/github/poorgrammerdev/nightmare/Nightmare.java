package io.github.poorgrammerdev.nightmare;

import java.io.File;
import java.io.IOException;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.poorgrammerdev.nightmare.utils.ServerProperties;

public class Nightmare extends JavaPlugin {
    private final String mainWorldName;
    private World nightmareWorld;

    public Nightmare() {
        super();

        //Read level name property from the server.properties file
        String mainWorldName;
        try {
            final File propertiesFile = new File("server.properties"); 
            mainWorldName = ServerProperties.getSetting(propertiesFile, "level-name");
        }
        catch (final IOException exception) {
            mainWorldName = "world";
        }
        this.mainWorldName = mainWorldName;
    }


    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        //Create nightmare world
        this.nightmareWorld = new NightmareWorld(this).createWorld();

        this.getServer().getPluginManager().registerEvents(new NightmareEnter(this), this);
    }

    @Override
    public void onDisable() {
    }

    /**
     * Get the main world's name (the level-name property in server.properties)
     */
    public String getMainWorldName() {
        return this.mainWorldName;
    }

    /**
     * Gets the nightmare world
     */
    public World getNightmareWorld() {
        return this.nightmareWorld;
    }
    
}