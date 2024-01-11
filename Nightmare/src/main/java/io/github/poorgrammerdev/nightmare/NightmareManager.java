package io.github.poorgrammerdev.nightmare;

import java.util.HashMap;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;

public class NightmareManager {
    private final int BOUNDARY = 20000000;
    private final int RANGE_SQUARED = 1000000; //range = 1000
    private final int CENTER_HEIGHT = 64;
    private final int MAX_TRIES = 100;
    private final int COPY_RANGE = 10;

    private final Nightmare plugin;

    /**
     * Map of players currently in the nightmare to their central locations (where they were spawned)
     */
    private final HashMap<UUID, Vector> activePlayers;

    public NightmareManager(final Nightmare plugin) {
        this.plugin = plugin;
        this.activePlayers = new HashMap<>();
    }

    public boolean enterNightmare(final Player player) {
        // First, find a valid location within the nightmare
        final Vector centerVec = getAvailableNightmareCenter();
        if (centerVec == null) return false;

        //Copy player's surroundings to this area
        final World originalWorld = player.getWorld();
        final World nightmareWorld = this.plugin.getNightmareWorld();
        final Location nightmareCenter = centerVec.toLocation(nightmareWorld);
        final Location realCenter = player.getLocation();

        //TODO: make it work
        // final int radius = 10;
        // for (int x = -radius; x <= radius; x++) {
        //     for (int z = -radius; z <= radius; z++) {
        //         for (int y = -radius; y <= radius; y++) {
        //             Block origWorldBlock = originalWorld.getBlockAt(realCenter.getBlockX() + x, realCenter.getBlockY() + y, realCenter.getBlockZ() + z);
        //             Block copyWorldBlock = nightmareWorld.getBlockAt(nightmareCenter.getBlockX() + x, nightmareCenter.getBlockY() + y, nightmareCenter.getBlockZ() + z);

        //             if (origWorldBlock.getType() != copyWorldBlock.getType()) {
        //                 copyWorldBlock.setBlockData(origWorldBlock.getBlockData(), false);
        //             }
        //         }
        //     }
        // }

        player.teleport(nightmareCenter, TeleportCause.PLUGIN);
        return true;
    }


    private Vector getAvailableNightmareCenter() {
        final Vector output = new Vector();

        for (int tries = 0; tries < MAX_TRIES; tries++) {
            output.setX(ThreadLocalRandom.current().nextInt(-BOUNDARY, BOUNDARY));
            output.setY(CENTER_HEIGHT);
            output.setZ(ThreadLocalRandom.current().nextInt(-BOUNDARY, BOUNDARY));

            //Adequately far away from any other players
            if (!isWithinRange(output)) {
                Bukkit.broadcastMessage("success after " + tries + " tries");
                return output;
            }
        }

        return null;
    }

    private boolean isWithinRange(final Vector vector) {
        return this.activePlayers.entrySet()
        .stream()
        .map(Entry<UUID,Vector>::getValue)
        .anyMatch(other -> (vector.distanceSquared(other) <= RANGE_SQUARED));
    }

}
