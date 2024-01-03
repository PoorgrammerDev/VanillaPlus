package io.github.poorgrammerdev.paxel;

import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;

/**
 * Handles repairing of Paxels via crafting, grindstones, etc.
 * Also prevents paxel-nonpaxel cross-repair.
 * @author Thomas Tran
 */
public class RepairingManager implements Listener {
    private final Paxel plugin;
    private final ToolMapper toolMapper;

    public RepairingManager(Paxel plugin, ToolMapper toolMapper) {
        this.plugin = plugin;
        this.toolMapper = toolMapper;
    }
   
    /*
     * REPAIR RULES:
     * Paxel + Paxel = Repaired Paxel
     * Paxel + NonPaxel is not allowed
     * NonPaxel + NonPaxel continues as normal (repaired regular item)
     * 
     * In text form:
     * Paxels can be combined with each other to repair, like regular items
     * Paxels cannot be combined with regular tools to repair the regular tool
     */

    /**
     * Implements paxel repairing rules for craft repairs
     */
    @EventHandler
    public void craftRepairing(PrepareItemCraftEvent event) {
        if (!event.isRepair()) return;

        //Find the two tools being repaired together
        final ArrayList<ItemStack> tools = new ArrayList<>();
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient != null) {
                tools.add(ingredient);
            }
        }

        if (tools.size() != 2) return;

        final boolean isPaxel1 = plugin.isPaxel(tools.get(0));
        final boolean isPaxel2 = plugin.isPaxel(tools.get(1));

        //Both tools are paxels -> perform paxel repair
        if (isPaxel1 && isPaxel2) {
            final ItemStack newPaxel = paxelize(event.getInventory().getResult());
            event.getInventory().setResult(newPaxel);
        }

        //Mismatching types, cancel the operation
        else if (isPaxel1 ^ isPaxel2) {
            event.getInventory().setResult(null);
        } 

        //Otherwise repairing regular tools, continue as normal
    }

    /**
     * Implements paxel repairing rules for grindstone repairs
     */
    @EventHandler
    public void grindstoneRepairing(PrepareGrindstoneEvent event) {
        final ItemStack[] ingredients = event.getInventory().getStorageContents();
        if (ingredients.length != 2) return;

        //If player is grindstoning a single item (one of them is null), ignore
        if (ingredients[0] == null ^ ingredients[1] == null) return;

        final boolean isPaxel1 = plugin.isPaxel(ingredients[0]);
        final boolean isPaxel2 = plugin.isPaxel(ingredients[1]);

        //Both tools are paxels -> perform paxel repair
        if (isPaxel1 && isPaxel2) {
            final ItemStack newPaxel = paxelize(event.getResult());
            event.setResult(newPaxel);
        }

        //Mismatching types, cancel the operation
        else if (isPaxel1 ^ isPaxel2) {
            event.setResult(null);
        } 

        //Otherwise repairing regular tools, continue as normal
    }

    /**
     * Implements paxel repairing rules for anvil repairs
     */
    @EventHandler
    public void anvilRepairing(PrepareAnvilEvent event) {
        final ItemStack[] items = event.getInventory().getStorageContents();
        if (items.length != 2) return;

        //If player is anvil'ing a single item (maybe renaming) -> ignore
        if (items[0] == null || items[1] == null) return;

        //If player is anvil'ing the paxel against a non-tool (repairing with mineral or apply ench. book) -> ignore
        if (!toolMapper.isTool(items[0].getType()) || !toolMapper.isTool(items[1].getType())) return;

        //Attempting paxel-nonpaxel cross repair -> disallow
        if (plugin.isPaxel(items[0]) ^ plugin.isPaxel(items[1])) {
            event.setResult(null);
        }

        //Otherwise everything is fine
    }

    /**
     * Tool should be renamed to new tier when upgraded in smithing table
     */
    @EventHandler
    public void smithingTable(PrepareSmithingEvent event) {
        final ItemStack result = event.getResult();
        if (result == null || result.getType() == Material.AIR) return;

        //Result must be a paxel
        final ItemMeta meta = result.getItemMeta();
        if (meta == null || !plugin.isPaxel(result)) return;
        
        //Compare the existing name with the expected name
        //If they match then nothing needs to be changed
        final String oldName = ChatColor.stripColor(meta.getDisplayName());
        final String newName = plugin.getPaxelName(toolMapper.getToolTier(result.getType()));
        if (oldName.equals(newName)) return;

        //Search for the ingredient (base) paxel
        ItemStack oldPaxel = null;
        for (final ItemStack ingredient : event.getInventory()) {
            if (this.plugin.isPaxel(ingredient)) {
                oldPaxel = ingredient;
                break;
            }
        }

        //Add the new name if the tool is not already renamed
        if (oldPaxel == null || oldName.equals(plugin.getPaxelName(toolMapper.getToolTier(oldPaxel.getType())))) {
            meta.setDisplayName(ChatColor.RESET + newName);
        }

        //Add the new description
        if (plugin.getConfig().getBoolean("write_description", true)) {
            meta.setLore(Arrays.asList(ChatColor.GRAY + newName));
        }

        result.setItemMeta(meta);
        event.setResult(result);
    }

    /**
     * Used in crafting and grindstone repairs to convert the output to a paxel.
     * Anvil repairs preserve NBT so this is not needed there.
     * @param result The regular non-paxel resulting item from repairing two paxels together
     * @return A paxel of the same mineral tier as the result item, with the correct durability
     */
    private ItemStack paxelize(ItemStack result) {
        //Convert the final result to a paxel but collect the damage first
        if (result != null && result.getItemMeta() instanceof Damageable) {
            Damageable meta = (Damageable) result.getItemMeta();

            //Create a new paxel based on the item tier
            final String tier = this.toolMapper.getToolTier(result.getType());
            if (tier != null) {
                final ItemStack newPaxel = plugin.createPaxel(tier);

                //Set the damage accordingly
                if (newPaxel != null && newPaxel.getItemMeta() instanceof Damageable) {
                    Damageable paxelMeta = (Damageable) newPaxel.getItemMeta();

                    paxelMeta.setDamage(meta.getDamage());
                    newPaxel.setItemMeta(paxelMeta);
                    return newPaxel;
                }
            }
        }
        return null;
    }

}
