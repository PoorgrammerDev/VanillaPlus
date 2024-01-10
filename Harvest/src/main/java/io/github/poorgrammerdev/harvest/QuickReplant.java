package io.github.poorgrammerdev.harvest;

import java.util.Collection;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Harvest and replant/replace a grown crop immediately by right clicking it with another crop.
 */
public class QuickReplant extends AbstractModule {
    private final CropSeedMapper cropSeedMapper;

    public QuickReplant(final Harvest plugin, final CropSeedMapper cropSeedMapper) {
        super(plugin);
        this.cropSeedMapper = cropSeedMapper;
    }

    @Override
    public boolean register() {
        return super.register("modules.quick_replant");
    }

    /**
     * Listens for a player right clicking a crop and delegates proper behaviour
     */
    @EventHandler(ignoreCancelled = true)
    public void cropInteract(PlayerInteractEvent event) {
        //Has to be a right click
        if (
            event.getAction() != Action.RIGHT_CLICK_BLOCK ||
            event.getHand() != EquipmentSlot.HAND
        ) return;

        //Players can disable this behaviour by sneaking
        final Player player = event.getPlayer();
        if (player.isSneaking()) return;

        final Block block = event.getClickedBlock();
        final ItemStack item = event.getItem();
        
        // Cannot be block placement
        // If the block is a seed then it overrides this rule
        // (e.g. nether wart right clicking is counted as a block placement)
        if (event.isBlockInHand() && !this.cropSeedMapper.isSeed(item.getType())) return;

        // Make sure that the held item is not a hoe
        // Since this triggers a different system (Crop Cascade)
        if (item != null && Tag.ITEMS_HOES.isTagged(item.getType())) return;

        attemptCropReplace(block, item);
    }

    /**
     * Attempt to replace a crop with a seed
     * @param block The block of the existing grown crop to be replaced
     * @param item The seed being used to replace it, if applicable
     * @return success
     */
    public boolean attemptCropReplace(Block block, ItemStack item) {
        //Make sure the block's a fully grown crop
        final World world = block.getWorld();
        if (block == null || world == null || !isGrownCrop(block)) return false;

        final Collection<ItemStack> drops = block.getDrops();

        //Using seed in hand -> subtract from hand
        if (item != null && item.getAmount() > 0) {
            final Material replacementAsCrop = cropSeedMapper.getCrop(item.getType());

            if (replacementAsCrop != null && cropSeedMapper.baseBlocksMatch(replacementAsCrop, block.getType())) {
                //Drop the expected drops
                drops.forEach(drop -> world.dropItemNaturally(block.getLocation(), drop));

                //Plant the crop
                item.setAmount(item.getAmount() - 1);
                block.setType(replacementAsCrop, true);
                return true;
            }
        }

        //Not a seed -> subtract from crop seed drop
        if (item == null || item.getType() != Material.BONE_MEAL) {
            //Remove one seed from the drops if possible
            final Material seed = cropSeedMapper.getSeed(block.getType());
            for (final ItemStack drop : drops) {
                if (drop.getType() == seed) {
                    drop.setAmount(drop.getAmount() - 1);
                    break;
                }
            }

            //Drop the expected drops
            drops.forEach(drop -> world.dropItemNaturally(block.getLocation(), drop));

            //Replant the crop
            block.setType(block.getType(), true);
            return true;
        }

        return false;
    }

    /**
     * Checks if the block is a crop and is fully grown
     */
    public boolean isGrownCrop(Block block) {
        if (!cropSeedMapper.isCrop(block.getType()) || !(block.getBlockData() instanceof Ageable)) return false;
        final Ageable data = (Ageable) block.getBlockData();
        return (data.getAge() == data.getMaximumAge());
    }
}
