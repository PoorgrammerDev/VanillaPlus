package io.github.poorgrammerdev.hammer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.permissions.Permission;

/**
 * Command to allow admins to give hammers to players
 * @author Thomas Tran
 */
public class GiveCommand implements CommandExecutor, TabCompleter {
    private final Hammer plugin;
    private final HashMap<String, Material> hammerMap;
    
    public GiveCommand(Hammer plugin, HashMap<Material, NamespacedKey> recipeKeyMap) {
        this.plugin = plugin;

        this.hammerMap = new HashMap<>();
        for (Material pickType : recipeKeyMap.keySet()) {
            hammerMap.put(
                recipeKeyMap.get(pickType).getKey().toUpperCase(),
                pickType
            );
        }

        //Manually add in the netherite hammer for now
        //TODO: clean this up
        this.hammerMap.put("NETHERITE_HAMMER", Material.NETHERITE_PICKAXE);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /givehammer <selector> <hammer-type> [count] [damage]
        if (!command.getName().equalsIgnoreCase("givehammer")) return false;
        if (!sender.hasPermission(new Permission("hammer.givehammer"))) return false;
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Too few arguments.");
            return false;
        }

        // Make sure the hammer type is valid
        final String hammerType = args[1].toUpperCase();
        if (!this.hammerMap.containsKey(hammerType)) {
            sender.sendMessage(ChatColor.RED + "Invalid hammer type.");
            return false;
        }

        // Get all players represented by selector
        final List<Entity> entities;
        try {
            entities = plugin.getServer().selectEntities(sender, args[0]);
        }
        catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid selector.");
            return false;
        }

        final ArrayList<Player> players = new ArrayList<>();
        for (Entity entity : entities) {
            if (entity instanceof Player) {
                players.add((Player) entity);
            }
        }

        //Make sure there is at least one player
        if (players.size() == 0) {
            sender.sendMessage(ChatColor.RED + "Selector returned no players.");
            return false;
        }

        //Get the hammer item
        final ItemStack hammer = plugin.createHammer(this.hammerMap.get(hammerType));

        //Adjust count if necessary
        if (args.length >= 3) {
            final int count;

            //atoi
            try {
                count = Integer.parseInt(args[2]);
            }
            catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid quantity, expected a number.");
                return false;
            }

            //Invalid quantity
            if (count <= 0 || count > 64) {
                sender.sendMessage(ChatColor.RED + "Invalid quantity, expected a number between 1 and 64.");
                return false;
            }

            hammer.setAmount(count);
        }

        //Adjust damage if necessary
        if (args.length >= 4) {
            if (!(hammer.getItemMeta() instanceof Damageable)) {
                sender.sendMessage(ChatColor.RED + "Internal Plugin Error.");
                return false;
            }

            final Damageable meta = (Damageable) hammer.getItemMeta();
            final int damage;

            //atoi
            try {
                damage = Integer.parseInt(args[3]);
            }
            catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid damage, expected a number.");
                return false;
            }

            meta.setDamage(damage);
            hammer.setItemMeta(meta);
        }
        
        //Give all specified players a hammer
        for (final Player player : players) {
            player.getInventory().addItem(hammer);

            //Send message
            sender.sendMessage("Gave " + hammer.getAmount() + " [" + plugin.getHammerName(this.hammerMap.get(hammerType)) + "] to " + player.getDisplayName());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        // /givehammer <selector> <hammer-type> [count] [damage]
        if (!command.getName().equalsIgnoreCase("givehammer")) return null;
        if (!sender.hasPermission(new Permission("hammer.givehammer"))) return null;

        final ArrayList<String> ret = new ArrayList<>();

        //Best code I've ever written :)
        switch (args.length) {
            case 1:
                //Adds all online players that still might be typed
                ret.addAll(plugin.getServer().getOnlinePlayers().stream().map(Player::getName).filter(name -> (name.toUpperCase().startsWith(args[0].toUpperCase()))).collect(Collectors.toList()));
                //Adds all selectors that still might be typed
                ret.addAll(Arrays.asList("@a", "@p", "@r", "@s").stream().filter(selector -> (selector.toUpperCase().startsWith(args[0].toUpperCase()))).collect(Collectors.toList()));
                break;
            case 2:
                //Adds all tiers that still might be typed
                ret.addAll(this.hammerMap.keySet().stream().filter(tier -> (tier.toUpperCase().startsWith(args[1].toUpperCase()))).map(String::toLowerCase).collect(Collectors.toList()));
                break;
            default:
                break;
        }

        return ret;
    }
    
}
