package io.github.poorgrammerdev.hammer;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;

/**
 * Manages cooldowns for players
 * Referencing code from this wiki post: https://www.spigotmc.org/wiki/feature-command-cooldowns
 */
public class CooldownManager {
    private final HashMap<UUID, Instant> cooldownMap;

    public CooldownManager() {
        this.cooldownMap = new HashMap<>();
    }

    /**
     * Checks if a player is on cooldown with their UUID
     * @param playerID player id
     * @return true if player is still on cooldown and false if not
     */
    public boolean isOnCooldown(final UUID playerID) {
        final Instant cooldown = this.cooldownMap.getOrDefault(playerID, null);
        return (cooldown != null && Instant.now().isBefore(cooldown));
    }

    /**
     * Checks if a player is on cooldown
     * @param player player to check
     * @return true if player is still on cooldown and false if not
     */
    public boolean isOnCooldown(final Player player) {
        return this.isOnCooldown(player.getUniqueId());
    }

    /**
     * Sets a player on cooldown with their UUID
     * @param playerID player id
     * @param duration how long to set their cooldown for
     */
    public void setCooldown(final UUID playerID, final Duration duration) {
        this.cooldownMap.put(playerID, Instant.now().plus(duration));
    }

    /**
     * Sets a player on cooldown
     * @param player player to set
     * @param duration how long to set their cooldown for
     */
    public void setCooldown(final Player player, final Duration duration) {
        this.setCooldown(player.getUniqueId(), duration);
    }

    /**
     * Remove a player's remaining cooldown with their UUID
     * @param playerID player id
     * @return previously stored cooldown (the point in time when the cooldown would've ended), or null if there was no cooldown
     */
    public Instant removeCooldown(final UUID playerID) {
        return this.cooldownMap.remove(playerID);
    }

    /**
     * Remove a player's remaining cooldown
     * @param player player to remove
     * @return previously stored cooldown (the point in time when the cooldown would've ended), or null if there was no cooldown
     */
    public Instant removeCooldown(final Player player) {
        return this.removeCooldown(player.getUniqueId());
    }


    /**
     * Gets the remaining time left before the player can use this again
     * @param playerID player id
     * @return duration until cooldown is over
     */
    public Duration getRemainingCooldown(final UUID playerID) {
        final Instant cooldown = this.cooldownMap.getOrDefault(playerID, null);
        final Instant now = Instant.now();

        //Cooldown is present, return duration
        if (cooldown != null && now.isBefore(cooldown)) return Duration.between(now, cooldown);

        //No cooldown present or cooldown has passed
        return Duration.ZERO;
    }

    /**
     * Gets the remaining time left before the player can use this again
     * @param player player to check
     * @return duration until cooldown is over
     */
    public Duration getRemainingCooldown(final Player player) {
        return this.getRemainingCooldown(player.getUniqueId());
    }
    
}
