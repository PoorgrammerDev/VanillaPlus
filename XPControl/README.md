# XPControl

XPControl is a plugin that allows the server owner to configure how experience points are kept, dropped, and lost on death.

There is also a feature to allow players to store experience points into Bottle o' Enchanting items at a configurable loss percentage.

Both of these features can be enabled or disabled independently of each other.

## Points vs. Levels
This plugin's calculations are done in experience points, not levels.

For example, if the config is set to drop 50% of EXP and keep 50% of EXP on death, a player that died with exactly 30 levels (no more, no less) will have 1395 EXP points at the time of death.

697.5 EXP points will be kept by the player on respawn, and 697.5 EXP points will be dropped where they died. This equates to approx. 21 levels worth of EXP on respawn, and picking up the rest will get them back to level 30.

This may seem strange as half of 30 levels would be 15 levels intuitively, but since the amount of EXP required to progress to the next level is not constant in Minecraft, level 21 is the approximate actual midway point between level 0 and level 30.

## EXP Storage

Right clicking an Enchanting Table while holding a Bottle o' Enchanting in the main hand will take one level from the player and store that amount of EXP points into the bottle, minus the set penalty percentage.

There is a configurable limit to how much EXP can be stored into one bottle.

## Installation
1. Simply drop the JAR file into your plugins folder and you should be good to go! There are no dependencies you have to install.

2. Verify that the plugin was added properly with /plugins (optional)

## Permissions
There is a give command `/givexpbottle` for admins to give players stored EXP bottles. This requires the permission node `xpcontrol.givexpbottle`.
