---
title: "Commands and Permissions"
sidebar_position: 16
---

Every EcoItems command and permission node, for reloading, giving and taking items, editing held items, and opening HUDs, glyphs, and dialogs. All commands sit under `/ecoitems`.

| Command                                   | Description                                                              | Permission                               |
|-------------------------------------------|--------------------------------------------------------------------------|------------------------------------------|
| `/ecoitems reload`                        | Reload the plugin configs                                                | `ecoitems.command.reload`                |
| `/ecoitems give <player> <item> [amount]` | Give an EcoItem to a player                                              | `ecoitems.command.give`                  |
| `/ecoitems gui`                           | Opens the operator item browser. Left-click an item to receive one copy. | `ecoitems.command.gui` (operators)       |
| `/ecoitems hud toggle <id>`               | Toggle a [HUD](how-to-make-a-hud) on or off                                     | `ecoitems.command.hud.toggle` (everyone) |
| `/ecoitems glyph <id>`                    | Echo a [glyph](how-to-make-a-glyph)'s character, e.g. to copy it from console   | `ecoitems.command.glyph` (operators)     |
| `/ecoitems glyphs`                        | Open the [glyph picker](how-to-make-a-glyph#the-glyph-picker) book (click a glyph to chat it)   | `ecoitems.command.glyphs` (everyone)     |
| `/ecoitems totem <item> [player]`         | Play a totem-of-undying animation showing an EcoItem's model            | `ecoitems.command.totem` (operators)     |
| `/ecoitems drop <item> <player\|x y z world> [amount]` | Drop an EcoItem at a player or coordinates                  | `ecoitems.command.drop` (operators)      |
| `/ecoitems take <player> <item> [amount]` | Remove an EcoItem from a player's inventory                              | `ecoitems.command.take` (operators)      |
| `/ecoitems rename <name>`                 | Rename the held item (colors and glyphs work)                            | `ecoitems.command.rename` (operators)    |
| `/ecoitems repair`                        | Fully repair the held item                                               | `ecoitems.command.repair` (operators)    |
| `/ecoitems durability <remaining>`        | Set the held item's remaining durability                                 | `ecoitems.command.durability` (operators)|
| `/ecoitems hitbox`                        | Outline nearby furniture barriers and hitboxes with particles            | `ecoitems.command.hitbox` (operators)    |
| `/ecoitems dialog <id> [player]`          | Open a [dialog](how-to-make-a-dialog) screen                                    | `ecoitems.command.dialog` (operators)    |

`ecoitems.command.gui` defaults to operators and is included in `ecoitems.command.*`.

`e`, `ei`, `ecoi`, and `items` all work as aliases of `/ecoitems` (e.g. `/e reload` also reloads plugin configs).

[HUDs](how-to-make-a-hud) with a `permission:` in their config additionally require that permission to be seen at all.

EcoItems items are also available through the eco item lookup system, so any plugin that accepts item lookup strings (crates, shops, GUIs) can use `ecoitems:<id>` directly.

[Glyphs](how-to-make-a-glyph) with a `permission:` in their config require that permission to be used in chat and on signs.

<hr/>

## Where to go next

- **Make an item:** [How to Make an Item](how-to-make-an-item) to create the items you
  give.
- **Recipes:** [Workstation Recipes](workstation-recipes) for the per-recipe
  `permission` option.
