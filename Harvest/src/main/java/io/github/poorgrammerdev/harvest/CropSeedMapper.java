package io.github.poorgrammerdev.harvest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Material;

/**
 * Maps seed items to crop items and vice versa.
 */
public class CropSeedMapper {
    private final HashMap<Material, Material> cropToSeedMap;
    private final HashMap<Material, Material> seedToCropMap;
    private final List<Material> hoes;

    public CropSeedMapper() {
        //Not using Tag.ITEMS_HOES for previous version support (not added until 1.19.4)
        hoes = Arrays.asList(Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE, Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE);
        this.cropToSeedMap = new HashMap<>();
        this.seedToCropMap = new HashMap<>();
        
        //Not using Tag.CROPS for previous version support (not added until 1.19.4)
        //Also Tag.CROPS includes torchflower and pitcher plants which are not supported

        this.cropToSeedMap.put(Material.WHEAT, Material.WHEAT_SEEDS);
        this.cropToSeedMap.put(Material.CARROTS, Material.CARROT);
        this.cropToSeedMap.put(Material.POTATOES, Material.POTATO);
        this.cropToSeedMap.put(Material.BEETROOTS, Material.BEETROOT_SEEDS);
        this.cropToSeedMap.put(Material.NETHER_WART, Material.NETHER_WART);

        this.seedToCropMap.put(Material.WHEAT_SEEDS, Material.WHEAT);
        this.seedToCropMap.put(Material.CARROT, Material.CARROTS);
        this.seedToCropMap.put(Material.POTATO, Material.POTATOES);
        this.seedToCropMap.put(Material.BEETROOT_SEEDS, Material.BEETROOTS);
        this.seedToCropMap.put(Material.NETHER_WART, Material.NETHER_WART);
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
     * Checks if this material is a hoe or not
     */
    public boolean isHoe(Material material) {
        //I know this is an O(n) operation where hashset is O(1) but the array size is fixed and small
        return this.hoes.contains(material);
    }

    /**
     * Returns a list of hoes.
     */
    public List<Material> getHoes() {
        return this.hoes;
    }
    
    
}
