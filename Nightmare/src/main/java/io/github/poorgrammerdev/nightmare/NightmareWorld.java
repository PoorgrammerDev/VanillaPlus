package io.github.poorgrammerdev.nightmare;

import java.util.Random;

import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

public class NightmareWorld extends ChunkGenerator {
    private final Nightmare plugin;

    public NightmareWorld(final Nightmare plugin) {
        this.plugin = plugin;
    }

    public World createWorld() {
        final WorldCreator creator = new WorldCreator(this.plugin.getMainWorldName() + "_nightmare");
        creator.environment(World.Environment.NORMAL);
        creator.generator(this);

        final World world = plugin.getServer().createWorld(creator);
        world.setDifficulty(Difficulty.HARD);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.FALL_DAMAGE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.SPAWN_RADIUS, 0);
        world.setGameRule(GameRule.DO_TILE_DROPS, false);
        world.setGameRule(GameRule.DISABLE_RAIDS, true);
        world.setGameRule(GameRule.DO_INSOMNIA, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        world.setGameRule(GameRule.FIRE_DAMAGE, false);
        world.setGameRule(GameRule.DROWNING_DAMAGE, false);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setSpawnLocation(0, 64, 0);
        world.setTime(18000L); //midnight

        return world;
    }

    @Override
    public void generateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
        //do nothing
    }
    
}
