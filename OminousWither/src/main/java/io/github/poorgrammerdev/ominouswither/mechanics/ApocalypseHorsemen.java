package io.github.poorgrammerdev.ominouswither.mechanics;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.coroutines.PassableLocationFinder;
import io.github.poorgrammerdev.ominouswither.internal.CoroutineManager;
import io.github.poorgrammerdev.ominouswither.internal.config.BossStat;
import io.github.poorgrammerdev.ominouswither.utils.ItemBuilder;

/**
 * Handles the Skeletons and Skeleton Horses spawned by the Apocalpyse Skull
 * @author Thomas Tran
 */
public class ApocalypseHorsemen implements Listener {
    private final OminousWither plugin;

    /**
     * Maps arbitrary Group ID to set of Entity IDs
     */
    private final HashMap<UUID, HashSet<UUID>> groupMap;
    
    public ApocalypseHorsemen(OminousWither plugin) {
        this.plugin = plugin;
        this.groupMap = new HashMap<>();
    }

    /**
     * <p>Handles everything that happens after an Apocalypse Skull hits its target</p>
     * <p>Summons time-restricted skeleton horsemen that target the entity</p>
     * @param wither Wither that fired the Apocalypse Skull
     * @param target Entity that was hit by the skull
     */
    public void startApocalypse(final Wither wither, final LivingEntity target) {
        final World world = wither.getWorld();
        if (world == null) return;

        final Location center = wither.getLocation();
        final int level = this.plugin.getLevel(wither, 1);
        final Difficulty difficulty = world.getDifficulty();

        //Find viable locations and summon skeleton horsemen
        final UUID groupID = UUID.randomUUID();
        final UUID targetID = target.getUniqueId();
        CoroutineManager.getInstance().enqueue(new PassableLocationFinder(
            center,
            new Vector(5, 5, 5),
            4,
            true,
            true,
            (int) this.plugin.getBossStatsManager().getStat(BossStat.APOCALYPSE_SPAWN_AMOUNT, wither),
            (location) -> {this.spawnHorseman(groupID, location, targetID, level, difficulty);},
            (amount) -> {this.activateTimer(groupID, amount, (int) this.plugin.getBossStatsManager().getStat(BossStat.APOCALYPSE_HORSEMAN_LIFESPAN, level, difficulty));}
        ));
    }


    /**
     * Spawns a skeleton horseman at this location
     * @param groupID UUID denoting the group that this horseman belongs to. all members spawning from the same skull should have the same group id
     * @param location location to spawn at
     * @param target entity to target
     * @param level level of Ominous Wither
     * @param difficulty difficulty of world
     */
    private void spawnHorseman(final UUID groupID, final Location location, final UUID targetID, final int level, final Difficulty difficulty) {
        final World world = location.getWorld();
        if (world == null) return;

        //Spawn the entities
        final Entity entity1 = world.spawnEntity(location, EntityType.SKELETON_HORSE);
        final Entity entity2 = world.spawnEntity(location, EntityType.SKELETON);
        if (!(entity1 instanceof SkeletonHorse) || !(entity2 instanceof Skeleton)) {
            //If something went wrong, remove them and log
            entity1.remove();
            entity2.remove();

            this.plugin.getLogger().warning("Failed to summon Skeleton Horseman");
            return;
        }

        final Skeleton skeleton = (Skeleton) entity2;
        this.applySkeletonEffects(skeleton, level, difficulty);

        final Entity target = this.plugin.getServer().getEntity(targetID);
        if (target instanceof LivingEntity && !target.isDead() && target.isInWorld()) {
            skeleton.setTarget((LivingEntity) target);
        }

        final SkeletonHorse horse = (SkeletonHorse) entity1;
        this.applyHorseEffects(horse, level, difficulty);

        horse.addPassenger(skeleton);

        //Add to group - create if not exists
        if (!this.groupMap.containsKey(groupID)) {
            this.groupMap.put(groupID, new HashSet<>());
        }
        this.groupMap.get(groupID).add(skeleton.getUniqueId());
        this.groupMap.get(groupID).add(horse.getUniqueId());
    }

    /**
     * Activates the timer that ticks down until the horsemen are despawned
     * @param groupID group of horsemen to begin tracking
     * @param amount the amount of horsemen that were successfully spawned
     * @param lifespan how long until the horsemen are removed
     */
    private void activateTimer(final UUID groupID, final int amount, final int lifespan) {
        //Must have spawned some amount
        if (amount <= 0) return;

        //Sanity check
        if (!this.groupMap.containsKey(groupID)) {
            this.plugin.getLogger().warning("Attempted to activate timer on nonexistent Group ID!");
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                final HashSet<UUID> entities = groupMap.getOrDefault(groupID, null);
                if (entities == null) {
                    plugin.getLogger().warning("Entity set under Group ID " + groupID + " is not present in the map at deletion time!");
                    return;
                }

                //Delete all entities that still exist, with a flash particle
                entities.stream()
                    .map((uuid) -> (plugin.getServer().getEntity(uuid)))
                    .filter((entity) -> (entity != null && !entity.isDead()))
                    .forEach((entity) -> {
                        final World world = entity.getWorld();
                        if (world != null) {
                            world.spawnParticle(Particle.FLASH, entity.getLocation().add(0, 1, 0), 1);
                        }

                        entity.remove();
                    })
                ;


                //Remove group from the map
                groupMap.remove(groupID);
            }
            
        }.runTaskLater(this.plugin, lifespan);
    }

    /**
     * Turns a skeleton into a Skeleton Horseman
     * @param skeleton skeleton to apply buffs and items to
     * @param level level of Ominous Wither that summoned this horseman
     * @param difficulty difficulty of world
     */
    private void applySkeletonEffects(final Skeleton skeleton, final int level, final Difficulty difficulty) {
        //Handle base stats, etc.
        skeleton.getPersistentDataContainer().set(this.plugin.getMinionKey(), PersistentDataType.BOOLEAN, true);
        skeleton.setLootTable(LootTables.EMPTY.getLootTable());
        skeleton.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0));
        skeleton.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(128);

        // ----- EQUIPMENT ------
        final EntityEquipment skeletonEquipment = skeleton.getEquipment();
        if (skeletonEquipment == null) return;

        //Cannot drop any equipment
        skeletonEquipment.setHelmetDropChance(-32767);
        skeletonEquipment.setChestplateDropChance(-32767);
        skeletonEquipment.setLeggingsDropChance(-32767);
        skeletonEquipment.setBootsDropChance(-32767);
        skeletonEquipment.setItemInMainHandDropChance(-32767);

        //Armor
        final int protectionLevel = (int) this.plugin.getBossStatsManager().getStat(BossStat.APOCALYPSE_HORSEMAN_ARMOR_PROTECTION, level, difficulty);
        skeletonEquipment.setArmorContents(new ItemStack[]{
            new ItemBuilder(Material.IRON_BOOTS).addEnchant(Enchantment.PROTECTION, protectionLevel, true).build(),
            new ItemBuilder(Material.IRON_LEGGINGS).addEnchant(Enchantment.PROTECTION, protectionLevel, true).build(),
            new ItemBuilder(Material.IRON_CHESTPLATE).addEnchant(Enchantment.PROTECTION, protectionLevel, true).build(),
            new ItemBuilder(Material.IRON_HELMET).addEnchant(Enchantment.PROTECTION, protectionLevel, true).build(),
        });

        //Weapon
        final ItemStack bow =
            new ItemBuilder(Material.BOW)
                .addEnchant(Enchantment.POWER, (int) this.plugin.getBossStatsManager().getStat(BossStat.APOCALYPSE_HORSEMAN_BOW_POWER, level, difficulty), true)
                .addEnchant(Enchantment.FLAME, 1, false)
            .build()
        ;
        skeletonEquipment.setItemInMainHand(bow);
    }

    /**
     * Turns a skeleton horse into the Skeleton Horseman's Horse
     * @param horse skeleton horse to apply buffs/stats to
     * @param level level of Ominous Wither that summoned this horse
     * @param difficulty difficulty of world
     */
    private void applyHorseEffects(final SkeletonHorse horse, final int level, final Difficulty difficulty) {
        horse.getPersistentDataContainer().set(this.plugin.getMinionKey(), PersistentDataType.BOOLEAN, true);
        horse.setLootTable(LootTables.EMPTY.getLootTable());
        horse.setTamed(true);
        horse.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0));
        horse.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(128);
        horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue((int) this.plugin.getBossStatsManager().getStat(BossStat.APOCALYPSE_HORSE_SPEED, level, difficulty));
        horse.getAttribute(Attribute.GENERIC_MOVEMENT_EFFICIENCY).setBaseValue((int) this.plugin.getBossStatsManager().getStat(BossStat.APOCALYPSE_HORSE_MOVEMENT_EFFICIENCY, level, difficulty));
    }
    
    //#region BehaviorControl
    // Region of code prevents unintended behaviors with Apocalypse Horses by removing them after Horseman death and disabling certain mechanics with them

    /**
     * If the skeleton dies, then the horse flashes out of existence
     */
    @EventHandler(ignoreCancelled = true)
    private void onSkeletonDeath(final EntityDeathEvent event) {
        //Must be a skeleton horseman
        if (event.getEntityType() != EntityType.SKELETON) return;

        final Entity entity = event.getEntity();
        if (!this.plugin.isMinion(entity)) return;

        //Get horse if exists
        final Entity horse = entity.getVehicle();
        if (horse == null || horse.getType() != EntityType.SKELETON_HORSE || !this.plugin.isMinion(horse)) return;

        //Play flash particle
        final World world = horse.getWorld();
        if (world != null) {
            world.spawnParticle(Particle.FLASH, horse.getLocation().add(0, 0.5, 0), 1);
        }

        horse.remove();        
    }

    /**
     * Prevents a non-minion entity from riding a Skeleton Horse minion and vice versa
     * This should already be impossible in regular gameplay since the Skeleton Horse will disappear if the Skeleton dies
     * The only known edge case is if the difficulty is set to Peaceful after the skull is launched
     */
    @EventHandler(ignoreCancelled = true)
    private void onRide(final VehicleEnterEvent event) {
        //Both entities must exist; vehicle must be a skeleton horse
        if (event.getEntered() == null || !(event.getVehicle() instanceof SkeletonHorse)) return;

        //If not matching type then cancel
        if (this.plugin.isMinion(event.getEntered()) ^ this.plugin.isMinion(event.getVehicle())) {
            event.setCancelled(true);
        }
    }

    /**
     * <p>Prevents players from interacting with Skeleton Horse minions</p>
     * <p>This prevents feeding, equipping a saddle, and opening inventory</p>
     */
    @EventHandler(ignoreCancelled = true)
    private void onInteract(final PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof SkeletonHorse)) return;

        //Non-minion player trying to interact with a minion horse -> cancel
        //(Not that minion players can even exist)
        final SkeletonHorse horse = (SkeletonHorse) event.getRightClicked();
        if (this.plugin.isMinion(horse) && !this.plugin.isMinion(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    //#endregion

    //#region RemoveOnLoad
    /**
     * Horsemen get removed on load
     */
    @EventHandler(ignoreCancelled = true)
    private void onUnload(final EntitiesLoadEvent event) {
        this.removeHorsemen(event.getEntities());
    }
    
    /**
     * Horsemen get removed when unloaded
     */
    @EventHandler(ignoreCancelled = true)
    private void onUnload(final EntitiesUnloadEvent event) {
        this.removeHorsemen(event.getEntities());
    }

    /**
     * Any existing Horsemen are removed on plugin enable
     */
    public void onPluginEnable() {
        for (final World world : this.plugin.getServer().getWorlds()) {
            this.removeHorsemen(world.getEntitiesByClasses(Skeleton.class, SkeletonHorse.class));
        }
    }

    /**
     * Finds any horsemen in the list of entities and despawns them
     */
    private void removeHorsemen(final Collection<Entity> entities) {
        for (final Entity entity : entities) {
            if (this.plugin.isMinion(entity) && (entity.getType() == EntityType.SKELETON || entity.getType() == EntityType.SKELETON_HORSE)) {
                entity.remove();
            }
        }
    }
    //#endregion
}
