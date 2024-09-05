package io.github.poorgrammerdev.paxel;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Implements special right click actions (stripping logs or pathing grass)
 * @author Thomas Tran
 */
public class PaxelSpecialActions implements Listener {
    private final Paxel plugin;
    private final ToolMapper toolMapper;
    
    public PaxelSpecialActions(Paxel plugin, ToolMapper toolMapper) {
        this.plugin = plugin;
        this.toolMapper = toolMapper;
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
        else return;

        //One tick later, swaps the tool back to continue using as normal
        new BukkitRunnable() {
            @Override
            public void run() {
                swapPaxelType(tool, ToolMapper.PICKAXE_INDEX);
            }
            
        }.runTaskLater(plugin, 1L);
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
}