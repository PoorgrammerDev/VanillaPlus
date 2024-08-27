package io.github.poorgrammerdev.recoverytotem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;

/**
 * Command to allow admins to give totems to players
 */
public class GiveCommand implements CommandExecutor, TabCompleter {
    private final RecoveryTotem plugin;
    
    public GiveCommand(RecoveryTotem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /giverecoverytotem <username> [count]
        if (!command.getName().equalsIgnoreCase("giverecoverytotem")) return false;
        if (!sender.hasPermission(new Permission("recoverytotem.giverecoverytotem"))) return false;
        
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Too few arguments, must specify player(s).");
            return false;
        }

        // Get all players represented by selector
        final List<Player> players;
        try {
            // Resolve selector and filter out non-player entities
            players = plugin.getServer().selectEntities(sender, args[0])
                .stream()
                .filter(entity -> (entity instanceof Player))
                .map(entity -> (Player) entity)
                .collect(Collectors.toList());
        }
        catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid selector.");
            return false;
        }

        //Make sure there is at least one player
        if (players.size() == 0) {
            sender.sendMessage(ChatColor.RED + "Selector returned no players.");
            return false;
        }

        //Get the totem item
        final ItemStack totem = this.plugin.createTotem();

        //Adjust count if necessary
        if (args.length >= 2) {
            final int count;

            //atoi
            try {
                count = Integer.parseInt(args[1]);
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

            // Update quantity
            totem.setAmount(count);
        }
        
        //Give all specified players a totem
        for (final Player player : players) {
            //Add item to player's inventory
            final HashMap<Integer, ItemStack> returnedMap = player.getInventory().addItem(totem);

            // If the player couldn't receive the item directly, drop it on the ground instead
            if (returnedMap.size() > 0) {
                player.getWorld().dropItemNaturally(player.getLocation(), totem);
            }

            //Send message
            sender.sendMessage("Gave " + totem.getAmount() + " [" + ChatColor.YELLOW + "Totem of Recovery" + ChatColor.RESET + "] to " + player.getDisplayName());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        // /giverecoverytotem <username> [count]
        if (!command.getName().equalsIgnoreCase("giverecoverytotem")) return null;
        if (!sender.hasPermission(new Permission("recoverytotem.giverecoverytotem"))) return null;

        final ArrayList<String> ret = new ArrayList<>();

        switch (args.length) {
            case 1:
                //Adds all online players that still might be typed
                ret.addAll(
                    plugin.getServer().getOnlinePlayers()
                        .stream()
                        .map(Player::getName)
                        .filter(name -> (name.toUpperCase().startsWith(args[0].toUpperCase())))
                        .collect(Collectors.toList())
                );

                //Adds all selectors that still might be typed
                ret.addAll(
                    Arrays.asList("@a", "@p", "@r", "@s")
                        .stream()
                        .filter(selector -> (selector.toUpperCase().startsWith(args[0].toUpperCase())))
                        .collect(Collectors.toList())
                );
                break;
            default:
                break;
        }

        return ret;
    }
    
}
