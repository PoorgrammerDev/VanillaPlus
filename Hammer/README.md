# Hammer

A hammer is a tool that breaks blocks in a 3x3 plane. It is a special type of pickaxe, so it is used on pickaxe-breakable blocks.

There is a hammer for every tool tier: wooden, stone, iron, golden, diamond, and netherite.

## Features
- Unlockable recipes
- Craftable, repairable, enchantable, and smith-able
- Respects Unbreaking, Fortune, and Silk Touch enchantments
- Displays breaking animation for all affected blocks
- Configurable gameplay values (e.g. hunger rate, block hardness range)
- Toggleable system to limit Efficiency level on Hammers for game balance (disabled by default)
- No permissions management required*
- Admin command to give players hammers (/givehammer)
- Can be easily retextured using CustomModelData -- values are editable in the config to prevent conflicts

## Recipe
![Recipe for crafting a hammer tool.](https://raw.githubusercontent.com/PoorgrammerDev/VanillaPlus/media/hammer-crafting.png)

The recipe shown above is for the diamond hammer, but the pattern is the same for every tier of hammers except Netherite.

Netherite hammers are upgraded from diamond hammers, just like Vanilla tools.

## Published To
- [Spigot](https://www.spigotmc.org/resources/hammer.119392/)
- [Paper](https://hangar.papermc.io/FullPotato/Hammer)
- [Modrinth](https://modrinth.com/plugin/craftablehammers)

## Installation
1. Simply drop the JAR file into your plugins folder and you should be good to go! There are no dependencies you have to install.

2. Verify that the plugin was added properly with /plugins (optional)

## Permissions
To use the `/givehammer` command, the player must have the permission node: `hammer.givehammer`

## Footnotes

This plugin is inspired by but in no way affiliated with the Tinker's Construct mod or its creator(s).

*All players can craft and use hammers with no permissions required. Only the give command requires permission.