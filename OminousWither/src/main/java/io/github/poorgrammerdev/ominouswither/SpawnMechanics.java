package io.github.poorgrammerdev.ominouswither;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.backend.CoroutineManager;
import io.github.poorgrammerdev.ominouswither.backend.OminousWitherSpawnEvent;
import net.md_5.bungee.api.ChatColor;

/**
 * This handles all of the mechanics that happen right as the Ominous Wither is spawned.
 * For example, the stat buffs, its name, spawning minion Wither Skeletons, etc.
 */
public class SpawnMechanics implements Listener {
    private final OminousWither plugin;
    private final HashMap<UUID, List<Location>> spawnMinionMap;

    public SpawnMechanics(final OminousWither plugin) {
        this.plugin = plugin;
        this.spawnMinionMap = new HashMap<>();
    }

    @EventHandler
    public void onOminousSpawn(final OminousWitherSpawnEvent event) {
        final Wither wither = event.getWither();
        final Player player = event.getSpawner();
        final World world = wither.getWorld();
        final int level = event.getLevel();

        //Remove the Bad Omen effect from the Player
        player.removePotionEffect(PotionEffectType.BAD_OMEN);

        //Modify the Wither's stats
        wither.getPersistentDataContainer().set(this.plugin.getOminousWitherKey(), PersistentDataType.BOOLEAN, true);
        wither.setCustomName(ChatColor.DARK_PURPLE + "Ominous Wither");
        wither.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(calculateMaxHealth(world.getDifficulty()));
        // wither.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(level * 2);
        wither.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).setBaseValue(level * 2);
        wither.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(1024);

        //Handle finding locations to summon minions
        //Adds them to a list under the Wither's ID so they can be summoned later
        this.spawnMinionMap.put(wither.getUniqueId(), new ArrayList<>());
        CoroutineManager.getInstance().enqueue(new PassableLocationFinder(
            wither.getLocation(),
            new Vector(10, 10, 10),
            3,
            true,
            true,
            20,
            ((location) -> {
                final List<Location> l = this.spawnMinionMap.get(wither.getUniqueId());
                l.add(location);

                CoroutineManager.getInstance().enqueue(new PersistentParticle(() -> {return false;}, location.clone().add(0, 1, 0), new ParticleInfo(Particle.SMOKE, 5, 0.25, 1.5, 0.25, 0, null)));

                //TODO: DO SOMETHING WITH THIS
            }),
            null
        ));

        //Constant ominous particle
        CoroutineManager.getInstance().enqueue(new PersistentTrackingParticle(
            this.plugin,
            ((entityID) -> {
                final Entity entity = plugin.getServer().getEntity(entityID);
                if (entity == null || !(entity instanceof LivingEntity)) return false;

                final LivingEntity livingEntity = (LivingEntity) entity;
                return livingEntity.isDead();
            }),
            wither.getUniqueId(),
            new ParticleInfo(Particle.TRIAL_OMEN, 2, 1, 1, 1, 0, null)
        ));
        CoroutineManager.getInstance().enqueue(new PersistentTrackingParticle(
            this.plugin,
            ((entityID) -> {
                final Entity entity = plugin.getServer().getEntity(entityID);
                if (entity == null || !(entity instanceof LivingEntity)) return false;

                final LivingEntity livingEntity = (LivingEntity) entity;
                return livingEntity.isDead();
            }),
            wither.getUniqueId(),
            new ParticleInfo(Particle.RAID_OMEN, 2, 1, 1, 1, 0, null)
        ));
    }

    /**
     * Scaling HP with difficulty for Bedrock Parity (as of 1.21 or 2024)
     * @param difficulty world difficulty
     * @return Max HP to use for Wither
     */
    private double calculateMaxHealth(final Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return 300.0;
            case NORMAL:
                return 450.0;
            case HARD:
                return 600.0;
            default:
                return 300.0;
        }
    }
    
}
