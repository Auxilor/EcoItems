---
title: "Commands and Permissions"
sidebar_position: 5
---

Every EcoItems command and the permission node it needs.

| Command | Description | Permission |
| --- | --- | --- |
| `/ecoitems reload` | Reload the plugin configs | `ecoitems.command.reload` |
| `/ecoitems give <player> <item> [amount]` | Give an EcoItem to a player | `ecoitems.command.give` |

EcoItems items are also available through the eco item lookup system, so any plugin that accepts item lookup strings (crates, shops, GUIs) can use `ecoitems:<id>` directly.

<hr/>

## Where to go next

- **Make an item:** [How to make an Item](how-to-make-a-custom-item/how-to-make-a-custom-item) to create the items you give.
- **Recipes:** [Additional Recipes](additional-configuration-options/additional-recipes) for standalone recipe configs and the per-item `crafting-permission` option.
