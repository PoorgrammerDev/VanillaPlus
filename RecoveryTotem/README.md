# [RecoveryTotem]()

A Totem of Recovery is a craftable and consumable item that provides a one-time keepInventory on death.

Upon death, one Totem of Recovery will be destroyed, but the player will respawn with all of their other items and experience points.

Unlike the Totem of Undying, this Totem does not need be held in the mainhand or offhand at the time of death - simply anywhere in the inventory will work*.

Due to its very powerful nature, this item requires unrenewable ingredients from Ancient Cities to craft.

## Features
- Unlockable recipes
- Visual and sound effects for Totem activation
- No permissions management required**
- Admin command to give players totems (/giverecoverytotem)
- Can be easily retextured using CustomModelData -- values are editable in the config to prevent conflicts

## Recipe
<<< TODO: ADD IMAGE >>>

## Installation
1. Simply drop the JAR file into your plugins folder and you should be good to go! There are no dependencies you have to install.

2. Verify that the plugin was added properly with /plugins (optional)

## Permissions
To use the `/giverecoverytotem` command, the player must have the permission node: `recoverytotem.giverecoverytotem`

## Resource Pack
There is a default resource pack available that adds a texture to this item: []()

It's not required for the plugin to function; however, without the resource pack, Totems of Undying will appear as Enchanted Books.

## Footnotes

*All slots work except for the 2x2 crafting grid or being moved around on the cursor with the inventory open, as these slots are not considered part of the inventory.

**All players can craft and use totems with no permissions required. Only the give command requires permission.