package io.github.poorgrammerdev.ominouswither;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.backend.CoroutineManager;
import io.github.poorgrammerdev.ominouswither.backend.ICoroutine;
import io.github.poorgrammerdev.ominouswither.backend.OminousWitherActivateEvent;
import io.github.poorgrammerdev.ominouswither.backend.OminousWitherLoadEvent;
import io.github.poorgrammerdev.ominouswither.backend.OminousWitherSpawnEvent;
import io.github.poorgrammerdev.ominouswither.backend.OminousWitherUnloadEvent;
import io.github.poorgrammerdev.ominouswither.coroutines.PassableLocationFinder;
import io.github.poorgrammerdev.ominouswither.coroutines.PersistentParticle;
import net.md_5.bungee.api.ChatColor;

/**
 * This handles all of the mechanics that happen as the Ominous Wither is spawning.
 * For example, its stat buffs and name, spawning minion Wither Skeletons, etc.
 * @author Thomas Tran
 */
public class SpawnMechanics implements Listener {
    private final OminousWither plugin;

    /**
     * Holds the locations of minions to be summoned once the Wither is fully spawned.
     */
    private final HashMap<UUID, List<Location>> spawnMinionMap;

    public SpawnMechanics(final OminousWither plugin) {
        this.plugin = plugin;
        this.spawnMinionMap = new HashMap<>();
    }

    @EventHandler(ignoreCancelled = true)
    public void onOminousSpawn(final OminousWitherSpawnEvent event) {
        final Wither wither = event.getWither();
        final Player player = event.getSpawner();
        final World world = wither.getWorld();
        final int level = event.getLevel();

        //Remove the Bad Omen effect from the Player
        player.removePotionEffect(PotionEffectType.BAD_OMEN);

        //Modify the Wither's stats
        wither.getPersistentDataContainer().set(this.plugin.getOminousWitherKey(), PersistentDataType.BOOLEAN, true);
        wither.getPersistentDataContainer().set(this.plugin.getLevelKey(), PersistentDataType.INTEGER, level);
        wither.getPersistentDataContainer().set(this.plugin.getSpawnerKey(), PersistentDataType.STRING, player.getUniqueId().toString());
        wither.setCustomName(ChatColor.DARK_PURPLE + "Ominous Wither");
        wither.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(calculateMaxHealth(world.getDifficulty()));
        wither.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).setBaseValue(level * 2);
        wither.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(1024);

        //Wither looks at its spawner while spawning
        this.performOminousStare(wither.getUniqueId(), player.getUniqueId());

        //Get locations for spawning minions
        this.populateMinionSpawnLocations(wither);
    }

    @EventHandler(ignoreCancelled = true)
    public void onUnload(final OminousWitherUnloadEvent event) {
        //Remove wither from map
        this.spawnMinionMap.remove(event.getWither().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onLoad(final OminousWitherLoadEvent event) {
        //Check if the Wither has not been fully spawned in yet
        final Wither wither = event.getWither();
        if (wither == null || wither.getPersistentDataContainer().getOrDefault(this.plugin.getIsFullySpawnedKey(), PersistentDataType.BOOLEAN, false)) return;

        //Minion spawning process
        this.populateMinionSpawnLocations(wither);
        
        //Stare
        final String spawnerIDString = wither.getPersistentDataContainer().getOrDefault(this.plugin.getSpawnerKey(), PersistentDataType.STRING, null);
        if (spawnerIDString == null) return;

        this.performOminousStare(wither.getUniqueId(), UUID.fromString(spawnerIDString));
    }

    /**
     * Handles events that occur when the Ominous Wither is fully spawned
     */
    @EventHandler(ignoreCancelled = true)
    public void onFullySpawned(final OminousWitherActivateEvent event) {
        final Wither wither = event.getWither();

        //Mark as fully spawned
        wither.getPersistentDataContainer().set(this.plugin.getIsFullySpawnedKey(), PersistentDataType.BOOLEAN, true);

        //Set Wither's health to its Max Health
        //(This could not be done at spawn-time since it seems to be hardcoded in MC to set to 300 at explode-time)
        wither.setHealth(wither.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

        //Summon minions
        this.spawnMinions(wither);
    }

    /**
     * Makes the Wither look at its spawner while spawning in
     * @param witherID Ominous Wither boss's UUID
     * @param playerID UUID of the player that spawned the wither
     */
    private void performOminousStare(final UUID witherID, final UUID playerID) {
        CoroutineManager.getInstance().enqueue(new ICoroutine() {

            @Override
            public void tick() {
                final Entity wither = plugin.getServer().getEntity(witherID);
                if (wither == null) return;

                final Player spawner = plugin.getServer().getPlayer(playerID);
                if (spawner == null) return;

                final Vector direction = spawner.getLocation().subtract(wither.getLocation()).toVector();
                wither.teleport(wither.getLocation().setDirection(direction));
            }

            @Override
            public boolean shouldBeRescheduled() {
                //Stop staring when the wither is fully spawned in
                return spawnMinionMap.containsKey(witherID);
            }
            
        });
    }

    /**
     * Finds suitable locations to spawn minions and records them to spawn later
     * Also displays a particle effect before they are spawned in
     * @param wither Ominous Wither boss
     */
    private void populateMinionSpawnLocations(final Wither wither) {
        final UUID witherUUID = wither.getUniqueId();
        this.spawnMinionMap.put(witherUUID, new ArrayList<>());

        //Find locations to summon minions
        CoroutineManager.getInstance().enqueue(new PassableLocationFinder(
            wither.getLocation(),
            new Vector(10, 10, 10),
            3,
            true,
            true,
            10,
            (location) -> {
                //Adds them to a list under the Wither's ID so they can be summoned once the wither is fully spawned
                final List<Location> list = this.spawnMinionMap.getOrDefault(wither.getUniqueId(), null);
                
                //If for some reason the list is null (e.g. this took too long and the Wither has already fully spawned)
                //Then just ignore all future locations
                if (list == null) return; 

                list.add(location);

                //Play visual effect at future spawn place
                CoroutineManager.getInstance().enqueue(new PersistentParticle(
                    () -> (!this.spawnMinionMap.containsKey(witherUUID)),
                    location.clone().add(0, 1, 0),
                    new ParticleInfo(
                        Particle.SMOKE,
                        5,
                        0.25,
                        1.5,
                        0.25,
                        0,
                        null
                    )
                ));
            },
            null
        ));

    }

    /**
     * Spawn minions at the recorded locations for this Wither boss
     * @param wither
     */
    private void spawnMinions(final Wither wither) {
        final UUID witherID = wither.getUniqueId();
        final List<Location> minionLocations = this.spawnMinionMap.getOrDefault(witherID, null);
        if (minionLocations == null) return;
        
        //Regardless of what happens next, clear this wither from the list as it's been handled
        this.spawnMinionMap.remove(witherID);

        final World world = wither.getWorld();
        if (world == null) return;

        //Get level of Wither; if invalid then failed -> clear locations and return
        final int level = wither.getPersistentDataContainer().getOrDefault(this.plugin.getLevelKey(), PersistentDataType.INTEGER, -1);
        if (level == -1) return;

        //Get player who spawned Wither
        final String playerIDString = wither.getPersistentDataContainer().getOrDefault(this.plugin.getSpawnerKey(), PersistentDataType.STRING, null);
        if (playerIDString == null) return;

        final Player spawner;
        try {
            final UUID playerID = UUID.fromString(playerIDString);
            spawner = this.plugin.getServer().getPlayer(playerID);
        }
        catch (IllegalArgumentException e) {
            return;
        }

        if (spawner == null) return;

        for (Location location : minionLocations) {
            //Make spawn location face the player who spawned the Wither
            location = location.setDirection(spawner.getLocation().subtract(location).toVector());

            //Play a flash particle
            world.spawnParticle(Particle.FLASH, location, 1);

            final Entity entity = world.spawnEntity(location, EntityType.WITHER_SKELETON);
            if (!(entity instanceof WitherSkeleton)) continue;

            final WitherSkeleton minion = (WitherSkeleton) entity;
            minion.getPersistentDataContainer().set(this.plugin.getMinionKey(), PersistentDataType.BOOLEAN, true);
            minion.setLootTable(null);
            minion.setCanPickupItems(false);
            minion.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(level * 2.0);
            minion.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).setBaseValue(level);
            minion.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.375);
            minion.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(128);

            //Targets the spawner if in survival or adventure mode
            if (spawner.getGameMode() == GameMode.SURVIVAL || spawner.getGameMode() == GameMode.ADVENTURE) {
                minion.setTarget(spawner);
            }

            final EntityEquipment equipment = minion.getEquipment();
            if (equipment == null) continue;

            equipment.setItemInMainHandDropChance(-32768);
            final ItemStack weapon = new ItemStack(Material.NETHERITE_SWORD);
            weapon.addEnchantment(Enchantment.SHARPNESS, level);
            weapon.addEnchantment(Enchantment.FIRE_ASPECT, 2);
            equipment.setItemInMainHand(weapon);

            if (level == 5) {
                equipment.setItemInOffHandDropChance(-32768);
                equipment.setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));
            }
        }
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
