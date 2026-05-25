---
title: Item Rarity
sidebar_position: 1
---

## How to add rarities
Each rarity is its own config file, placed in the `/rarities/` folder, and you can add or remove them as you please. There's an example config called `_example.yml` to help you out!

The ID of the rarity is the file name. This is what you use in the EcoItem configs, read here for more: [How to make an Item](https://plugins.auxilor.io/ecoitems/how-to-make-a-custom-item).
ID's must be lowercase letters, numbers, and underscores only.

Rarities in EcoItems are a system for categorizing items based on their rarity level. Each rarity is defined by a specific tag in lore to show players which items are harder to obtain and/or which loot items are better.

You can find more options for rarities in [config.yml](https://github.com/Auxilor/EcoItems/blob/master/eco-core/core-plugin/src/main/resources/config.yml).

## Example Rarity Config

```yaml
# The lore added to items with this rarity
lore:
  - "&a&lCOMMON"

# The weight of the rarity. Higher weights take precedence over lower weights,
# so if an item has multiple rarities, the one with the highest weight will be used.
weight: 1

# The items that have this rarity
# Read here: https://plugins.auxilor.io/the-item-lookup-system
# EcoItems items should specify the rarity in their config rather than here
items:
  - diamond
```

## Understanding all the sections

```yaml
# The lore added to items with this rarity
lore:
  - "&a&lCOMMON"

# The weight of the rarity. Higher weights take precedence over lower weights,
# so if an item has multiple rarities, the one with the highest weight will be used.
weight: 1

# The items that have this rarity
# Read here: https://plugins.auxilor.io/the-item-lookup-system
# EcoItems items should specify the rarity in their config rather than here
items:
  - diamond
```