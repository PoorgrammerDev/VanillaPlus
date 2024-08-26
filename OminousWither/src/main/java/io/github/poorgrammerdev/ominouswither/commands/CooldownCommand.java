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
import io.github.poorgrammerdev.ominouswither.utils.Utils;

/**
 * Command to manage player cooldowns
 * @author Thomas Tran
 */
public class CooldownCommand implements CommandExecutor, TabCompleter {
    private final OminousWither plugin;
    private final SpawnCooldown cooldownManager;

    private final boolean globalAllowViewOwnCooldown;

    //Messages
    private final String missingSubcommand;
    private final String invalidSubcommand;
    private final String insufficientPermissionsCommand;
    private final String insufficientPermissionsSubcommand;
    private final String consolePlayerRequired;
    private final String invalidPlayer;
    private final String missingPlayerOrDuration;
    private final String invalidDurationType;
    private final String invalidDurationValue;
    private final String missingPlayer;
    private final String getSelfNoCooldown;
    private final String getSelfCooldown;
    private final String getOtherNoCooldown;
    private final String getOtherCooldown;
    private final String setCooldown;
    private final String removedNoCooldown;
    private final String removedCooldown;


    public CooldownCommand(OminousWither plugin, SpawnCooldown cooldownManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;

        this.globalAllowViewOwnCooldown = plugin.getConfig().getBoolean("spawn_cooldown.global_allow_view_own_cooldown", true);

        this.missingSubcommand = plugin.getConfig().getString("messages.missing_subcommand", "");
        this.invalidSubcommand = plugin.getConfig().getString("messages.invalid_subcommand", "");
        this.insufficientPermissionsCommand = plugin.getConfig().getString("messages.insufficient_permissions_command", "");
        this.insufficientPermissionsSubcommand = plugin.getConfig().getString("messages.insufficient_permissions_subcommand", "");
        this.consolePlayerRequired = plugin.getConfig().getString("messages.console_player_required", "");
        this.invalidPlayer = plugin.getConfig().getString("messages.invalid_player", "");
        this.missingPlayerOrDuration = plugin.getConfig().getString("messages.missing_player_or_duration", "");
        this.invalidDurationType = plugin.getConfig().getString("messages.invalid_duration_type", "");
        this.invalidDurationValue = plugin.getConfig().getString("messages.invalid_duration_val", "");
        this.missingPlayer = plugin.getConfig().getString("messages.missing_player", "");
        this.getSelfNoCooldown = plugin.getConfig().getString("messages.get_self_no_cooldown", "");
        this.getSelfCooldown = plugin.getConfig().getString("messages.get_self_cooldown", "");
        this.getOtherNoCooldown = plugin.getConfig().getString("messages.get_other_no_cooldown", "");
        this.getOtherCooldown = plugin.getConfig().getString("messages.get_other_cooldown", "");
        this.setCooldown = plugin.getConfig().getString("messages.set_cooldown", "");
        this.removedNoCooldown = plugin.getConfig().getString("messages.remove_no_cooldown", "");
        this.removedCooldown = plugin.getConfig().getString("messages.remove_cooldown", "");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /cooldown get                       | ominouswither.cooldown_get_self
        // /cooldown get <player>              | ominouswither.cooldown_get_others
        // /cooldown set <player> <seconds>    | ominouswither.cooldown_modify
        // /cooldown remove <player>           | ominouswither.cooldown_modify

        if (!command.getName().equalsIgnoreCase("cooldown")) return false;

        if (args.length < 1) {
            sender.sendMessage(Utils.formatMessage(this.missingSubcommand));
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
                sender.sendMessage(Utils.formatMessage(this.invalidSubcommand));
                return false;
        }
    }

    private boolean subcommandGet(CommandSender sender, Command command, String label, String[] args) {
        //Self subcommand
        if (args.length < 2) {
            if (!this.globalAllowViewOwnCooldown && !sender.hasPermission("ominouswither.cooldown_get_self")) {
                sender.sendMessage(Utils.formatMessage(this.insufficientPermissionsCommand));
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(Utils.formatMessage(this.consolePlayerRequired));
                return false;
            }
            
            final Player player = (Player) sender;
            final Duration duration = this.cooldownManager.getRemainingCooldown(player);

            player.sendMessage(
                duration.equals(Duration.ZERO) ?
                Utils.formatMessage(this.getSelfNoCooldown) :
                Utils.formatMessage(this.getSelfCooldown, duration.toSeconds())
            );
            return true;
        }

        //Others subcommand
        if (!sender.hasPermission("ominouswither.cooldown_get_others")) {
            sender.sendMessage(Utils.formatMessage(this.insufficientPermissionsSubcommand));
            return true;
        }
        
        final Player target = this.plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Utils.formatMessage(this.invalidPlayer));
            return false;
        }

        final Duration duration = this.cooldownManager.getRemainingCooldown(target);

        sender.sendMessage(
            duration.equals(Duration.ZERO) ?
            Utils.formatMessage(this.getOtherNoCooldown, target.getName()) :
            Utils.formatMessage(this.getOtherCooldown, target.getName(), duration.toSeconds())
        );
        return true;
    }

    private boolean subcommandSet(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ominouswither.cooldown_modify")) {
            sender.sendMessage(Utils.formatMessage(this.insufficientPermissionsSubcommand));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Utils.formatMessage(this.missingPlayerOrDuration));
            return false;
        }

        final Player target = this.plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Utils.formatMessage(this.invalidPlayer));
            return false;
        }

        int duration;
        try {
            duration = Integer.parseInt(args[2]);
        }
        catch (NumberFormatException e) {
            sender.sendMessage(Utils.formatMessage(this.invalidDurationType));
            return false;
        }

        if (duration < 0) {
            sender.sendMessage(Utils.formatMessage(this.invalidDurationValue));
            return false;
        }

        this.cooldownManager.setCooldown(target, Duration.ofSeconds(duration));
        sender.sendMessage(Utils.formatMessage(this.setCooldown, target.getName(), duration));
        return true;
    }

    private boolean subcommandRemove(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ominouswither.cooldown_modify")) {
            sender.sendMessage(Utils.formatMessage(this.insufficientPermissionsSubcommand));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Utils.formatMessage(this.missingPlayer));
            return false;
        }

        final Player target = this.plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Utils.formatMessage(this.invalidPlayer));
            return false;
        }

        final Instant instant = this.cooldownManager.removeCooldown(target);

        sender.sendMessage(instant != null ?
            Utils.formatMessage(this.removedCooldown, Duration.between(Instant.now(), instant).toSeconds(), target.getName()) :
            Utils.formatMessage(this.removedNoCooldown, target.getName())
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
