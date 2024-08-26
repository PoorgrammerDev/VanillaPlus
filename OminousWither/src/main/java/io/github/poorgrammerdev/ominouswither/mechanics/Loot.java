package io.github.poorgrammerdev.ominouswither.mechanics;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.scheduler.BukkitRunnable;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.config.BossStat;

/**
 * <p>Manages the loot and EXP that an Ominous Wither drops</p>
 * <p>Implements multipliers and custom loot tables</p>
 * @author Thomas Tran
 */
public class Loot implements Listener {
    //Value from Minecraft Wiki as of 22 August 2024: https://minecraft.wiki/w/Experience
    private static final int MAX_EXP_PER_ORB = 32767;

    private final OminousWither plugin;
    private final Random random;
    private final LootTable lootTable;

    private final boolean invulnerableLoot;
    private final boolean immortalLoot;
    private final boolean glowingLoot;

    public Loot(OminousWither plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.lootTable = this.getCustomLootTable();

        this.invulnerableLoot = plugin.getConfig().getBoolean("invulnerable_loot", true);
        this.immortalLoot = plugin.getConfig().getBoolean("immortal_loot", true);
        this.glowingLoot = plugin.getConfig().getBoolean("glowing_loot", false);
    }

    @EventHandler(ignoreCancelled = true)
    private void onDeath(final EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Wither)) return;

        final Wither wither = (Wither) event.getEntity();
        if (!this.plugin.isOminous(wither)) return;

        final World world = wither.getWorld();
        final Location location = wither.getLocation();        

        //Get stats
        final int totalExpToDrop = (int) (event.getDroppedExp() * this.plugin.getBossStatsManager().getStat(BossStat.LOOT_EXP_MULTIPLIER, wither));
        final int lootMultiplier = (int) this.plugin.getBossStatsManager().getStat(BossStat.LOOT_ITEM_MULTIPLIER, wither);
        
        //Handle EXP
        if (totalExpToDrop <= MAX_EXP_PER_ORB) {
            event.setDroppedExp(totalExpToDrop);
        }
        else {
            event.setDroppedExp(MAX_EXP_PER_ORB);
            final int missedExp = this.dropExp(world, location, totalExpToDrop - MAX_EXP_PER_ORB);
            if (missedExp > 0) {
                this.plugin.getLogger().warning("Failed to generate " + missedExp + " experience points for death of Ominous Wither " + wither.getUniqueId());
            }
        }

        //Handle loot
        if (this.lootTable == null) {
            //Using default Wither loot
            this.populateDefaultLoot(event.getDrops(), lootMultiplier);
        }
        else {
            //Using custom loot table
            final LootContext.Builder contextBuilder = new LootContext.Builder(location.clone()).lootedEntity(wither);
            if (event.getDamageSource().getCausingEntity() instanceof HumanEntity) {
                contextBuilder.killer((HumanEntity) event.getDamageSource().getCausingEntity());
            }

            //Since loot tables can have random chance, we can't simply roll once and multiply
            //So each multiplication is a separate roll
            event.getDrops().clear();
            for (int i = 0; i < lootMultiplier; ++i) {
                event.getDrops().addAll(this.lootTable.populateLoot(this.random, contextBuilder.build()));
            }
        }

        //Loot entity modifiers
        if (this.invulnerableLoot || this.immortalLoot || this.glowingLoot) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (final Entity entity : world.getNearbyEntities(location, 0.1, 0.1, 0.1)) {
                        final boolean isItem = entity instanceof Item;
                        if (!isItem && !(entity instanceof ExperienceOrb)) continue;

                        if (Loot.this.invulnerableLoot) entity.setInvulnerable(true);
                        if (Loot.this.glowingLoot) entity.setGlowing(true);

                        if (isItem && Loot.this.immortalLoot) {
                            ((Item) entity).setUnlimitedLifetime(true);
                        }
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    /**
     * Handles the multiplication of default loot, respecting max stack sizes
     * @param drops list to modify; should be event.getDrops()
     * @param lootMultiplier loot multiplier stat
     */
    private void populateDefaultLoot(final List<ItemStack> drops, final int lootMultiplier) {
        final int n = drops.size();

        for (int i = 0; i < n; ++i) {
            final ItemStack drop = drops.get(i);
            final int count = drop.getAmount() * lootMultiplier;

            final int maxStackSize = drop.getMaxStackSize();

            //If exceeded max stack size for this item after multiplying, add extra stacks
            if (count > maxStackSize) {
                final int extraStacks = (count / maxStackSize) - 1; //Subtracting 1 to account for the first already-present stack
                final int lastStackSize = count % maxStackSize;
                drop.setAmount(maxStackSize);

                //Handle all full stacks after the 1st one
                for (int j = 0; j < extraStacks; ++j) {
                    final ItemStack duplicate = drop.clone();
                    duplicate.setAmount(maxStackSize);
                    drops.add(duplicate);
                }

                //If present, handle last partial stack
                if (lastStackSize > 0) {
                    final ItemStack duplicate = drop.clone();
                    duplicate.setAmount(lastStackSize);
                    drops.add(duplicate);
                }
            }
            //Otherwise just set stack size
            else {
                drop.setAmount(count);
            }

        }
    }

    /**
     * Splits total exp into multiple orbs if necessary and drops them at a location
     * @param world world to spawn orbs in
     * @param location location to spawn orbs at
     * @param totalExpToDrop total exp to drop across all orbs
     * @return the amount of exp that couldn't be spawned (if everything goes well, this should be 0)
     */
    private int dropExp(final World world, final Location location, final int totalExpToDrop) {
        final int fullyLoadedOrbs = totalExpToDrop / MAX_EXP_PER_ORB;
        final int partialOrbExpCount = totalExpToDrop % MAX_EXP_PER_ORB;

        int missedExp = 0;

        for (int i = 0; i < fullyLoadedOrbs; ++i) {
            missedExp += spawnExpOrb(world, location, MAX_EXP_PER_ORB);
        }

        if (partialOrbExpCount > 0) {
            missedExp += spawnExpOrb(world, location, partialOrbExpCount);
        }

        return missedExp;
    }

    /**
     * Spawns a single exp orb at a location with a specified exp value
     * @param world world to spawn orb in
     * @param location location to spawn orb at
     * @param exp exp value to be stored in this orb (should not exceed MAX_EXP_PER_ORB)
     * @return the amount of exp that couldn't be spawned (if everything goes well, this should be 0)
     */
    private int spawnExpOrb(final World world, final Location location, final int exp) {
        final Entity entity = world.spawnEntity(location, EntityType.EXPERIENCE_ORB);
        if (!(entity instanceof ExperienceOrb)) return exp;

        final ExperienceOrb orb = (ExperienceOrb) entity;
        orb.setExperience(exp);

        return 0;
    }
    

    /**
     * Gets the appropriate loot table for this Wither to initialize field
     * @return custom loot table if valid and found or null to indicate fallback to built-in wither table
     */
    private @Nullable LootTable getCustomLootTable() {
        //NOTE: The reason I'm not using LootTables.WITHER is because:
        //In testing in 1.21.1 (Aug 2024), this loot table seems to drop nothing regardless of loot context given
        //Even in vanilla with the /loot command, the corresponding vanilla loot table "minecraft:entities/wither" contains nothing
        //Using the vanilla /loot command with the subcommand 'kill' on a Wither entity yields the same empty result
        //This likely means the Nether Star drop is hardcoded into the game and is not a loot table

        final String customLootTableName = plugin.getConfig().getString("custom_loot_table", null);

        //Not defined
        if (customLootTableName == null) {
            plugin.getLogger().info("Custom loot table not defined, using vanilla Wither loot table");
            return null;
        }

        //Invalid key
        final NamespacedKey key = NamespacedKey.fromString(customLootTableName.toLowerCase());
        if (key == null) {
            plugin.getLogger().warning("Supplied key \"" + customLootTableName + "\" for custom loot table is malformed, falling back to vanilla Wither loot table");
            return null;
        }

        //Missing table
        //NOTE: current spigot api will always return a loot table even if invalid
        final LootTable lootTable = plugin.getServer().getLootTable(key);
        if (lootTable == null) {
            plugin.getLogger().warning("Could not find custom loot table with key \"" + customLootTableName + "\", falling back to vanilla Wither loot table");
            return null;
        }

        //Table found
        plugin.getLogger().info("Custom loot table \"" + lootTable.toString() + "\" successfully loaded");
        return lootTable;
    }

}
