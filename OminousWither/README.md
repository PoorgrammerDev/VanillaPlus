# [OminousWither]()

Adds a thrilling boss fight experience, summoned by building the Wither with Bad Omen.

There are 5 different levels of Ominous Withers, one for each level of Bad Omen.

## Features
- Handcrafted boss fight experience complete with custom moveset and two distinct phases
- Extremely customizable - every boss stat can be tweaked to your liking
- Support for custom loot tables to drop on the Ominous Wither's death
- Toggleable cooldown system to prevent spam-spawning Ominous Withers
- Survival friendly: no permissions management required for basic use
- Support for translation - every message sent to players can be customized

## Installation
1. Simply drop the JAR file into your plugins folder and you should be good to go! There are no dependencies you have to install.

2. Verify that the plugin was added properly with /plugins (optional)

## Cooldown System
The cooldown system is disabled by default, but can be enabled in the config by setting the duration to any positive integer.

With the system enabled, players who successfully build an Ominous Wither will be placed in cooldown for the defined number of seconds.

Once the Ominous Wither that they had spawned is killed, their cooldown will be immediately reset. This mechanic allows normal gameplay to continue unimpeded, but impedes spamming with intent to grief.

By default, all players can see their own cooldown by either attempting to spawn an Ominous Wither or doing `/cooldown get` without any required permissions.

However, if you want to restrict access to this command, you can set the config setting `global_allow_view_own_cooldown` to `false`. This will require players to have the permission node `ominouswither.cooldown_get_self` to use this command.

By default, players in Creative Mode will bypass cooldowns. Ominous Withers built in Creative Mode will neither check nor apply a cooldown.

However, if you also want to subject Creative Mode players to cooldowns, you can set the config setting `global_creative_bypass` to `false`. This will require players to have the permission node `ominouswither.creative_bypass_spawn_cooldown` to bypass cooldowns in Creative Mode. Please note that players with this permission are still subject to cooldowns in Survival Mode.

## Commands
`/summonominouswither <x> <y> <z> <level>`
 - Summons an Ominous Wither of the desired level at the desired location
 - Supports relative (`~`) coordinates 
 - Alias: `spawnominouswither`
 - Requires permission node ``

`/cooldown get`
 - View your own cooldown status
 - See `ominouswither.cooldown_get_self` under the Permissions section for permission details

`/cooldown get <player>`
 - View a player's cooldown status
 - Requires permission node `ominouswither.cooldown_get_others`

`/cooldown set <player> <duration>`
 - Set a player's cooldown status
 - Requires permission node `ominouswither.cooldown_modify`

`/cooldown remove <player>`
 - Remove a player's cooldown, if present 
 - Requires permission node `ominouswither.cooldown_modify`

## Permissions
`ominouswither.creative_bypass_spawn_cooldown`
 - If the config setting `global_creative_bypass` is set to `false`, this permission node is required for Creative Mode players to bypass the cooldown system
 - Players are still subject the cooldown system in Survival Mode, regardless of this permission
 - The config setting above is set to `true` by default.

`ominouswither.summonominouswither`
 - Allows a player to use a command to summon Ominous Withers

`ominouswither.cooldown_get_self`
- If the config setting `global_allow_view_own_cooldown` is set to `false`, this permission node is required for players to use the command `/cooldown get` to view their own cooldown status
- The config setting above is set to `true` by default.

`ominouswither.cooldown_get_others`
- Allow players to view any player's cooldown status via a command

`ominouswither.cooldown_modify`
- Allow players to set or remove cooldown statuses via a command