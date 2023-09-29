package io.github.poorgrammerdev.xpcontrol;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import io.github.poorgrammerdev.xpcontrol.external.Experience;
import net.md_5.bungee.api.ChatColor;

/**
 * Allows the player to store experience points into an item
 * @author Thomas Tran
 */
public class XPStorage extends AbstractModule {
    public static final String STORAGE_BOTTLE_NAME = ChatColor.RESET + "Experience Storage Bottle";

    private final NamespacedKey itemKey;
    private final NamespacedKey storedKey;

    private final int percentLoss;
    private final boolean keepVanillaXP;
    private final int customModelData;
    private final int maxExperience;

    public XPStorage(final XPControl plugin) {
        super(plugin);
        this.itemKey = new NamespacedKey(plugin, "is_storage_bottle");
        this.storedKey = new NamespacedKey(plugin, "stored_points");
        
        //Read percent loss and validate/clamp values
        int percentLoss = plugin.getConfig().getInt("xp_storage.percent_loss", 25);
        this.percentLoss = Math.min(Math.max(percentLoss, 0), 100);
        if (this.percentLoss != percentLoss) {
            plugin.getLogger().log(Level.WARNING, "XPStorage percent_loss value outside of bounds, using clamped value " + this.percentLoss + " instead!");
        }

        //Max EXP validation -- if any negative value is used then set to int limit instead
        int maxExperience = plugin.getConfig().getInt("xp_storage.bottle_max_exp", -1);
        this.maxExperience = (maxExperience >= 0 ? maxExperience : Integer.MAX_VALUE);

        //These config values don't need validation
        this.keepVanillaXP = plugin.getConfig().getBoolean("xp_storage.bottle_keep_vanilla_xp", true);
        this.customModelData = plugin.getConfig().getInt("xp_storage.storage_bottle_custom_model_data", 104);
    }

    @Override
    protected String getModuleConfigPath() {
        return "xp_storage.enabled";
    }

    /**
     * Handles the storing of experience points into a bottle
     */
    @EventHandler(ignoreCancelled = true)
    public void storeExperience (final PlayerInteractEvent event) {
        //Player must be right clicking
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        //Item must be in the main hand only -- do not register two clicks at once
        //TODO: Maybe expand this system to allow in either hand but somehow don't allow both in one go
        if (event.getHand() != EquipmentSlot.HAND) return;

        //Check if the player is sneaking -- overrides this action
        final Player player = event.getPlayer();
        if (player.isSneaking()) return;

        //Must be holding an experience bottle
        final ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.EXPERIENCE_BOTTLE) return;

        //Must be right clicking an enchanting table
        final Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENCHANTING_TABLE) return;

        //Prevent the enchanting table from opening or splashing the bottle
        event.setCancelled(true);

        //Check if the player has at least one level
        final int originalLevel = player.getLevel();
        if (originalLevel < 1) return;

        //Cannot have surpassed the limit already -- if so do nothing
        final int alreadyStored = (isStorageBottle(item) ? getStoredAmount(item) : 0);
        if (alreadyStored >= this.maxExperience) return;

        //Final calculation of how many EXP points to put into this bottle
        final long rawPoints = (long) (Experience.getExpToNext(originalLevel - 1) * ((100 - this.percentLoss) / 100.0D) + alreadyStored);
        final int points = (int) Math.min(rawPoints, (long) this.maxExperience);

        //Singular item -- edit this itemstack
        if (item.getAmount() == 1) {
            if (isStorageBottle(item)) {
                //Update the amount of stored experience
                setStoredAmount(item, points);
            }
            else {
                //Create the storage bottle from the item. If everything works continue as normal
                if (createStorageBottle(item, points) == null) return;
            }
        }
        
        //Stacked items -- subtract one and give the player a storage bottle
        else {
            item.setAmount(item.getAmount() - 1);

            final ItemStack storageBottle = createStorageBottle(points);

            //Add to the player's inventory or drop if full
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(storageBottle);
            }
            else {
                player.getWorld().dropItemNaturally(player.getLocation(), storageBottle);
            }
        }

        //Subtract a level from the player and play a sound effect
        player.setLevel(originalLevel - 1);
        player.getWorld().playSound(block.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }
    
    /**
     * Handles the usage of a storage bottle
     */
    @EventHandler(ignoreCancelled = true)
    public void useStorageBottle(final ExpBottleEvent event) {
        final ThrownExpBottle entity = event.getEntity();
        if (entity == null) return;

        //Makes sure we are dealing with a storage bottle
        final ItemStack item = entity.getItem();
        if (!isStorageBottle(item)) return;

        //Set the exp accordingly to the stored amount -- adding to the base amount of the config is set as such
        final long experiencePoints = ((long) getStoredAmount(item)) + (this.keepVanillaXP ? event.getExperience() : 0);

        event.setExperience((int) (Math.min(experiencePoints, (long) Integer.MAX_VALUE)));
    }

    /**
     * Checks if an item is an exp storage bottle 
     * @param item the item to check
     * @return if it is an exp storage bottle
     */
    public boolean isStorageBottle(@Nullable final ItemStack item) {
        if (item == null) return false;

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return meta.getPersistentDataContainer().getOrDefault(this.itemKey, PersistentDataType.BOOLEAN, false);
    }

    /**
     * Gets the stored amount of exp from a storage bottle
     * @param item EXP storage bottle
     * @return stored amount or 0 if invalid item
     */
    public int getStoredAmount(final ItemStack item) {
        if (item == null) return 0;

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;

        return (meta.getPersistentDataContainer().getOrDefault(this.storedKey, PersistentDataType.INTEGER, 0));
    }

    /**
     * Creates a new exp storage bottle item
     * @param storedAmount amount of exp points to store into it
     * @return EXP Storage Bottle item
     */
    public ItemStack createStorageBottle(final int storedAmount) {
        return new ItemBuilder(Material.EXPERIENCE_BOTTLE)
            .setName(STORAGE_BOTTLE_NAME)
            .setLore(getDescription(storedAmount))
            .setCustomModelData(this.customModelData)
            .setPersistentData(this.itemKey, PersistentDataType.BOOLEAN, true)
            .setPersistentData(this.storedKey, PersistentDataType.INTEGER, storedAmount)
            .build();
    }

    /**
     * Converts an existing experience bottle into an exp storage bottle item
     * @param baseItem An existing bottle o' enchanting item
     * @param storedAmount amount of exp points to store into it
     * @return the existing item (baseItem) if it worked, or null if something went wrong
     */
    public ItemStack createStorageBottle(final ItemStack baseItem, final int storedAmount) {
        final ItemMeta meta = baseItem.getItemMeta();
        if (meta == null) return null;
        
        //Update all the item properties
        meta.setDisplayName(STORAGE_BOTTLE_NAME);
        meta.setCustomModelData(this.customModelData);
        meta.getPersistentDataContainer().set(this.itemKey, PersistentDataType.BOOLEAN, true);
        baseItem.setItemMeta(meta);

        //Write saved xp points
        setStoredAmount(baseItem, storedAmount);
        return baseItem;
    }

    /**
     * Sets the stored amount of exp into a storage bottle and updates description accordingly
     * @param item EXP storage bottle
     * @param amount the amount to set as stored in the bottle
     */
    private void setStoredAmount(final ItemStack item, final int amount) {
        if (item == null) return;

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        //Set the internal value
        meta.getPersistentDataContainer().set(this.storedKey, PersistentDataType.INTEGER, amount);
        
        //Update the description
        final List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        final String desc = getDescription(amount);
        if (lore.size() < 1) {
            lore.add(desc);
        }
        else {
            lore.set(0, desc);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /**
     * Gets the description display string
     * @param storedAmount amount of exp being held in the bottle
     * @return description display string
     */
    private String getDescription(final int storedAmount) {
        return ChatColor.GREEN.toString() + storedAmount + ChatColor.GRAY + " Experience Points";
    }
    
}
