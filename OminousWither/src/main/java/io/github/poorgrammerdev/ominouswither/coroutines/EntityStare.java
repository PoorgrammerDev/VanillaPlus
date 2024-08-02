package io.github.poorgrammerdev.ominouswither.coroutines;

import java.util.UUID;
import java.util.function.BiPredicate;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.ICoroutine;

public class EntityStare implements ICoroutine {
    private final OminousWither plugin;
    private final UUID starerID;
    private final UUID targetID;
    private final boolean isTargetPlayer;
    private final BiPredicate<UUID, UUID> stareCondition;
    private final BiPredicate<UUID, UUID> stopCondition;

    /**
     * Convenience constructor
     * @param plugin instance of main plugin class
     * @param starer entity that will be staring
     * @param target entity that will be stared at
     * @param stareCondition when to stare (e.g. "will only stare when target is not sneaking") [1st UUID is the starer, 2nd UUID is the target]
     * @param stopCondition when to cancel the coroutine [1st UUID is the starer, 2nd UUID is the target]
     */
    public EntityStare(OminousWither plugin, Entity starer, Entity target, BiPredicate<UUID, UUID> stareCondition,
            BiPredicate<UUID, UUID> stopCondition) {
        this.plugin = plugin;
        this.starerID = starer.getUniqueId();
        this.targetID = target.getUniqueId();
        this.isTargetPlayer = (target instanceof Player);
        this.stareCondition = stareCondition;
        this.stopCondition = stopCondition;
    }


    /**
     * Direct constructor, user must specify if target is a player
     * @param plugin instance of main plugin class
     * @param starerID UUID of staring entity
     * @param targetID UUID of target entity (entity to be stared at)
     * @param isTargetPlayer is the target entity a player?
     * @param stareCondition when to stare (e.g. "will only stare when target is not sneaking") [1st UUID is the starer, 2nd UUID is the target]
     * @param stopCondition when to cancel the coroutine [1st UUID is the starer, 2nd UUID is the target]
     */
    public EntityStare(OminousWither plugin, UUID starerID, UUID targetID, boolean isTargetPlayer,
            BiPredicate<UUID, UUID> stareCondition, BiPredicate<UUID, UUID> stopCondition) {
        this.plugin = plugin;
        this.starerID = starerID;
        this.targetID = targetID;
        this.isTargetPlayer = isTargetPlayer;
        this.stareCondition = stareCondition;
        this.stopCondition = stopCondition;
    }

    @Override
    public boolean tick() {
        //If starer can't be found, cancel coroutine
        final Entity starer = plugin.getServer().getEntity(this.starerID);
        if (starer == null) return false;

        //If target can't be found, cancel coroutine
        final Entity target = this.isTargetPlayer ? plugin.getServer().getPlayer(targetID) : plugin.getServer().getEntity(targetID);
        if (target == null) return false;

        //Must be in the same world
        final World starerWorld = starer.getWorld();
        final World targetWorld = target.getWorld();
        if (starerWorld != null && targetWorld != null && starerWorld.equals(targetWorld)) {
            //Must meet stare condition to stare
            if (this.stareCondition.test(this.starerID, this.targetID)) {
                //Look at target
                final Vector direction = target.getLocation().subtract(starer.getLocation()).toVector();
                final Location location = starer.getLocation().setDirection(direction);
                starer.setRotation(location.getYaw(), location.getPitch());
            }
        }

        //Test for stop condition
        return !this.stopCondition.test(this.starerID, this.targetID);
    }
    
}
