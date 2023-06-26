package io.github.poorgrammerdev.paxel;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class Paxel extends JavaPlugin {
    private final NamespacedKey paxelKey;

    public Paxel() {
        this.paxelKey = new NamespacedKey(this, "is_paxel");
    }

    @Override
    public void onEnable() {
        final ToolMapper toolMapper = new ToolMapper(this);

        final CraftingManager craftingManager = new CraftingManager(this, toolMapper);
        craftingManager.registerAllRecipes();
        // this.getServer().getPluginManager().registerEvents(craftingManager, this);

        final PaxelMechanism paxelMechanism = new PaxelMechanism(this, toolMapper);
        this.getServer().getPluginManager().registerEvents(paxelMechanism, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public NamespacedKey getPaxelKey() {
        return paxelKey;
    }
}
