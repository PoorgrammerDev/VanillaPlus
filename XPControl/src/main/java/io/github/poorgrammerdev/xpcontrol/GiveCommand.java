package io.github.poorgrammerdev.xpcontrol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;

/**
 * Command to allow admins to give EXP storage bottles to players
 * @author Thomas Tran
 */
public class GiveCommand implements CommandExecutor, TabCompleter {
    private final XPControl plugin;
    private final XPStorage storageManager;
    
    public GiveCommand(final XPControl plugin, final XPStorage storageManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /givexpbottle <selector> <storedxp> [count]
        if (!command.getName().equalsIgnoreCase("givexpbottle")) return false;
        if (!sender.hasPermission(new Permission("xpcontrol.givexpbottle"))) return false;
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Too few arguments.");
            return false;
        }

        // Make sure the integer is valid
        int storedAmount = 0;
        try {
            storedAmount = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException exception) {
            sender.sendMessage(ChatColor.RED + "Invalid amount of experience points to store.");
            return false;
        }

        // Stored amount cannot be negative
        if (storedAmount < 0) {
            sender.sendMessage(ChatColor.RED + "Amount of experience points cannot be negative.");
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

        //Get the exp storage item
        final ItemStack item = this.storageManager.createStorageBottle(storedAmount);

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

            item.setAmount(count);
        }
        
        //Give all specified players the item
        for (final Player player : players) {
            player.getInventory().addItem(item);

            //Send message
            sender.sendMessage("Gave " + item.getAmount() + " [" + XPStorage.STORAGE_BOTTLE_NAME + "] to " + player.getDisplayName());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        // /givexpbottle <selector> <storedxp> [count]
        if (!command.getName().equalsIgnoreCase("givexpbottle")) return null;
        if (!sender.hasPermission(new Permission("xpcontrol.givexpbottle"))) return null;

        final ArrayList<String> ret = new ArrayList<>();

        //Best code I've ever written :)
        switch (args.length) {
            case 1:
                //Adds all online players that still might be typed
                ret.addAll(plugin.getServer().getOnlinePlayers().stream().map(Player::getName).filter(name -> (name.toUpperCase().startsWith(args[0].toUpperCase()))).collect(Collectors.toList()));
                //Adds all selectors that still might be typed
                ret.addAll(Arrays.asList("@a", "@p", "@r", "@s").stream().filter(selector -> (selector.toUpperCase().startsWith(args[0].toUpperCase()))).collect(Collectors.toList()));
                break;
            default:
                break;
        }

        return ret;
    }
    
}
