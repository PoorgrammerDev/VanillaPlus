package io.github.poorgrammerdev.hammer;

import java.util.Random;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
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
    private final FauxBlockDamage fauxBlockDamage;

    private final Vector[][] planeOffsets;
    
    //Config Options
    private final double raytraceDistance;
    private final float hardnessBuffer;
    private final float exhaustionMultiplier;

    public HammerMechanism(final Hammer plugin, final Random random, final FauxBlockDamage fauxBlockDamage) {
        this.plugin = plugin;
        this.random = random;
        this.fauxBlockDamage = fauxBlockDamage;

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
        final boolean creativeMode = (player.getGameMode() == GameMode.CREATIVE);
        final Block targetBlock = event.getBlock();

        this.fauxBlockDamage.deactivate(player, targetBlock);
        if (player.isSneaking()) return;

        final EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return;

        //Check if the tool is a hammer
        final ItemStack tool = equipment.getItemInMainHand();
        if (tool == null || !this.plugin.isHammer(tool)) return;

        final ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable)) return;

        final Damageable damageableMeta = (Damageable) meta;

        // Center block must be pickaxe-mineable
        if (!canBlockActivateHammer(targetBlock, tool)) return;

        // Get plane of blocks to break in
        final int planeIndex = getPlaneIndex(player, targetBlock);
        if (planeIndex == -1) return;

        final Location middleLocation = targetBlock.getLocation();
        final float hardness = targetBlock.getType().getHardness() + this.hardnessBuffer;

        //More hammer information
        final int unbreaking = damageableMeta.getEnchantLevel(Enchantment.DURABILITY);
        final int maxDamage = tool.getType().getMaxDurability() - 1;
        int damage = damageableMeta.getDamage();
        
        //Loop through blocks in plane and break them if they can be broken
        int count = 0;
        for (final Vector offset : this.planeOffsets[planeIndex]) {
            //If tool is broken, stop area mining early
            if (damage >= maxDamage) break;

            //Get location of current block
            targetBlock.getLocation(middleLocation);
            final Block adjacent = middleLocation.add(offset).getBlock();

            if (isBlockHammerable(adjacent, tool, hardness)) {
                //Break block -- do not drop items if dug in creative mode
                adjacent.breakNaturally(!creativeMode ? tool : new ItemStack(Material.AIR));

                //Add damage to tool with resepct to Unbreaking enchantment
                //(Unbreaking calculation is derived from Minecraft Wiki as of 2023)
                if ((!meta.isUnbreakable() && !creativeMode) && (unbreaking == 0 || ((random.nextInt(100) + 1) <= (100 / (unbreaking + 1))))) {
                    ++damage;
                }
                
                ++count;
            }
        }

        //No need to continue if in creative mode -- the remaining parts only affect survival
        if (creativeMode) return;

        //Check if tool should be broken
        if (damage >= maxDamage) {
            //Play SFX & VFX for tool breaking
            final World world = player.getWorld();
            if (world != null) {
                world.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
                world.spawnParticle(Particle.ITEM_CRACK, player.getEyeLocation().subtract(0, 0.25, 0).add(player.getEyeLocation().getDirection().normalize().multiply(0.5f)), 15, 0.05, 0.01, 0.05, 0.1, tool);
            }

            //Break tool
            tool.setAmount(tool.getAmount() - 1);
        }
        //Otherwise commit damage to tool
        else {
            damageableMeta.setDamage(damage);
            tool.setItemMeta((ItemMeta) damageableMeta);
        }

        //Apply exhaustion/hunger cost to player
        player.setExhaustion(player.getExhaustion() + (count * BLOCK_BREAK_EXHAUSTION * this.exhaustionMultiplier));
    }

    /**
     * Shows block damage (cracking) effect on adjacent blocks while mining
     */
    @EventHandler(ignoreCancelled = true)
    public void adjacentBlockCracking(final BlockDamageEvent event) {
        if (event.getInstaBreak()) return;

        final Player player = event.getPlayer();
        if (player.isSneaking()) return;

        // Tool must be a hammer
        final ItemStack tool = event.getItemInHand();
        if (!this.plugin.isHammer(tool)) return;

        // Center block must be pickaxe-mineable
        final Block targetBlock = event.getBlock();
        if (!canBlockActivateHammer(targetBlock, tool)) return;

        // Get plane of blocks the player wants to break
        final int planeIndex = getPlaneIndex(player, targetBlock);
        if (planeIndex == -1) return;

        final Location location = targetBlock.getLocation();
        final float hardness = targetBlock.getType().getHardness() + this.hardnessBuffer;

        //Registers the player into the system and then updates the adjacent blocks
        final FauxDamageData data = this.fauxBlockDamage.register(player, targetBlock);
        if (data == null) return; //System is disabled (or something else went wrong)

        //TODO: This is most likely bad code architecture, find a way to clean this up without constant memory allocation
        int i = 0;
        for (final Vector offset : this.planeOffsets[planeIndex]) {
            targetBlock.getLocation(location);
            location.add(offset);

            //Calculate block location if hammerable
            if (isBlockHammerable(location.getBlock(), tool, hardness)) {
                if (data.adjacentBlocks[i] == null) {
                    data.adjacentBlocks[i] = location.clone();
                }
                else {
                    data.adjacentBlocks[i].setWorld(location.getWorld());
                    data.adjacentBlocks[i].setX(location.getX());
                    data.adjacentBlocks[i].setY(location.getY());
                    data.adjacentBlocks[i].setZ(location.getZ());
                }
                i++;
            }
        }

        data.adjacentCount = i;
    }

    /**
     * Stops displaying adjacent breaking effects to the player if they stop mining
     */
    @EventHandler(ignoreCancelled = true)
    public void stopDigging(BlockDamageAbortEvent event) {
        // No need to check if they are using a hammer here because
        // (1) they can swap off of it
        // (2) if they weren't using it they wouldn't have been registered in the first place so this would do nothing
        this.fauxBlockDamage.deactivate(event.getPlayer(), event.getBlock());
    }

    /**
     * Raycasts to find target block face and returns matching plane index
     * @param player player using the hammer
     * @param targetBlock center block the player is using the hammer on
     * @return plane index or -1 if failure
     */
    private int getPlaneIndex(final Player player, final Block targetBlock) {
        //Raycast out from the player to find the face of the block that they're targeting
        final RayTraceResult rayTraceResult = player.rayTraceBlocks(this.raytraceDistance);
        if (rayTraceResult == null || rayTraceResult.getHitBlock() == null || !rayTraceResult.getHitBlock().equals(targetBlock)) return -1;

        final BlockFace blockFace = rayTraceResult.getHitBlockFace();
        if (blockFace == null) return -1;

        //Get the plane of blocks to break
        switch (blockFace) {
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

    /**
     * Checks if an adjacent block can be broken by the hammer's area mining ability
     * @param block Block to check for viability
     * @param tool Hammer
     * @param centerHardness Hardness value to compare against (with the offset already added)
     * @return if the adjacent block can be broken by the hammer
     */
    private boolean isBlockHammerable(final Block block, final ItemStack tool, final float centerHardness) {
        return (
            canBlockActivateHammer(block, tool) && // Base requirements 
            block.getType().getHardness() <= centerHardness // Hardness value is within range of the center block
        );
    }

    /**
     * Checks if a center block can activate the hammer's ability
     * @param block Block to check for viability
     * @param tool Hammer
     * @return if the block can activate the hammer's breaking ability
     */
    private boolean canBlockActivateHammer(final Block block, final ItemStack tool) {
        return (
            Tag.MINEABLE_PICKAXE.isTagged(block.getType()) && // Pickaxe-minable block
            block.getBlockData().isPreferredTool(tool) && // This tier tool can mine it (e.g. stone tools cannot mine diamond ore)
            block.getType().getHardness() != Material.BEDROCK.getHardness() // Safety check - cannot be equal to bedrock's hardness
        );
    }

}
