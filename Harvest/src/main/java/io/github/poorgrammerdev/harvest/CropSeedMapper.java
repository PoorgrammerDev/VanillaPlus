package io.github.poorgrammerdev.harvest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;

/**
 * Maps seed items to crop items and vice versa.
 */
public class CropSeedMapper {
    private static final int ENTRY_CROP_INDEX = 0;
    private static final int ENTRY_SEED_INDEX = 1;
    private static final int ENTRY_BASE_INDEX = 2;


    private final Harvest plugin;
    private final HashMap<Material, Material> cropToSeedMap;
    private final HashMap<Material, Material> seedToCropMap;
    private final HashMap<Material, Material> cropToBaseMap;

    public CropSeedMapper(final Harvest plugin) {
        this.plugin = plugin;

        this.cropToSeedMap = new HashMap<>();
        this.seedToCropMap = new HashMap<>();
        this.cropToBaseMap = new HashMap<>();

        final boolean populateSuccess = populateCropDefinitions();
        if (!populateSuccess) {
            //Unsuccessful, fallback to default values
            this.plugin.getLogger().severe("Crop Definitions population unsucessful. Falling back to default built-in values!");

            //Forward map
            this.cropToSeedMap.put(Material.WHEAT, Material.WHEAT_SEEDS);
            this.cropToSeedMap.put(Material.CARROTS, Material.CARROT);
            this.cropToSeedMap.put(Material.POTATOES, Material.POTATO);
            this.cropToSeedMap.put(Material.BEETROOTS, Material.BEETROOT_SEEDS);
            this.cropToSeedMap.put(Material.NETHER_WART, Material.NETHER_WART);

            //Inverse map
            for (final Map.Entry<Material, Material> entry : this.cropToSeedMap.entrySet()) {
                this.seedToCropMap.put(entry.getValue(), entry.getKey());
            }

            //Base map
            this.cropToBaseMap.put(Material.WHEAT, Material.FARMLAND);
            this.cropToBaseMap.put(Material.CARROTS, Material.FARMLAND);
            this.cropToBaseMap.put(Material.POTATOES, Material.FARMLAND);
            this.cropToBaseMap.put(Material.BEETROOTS, Material.FARMLAND);
            this.cropToBaseMap.put(Material.NETHER_WART, Material.SOUL_SAND);
        }
    }

    /**
     * Checks if an item is a crop
     */
    public boolean isCrop(Material material) {
        return this.cropToSeedMap.containsKey(material);
    }

    /**
     * Checks if an item is a seed
     */
    public boolean isSeed(Material material) {
        return this.seedToCropMap.containsKey(material);
    }

    /**
     * Gets the corresponding crop from a seed
     */
    public Material getCrop(Material seed) {
        return isSeed(seed) ? this.seedToCropMap.get(seed) : null;
    }

    /**
     * Gets the corresponding seed from a crop
     */
    public Material getSeed(Material crop) {
        return isCrop(crop) ? this.cropToSeedMap.get(crop) : null;
    }

    /**
     * Gets the corresponding base block from a crop
     */
    public Material getBaseBlock(final Material crop) {
        return isCrop(crop) ? this.cropToBaseMap.get(crop) : null;
    }

    public boolean baseBlocksMatch(final Material crop1, final Material crop2) {
        return (isCrop(crop1) && isCrop(crop2) && this.cropToBaseMap.get(crop1) == this.cropToBaseMap.get(crop2));
    }

    private boolean populateCropDefinitions() {
        final List<?> definitions = plugin.getConfig().getList("crop_definitions", null);
        if (definitions == null) return false;

        for (final Object obj : definitions) {
            if (obj instanceof List) {
                //Entry must be 3 fields to conform to format
                final List<?> entry = (List<?>) obj;
                final Material[] parsedEntry = parseEntry(entry);

                if (parsedEntry != null) {
                    this.cropToSeedMap.put(parsedEntry[ENTRY_CROP_INDEX], parsedEntry[ENTRY_SEED_INDEX]);
                    this.seedToCropMap.put(parsedEntry[ENTRY_SEED_INDEX], parsedEntry[ENTRY_CROP_INDEX]);
                    this.cropToBaseMap.put(parsedEntry[ENTRY_CROP_INDEX], parsedEntry[ENTRY_BASE_INDEX]);
                    continue;
                }
            }
            
            this.plugin.getLogger().warning("Entry " + obj.toString() + " contained an error, skipping!");
        }

        return true;
    }

    private Material[] parseEntry(final List<?> entry) {
        if (entry.size() != 3) return null;

        final Material[] output = new Material[3];
        for (int i = 0; i < 3; i++) {
            if (entry.get(i) instanceof String) {
                try {
                    output[i] = Material.valueOf((String) entry.get(i));
                }
                catch (IllegalArgumentException exception) {
                    return null;
                }
            }
            else {
                return null;
            }
        }

        return output;
    }
    
    
}
