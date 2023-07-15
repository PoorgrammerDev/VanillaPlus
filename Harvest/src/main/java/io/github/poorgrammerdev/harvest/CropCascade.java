package io.github.poorgrammerdev.harvest;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * QuickReplace in a large area when used with a hoe.
 * Seed is to be held in the offhand
 */
public class CropCascade extends AbstractModule {
    private static final int DEFAULT_RADIUS = 1;
    private static final int MINIMUM_RADIUS = 1;

    private final CropSeedMapper cropSeedMapper;
    private final QuickReplant cropReplacer;
    private final HashMap<Material, Integer> radiusMap; 
    private final BlockFace[] relative;
    private final boolean preserveHoe;
    private final boolean seedCancel;

    public CropCascade(final Harvest plugin, final CropSeedMapper cropSeedMapper, final QuickReplant cropReplacer) {
        super(plugin);
        this.cropSeedMapper = cropSeedMapper;
        this.cropReplacer = cropReplacer;

        this.relative = new BlockFace[]{
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.SOUTH,
        };

        //Fetch options from config
        this.preserveHoe = plugin.getConfig().getBoolean("cascade_preserve_hoe", true);
        this.seedCancel = plugin.getConfig().getBoolean("cascade_seed_cancel", true);

        this.radiusMap = new HashMap<>();
        for (final Material hoe : this.cropSeedMapper.getHoes()) {
            final String path = "cascade_radii." + hoe.name().toLowerCase();

            this.radiusMap.put(
                hoe,
                //Radius must be at least threshold. If not found, use default
                Math.max(plugin.getConfig().getInt(path, DEFAULT_RADIUS), MINIMUM_RADIUS)
            );
        }
    }

    @Override
    public boolean register() {
        return super.register("modules.crop_cascade");
    }

    /**
     * Handles right click interaction and performs floodfill.
     */
    @EventHandler(ignoreCancelled = true)
    public void cropInteract(PlayerInteractEvent event) {
        //Has to be a right click and cannot be block placement
        if (
            event.getAction() != Action.RIGHT_CLICK_BLOCK ||
            event.getHand() != EquipmentSlot.HAND ||
            event.isBlockInHand()
        ) return;

        //Players can disable this behaviour by sneaking
        final Player player = event.getPlayer();
        if (player.isSneaking()) return;

        //Target block must be a crop
        final Block center = event.getClickedBlock();
        final Material centerMat = center.getType();
        if (!this.cropSeedMapper.isCrop(centerMat)) return;

        //Tool must be a hoe
        final ItemStack hoe = event.getItem();
        if (
            hoe == null ||
            !this.radiusMap.containsKey(hoe.getType()) ||
            !(hoe.getItemMeta() instanceof Damageable)
        ) return;

        //Durability
        final Damageable hoeMeta = (Damageable) hoe.getItemMeta();
        final int maxDamage = hoe.getType().getMaxDurability() + (this.preserveHoe ? -1 : 0); //subtracting 1 limits lowest durability to 1 instead of 0
        final int unbreaking = ((ItemMeta) hoeMeta).getEnchantLevel(Enchantment.DURABILITY);

        //Use the item in the offhand as the seed/empty item
        final EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return;

        final ItemStack offhand = equipment.getItemInOffHand();
        final boolean isSeed = offhand != null && this.cropSeedMapper.isSeed(offhand.getType());

        //Do a BFS floodfill within range constraints
        final Queue<Block> queue = new ArrayDeque<>();
        final Random random = new Random();
        final int maxLayer = this.radiusMap.get(hoe.getType());
        int layer = 0;
        int innerLayerCount = 1;

        queue.add(center);
        while (
            !queue.isEmpty() && // still have neighbours
            layer < maxLayer && // haven't reached the range limit
            hoeMeta.getDamage() < maxDamage //tool hasn't broken
        ) {
            //Check the seed count if enabled
            if (this.seedCancel && isSeed && offhand.getAmount() <= 0) break;
            
            //replace the crop and if it is successful, enqueue all its neighbors
            final Block block = queue.remove();
            if (this.cropReplacer.attemptCropReplace(block, offhand)) {
                for (BlockFace face : this.relative) {
                    final Block neighbour = block.getRelative(face);
                    if (neighbour.getType() == centerMat && this.cropReplacer.isGrownCrop(neighbour)) {
                        queue.add(neighbour);
                    }
                }

                //Subtract durability with respect to unbreakable and unbreaking
                if (!((ItemMeta) hoeMeta).isUnbreakable() && (unbreaking == 0 || ((random.nextInt(100) + 1) <= (100 / (unbreaking + 1))))) {
                    hoeMeta.setDamage(hoeMeta.getDamage() + 1);
                }
            }

            innerLayerCount--;
            if (innerLayerCount <= 0) {
                layer++;
                innerLayerCount = queue.size();
            }
        }
           
        //If hoe is broken, break it
        if (hoeMeta.getDamage() >= hoe.getType().getMaxDurability()) {
            hoe.setAmount(hoe.getAmount() - 1);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        }
        //Otherwise apply damage to hoe
        else {
            hoe.setItemMeta((ItemMeta) hoeMeta);
        }

    }
}
