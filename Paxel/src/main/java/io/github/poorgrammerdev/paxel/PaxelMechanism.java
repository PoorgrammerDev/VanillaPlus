package io.github.poorgrammerdev.paxel;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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

        final Material blockType = event.getBlock().getType();
        
        //If there is no mismatch or if it is not applicable -> do nothing and return
        final int toolSwapType = getToolSwapType(tool.getType(), blockType);
        if (toolSwapType < 0) return;

        //Swap the paxel type to match the block player is mining
        swapPaxelType(tool, toolSwapType);
    }

    /**
     * Handles the paxel being able to use the axe's log strip functionality
     * or the shovel's pathing functionality from any of the paxel tools
     */
    @EventHandler(ignoreCancelled = true)
    public void paxelInteract(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        final ItemStack tool = event.getItem();
        if (!this.plugin.isPaxel(tool)) return;

        final Block block = event.getClickedBlock();
        if (block == null) return;

        //Attempting to strip a log without the paxel being in axe mode
        if (Tag.LOGS.isTagged(block.getType()) && !Tag.ITEMS_AXES.isTagged(tool.getType())) {
            //Swaps to the correct type
            swapPaxelType(tool, ToolMapper.AXE_INDEX);

            //After swapping the type the intended action is handled automatically (though I'm not exactly sure why)
        }

        //Attempting to path a dirt block without the paxel in shovel mode
        else if (Tag.DIRT.isTagged(block.getType()) && !Tag.ITEMS_SHOVELS.isTagged(tool.getType())) {
            //Swaps to the correct type
            swapPaxelType(tool, ToolMapper.SHOVEL_INDEX);

            //After swapping the type the intended action is handled automatically (though I'm not exactly sure why)
        }
    }

    /**
     * Swaps the paxel type 
     * @param tool Paxel tool (does not check if it's a paxel, must confirm before using function)
     * @param toolSwapIndex tool mapper index
     */
    private void swapPaxelType(final ItemStack tool, final int toolSwapIndex) {
        //Lookup the appropriate item with error checking
        final String tier = toolMapper.getToolTier(tool.getType());
        if (tier == null) return;

        final Material[] toolSet = toolMapper.getToolSet(tier);
        if (toolSet == null || toolSet.length <= toolSwapIndex) return;
        
        //Swap the item type
        tool.setType(toolSet[toolSwapIndex]);
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