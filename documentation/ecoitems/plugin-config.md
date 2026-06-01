---
title: "Plugin Config"
sidebar_position: 6
---

This is the main plugin config, `config.yml`, found at `/plugins/EcoItems/config.yml`. It controls recipe discovery and the global rarity system. After changing anything here, run `/ecoitems reload` to apply it.

## Default config.yml

```yaml
discover-recipes: true # If all EcoItems recipes should be automatically unlocked for players

rarity:
  enabled: false # If the rarity system should be enabled
  blank-lore-line: true # If a blank lore line should separate the rarity tag from the item lore
  default: common # The rarity given to items with no rarity specified
  display-default: true # If items with no rarity should be shown with the default rarity
```

<hr/>

## Where to go next

- **Rarities:** [Item Rarity](additional-configuration-options/item-rarity) covers building the rarities the options above point to.
- **Make an item:** [How to make an Item](how-to-make-a-custom-item/how-to-make-a-custom-item) to start adding items.