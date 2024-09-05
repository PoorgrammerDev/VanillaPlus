package io.github.poorgrammerdev.paxel;

import java.util.ArrayList;
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
    private final HashMap<String, Double> speedMap;
    private final HashMap<String, String> incorrectTagMap;

    public ToolMapper(Paxel plugin) {
        this.plugin = plugin;
        this.toolsMap = new HashMap<>();
        this.tierMap = new HashMap<>();
        this.speedMap = new HashMap<>();
        this.incorrectTagMap = new HashMap<>();

        this.initializeToolCategory(Tag.ITEMS_AXES, AXE_INDEX);
        this.initializeToolCategory(Tag.ITEMS_PICKAXES, PICKAXE_INDEX);
        this.initializeToolCategory(Tag.ITEMS_SHOVELS, SHOVEL_INDEX);

        this.deleteMarkedTiers(this.initializeSpeedMap(), "Base Mining Speed");
        this.deleteMarkedTiers(this.initializeIncorrectTagMap(), "Incorrect Tag");
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
     * Gets this tool tier's mining speed
     * @param tier Mineral tier of the tool (e.g. IRON)
     * @return mining speed value or null if tier is invalid
     */
    public Double getSpeed(final String tier) {
        return this.speedMap.getOrDefault(tier, null);
    }

    /**
     * Gets this tool tier's block tag for incorrect blocks
     * @param tier Mineral tier of the tool (e.g. IRON)
     * @return incorrect blocks tag (e.g. "incorrect_for_diamond_tool") or null if tier is invalid
     */
    public String getIncorrectTag(final String tier) {
        return this.incorrectTagMap.getOrDefault(tier, null);
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

    /**
     * <p>Populates the speed map with values from the config.</p>
     * <p>Precondition: Must be ran after at all invocations to initializeToolCategory!</p>
     * @return list of tiers marked for deletion, if any
     */
    private ArrayList<String> initializeSpeedMap() {
        final ArrayList<String> markedForDeletion = new ArrayList<>();

        for (final String tier : this.toolsMap.keySet()) {
            final String configPath = "base_mining_speeds." + tier.toLowerCase();

            // Speed must be present in config
            // If it isn't, cannot add components to perform paxel mechanism since we have no idea what speed to set it to. So, just disable that tier's paxel altogether
            // The alternative of setting to some default value instead is not being considered since this data is saved into the item itself and will be a mess to fix
            if (!this.plugin.getConfig().contains(configPath)) {
                markedForDeletion.add(tier);
                continue;
            }

            //Populate speed value for tier
            final double speed = this.plugin.getConfig().getDouble(configPath);
            this.speedMap.put(tier, speed);
        }

        return markedForDeletion;
    }

    /**
     * <p>Populates the incorrect tag map with values from the config.</p>
     * <p>Precondition: Must be ran after at all invocations to initializeToolCategory!</p>
     * @return list of tiers marked for deletion, if any
     */
    private ArrayList<String> initializeIncorrectTagMap() {
        final ArrayList<String> markedForDeletion = new ArrayList<>();

        for (final String tier : this.toolsMap.keySet()) {
            final String configPath = "incorrect_tag." + tier.toLowerCase();

            // Incorrect Tag must be present in config
            // If it isn't, cannot add components to perform paxel mechanism since we have no idea what shouldn't be able to be mined. So, just disable that tier's paxel altogether
            // The alternative of setting to some default value instead is not being considered since this data is saved into the item itself and will be a mess to fix
            if (!this.plugin.getConfig().contains(configPath)) {
                markedForDeletion.add(tier);
                continue;
            }

            //Populate incorrect tag value for tier
            this.incorrectTagMap.put(tier, this.plugin.getConfig().getString(configPath));
        }

        return markedForDeletion;
    }

    private void deleteMarkedTiers(final ArrayList<String> markedForDeletion, final String setting) {
        for (final String tier : markedForDeletion) {
            this.plugin.getLogger().severe("Setting \"" + setting + "\" for tier \"" + tier.toLowerCase() + "\" is not defined in the config! Crafting and obtaining paxels of this tier are disabled.");
            this.toolsMap.remove(tier);
            this.tierMap.entrySet().removeIf(entry -> (tier.equals(entry.getValue())));
        }
    }
}
