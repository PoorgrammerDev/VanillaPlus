package io.github.poorgrammerdev.harvest;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Dispenser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Dispensers can plant crops onto farmland
 */
public class AutoPlanter extends AbstractModule {
    private static final int MIN_RANGE = 1;

    private final CropSeedMapper cropSeedMapper;
    private final int maxRange;

    public AutoPlanter(final Harvest plugin, final CropSeedMapper cropSeedMapper) {
        super(plugin);
        this.cropSeedMapper = cropSeedMapper;

        this.maxRange = Math.max(plugin.getConfig().getInt("auto_planter_max_range", 4), MIN_RANGE);
    }

    @Override
    public boolean register() {
        return super.register("modules.auto_planter");
    }

    @EventHandler(ignoreCancelled = true)
    public void plantCrop(BlockDispenseEvent event) {
        //Must be dispensed from a dispenser, not a dropper or any other block
        final Block dispenser = event.getBlock();
        if (dispenser == null || dispenser.getType() != Material.DISPENSER || !(dispenser.getBlockData() instanceof Dispenser)) return;

        //Cannot be facing up or down
        final BlockFace facing = ((Dispenser) dispenser.getBlockData()).getFacing();
        if (facing != BlockFace.NORTH && facing != BlockFace.EAST && facing != BlockFace.WEST && facing != BlockFace.SOUTH) return;

        //Dispensed item must be a seed
        final ItemStack item = event.getItem();
        if (item == null || !this.cropSeedMapper.isSeed(item.getType())) return;

        final Material crop = this.cropSeedMapper.getCrop(item.getType());
        final Material baseBlock = (crop == Material.NETHER_WART) ? Material.SOUL_SAND : Material.FARMLAND;

        //Search in range for valid block to plant on
        for (int i = 1; i <= this.maxRange; i++) {
            final Block block = dispenser.getRelative(facing, i);

            //Make sure there's no solid block blocking -- cannot plant through walls!
            final Material type = block.getType();
            if (type.isSolid() && !this.cropSeedMapper.isCrop(type)) return;

            //check if plantable
            final Block below = block.getRelative(BlockFace.DOWN);
            if (type.isAir() && below.getType() == baseBlock) {
                block.setType(crop);
                event.setItem(new ItemStack(Material.AIR, 0));
                return;
            }
        }
    }
    
}
