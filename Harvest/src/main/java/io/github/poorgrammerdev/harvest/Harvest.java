package io.github.poorgrammerdev.harvest;

import org.bukkit.plugin.java.JavaPlugin;

public class Harvest extends JavaPlugin {

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        // this.getConfig().options().copyDefaults(true);
        // this.saveConfig();

        final CropSeedMapper cropSeedMapper = new CropSeedMapper(this);

        final QuickReplant quickReplace = new QuickReplant(this, cropSeedMapper);
        quickReplace.register();

        final CropCascade cropCascade = new CropCascade(this, cropSeedMapper, quickReplace);
        cropCascade.register();
        
        final AutoPlanter autoPlanter = new AutoPlanter(this, cropSeedMapper);
        autoPlanter.register();
    }

    @Override
    public void onDisable() {
    }
    
}
