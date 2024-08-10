package io.github.poorgrammerdev.ominouswither.internal.config;

import java.util.HashMap;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Wither;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import redempt.crunch.functional.EvaluationEnvironment;

/**
 * <p>Handles retrieval of configurable Ominouswither-related stats/settings from the plugin config</p>
 * <p>To allow for easy modification, each stat in the config is defined as a mathematical expression</p>
 * <p>To minimize performance impact at query-time, this expression is evaluated for all possible assignments of each variable at load-time and their values are cached</p>
 * <p>The cached value is simply retrieved at query-time</p>
 * @author Thomas Tran
 */
public class BossStatsManager {
    private final OminousWither plugin;
    private final HashMap<BossStat, CachedBossStat> settingsMap;

    public BossStatsManager(final OminousWither plugin) {
        this.plugin = plugin;
        this.settingsMap = new HashMap<>();
    }

    /**
     * Load or reload settings from config and recalculate all values
     * This will clear any values in the map if present
     * @param plugin instance of plugin class
     */
    public void load() {
        // Clear the map if not empty
        if (this.settingsMap.size() > 0) this.settingsMap.clear();

        // Perform calculations and populate stat map
        final EvaluationEnvironment evalEnv = new EvaluationEnvironment();
        evalEnv.setVariableNames("level", "difficulty");

        for (final BossStat stat : BossStat.values()) {
            final ConfigurationSection entrySection = this.plugin.getConfig().getConfigurationSection(stat.getConfigPath());
            if (entrySection == null) throw new IllegalStateException("Config is missing boss stat " + stat + " expected at location " + stat.getConfigPath());

            final BossStatEntry entry = new BossStatEntry(entrySection.getValues(true), stat.toString());

            this.settingsMap.put(stat, new CachedBossStat(entry, evalEnv));
        }
    }

    /**
     * Gets a cached stat in the map
     * @param bossStat the stat to retrieve
     * @param level level of Wither in [1,5] range
     * @param difficulty current difficulty of world
     * 
     * @return stat value
     * 
     * @throws IllegalArgumentException if requested stat is not in the map
     * @throws ArrayIndexOutOfBoundsException if level is invalid
     */
    public double getStat(final BossStat bossStat, int level, Difficulty difficulty) throws IllegalArgumentException {
        final CachedBossStat setting = this.settingsMap.getOrDefault(bossStat, null);
        if (setting == null) throw new IllegalArgumentException("Stat " + setting +  " does not exist in the map");

        return setting.getValue(level, difficulty);
    }

    /**
     * Convenience method to get a cached stat in the map without having to manually retrieve level and difficulty
     * @param bossStat the stat to retrieve
     * @param wither Ominous Wither entity
     * 
     * @return stat value
     * 
     * @throws IllegalArgumentException if requested stat is not in the map
     * @throws ArrayIndexOutOfBoundsException if stored level is invalid
     */
    public double getStat(final BossStat bossStat, final Wither wither) throws IllegalArgumentException {
        final World world = wither.getWorld();

        //If world is not available for some reason, assume easy difficulty
        final Difficulty difficulty = ((world != null) ? world.getDifficulty() : Difficulty.EASY);

        //If level is not available for some reason, assume lowest level
        final int level = this.plugin.getLevel(wither, 1);

        return this.getStat(bossStat, level, difficulty);
    }

}