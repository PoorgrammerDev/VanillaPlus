package io.github.poorgrammerdev.paxel;

import java.util.Collection;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.Tag;

/**
 * Creates mappings to get the sets of tools from each tool
 * @author Thomas Tran
 */
public class ToolMapper {
    public static final int AXE_INDEX = 0;
    public static final int PICKAXE_INDEX = 1;
    public static final int SHOVEL_INDEX = 2;

    private final Paxel plugin;
    private final HashMap<String, Material[]> toolsMap;
    private final HashMap<Material, String> tierMap;

    public ToolMapper(Paxel plugin) {
        this.plugin = plugin;
        this.toolsMap = new HashMap<>();
        this.tierMap = new HashMap<>();

        initializeToolCategory(Tag.ITEMS_AXES, AXE_INDEX);
        initializeToolCategory(Tag.ITEMS_PICKAXES, PICKAXE_INDEX);
        initializeToolCategory(Tag.ITEMS_SHOVELS, SHOVEL_INDEX);
    }

    /**
     * Get the corresponding tool set from the tier name
     * @param tier Mineral tier of the tool (e.g. IRON)
     * @return 3-valued array with the tools in question (e.g. {IRON_AXE, IRON_PICKAXE, IRON_SHOVEL}) or null if input is invalid
     */
    public Material[] getToolSet(String tier) {
        return (this.toolsMap.containsKey(tier)) ? this.toolsMap.get(tier) : null;
    }

    /**
     * Get the corresponding tool set from the tool
     * @param material The tool item type (e.g. IRON_PICKAXE)
     * @return 3-valued array with the tools in question (e.g. {IRON_AXE, IRON_PICKAXE, IRON_SHOVEL}) or null if input is invalid
     */
    public Material[] getToolSet(Material material) {
        final String toolTier = getToolTier(material);
        return (toolTier != null) ? getToolSet(toolTier) : null;
    }

    /**
     * Get the corresponding tier for a given tool
     * @param material The tool item type (e.g. IRON_PICKAXE)
     * @return The mineral tier of the tool (e.g. IRON) or null if the input is invalid
     */
    public String getToolTier(Material material) {
       return (this.tierMap.containsKey(material)) ? this.tierMap.get(material) : null; 
    }

    /**
     * Gets all mineral tiers that were manually gathered
     * @return all mineral tool tiers
     */
    public Collection<String> getAllTiers() {
        return this.toolsMap.keySet();
    }

    /**
     * Checks if the string is a valid mineral tier
     */
    public boolean isTier(String tier) {
        return (this.toolsMap.containsKey(tier));
    }

    /**
     * Checks to see if the material is a tool
     */
    public boolean isTool(Material material) {
        return this.tierMap.containsKey(material);
    }

    /**
     * For a category of tools (e.g. pickaxes), initialize their values in the maps
     * @param toolCategory
     * @param toolSetIndex
     */
    private void initializeToolCategory(Tag<Material> toolCategory, int toolSetIndex) {
        // Create the tool tiers and tool sets from scratch
        for (final Material type : toolCategory.getValues()) {
            final String[] typeNameSplitted = type.name().split("_");

            //Unexpected error -> log to console
            if (typeNameSplitted == null || typeNameSplitted.length == 0) {
                this.plugin.getLogger().warning("ToolMapper received unexpected value from item " + type.name());
                continue;
            }

            // We are assuming that in a given tier of tools,
            // their Material names before the first underscore will be equivalent
            // e.g. Material.IRON_PICKAXE -> IRON ; Material.IRON_AXE -> IRON
            final String tierName = typeNameSplitted[0];
            if (!toolsMap.containsKey(tierName)) {
                //Array of size 3 for the three tools we are using (axe, pick, shovel)
                toolsMap.put(tierName, new Material[3]);
            }

            toolsMap.get(tierName)[toolSetIndex] = type;
            tierMap.put(type, tierName);
        }
    }
}
