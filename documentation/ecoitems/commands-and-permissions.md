---
title: "Commands and Permissions"
sidebar_position: 5
---

Every EcoItems command and the permission node it needs.

| Command | Description | Permission |
| --- | --- | --- |
| `/ecoitems reload` | Reload the plugin configs | `ecoitems.command.reload` |
| `/ecoitems give <player> <item> [amount]` | Give an EcoItem to a player | `ecoitems.command.give` |
| `/ecoitems gui` | Opens the operator item browser. Left-click an item to receive one copy. | `ecoitems.command.gui` (operators) |
| `/ecoitems hud toggle <id>` | Toggle a [HUD](huds/index) on or off | `ecoitems.command.hud.toggle` (everyone) |

`ecoitems.command.gui` defaults to operators and is included in `ecoitems.command.*`.

[HUDs](huds/index) with a `permission:` in their config additionally require that permission to be seen at all.

EcoItems items are also available through the eco item lookup system, so any plugin that accepts item lookup strings (crates, shops, GUIs) can use `ecoitems:<id>` directly.

[Glyphs](glyphs/index) with a `permission:` in their config (like the shipped chat tags, `ecoitems.glyph.<id>`) require that permission to be used in chat and on signs.

<hr/>

## Where to go next

- **Make an item:** [How to make an Item](how-to-make-a-custom-item/how-to-make-a-custom-item) to create the items you give.
- **Recipes:** [Additional Recipes](additional-configuration-options/additional-recipes) for standalone recipe configs and the per-item `crafting-permission` option.
