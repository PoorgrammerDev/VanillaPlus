package io.github.poorgrammerdev.harvest;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Dispensers can plant crops onto farmland
 */
public class AutoPlanter extends AbstractModule {
    private static final int MIN_RANGE = 1;

    private final CropSeedMapper cropSeedMapper;
    private final int maxRange;
    private final boolean allowWorkaround;

    public AutoPlanter(final Harvest plugin, final CropSeedMapper cropSeedMapper) {
        super(plugin);
        this.cropSeedMapper = cropSeedMapper;

        this.maxRange = Math.max(plugin.getConfig().getInt("auto_planter_max_range", 4), MIN_RANGE);
        this.allowWorkaround = plugin.getConfig().getBoolean("auto_planter_allow_workaround", true);
    }

    @Override
    public boolean register() {
        return super.register("modules.auto_planter");
    }

    @EventHandler(ignoreCancelled = true)
    public void autoPlanting(BlockDispenseEvent event) {
        //Must be dispensed from a dispenser, not a dropper or any other block
        final Block dispenserBlock = event.getBlock();
        if (dispenserBlock == null || dispenserBlock.getType() != Material.DISPENSER || !(dispenserBlock.getBlockData() instanceof Directional) || !(dispenserBlock.getState() instanceof Dispenser)) return;

        //Cannot be facing up or down
        final BlockFace facing = ((Directional) dispenserBlock.getBlockData()).getFacing();
        if (facing != BlockFace.NORTH && facing != BlockFace.EAST && facing != BlockFace.WEST && facing != BlockFace.SOUTH) return;

        //Dispensed item must be a seed
        final ItemStack item = event.getItem();
        if (item == null || !this.cropSeedMapper.isSeed(item.getType())) return;

        final Material crop = this.cropSeedMapper.getCrop(item.getType());
        final Material baseBlock = this.cropSeedMapper.getBaseBlock(crop);

        //Find a valid block to plant the crop on
        final Block cropBlock = findValidCropPlacement(dispenserBlock, facing, baseBlock);
        if (cropBlock == null) return;

        //Search for the item we're dispensing inside the dispenser's inventory
        final ItemStack inventoryItem = findMatchingItem((Dispenser) dispenserBlock.getState(), item);
        
        //Special case -- item was not found
        // (More specifically, there is exactly one seed left in the dispenser: weird Bukkit quirk it seems)
        if (inventoryItem == null) {
            // Workaround is allowed by config
            if (this.allowWorkaround) {
                //Prevent dispense and re-attempt one tick later
                event.setItem(new ItemStack(Material.AIR, 0));

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        final ItemStack inventoryItemNew = findMatchingItem((Dispenser) dispenserBlock.getState(), item);
                        if (inventoryItemNew != null) {
                            //Since one tick has passed, it's EXTREMELY unlikely but also possible
                            //that the cropBlock location from earlier may be infeasible now
                            //So we will search for a valid location again
                            final Block cropBlockNew = findValidCropPlacement(dispenserBlock, facing, baseBlock);
                            if (cropBlockNew != null) {
                                inventoryItemNew.setAmount(inventoryItemNew.getAmount() - 1);
                                cropBlockNew.setType(crop);
                            }
                        }
                    }
                }.runTaskLater(this.plugin, 1L);
            }
            else {
                event.setCancelled(true);
            }
        }

        //General case -- item was found, continue with planting
        else {
            event.setItem(new ItemStack(Material.AIR, 0));
            inventoryItem.setAmount(inventoryItem.getAmount() - 1);
            cropBlock.setType(crop);
        }

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
     * Finds a valid location to plant the crop at
     * @param dispenserBlock Activated dispenser block
     * @param facing Direction the dispenser block is facing
     * @param targetBaseBlock What blocktype the crop must be planted on (e.g. farmland for wheat seeds)
     * @return Block object if suitable place is found, otherwise null
     */
    private Block findValidCropPlacement(final Block dispenserBlock, final BlockFace facing, final Material targetBaseBlock) {
        //Search in range for valid block to plant on
        for (int i = 1; i <= this.maxRange; i++) {
            final Block block = dispenserBlock.getRelative(facing, i);

            //Make sure there's no solid block blocking -- cannot plant through walls!
            final Material type = block.getType();
            if (type.isSolid() && !this.cropSeedMapper.isCrop(type)) return null;

            //check if plantable
            final Block below = block.getRelative(BlockFace.DOWN);
            if (type.isAir() && below.getType() == targetBaseBlock) {
                return block;
            }
        }

        return null;
    }
}
