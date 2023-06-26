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
        // Plugin startup logic

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public NamespacedKey getPaxelKey() {
        return paxelKey;
    }
}
