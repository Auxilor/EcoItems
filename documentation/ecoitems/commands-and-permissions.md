---
title: "Commands and Permissions"
sidebar_position: 5
---

Every EcoItems command and the permission node it needs.

| Command                                   | Description                                                              | Permission                               |
|-------------------------------------------|--------------------------------------------------------------------------|------------------------------------------|
| `/ecoitems reload`                        | Reload the plugin configs                                                | `ecoitems.command.reload`                |
| `/ecoitems give <player> <item> [amount]` | Give an EcoItem to a player                                              | `ecoitems.command.give`                  |
| `/ecoitems gui`                           | Opens the operator item browser. Left-click an item to receive one copy. | `ecoitems.command.gui` (operators)       |
| `/ecoitems hud toggle <id>`               | Toggle a [HUD](huds/index) on or off                                     | `ecoitems.command.hud.toggle` (everyone) |
| `/ecoitems glyph <id>`                    | Echo a [glyph](glyphs/index)'s character, e.g. to copy it from console   | `ecoitems.command.glyph` (operators)     |
| `/ecoitems glyphs`                        | Open the [glyph picker](glyphs/index) book (click a glyph to chat it)   | `ecoitems.command.glyphs` (everyone)     |
| `/ecoitems totem <item> [player]`         | Play a totem-of-undying animation showing an EcoItem's model            | `ecoitems.command.totem` (operators)     |
| `/ecoitems drop <item> <player\|x y z world> [amount]` | Drop an EcoItem at a player or coordinates                  | `ecoitems.command.drop` (operators)      |
| `/ecoitems take <player> <item> [amount]` | Remove an EcoItem from a player's inventory                              | `ecoitems.command.take` (operators)      |
| `/ecoitems rename <name>`                 | Rename the held item (colors and glyphs work)                            | `ecoitems.command.rename` (operators)    |
| `/ecoitems repair`                        | Fully repair the held item                                               | `ecoitems.command.repair` (operators)    |
| `/ecoitems durability <remaining>`        | Set the held item's remaining durability                                 | `ecoitems.command.durability` (operators)|
| `/ecoitems hitbox`                        | Outline nearby furniture barriers and hitboxes with particles            | `ecoitems.command.hitbox` (operators)    |
| `/ecoitems dialog <id> [player]`          | Open a [dialog](dialogs/index) screen                                    | `ecoitems.command.dialog` (operators)    |

`ecoitems.command.gui` defaults to operators and is included in `ecoitems.command.*`.

`e`, `ei`, `ecoi`, and `items` all work as aliases of `/ecoitems` (e.g. `/e reload` will also reload plugin configs)

[HUDs](huds/index) with a `permission:` in their config additionally require that permission to be seen at all.

EcoItems items are also available through the eco item lookup system, so any plugin that accepts item lookup strings (
crates, shops, GUIs) can use `ecoitems:<id>` directly.

[Glyphs](glyphs/index) with a `permission:` in their config (like the shipped chat tags, `ecoitems.glyph.<id>`) require
that permission to be used in chat and on signs.

<hr/>

## Where to go next

- **Make an item:** [How to make an Item](how-to-make-a-custom-item/how-to-make-a-custom-item) to create the items you
  give.
- **Recipes:** [Workstation Recipes](additional-configuration-options/workstation-recipes) for the per-recipe
  `permission` option.
