package io.github.poorgrammerdev.xpcontrol;


import java.util.logging.Level;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import io.github.poorgrammerdev.xpcontrol.external.Experience;
import net.md_5.bungee.api.ChatColor;

/**
 * Part of the DeathXPControl module. Manages the XPVoucher item, a workaround used for when EXP points exceed the integer limit.
 * @author Thomas Tran
 */
public class XPVoucher implements Listener {
    private final XPControl plugin;
    private final NamespacedKey itemKey;
    private final NamespacedKey storedXPKey;

    private final boolean allowWorkarounds;
    private final int customModelData;

    public XPVoucher(XPControl plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "is_xp_voucher");
        this.storedXPKey = new NamespacedKey(plugin, "stored_points");
        this.allowWorkarounds = this.plugin.getConfig().getBoolean("death_xp_control.allow_experimental_workarounds", false);
        this.customModelData = this.plugin.getConfig().getInt("death_xp_control.voucher_custom_model_data", 103);
    }

    @EventHandler(ignoreCancelled = true)
    public void onVoucherPickup(final EntityPickupItemEvent event) {
        //Item must be a voucher
        final ItemStack item = getVoucherOrNull(event.getItem());
        if (item == null) return;

        //No one is actually allowed to pick up this item
        event.setCancelled(true);
        
        //Check if this workaround is even allowed to be used. If not then ignore
        if (!this.allowWorkarounds) return;

        //This mechanic is only relevant to players
        if (!(event.getEntity() instanceof Player)) return;

        final Player player = (Player) event.getEntity();
        final double currentXP = Experience.getExp(player);

        //Convert to levels and apply to player
        final double storedXP = getStoredXP(item);
        final double levelWithProgress = Math.min(Experience.getLevelFromExp(currentXP + storedXP), (double) Integer.MAX_VALUE);

        //Sanity check
        if (levelWithProgress >= 0.0D) {
            final int levels = (int) levelWithProgress;
            final int progressXP = (int) ((levelWithProgress - levels) * Experience.getExpToNext(levels));

            player.setLevel(levels);
            player.giveExp(progressXP);
        }
        else {
            //Log severe error -- item will still be removed to avoid spamming the console. Stored EXP will be lost
            this.plugin.getLogger().log(Level.SEVERE, "XPVoucher pickup calculation yielded negative EXP value " + levelWithProgress);
        }

        //Remove the item from existence
        item.setAmount(item.getAmount() - 1);
        event.getItem().setItemStack(item);
    }

    /**
     * Prevent this item from being collected by hoppers
     */
    @EventHandler(ignoreCancelled = true)
    public void preventVoucherHopper(final InventoryPickupItemEvent event) {
        final ItemStack item = getVoucherOrNull(event.getItem());
        if (item != null) event.setCancelled(true);
    }

    /**
     * Prevent this item from being merged into another
     */
    @EventHandler(ignoreCancelled = true)
    public void preventVoucherMerge(final ItemMergeEvent event) {
        final ItemStack item1 = getVoucherOrNull(event.getEntity());
        final ItemStack item2 = getVoucherOrNull(event.getTarget());

        if (item1 != null || item2 != null) event.setCancelled(true);
    }

    /**
     * Creates an XP voucher that holds this amount of XP
     * @param exp the amount of experience to store
     * @return XP Voucher item
     */
    public ItemStack createVoucher(final double exp) {
        return new ItemBuilder(Material.EXPERIENCE_BOTTLE)
            .setName(ChatColor.RESET + "Experience Voucher")
            .setPersistentData(this.itemKey, PersistentDataType.BOOLEAN, true)
            .setPersistentData(this.storedXPKey, PersistentDataType.DOUBLE, exp)
            .setLore(ChatColor.RESET + "Holds experience to be exchanged instantly upon pickup.", ChatColor.RESET + "If you are reading this, something went wrong!")
            .setCustomModelData(this.customModelData)
            .addEnchant(Enchantment.DURABILITY, 1, true)
            .addItemFlags(ItemFlag.HIDE_ENCHANTS)
            .setRepairCost(Integer.MAX_VALUE)
            .build();
    }

    /**
     * Checks if an item is an XP Voucher or not
     * @param item item to check 
     * @return is an xp voucher
     */
    public boolean isXPVoucher(final ItemStack item) {
        if (item == null) return false;

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return (meta.getPersistentDataContainer().getOrDefault(this.itemKey, PersistentDataType.BOOLEAN, false));
    }

    /**
     * Retrieve stored total experience in the voucher
     * @param item XP Voucher
     * @return total experience stored
     */
    public double getStoredXP(final ItemStack item) {
        if (!isXPVoucher(item)) return 0.0;

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0.0;

        return (meta.getPersistentDataContainer().getOrDefault(this.storedXPKey, PersistentDataType.DOUBLE, 0.0));
    }

    /**
     * Made to prevent duplicating code. If the item entity's internal item is a voucher it will be returned. If not it is null.
     * @param itemEntity the item entity to check
     * @return Voucher item or null
     */
    private ItemStack getVoucherOrNull(@Nullable Item itemEntity) {
        if (itemEntity == null) return null;

        final ItemStack item = itemEntity.getItemStack();
        if (item == null) return null;

        return (isXPVoucher(item) ? item : null);
    }

}
