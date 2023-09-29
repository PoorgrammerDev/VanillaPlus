package io.github.poorgrammerdev.hammer;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Implements the hammer area mining mechanism
 * @author Thomas Tran
 */
public class HammerMechanism implements Listener {
    //Constants
    private static final float BLOCK_BREAK_EXHAUSTION = 0.005f; //Source: Minecraft Wiki, 2023
    private static final int NORTH_SOUTH = 0;
    private static final int EAST_WEST = 1;
    private static final int UP_DOWN = 2;

    private final Hammer plugin;
    private final Random random;
    private final Vector[][] planeOffsets;
    
    //Config Options
    private final double raytraceDistance;
    private final float hardnessBuffer;
    private final float exhaustionMultiplier;

    public HammerMechanism(final Hammer plugin, final Random random) {
        this.plugin = plugin;
        this.random = random;

        //Load options from config
        this.raytraceDistance = plugin.getConfig().getDouble("raytrace_distance", 10.0);
        this.hardnessBuffer = (float) plugin.getConfig().getDouble("hardness_buffer", 3.0);
        this.exhaustionMultiplier = (float) plugin.getConfig().getDouble("exhaustion_multiplier", 2.0);

        //Populate baked-in values for the adjacent offsets
        this.planeOffsets = new Vector[3][];
        this.planeOffsets[NORTH_SOUTH] = new Vector[]{
            new Vector(0, 1, 0),
            new Vector(0, -1, 0),
            new Vector(1, 0, 0),
            new Vector(1, 1, 0),
            new Vector(1, -1, 0),
            new Vector(-1, 0, 0),
            new Vector(-1, 1, 0),
            new Vector(-1, -1, 0),
        };

        this.planeOffsets[EAST_WEST] = new Vector[]{
            new Vector(0, 1, 0),
            new Vector(0, -1, 0),
            new Vector(0, 0, 1),
            new Vector(0, 1, 1),
            new Vector(0, -1, 1),
            new Vector(0, 0, -1),
            new Vector(0, 1, -1),
            new Vector(0, -1, -1),
        };

        this.planeOffsets[UP_DOWN] = new Vector[]{
            new Vector(1, 0, 0),
            new Vector(-1, 0, 0),
            new Vector(0, 0, 1),
            new Vector(1, 0, 1),
            new Vector(-1, 0, 1),
            new Vector(0, 0, -1),
            new Vector(1, 0, -1),
            new Vector(-1, 0, -1),
        };
    }

    /**
     * Breaks blocks in a 3x3 flat plane when mining with the hammer
     */
    @EventHandler(ignoreCancelled = true)
    public void useHammer(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        if (player.isSneaking()) return;

        final EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return;

        //Check if the tool is a hammer
        final ItemStack tool = equipment.getItemInMainHand();
        if (tool == null || !this.plugin.isHammer(tool)) return;

        final ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable)) return;

        final Damageable damageableMeta = (Damageable) meta;
        final Block targetBlock = event.getBlock();

        //Now we're raycasting out from the player to find the face of the block that they're targeting.
        //(Getting which plane to break the 3x3 in)
        final RayTraceResult rayTraceResult = player.rayTraceBlocks(this.raytraceDistance);
        if (rayTraceResult == null || rayTraceResult.getHitBlock() == null || !rayTraceResult.getHitBlock().equals(targetBlock)) return;

        final BlockFace blockFace = rayTraceResult.getHitBlockFace();
        if (blockFace == null) return;

        //Get the plane of blocks to break
        final int planeIndex = getPlaneIndex(blockFace);
        if (planeIndex == -1) return;

        final Location middleLocation = targetBlock.getLocation();
        final float hardness = targetBlock.getType().getHardness() + this.hardnessBuffer;

        //More hammer information
        final int unbreaking = damageableMeta.getEnchantLevel(Enchantment.DURABILITY);
        final int maxDamage = tool.getType().getMaxDurability();
        int damage = damageableMeta.getDamage();
        
        //Loop through blocks in plane and break them if they can be broken
        int count = 0;
        for (final Vector offset : this.planeOffsets[planeIndex]) {
            targetBlock.getLocation(middleLocation);
            final Block adjacent = middleLocation.add(offset).getBlock();
            final Material type = adjacent.getType();

            if (Tag.MINEABLE_PICKAXE.isTagged(type) && adjacent.getBlockData().isPreferredTool(tool) && type.getHardness() <= hardness) {
                //Break block
                adjacent.breakNaturally(tool);

                //Add damage to tool with resepct to Unbreaking enchantment
                //(Unbreaking calculation is derived from Minecraft Wiki as of 2023)
                if (!(meta.isUnbreakable()) && (unbreaking == 0 || ((random.nextInt(100) + 1) <= (100 / (unbreaking + 1))))) {
                    ++damage;
                }
                
                ++count;

                //If tool is broken, stop area mining early
                if (damage == maxDamage) break;
            }
        }

        //Commit damage to tool
        if (damage >= maxDamage) {
            tool.setAmount(tool.getAmount() - 1); //break the tool if it is broken
        }
        else {
            damageableMeta.setDamage(damage);
            tool.setItemMeta((ItemMeta) damageableMeta);
        }

        //Apply exhaustion/hunger cost to player
        player.setExhaustion(player.getExhaustion() + (count * BLOCK_BREAK_EXHAUSTION * this.exhaustionMultiplier));
    }

    /**
     * Gets the proper index for the offset look-up table (this.planeOffsets)
     * @param face block face that the player is targeting
     * @return
     */
    private int getPlaneIndex(BlockFace face) {
        switch (face) {
            case NORTH:
            case SOUTH:
                return NORTH_SOUTH;

            case EAST:
            case WEST:
                return EAST_WEST;

            case UP:
            case DOWN:
                return UP_DOWN;

            default:
                return -1;
        }
    }
}
