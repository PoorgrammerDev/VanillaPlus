package io.github.poorgrammerdev.ominouswither.mechanics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Difficulty;
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
import org.bukkit.loot.LootTables;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherActivateEvent;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherLoadEvent;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherSpawnEvent;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherUnloadEvent;
import io.github.poorgrammerdev.ominouswither.utils.ItemBuilder;
import io.github.poorgrammerdev.ominouswither.utils.ParticleInfo;
import io.github.poorgrammerdev.ominouswither.utils.Utils;
import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.coroutines.EntityStare;
import io.github.poorgrammerdev.ominouswither.coroutines.PassableLocationFinder;
import io.github.poorgrammerdev.ominouswither.coroutines.PersistentParticle;
import io.github.poorgrammerdev.ominouswither.internal.ICoroutine;
import io.github.poorgrammerdev.ominouswither.internal.config.BossStat;

/**
 * This handles all of the mechanics that happen as the Ominous Wither is spawning.
 * For example, its stat buffs and name, spawning minion Wither Skeletons, etc.
 * @author Thomas Tran
 */
public class SpawnMechanics implements Listener {
    private final OminousWither plugin;

    /**
     * Config setting for if spawning an Ominous Wither should remove the Bad Omen effect from players in Creative Mode
     */
    private final boolean creativeRemoveOmen;

    /**
     * Holds the locations of minions to be summoned once the Wither is fully spawned.
     */
    private final HashMap<UUID, List<Location>> spawnMinionMap;

    public SpawnMechanics(final OminousWither plugin) {
        this.plugin = plugin;
        this.creativeRemoveOmen = plugin.getConfig().getBoolean("creative_remove_omen", true);

        this.spawnMinionMap = new HashMap<>();
    }

    @EventHandler(ignoreCancelled = true)
    private void onOminousSpawn(final OminousWitherSpawnEvent event) {
        final Wither wither = event.getWither();
        final Player player = event.getSpawner();
        final World world = wither.getWorld();
        final int level = event.getLevel();
        final Difficulty difficulty = world.getDifficulty();

        //Remove the Bad Omen effect from the Player
        if (this.creativeRemoveOmen || Utils.isTargetable(player)) player.removePotionEffect(PotionEffectType.BAD_OMEN);

        //Modify the Wither's stats
        final String levelRoman = Utils.getLevelRomanNumeral(level);
        final String witherName = Utils.WITHER_NAME_COLOR + "Ominous Wither" + (levelRoman != null ? (" " + levelRoman) : "");

        wither.setCustomName(witherName);
        wither.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(this.plugin.getBossStatsManager().getStat(BossStat.BOSS_MAX_HEALTH, level, difficulty));
        wither.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(this.plugin.getBossStatsManager().getStat(BossStat.FIRST_PHASE_ARMOR, level, difficulty));
        wither.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).setBaseValue(this.plugin.getBossStatsManager().getStat(BossStat.FIRST_PHASE_ARMOR_TOUGHNESS, level, difficulty));
        wither.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(1024);

        //Wither looks at its spawner while spawning
        this.performOminousStare(wither.getUniqueId(), player.getUniqueId());

        //Get locations for spawning minions
        this.populateMinionSpawnLocations(wither);
    }

    @EventHandler(ignoreCancelled = true)
    private void onUnload(final OminousWitherUnloadEvent event) {
        //Remove wither from map
        this.spawnMinionMap.remove(event.getWither().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    private void onLoad(final OminousWitherLoadEvent event) {
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
    private void onFullySpawned(final OminousWitherActivateEvent event) {
        final Wither wither = event.getWither();

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
        this.plugin.getCoroutineManager().enqueue(new EntityStare(
            this.plugin,
            witherID,
            playerID,
            true,
            (starerID, targetID) -> {return true;},
            (starerID, targetID) -> {return !spawnMinionMap.containsKey(starerID);}            
        ));
    }

    /**
     * Finds suitable locations to spawn minions and records them to spawn later
     * Also displays a particle effect before they are spawned in
     * @param wither Ominous Wither boss
     */
    private void populateMinionSpawnLocations(final Wither wither) {
        final UUID witherUUID = wither.getUniqueId();
        this.spawnMinionMap.put(witherUUID, new ArrayList<>());

        final double spawnRange = this.plugin.getBossStatsManager().getStat(BossStat.MINION_SPAWN_RANGE, wither);

        //Find locations to summon minions
        this.plugin.getCoroutineManager().enqueue(new PassableLocationFinder(
            wither.getEyeLocation(),
            new Vector(spawnRange, spawnRange, spawnRange),
            3,
            true,
            true,
            (int) this.plugin.getBossStatsManager().getStat(BossStat.MINION_AMOUNT, wither),
            (location) -> {
                //Adds them to a list under the Wither's ID so they can be summoned once the wither is fully spawned
                final List<Location> list = this.spawnMinionMap.getOrDefault(wither.getUniqueId(), null);
                
                //If for some reason the list is null (e.g. this took too long and the Wither has already fully spawned)
                //Then just ignore all future locations
                if (list == null) return; 

                list.add(location);

                //Play visual effect at future spawn place
                this.plugin.getCoroutineManager().enqueue(new PersistentParticle(
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

        //Track the Wither to make sure it's still alive and loaded
        //If not, remove it from the map (this will prevent a memory leak and also clear the smoke particles)
        //The only known instance of this being an issue is when the difficulty is set to Peaceful during Wither spawn
        this.plugin.getCoroutineManager().enqueue(new ICoroutine() {

            @Override
            public boolean tick() {
                final Entity entity = plugin.getServer().getEntity(witherUUID);
                
                //Entity has been removed or unloaded
                if (!(entity instanceof Wither) || entity.isDead() || !entity.isInWorld()) {
                    spawnMinionMap.remove(witherUUID);
                    return false;
                }
                
                //Wither has fully spawned in, no longer need this task
                final Wither wither = (Wither) entity;
                if (wither.getPersistentDataContainer().getOrDefault(plugin.getIsFullySpawnedKey(), PersistentDataType.BOOLEAN, false)) return false;

               
                //Otherwise continue
                return true;
            }
            
        });
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
        final int level = this.plugin.getLevel(wither, -1);
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

        final Difficulty difficulty = world.getDifficulty();
        for (Location location : minionLocations) {
            //Make spawn location face the player who spawned the Wither
            location = location.setDirection(spawner.getLocation().subtract(location).toVector());

            //Play a flash particle
            world.spawnParticle(Particle.FLASH, location, 1);

            final Entity entity = world.spawnEntity(location, EntityType.WITHER_SKELETON);
            if (!(entity instanceof WitherSkeleton)) continue;

            final WitherSkeleton minion = (WitherSkeleton) entity;
            minion.setPersistent(true);
            minion.getPersistentDataContainer().set(this.plugin.getMinionKey(), PersistentDataType.BOOLEAN, true);
            minion.setLootTable(LootTables.EMPTY.getLootTable());
            minion.setCanPickupItems(false);
            minion.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(this.plugin.getBossStatsManager().getStat(BossStat.MINION_ARMOR, level, difficulty));
            minion.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).setBaseValue(this.plugin.getBossStatsManager().getStat(BossStat.MINION_ARMOR_TOUGHNESS, level, difficulty));
            minion.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(this.plugin.getBossStatsManager().getStat(BossStat.MINION_MOVEMENT_SPEED, level, difficulty));
            minion.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(1024);

            //Targets the spawner if possible
            if (Utils.isTargetable(spawner)) {
                minion.setTarget(spawner);
            }

            final EntityEquipment equipment = minion.getEquipment();
            if (equipment == null) continue;

            equipment.setItemInMainHandDropChance(-32768);
            final ItemStack weapon =
                new ItemBuilder(Material.NETHERITE_SWORD)
                    .addEnchant(Enchantment.SHARPNESS, (int) this.plugin.getBossStatsManager().getStat(BossStat.MINION_SWORD_SHARPNESS, level, difficulty), true)
                    .addEnchant(Enchantment.FIRE_ASPECT, 3, false)
                .build()
            ;

            equipment.setItemInMainHand(weapon);

            if (level == 5) {
                equipment.setItemInOffHandDropChance(-32768);
                equipment.setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));
            }
        }
    }


}
