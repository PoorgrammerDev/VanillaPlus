package io.github.poorgrammerdev.paxel;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Swaps the paxel's base item around during mining to replicate effect
 * @author Thomas Tran
 */
public class PaxelMechanism implements Listener {
    private final Paxel plugin;
    private final ToolMapper toolMapper;
    
    public PaxelMechanism(Paxel plugin, ToolMapper toolMapper) {
        this.plugin = plugin;
        this.toolMapper = toolMapper;
    }

    /**
     * Listens for when the player begins mining a block to switch paxel base tools
     */
    @EventHandler
    public void beginMining(BlockDamageEvent event) {
        if (event.isCancelled()) return;
        
        final ItemStack tool = event.getItemInHand();

        //Make sure the item in question is a paxel
        if (!plugin.isPaxel(tool)) return;

        final Material toolType = tool.getType();
        final Material blockType = event.getBlock().getType();
        
        //If there is no mismatch or if it is not applicable -> do nothing and return
        final int toolSwapType = getToolSwapType(toolType, blockType);
        if (toolSwapType < 0) return;

        //Lookup the appropriate item with error checking
        final String tier = toolMapper.getToolTier(toolType);
        if (tier == null) return;

        final Material[] toolSet = toolMapper.getToolSet(tier);
        if (toolSet == null || toolSet.length <= toolSwapType) return;
        
        //Swap the item type
        tool.setType(toolSet[toolSwapType]);
    }

    /**
     * Checks if the tool being used to mine the block is not the optimal tool.
     * @param toolType
     * @param blockType
     * @return Toolset index for the correct tool to use, or -1 if optimal or -2 if not applicable
     */
    private int getToolSwapType(final Material toolType, final Material blockType) {
        if (Tag.MINEABLE_AXE.isTagged(blockType)) {
            if (!Tag.ITEMS_AXES.isTagged(toolType)) return ToolMapper.AXE_INDEX;
            return -1;
        }
        if (Tag.MINEABLE_PICKAXE.isTagged(blockType)) {
            if (!Tag.ITEMS_PICKAXES.isTagged(toolType)) return ToolMapper.PICKAXE_INDEX;
            return -1;
        }
        if (Tag.MINEABLE_SHOVEL.isTagged(blockType)) {
            if (!Tag.ITEMS_SHOVELS.isTagged(toolType)) return ToolMapper.SHOVEL_INDEX;
            return -1;
        }

        return -2;
    }
}