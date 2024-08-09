package io.github.poorgrammerdev.ominouswither.internal.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.entity.Wither;

import io.github.poorgrammerdev.ominouswither.OminousWither;

/**
 * <p>Handles retrieval of configurable Ominouswither-related stats/settings from the plugin config</p>
 * <p>To allow for easy modification, each setting in the config is defined as a list of modifiers or rules</p>
 * <p>To minimize performance impact at query-time, these rules are applied for all possible values of each metric (Wither level and difficulty) at load-time and their values are cached</p>
 * <p>The cached value is simply retrieved at query-time</p>
 * @author Thomas Tran
 */
public class BossSettingsManager {
    private static final String CONFIG_SECTION = "boss_settings";

    private final OminousWither plugin;
    private final HashMap<String, CachedBossSetting> settingsMap;

    public BossSettingsManager(final OminousWither plugin) {
        this.plugin = plugin;
        this.settingsMap = new HashMap<>();
    }

    /**
     * Load or reload settings from disk and recalculate all values
     * This will clear any values in the map if present
     * @param plugin instance of plugin class
     */
    @SuppressWarnings("unchecked")
    public void load() {
        // Clear the map if not empty
        if (this.settingsMap.size() > 0) this.settingsMap.clear();

        //Gets all boss settings and their respective rule lists
        final Map<String, Object> rawMap = this.plugin.getConfig().getConfigurationSection(CONFIG_SECTION).getValues(false);

        for (final Entry<String, Object> entry : rawMap.entrySet()) {
            //Type check the list object
            if (!(entry.getValue() instanceof List)) {
                throw new IllegalArgumentException("Invalid type for boss setting " + entry.getKey() + ", expected List and got " + entry.getValue().getClass());
            }

            //Type-check each modifier entry
            //TODO: see if theres a cleaner and more robust way of doing this
            final List<?> list = (List<?>) entry.getValue();
            final List<BossModifier> modifierList = new ArrayList<>(list.size());
            for (int i = 0; i < list.size(); ++i) {
                final Object obj = list.get(i);

                //Modifier must be a map
                if (!(obj instanceof Map)) {
                    throw new IllegalArgumentException("Invalid type for boss setting " + entry.getKey() + " modifier #" + i + ", expected Map and got " + obj.getClass());
                }

                //Map key must be a string
                final Map<?, ?> map = (Map<?, ?>) obj;
                if (!map.keySet().stream().allMatch(String.class::isInstance)) {
                    throw new IllegalArgumentException("Invalid type for boss setting " + entry.getKey() + " modifier #" + i + " key, expected String");
                }

                //Entry must have required fields
                if (!map.containsKey("type") || !map.containsKey("value")) {
                    throw new IllegalArgumentException("Missing required field for boss setting " + entry.getKey() + " modifier #" + i + ", needs both \"type\" and \"value\" to function");
                }

                modifierList.add(new BossModifier((Map<String, Object>) map));
            }

            //Perform calculations and store in map
            this.settingsMap.put(entry.getKey(), new CachedBossSetting(modifierList));
        }
    }

    /**
     * Gets a cached setting in the map
     * @param settingName name of setting
     * @param level level of Wither in [1,5] range
     * @param difficulty current difficulty of world
     * 
     * @return setting value
     * 
     * @throws IllegalArgumentException if requested setting is not in the map
     * @throws ArrayIndexOutOfBoundsException if level is invalid
     */
    public double getSetting(final String settingName, int level, Difficulty difficulty) throws IllegalArgumentException {
        final CachedBossSetting setting = this.settingsMap.getOrDefault(settingName, null);
        if (setting == null) throw new IllegalArgumentException("Setting does not exist in the map");

        return setting.getValue(level, difficulty);
    }

    /**
     * Convenience method to get a cached setting in the map without having to manually retrieve level and difficulty
     * @param settingName name of setting
     * @param wither Ominous Wither entity
     * 
     * @return setting value
     * 
     * @throws IllegalArgumentException if requested setting is not in the map
     * @throws ArrayIndexOutOfBoundsException if stored level is invalid
     */
    public double getSetting(final String settingName, final Wither wither) throws IllegalArgumentException {
        final World world = wither.getWorld();

        //If world is not available for some reason, assume easy difficulty
        final Difficulty difficulty = ((world != null) ? world.getDifficulty() : Difficulty.EASY);

        //If level is not availablef or some reason, assume lowest level
        final int level = this.plugin.getLevel(wither, 1);

        return this.getSetting(settingName, level, difficulty);
    }

    /**
     * Gets all settings' names
     */
    public Set<String> getSettingNames() {
        return settingsMap.keySet();
    }

}