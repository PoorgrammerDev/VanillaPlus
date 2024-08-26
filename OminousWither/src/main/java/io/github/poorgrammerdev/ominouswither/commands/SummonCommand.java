package io.github.poorgrammerdev.ominouswither.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.permissions.Permission;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherSpawnEvent;
import io.github.poorgrammerdev.ominouswither.internal.events.OminousWitherSpawnEvent.SpawnReason;
import io.github.poorgrammerdev.ominouswither.utils.Utils;
import net.md_5.bungee.api.ChatColor;

/**
 * Command to summon Ominous Withers
 * @author Thomas Tran
 */
public class SummonCommand implements CommandExecutor, TabCompleter {
    private static final int DEFAULT_INVULN_TICKS = 220;

    private final OminousWither plugin;

    public SummonCommand(OminousWither plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /summonominouswither <x> <y> <z> <level>

        if (!command.getName().equalsIgnoreCase("summonominouswither")) return false;
        if (!sender.hasPermission(new Permission("ominouswither.summonominouswither"))) return false;

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return false;
        }

        final Player player = (Player) sender;
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Too few arguments, must specify position and level.");
            return false;
        }

        //Parse coordinates
        final Location location = player.getLocation();

        try {
            location.setX(this.parseLocationArg(args[0], location.getX()));
            location.setY(this.parseLocationArg(args[1], location.getY()));
            location.setZ(this.parseLocationArg(args[2], location.getZ()));
        }
        catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
            return false;
        }
        
        //Parse level
        int level;
        try {
            level = Integer.parseInt(args[3]);
        }
        catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Level argument must be a valid integer.");
            return false;
        }

        //Validate level
        if (level <= 0 || level >= 6) {
            sender.sendMessage(ChatColor.RED + "Invalid level, must be in interval [1,5]");
            return false;
        }

        // --- All syntax is correct at this point ---

        final World world = player.getWorld();
        final Entity entity = world.spawnEntity(location, EntityType.WITHER);

        if (!(entity instanceof Wither)) {
            sender.sendMessage(ChatColor.RED + "An internal error occurred: could not cast summoned entity to Wither");
            this.plugin.getLogger().severe("SummonCommand failure: could not cast summoned entity to Wither");

            entity.remove();
            return true;
        }

        //Command success -> continue with summoning Wither
        final Wither wither = (Wither) entity;
        wither.setInvulnerabilityTicks(DEFAULT_INVULN_TICKS);
        this.plugin.getServer().getPluginManager().callEvent(new OminousWitherSpawnEvent(wither, player, level, SpawnReason.COMMAND));

        sender.sendMessage("Summoned new " + Utils.WITHER_NAME_COLOR + "Ominous Wither " + Utils.getLevelRomanNumeral(level) + ChatColor.RESET);
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        // /summonominouswither <x> <y> <z> <level>

        if (!command.getName().equalsIgnoreCase("summonominouswither")) return null;
        if (!sender.hasPermission(new Permission("ominouswither.summonominouswither"))) return null;
        if (!(sender instanceof Player)) return null;

        final Player player = (Player) sender;
        final ArrayList<String> ret = new ArrayList<>();

        //Handle positioning
        if (args.length >= 1 && args.length <= 3) {
            final Block block = player.getTargetBlockExact(10);

            //Relative positioning
            if (block == null || block.getType().isAir()) {
                if (args[args.length - 1].isEmpty()) {
                    ret.add("~");
                }
            }

            //Target block
            else {
                int val = 0;
                switch (args.length) {
                    case 1:
                        val = block.getX();
                        break;
                    case 2:
                        val = block.getY();
                        break;
                    case 3:
                        val = block.getZ();
                        break;
                }

                final String argStr = Integer.toString(val);
                if (argStr.startsWith(args[args.length - 1])) {
                    ret.add(argStr);
                }
            }
        }

        //Level
        if (args.length == 4) {
            for (int level = 1; level <= 5; ++level) {
                final String levelStr = Integer.toString(level);
                if (levelStr.startsWith(args[3])) {
                    ret.add(levelStr);
                }
            }
        }

        return ret;
    }

    /**
     * Parses a location string argument and applies relative addition if required
     * @param arg user input
     * @param baseValue base value (e.g. where the player is standing)
     * @return final parsed location value (if ~ was used, offset is already applied)
     * @throws IllegalArgumentException if user input is invalid
     */
    private double parseLocationArg(String arg, final double baseValue) throws IllegalArgumentException {
        if (arg == null || arg.length() <= 0) throw new IllegalArgumentException("Position argument cannot be null or empty.");
        
        final boolean isRelative = arg.charAt(0) == '~';
        
        //Relative coordinate
        if (isRelative) {
            //Remove tilde from string to continue with value parsing
            arg = arg.substring(1);
            
            //Special case: empty string after tilde -> no offset
            if (arg.isEmpty()) return baseValue;
        }

        double val;
        try {
            val = Double.parseDouble(arg);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Position argument must be a valid number.");
        }

        return (isRelative ? (baseValue + val) : val);
    }

}
