package io.github.poorgrammerdev.ominouswither.commands;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.mechanics.SpawnCooldown;

import net.md_5.bungee.api.ChatColor;

/**
 * Command to manage player cooldowns
 * @author Thomas Tran
 */
public class CooldownCommand implements CommandExecutor, TabCompleter {
    private final OminousWither plugin;
    private final SpawnCooldown cooldownManager;

    private final boolean globalAllowViewOwnCooldown;

    public CooldownCommand(OminousWither plugin, SpawnCooldown cooldownManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;

        this.globalAllowViewOwnCooldown = plugin.getConfig().getBoolean("spawn_cooldown.global_allow_view_own_cooldown", true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /cooldown get                       | ominouswither.cooldown_get_self
        // /cooldown get <player>              | ominouswither.cooldown_get_others
        // /cooldown set <player> <seconds>    | ominouswither.cooldown_modify
        // /cooldown remove <player>           | ominouswither.cooldown_modify

        if (!command.getName().equalsIgnoreCase("cooldown")) return false;

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Too few arguments, must specify subcommand.");
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "get":
                return this.subcommandGet(sender, command, label, args);

            case "set":
                return this.subcommandSet(sender, command, label, args);
                
            case "remove":
                return this.subcommandRemove(sender, command, label, args);

            default:
                sender.sendMessage(ChatColor.RED + "Invalid subcommand.");
                return false;
        }
    }

    private boolean subcommandGet(CommandSender sender, Command command, String label, String[] args) {
        //Self subcommand
        if (args.length < 2) {
            if (!this.globalAllowViewOwnCooldown && !sender.hasPermission("ominouswither.cooldown_get_self")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Must specify player when using this command from console.");
                return false;
            }
            
            final Player player = (Player) sender;
            final Duration duration = this.cooldownManager.getRemainingCooldown(player);

            player.sendMessage(duration.equals(Duration.ZERO) ? "You do not have a cooldown active." : "You have " + duration.toSeconds() + " seconds remaining in cooldown.");
            return true;
        }

        //Others subcommand
        if (!sender.hasPermission("ominouswither.cooldown_get_others")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this sub-command.");
            return true;
        }
        
        final Player target = this.plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Could not find specified player.");
            return false;
        }

        final Duration duration = this.cooldownManager.getRemainingCooldown(target);
        sender.sendMessage(duration.equals(Duration.ZERO) ? target.getName() + " does not have a cooldown active." : target.getName() + " has " + duration.toSeconds() + " seconds remaining in cooldown.");
        return true;
    }

    private boolean subcommandSet(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ominouswither.cooldown_modify")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this sub-command.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Too few arguments, must specify player and duration in seconds.");
            return false;
        }

        final Player target = this.plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Could not find specified player.");
            return false;
        }

        int duration;
        try {
            duration = Integer.parseInt(args[2]);
        }
        catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Duration must be an integer (in seconds).");
            return false;
        }

        if (duration < 0) {
            sender.sendMessage(ChatColor.RED + "Duration must be a non-negative integer (in seconds).");
            return false;
        }

        this.cooldownManager.setCooldown(target, Duration.ofSeconds(duration));
        sender.sendMessage("Set " + target.getName() + "'s cooldown to " + duration + " seconds.");
        return true;
    }

    private boolean subcommandRemove(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ominouswither.cooldown_modify")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this sub-command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Too few arguments, must specify player.");
            return false;
        }

        final Player target = this.plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Could not find specified player.");
            return false;
        }

        final Instant instant = this.cooldownManager.removeCooldown(target);
        sender.sendMessage(instant != null ?
            "Removed cooldown of " + Duration.between(Instant.now(), instant).toSeconds() + " seconds from " + target.getName() + "." :
            target.getName() + " did not have an active cooldown to remove."
        );
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        // /cooldown get                       | ominouswither.cooldown_get_self
        // /cooldown get <player>              | ominouswither.cooldown_get_others
        // /cooldown set <player> <seconds>    | ominouswither.cooldown_modify
        // /cooldown remove <player>           | ominouswither.cooldown_modify

        final ArrayList<String> retUnfiltered = new ArrayList<>();
        
        final boolean hasGetSelf = sender.hasPermission("ominouswither.cooldown_get_self");
        final boolean hasGetOthers = sender.hasPermission("ominouswither.cooldown_get_others");
        final boolean hasModify = sender.hasPermission("ominouswither.cooldown_modify");

        switch (args.length) {
            case 1:
                if (this.globalAllowViewOwnCooldown || hasGetSelf || hasGetOthers) {
                    retUnfiltered.add("get");
                }
                if (hasModify) {
                    retUnfiltered.add("set");
                    retUnfiltered.add("remove");
                }
                break;
            case 2:
                if (hasGetOthers || hasModify) {
                    //Adds all online players
                    retUnfiltered.addAll(plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList());
                }
                break;
            default:
                break;         
        }

        return retUnfiltered
            .stream()
            .filter(val -> (val.toLowerCase().startsWith(args[args.length - 1].toLowerCase())))
            .toList()
        ;
    }

}
