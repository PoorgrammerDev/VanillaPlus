# RecoveryTotem

A Totem of Recovery is a craftable and consumable item that provides a one-time keepInventory on death. The player will respawn with all of their items and experience points, except for the used totem itself.

Unlike the Totem of Undying, this Totem does not need be held in the mainhand or offhand - simply anywhere in the inventory will work*.

## Features
- Unlockable recipes
- No permissions management required**
- Admin command to give players totems (/giverecoverytotem)
- Can be easily retextured using CustomModelData -- values are editable in the config to prevent conflicts

## Recipe
![Recipe for crafting a Totem of Recovery. Surround a Totem of Undying in 8 Echo Shards to craft.](https://raw.githubusercontent.com/PoorgrammerDev/VanillaPlus/media/totem-recipe.png)

## Installation
1. Simply drop the JAR file into your plugins folder and you should be good to go! There are no dependencies you have to install.

2. Verify that the plugin was added properly with /plugins (optional)

## Permissions
To use the `/giverecoverytotem` command, the player must have the permission node: `recoverytotem.giverecoverytotem`

## Resource Pack
There is a default resource pack available [here](https://github.com/PoorgrammerDev/VanillaPlus/releases/download/recoverytotem-v1.0.0/recovery-totem-v1.0.0.zip).

It's not required for the plugin to function; however, without the resource pack, the Totem of Recovery will appear as an Enchanted Book.

## Published To
- [Spigot](https://www.spigotmc.org/resources/recovery-totem.119239/)
- [Paper](https://hangar.papermc.io/FullPotato/RecoveryTotem)
- [Modrinth](https://modrinth.com/plugin/recovery-totem)

## Footnotes
*All slots work except for the 2x2 crafting grid or being moved around on the cursor with the inventory open, as these slots are not considered part of the inventory.

**All players can craft and use totems with no permissions required. Only the give command requires permission.