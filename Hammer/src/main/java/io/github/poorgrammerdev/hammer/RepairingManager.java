package io.github.poorgrammerdev.hammer;

import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;
import org.bukkit.Tag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;

/**
 * Handles repairing of Hammers via crafting, grindstones, etc.
 * Also prevents hammer-nonhammer cross-repair.
 * @author Thomas Tran
 */
public class RepairingManager implements Listener {
    private final Hammer plugin;

    public RepairingManager(Hammer plugin) {
        this.plugin = plugin;
    }
   
    /*
     * REPAIR RULES:
     * Hammer + Hammer = Repaired Hammer
     * Hammer + NonHammer is not allowed
     * NonHammer + NonHammer continues as normal (repaired regular item)
     * 
     * In text form:
     * Hammers can be combined with each other to repair, like regular items
     * Hammers cannot be combined with regular tools to repair the regular tool
     */

    /**
     * Implements hammer repairing rules for craft repairs
     */
    @EventHandler(ignoreCancelled = true)
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

        final boolean isHammer1 = plugin.isHammer(tools.get(0));
        final boolean isHammer2 = plugin.isHammer(tools.get(1));

        //Both tools are hammers -> perform hammer repair
        if (isHammer1 && isHammer2) {
            final ItemStack newHammer = toHammer(event.getInventory().getResult());
            event.getInventory().setResult(newHammer);
        }

        //Mismatching types, cancel the operation
        else if (isHammer1 ^ isHammer2) {
            event.getInventory().setResult(null);
        } 

        //Otherwise repairing regular tools, continue as normal
    }

    /**
     * Implements hammer repairing rules for grindstone repairs
     */
    @EventHandler(ignoreCancelled = true)
    public void grindstoneRepairing(PrepareGrindstoneEvent event) {
        final ItemStack[] ingredients = event.getInventory().getStorageContents();
        if (ingredients.length != 2) return;

        //If player is grindstoning a single item (one of them is null), ignore
        if (ingredients[0] == null ^ ingredients[1] == null) return; //TODO: why is this XOR and anvil one is inclusive OR?

        final boolean isHammer1 = plugin.isHammer(ingredients[0]);
        final boolean isHammer2 = plugin.isHammer(ingredients[1]);

        //Both tools are hammers -> perform hammer repair
        if (isHammer1 && isHammer2) {
            final ItemStack newHammer = toHammer(event.getResult());
            event.setResult(newHammer);
        }

        //Mismatching types, cancel the operation
        else if (isHammer1 ^ isHammer2) {
            event.setResult(null);
        } 

        //Otherwise repairing regular tools, continue as normal
    }

    /**
     * Implements hammer repairing rules for anvil repairs
     */
    @EventHandler(ignoreCancelled = true)
    public void anvilRepairing(PrepareAnvilEvent event) {
        final ItemStack[] items = event.getInventory().getStorageContents();
        if (items.length != 2) return;

        //If player is anvil'ing a single item (maybe renaming) -> ignore
        if (items[0] == null || items[1] == null) return;

        //If player is anvil'ing the hammer against a non-tool (repairing with mineral or apply ench. book) -> ignore
        if (!Tag.ITEMS_PICKAXES.isTagged(items[0].getType()) || !Tag.ITEMS_PICKAXES.isTagged(items[1].getType())) return;

        //Attempting hammer-nonhammer cross repair -> disallow
        if (plugin.isHammer(items[0]) ^ plugin.isHammer(items[1])) {
            event.setResult(null);
        }

        //Otherwise everything is fine
    }

    /**
     * Tool should be renamed to new tier when upgraded in smithing table
     */
    @EventHandler(ignoreCancelled = true)
    public void smithingTable(PrepareSmithingEvent event) {
        final ItemStack result = event.getResult();
        if (result == null) return;

        final ItemMeta meta = result.getItemMeta();
        if (meta == null || !plugin.isHammer(result)) return;
        
        //Compare the existing name with the expected name
        //If they match then nothing needs to be changed
        final String oldName = ChatColor.stripColor(meta.getDisplayName());
        final String newName = plugin.getHammerName(result.getType());
        if (oldName.equals(newName)) return;

        //Search for the ingredient (base) hammer
        ItemStack oldHammer = null;
        for (final ItemStack ingredient : event.getInventory()) {
            if (this.plugin.isHammer(ingredient)) {
                oldHammer = ingredient;
                break;
            }
        }

        //Add the new name if the tool is not already renamed
        if (oldHammer == null || oldName.equals(plugin.getHammerName(oldHammer.getType()))) {
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
     * Used in crafting and grindstone repairs to convert the output to a hammer.
     * Anvil repairs preserve NBT so this is not needed there.
     * @param result The regular non-hammer resulting item from repairing two hammers together
     * @return A hammer of the same mineral tier as the result item, with the correct durability
     */
    private ItemStack toHammer(ItemStack result) {
        //Convert the final result to a hammer but collect the damage first
        if (result != null && result.getItemMeta() instanceof Damageable) {
            Damageable meta = (Damageable) result.getItemMeta();
            final ItemStack newHammer = plugin.createHammer(result.getType());

            //Set the damage accordingly
            if (newHammer != null && newHammer.getItemMeta() instanceof Damageable) {
                Damageable hammerMeta = (Damageable) newHammer.getItemMeta();

                hammerMeta.setDamage(meta.getDamage());
                newHammer.setItemMeta(hammerMeta);
                return newHammer;
            }
        }
        return null;
    }

}
