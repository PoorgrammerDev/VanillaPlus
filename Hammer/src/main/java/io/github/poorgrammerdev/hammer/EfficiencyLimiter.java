package io.github.poorgrammerdev.hammer;

import java.time.Duration;
import java.util.Map;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatColor;

/**
 * Toggleable system that limits level of Efficiency that a Hammer can have
 * @author Thomas Tran
 */
public class EfficiencyLimiter implements Listener {
    private final Hammer plugin;
    private final CooldownManager messageCooldownManager;

    private final boolean enabled;
    private final int maxLevel;
    private final boolean sendMessageToPlayers;
    private final int messageCooldownMinutes;

    public EfficiencyLimiter(final Hammer plugin) {
        this.plugin = plugin;
        this.messageCooldownManager = new CooldownManager();

        this.enabled = plugin.getConfig().getBoolean("efficiency_limiter.enabled", false);
        this.maxLevel = plugin.getConfig().getInt("efficiency_limiter.max_level", 3);
        this.sendMessageToPlayers = plugin.getConfig().getBoolean("efficiency_limiter.send_info_message_to_players", true);
        this.messageCooldownMinutes = plugin.getConfig().getInt("efficiency_limiter.message_cooldown_minutes", 10);
    }

    /**
     * Implements limit visually for Enchanting Table GUI offers
     */
    @EventHandler(ignoreCancelled = true)
    private void onViewEnchants(final PrepareItemEnchantEvent event) {
        if (!this.enabled) return;

        //Item must exist and be a hammer
        final ItemStack item = event.getItem();
        if (!this.plugin.isHammer(item)) return;
        
        //For any Efficiency enchantment offerring
        boolean madeAnyChanges = false;
        for (final EnchantmentOffer offer : event.getOffers()) {
            if (offer.getEnchantment() != Enchantment.DIG_SPEED) continue;

            //Limit level to the max level
            if (offer.getEnchantmentLevel() > this.maxLevel) {
                offer.setEnchantmentLevel(this.maxLevel);
                madeAnyChanges = true;
            }
        }
        
        //Send message if setting enabled and item was changed at all
        final Player player = event.getEnchanter();
        if (madeAnyChanges && this.sendMessageToPlayers && !this.messageCooldownManager.isOnCooldown(player)) {
            player.sendMessage(ChatColor.GRAY + "[INFO] The max Efficiency level on a Hammer has been set to " + this.maxLevel + ".");
            this.messageCooldownManager.setCooldown(player, Duration.ofMinutes(this.messageCooldownMinutes));
        }
    }

    /**
     * Implements end result for enchanting with Enchanting Tables
     */
    @EventHandler(ignoreCancelled = true)
    private void onTableEnchant(final EnchantItemEvent event) {
        if (!this.enabled) return;

        //Item must exist and be a hammer
        final ItemStack item = event.getItem();
        if (!this.plugin.isHammer(item)) return;

        final Map<Enchantment, Integer> enchants = event.getEnchantsToAdd();

        final Integer level = enchants.getOrDefault(Enchantment.DIG_SPEED, null);
        if (level == null || level <= this.maxLevel) return;

        enchants.put(Enchantment.DIG_SPEED, this.maxLevel);
    }

    /**
     * Implements limit for enchanting with Anvils
     */
    @EventHandler(ignoreCancelled = true)
    private void onAnvilEnchant(final PrepareAnvilEvent event) {
        if (!this.enabled) return;

        //Item must exist and be a hammer
        final ItemStack result = event.getResult();
        if (!this.plugin.isHammer(result)) return;

        //Limit level to the max level
        if (result.getEnchantmentLevel(Enchantment.DIG_SPEED) <= this.maxLevel) return;
        result.removeEnchantment(Enchantment.DIG_SPEED);
        result.addEnchantment(Enchantment.DIG_SPEED, this.maxLevel);

        //Send message if setting enabled
        if (this.sendMessageToPlayers) {
            for (final HumanEntity player : event.getViewers()) {
                if (!this.messageCooldownManager.isOnCooldown(player.getUniqueId())) {
                    player.sendMessage(ChatColor.GRAY + "[INFO] The max Efficiency level on a Hammer has been set to " + this.maxLevel + ".");
                    this.messageCooldownManager.setCooldown(player.getUniqueId(), Duration.ofMinutes(this.messageCooldownMinutes));
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onQuit(final PlayerQuitEvent event) {
        if (!this.enabled || !this.sendMessageToPlayers) return;

        this.messageCooldownManager.removeCooldown(event.getPlayer());
    }

    /**
     * Gets if this system as a whole is enabled or not
     */
    public boolean isEnabled() {
        return this.enabled;
    }
    
}

