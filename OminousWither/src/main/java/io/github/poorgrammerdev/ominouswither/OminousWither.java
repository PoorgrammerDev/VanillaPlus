package io.github.poorgrammerdev.ominouswither;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wither;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.poorgrammerdev.ominouswither.backend.CoroutineManager;
import io.github.poorgrammerdev.ominouswither.backend.SpawnDetector;

public final class OminousWither extends JavaPlugin {
    private final NamespacedKey ominousWitherKey = new NamespacedKey(this, "is_ominous");
    private final NamespacedKey isFullySpawnedKey = new NamespacedKey(this, "is_fully_spawned");
    private final NamespacedKey levelKey = new NamespacedKey(this, "level");
    private final NamespacedKey spawnerKey = new NamespacedKey(this, "spawner");
    private final NamespacedKey minionKey = new NamespacedKey(this, "is_minion");


    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(new SpawnDetector(this), this);
        this.getServer().getPluginManager().registerEvents(new SpawnMechanics(this), this);
        this.getServer().getPluginManager().registerEvents(new OminousAura(this), this);
        this.getServer().getPluginManager().registerEvents(new SkullBarrage(this), this);

        CoroutineManager.getInstance().runTaskTimer(this, 0L, 1L);
    }

    /**
     * Checks if a Wither is Ominous or not
     */
    public boolean isOminous(final Wither wither) {
        return wither.getPersistentDataContainer().getOrDefault(this.ominousWitherKey, PersistentDataType.BOOLEAN, false);
    }

    /**
     * Checks if an entity is a minion of the Ominous Wither
     */
    public boolean isMinion(final LivingEntity entity) {
        return entity.getPersistentDataContainer().getOrDefault(this.minionKey, PersistentDataType.BOOLEAN, false);
    }

    /**
     * PDC Key (boolean) to determine if Wither is Ominous or not
     */
    public NamespacedKey getOminousWitherKey() {
        return this.ominousWitherKey;
    }

    /**
     * PDC Key (boolean) to determine if the Wither is fully spawned in yet or not
     * If true, the explosion has occurred and the wither is no longer stationary and invulnerable.
     */
    public NamespacedKey getIsFullySpawnedKey() {
        return isFullySpawnedKey;
    }

    /**
     * PDC Key (int) to hold the level of the Ominous Wither
     */
    public NamespacedKey getLevelKey() {
        return levelKey;
    }

    /**
     * PDC Key (string) to hold the UUID of the Player that (most likely) spawned the Ominous Wither
     */
    public NamespacedKey getSpawnerKey() {
        return spawnerKey;
    }

    /**
     * PDC Key (boolean) to determine if a mob is a minion of the Wither
     */
    public NamespacedKey getMinionKey() {
        return minionKey;
    }

}
