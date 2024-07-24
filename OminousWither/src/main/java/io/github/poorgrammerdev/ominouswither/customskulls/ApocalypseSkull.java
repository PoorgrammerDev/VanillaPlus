package io.github.poorgrammerdev.ominouswither.customskulls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.Wither;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.ApocalypseHorsemen;
import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.ParticleInfo;
import io.github.poorgrammerdev.ominouswither.backend.CoroutineManager;
import io.github.poorgrammerdev.ominouswither.coroutines.PassableLocationFinder;

/**
 * Skull that spawns Skeleton Horsemen on hit
 * @author Thomas Tran
 */
public class ApocalypseSkull extends AbstractSkullHandler {
    // private static final int MIN_BUFF_LEVEL = 4;
    
    private final ApocalypseHorsemen apocalypseHorsemen;
    // private final Random random;

    public ApocalypseSkull(final OminousWither plugin, final ApocalypseHorsemen apocalypseHorsemen) {
        super(plugin, 5.0D, new ParticleInfo(Particle.CLOUD, 1, 0, 0, 0, 0, null));

        this.apocalypseHorsemen = apocalypseHorsemen;
        // this.random = new Random();
    }

    @Override
    public void onHit(final ProjectileHitEvent event, final Wither wither) {
        //Projectile must have hit a living entity straight-on to activate
        if (!(event.getHitEntity() instanceof LivingEntity)) return;

        //Hit entity cannot be a minion
        final LivingEntity hitEntity = (LivingEntity) event.getHitEntity();
        if (this.plugin.isMinion(hitEntity)) return;

        //Last damage cause must be this skull and it must've done damage after negation
        //Unfortunately the Absorption potion effect seems to count as damage negation and the only method of detecting that is deprecated
        //So Absorption is a counter to the Apocalypse skull for the time being 
        final EntityDamageEvent lastDamageCause = hitEntity.getLastDamageCause();
        if (
            lastDamageCause == null ||
            lastDamageCause.getDamageSource() == null ||
            lastDamageCause.getDamageSource().getDirectEntity() == null ||
            !lastDamageCause.getDamageSource().getDirectEntity().equals(event.getEntity()) ||
            lastDamageCause.getFinalDamage() <= 0.0D
        ) return;

        final int level = wither.getPersistentDataContainer().getOrDefault(this.plugin.getLevelKey(), PersistentDataType.INTEGER, 1);
        final Location center = event.getEntity().getLocation();

        //Cannot surpass a maximum amount of horsemen
        // final List<Skeleton> nearbyHorsemen = this.getNearbySkeletons(center);
        // if (nearbyHorsemen.size() >= MAX_HORSEMEN) {
        //     // //If max amount reached and is over a level threshold, it will buff the existing horsemen instead of spawning more
        //     // //Otherwise, nothing happens
        //     // if (level >= MIN_BUFF_LEVEL) {
        //     //     this.buffSkeletons(nearbyHorsemen, center, level);
        //     // }
            
        //     return;
        // }


        //Strike lightning like in a real skeleton horse trap
        final World centerWorld = center.getWorld();
        if (centerWorld != null) {
            centerWorld.strikeLightningEffect(center);
        }
        
        //Find viable locations and summon skeleton horsemen
        final UUID groupID = UUID.randomUUID();
        CoroutineManager.getInstance().enqueue(new PassableLocationFinder(
            center,
            new Vector(5, 5, 5),
            4,
            true,
            true,
            level,
            (location) -> {this.apocalypseHorsemen.spawnHorseman(location, groupID);},
            (amount) -> {this.apocalypseHorsemen.activateTimer(amount, groupID);}
        ));
    }

    @Override
    public String getSkullTag() {
        return "apocalypse";
    }
    


    // private List<Skeleton> getNearbySkeletons(final Location center) {
    //     final World world = center.getWorld();
    //     if (world == null) return null;

    //     return world.getNearbyEntities(center, 20, 20, 20,
    //             (entity) -> ((entity.getType() == EntityType.SKELETON))
    //         )
    //         .stream()
    //         .filter((entity) -> (entity.getPersistentDataContainer().getOrDefault(this.plugin.getMinionKey(), PersistentDataType.BOOLEAN, false)))
    //         .filter((entity) -> (entity instanceof Skeleton))
    //         .map((entity) -> (Skeleton) entity)
    //         .collect(Collectors.toList());

    // }

    // private void buffSkeletons(final List<Skeleton> allSkeletons, final Location center, final int witherLevel) {
    //     //Select a sample of skeletons to buff
    //     final List<Skeleton> sample = getSample(allSkeletons, witherLevel);

    //     for (final Skeleton skeleton : sample) {
    //         final EntityEquipment equipment = skeleton.getEquipment();
    //         if (equipment == null) continue;

    //         final ItemStack[] armor = equipment.getArmorContents();
    //         if (armor == null) continue;

    //         //Upgrade Protection enchantment level of their armor
    //         for (final ItemStack item : armor) {
    //             final int enchantmentLevel = item.getEnchantmentLevel(Enchantment.PROTECTION);
    //             if (enchantmentLevel < Enchantment.PROTECTION.getMaxLevel()) {
    //                 item.addEnchantment(Enchantment.PROTECTION, enchantmentLevel + 1);
    //             }
    //         }

    //         equipment.setArmorContents(armor);
    //     }
    // }

    // private List<Skeleton> getSample(final List<Skeleton> allSkeletons, final int amount) {
    //     final int length = allSkeletons.size();

    //     if ((length - amount) >= 200) {
    //         final HashSet<Integer> indices = new HashSet<>();
    //         for (int i = 0; i < amount; ++i) {
    //             final int next = this.random.nextInt(length);
    //             if (indices.contains(next)) {
    //                 i--;
    //             }
    //             else {
    //                 indices.add(next);
    //             }
    //         }

    //         final List<Skeleton> output = new ArrayList<Skeleton>();
    //         for (final Integer index : indices) {
    //             output.add(allSkeletons.get(index));
    //         }
            
    //         return output;
    //     }
    //     else {
    //         Collections.shuffle(allSkeletons);
    //         return allSkeletons.subList(0, amount);
    //     }
    // }

}
