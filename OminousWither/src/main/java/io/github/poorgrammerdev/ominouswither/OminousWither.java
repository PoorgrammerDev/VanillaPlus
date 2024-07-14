package io.github.poorgrammerdev.ominouswither;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Wither;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.poorgrammerdev.ominouswither.backend.CoroutineManager;
import io.github.poorgrammerdev.ominouswither.backend.SpawnDetector;

public final class OminousWither extends JavaPlugin {
    private final NamespacedKey ominousWitherKey = new NamespacedKey(this, "is_ominous");

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(new SpawnDetector(this), this);
        this.getServer().getPluginManager().registerEvents(new SpawnMechanics(this), this);

        CoroutineManager.getInstance().runTaskTimer(this, 0L, 1L);
    }

    public NamespacedKey getOminousWitherKey() {
        return this.ominousWitherKey;
    }

    public boolean isOminous(final Wither wither) {
        return wither.getPersistentDataContainer().getOrDefault(this.ominousWitherKey, PersistentDataType.BOOLEAN, false);
    }
}
