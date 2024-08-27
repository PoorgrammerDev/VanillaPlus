package io.github.poorgrammerdev.dispensersshearpumpkins;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;

public final class DispensersShearPumpkins extends JavaPlugin implements Listener {
    final Random random = new Random();

    @Override
    public void onEnable() {
        super.onEnable();
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    /**
     * Handles the plugin's main mechanism
     * @param event
     */
    @EventHandler(ignoreCancelled = true)
    public void onDispense(final BlockDispenseEvent event) {
        //Must be dispensed from a dispenser, not a dropper or any other block
        final Block dispenserBlock = event.getBlock();
        if (dispenserBlock == null || dispenserBlock.getType() != Material.DISPENSER || !(dispenserBlock.getBlockData() instanceof Directional) || !(dispenserBlock.getState() instanceof Dispenser)) return;

        final World world = dispenserBlock.getWorld();
        if (world == null) return;

        //Dispenser must be using shears
        final ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.SHEARS) return;

        //The block that the dispenser is facing must be an uncarved pumpkin
        final BlockFace facing = ((Directional) dispenserBlock.getBlockData()).getFacing();
        final Block targetBlock = dispenserBlock.getRelative(facing);
        if (targetBlock == null || targetBlock.getType() != Material.PUMPKIN) return;

        final Dispenser dispenserState = (Dispenser) dispenserBlock.getState();
        final ItemStack matchingShears = findMatchingItem(dispenserState, item);
        
        // Couldn't find matching item in dispenser inventory
        if (matchingShears == null) return;

        //Handle durability and update inventory 
        handleDurability(matchingShears);
        dispenserState.update(true);

        //Carve the pumpkin and face away from dispenser if possible
        targetBlock.setType(Material.CARVED_PUMPKIN);
        if (facing != BlockFace.UP && facing != BlockFace.DOWN && targetBlock.getBlockData() instanceof Directional) {
            final Directional targetBlockData = (Directional) targetBlock.getBlockData();
            targetBlockData.setFacing(facing);
            targetBlock.setBlockData(targetBlockData);
        }

        //Drop seeds and play sfx
        world.dropItemNaturally(targetBlock.getLocation(), new ItemStack(Material.PUMPKIN_SEEDS, 4));
        world.playSound(dispenserBlock.getLocation(), Sound.BLOCK_PUMPKIN_CARVE, SoundCategory.BLOCKS, 1.0f, 1.0f);
        event.setItem(new ItemStack(Material.AIR, 0)); // makes the dispenser play success sound
    }

    /**
     * Searches the dispenser's inventory for the matching dispensed item
     * @param state Dispenser block state
     * @param dispensed Dispensed item
     * @return Matching inventory item if found, or null if not found
     */
    private ItemStack findMatchingItem(final Dispenser state, final ItemStack dispensed) {
        for (final ItemStack inventoryItem : state.getInventory()) {
            if (inventoryItem != null && inventoryItem.isSimilar(dispensed)) {
                return inventoryItem;
            }
        }
        return null;
    }

    /**
     * Subtracts durability from shears on use, respects Unbreaking enchantment
     */
    private void handleDurability(final ItemStack shears) {
        if (!(shears.getItemMeta() instanceof Damageable)) return;
        final Damageable meta = (Damageable) shears.getItemMeta();

        //Unbreaking calculation is derived from Minecraft Wiki as of 2024: https://minecraft.wiki/w/Unbreaking
        final int unbreaking = meta.getEnchantLevel(Enchantment.UNBREAKING);
        if (!meta.isUnbreakable() && (unbreaking == 0 || ((random.nextInt(100) + 1) <= (100 / (unbreaking + 1))))) {
            final int damage = meta.getDamage() + 1;

            //Handle tool breaking if necessary
            if (
                (meta.hasMaxDamage() && damage >= meta.getMaxDamage()) ||
                (damage >= Material.SHEARS.getMaxDurability())
            ) {
                shears.setAmount(shears.getAmount() - 1);
            }
            else {
                meta.setDamage(damage);
                shears.setItemMeta(meta);
            }
        }
    }



}
