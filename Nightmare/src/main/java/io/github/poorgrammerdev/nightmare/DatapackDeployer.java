package io.github.poorgrammerdev.nightmare;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.bukkit.NamespacedKey;
import org.bukkit.scheduler.BukkitRunnable;

public class DatapackDeployer {
    private final Nightmare plugin;

    public DatapackDeployer(final Nightmare plugin) {
        this.plugin = plugin;
    }

    public void deploy() throws IOException {
        final String datapackLocation = this.plugin.getMainWorldName() + "/datapacks/nightmare-datapack/";
        if (new File(datapackLocation).exists()) return;

        //Generate the pack meta
        copyFileFromResource(datapackLocation, "pack.mcmeta");
        
        //Generate the custom biome
        copyFileFromResource(datapackLocation + "data/nightmare/worldgen/biome", "nightmare-biome.json");

        //Generate the dimension
        copyFileFromResource(datapackLocation + "data/nightmare/dimension", "nightmare-dimension.json");

        new BukkitRunnable() {

            @Override
            public void run() {
                plugin.getServer().getDataPackManager().getDataPacks().forEach(datapack -> {
                    if (!datapack.isEnabled() && datapack.getKey().equals(NamespacedKey.minecraft("file/nightmare-datapack"))) {
                        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "minecraft:reload");
                    }
                });
            }
            
        }.runTaskLater(plugin, 20L);
    }

    /**
     * Copies a file from resources; creates all necessary folders
     * @param destinationPath file to write to (this MUST include the destination file too, e.g. folder/folder2 is NOT valid, folder/folder2/destfile.txt IS valid)
     * @param resourceName name of resource to copy from
     */
    private void copyFileFromResource(final String destinationPath, final String resourceName) throws IOException {
        final File folder = new File(destinationPath);
        folder.mkdirs();

        final File file = new File(folder, resourceName);
        file.createNewFile();
        
        final FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(new String(this.plugin.getResource(resourceName).readAllBytes()));
        fileWriter.close();
    }
    
}
