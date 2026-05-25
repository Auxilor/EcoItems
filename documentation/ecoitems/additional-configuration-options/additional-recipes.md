---
title: Additional Recipes
sidebar_position: 2
---

## How to add additional recipes

With EcoItems, you can create additional recipes for your items.

You can do this for other EcoItems, Vanilla Items, items from other EcoPlugins, or items from other plugins. See [Item Lookup System](https://plugins.auxilor.io/the-item-lookup-system) for more info on this.

Each additional recipe is its own config file, placed in the `/recipes/` folder, and you can add or remove them as you please. There's an example config called `_example.yml` to help you out!

The ID of the recipe is the file name. These currently do not have function, just make sure they're unique.
ID's must be lowercase letters, numbers, and underscores only.

## Example Recipe Config
```yaml
result: ecoitems:enchanted_emerald 9

recipe:
  - ""
  - emerald_block 32
  - ""
  - emerald_block 32
  - emerald_block 32
  - emerald_block 32
  - ""
  - emerald_block 32
  - ""

permission: "ecoitems.craft.enchanted_emerald_block_craft"
```
You can read more about recipes here: [Crafting Recipes](https://plugins.auxilor.io/the-item-lookup-system/recipes)
