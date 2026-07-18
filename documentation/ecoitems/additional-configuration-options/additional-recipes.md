---
title: "Additional Recipes"
sidebar_position: 2
---

An additional recipe lets you craft any item, not just EcoItems, through a config file: you set the **result**, the **recipe** grid, and an optional crafting **permission**. This page covers adding one of these standalone recipes.

## Quick start

1. Open the `/plugins/EcoItems/recipes/` folder.
2. Copy `_example.yml` and rename it to your recipe's ID, e.g. `enchanted_emerald_block_craft.yml`.
3. Set the `result` to the item you want to craft and fill in the `recipe` grid.
4. Run `/ecoitems reload`.
5. Open a crafting table, lay out the recipe, and confirm the result appears.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real recipe. You can also organise recipes into subfolders inside `recipes/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the recipe's ID. IDs currently have no in-game function, so just keep them unique. The items you reference are resolved through the [Item Lookup System](https://plugins.auxilor.io/the-item-lookup-system), which also covers EcoItems, other eco plugins, and vanilla items.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the recipe will not load.
:::

## The structure of a recipe

| Part | What it controls |
| --- | --- |
| **Result** | The item, and amount, the recipe produces |
| **Recipe** | The crafting grid of ingredients |
| **Permission** | An optional permission required to craft it |

```yaml
# === Result: what you get ===
result: ecoitems:enchanted_emerald 9 # The item to give, with an optional amount

# === Permission: who can craft it ===
permission: "ecoitems.craft.enchanted_emerald_block_craft" # Optional; permission required to craft this recipe

# === Recipe: the crafting grid ===
shapeless: false # Optional; whether the recipe is shapeless, defaults to false
recipe: # Ingredients: https://plugins.auxilor.io/the-item-lookup-system/recipes
  - ""
  - emerald_block 32
  - ""
  - emerald_block 32
  - emerald_block 32
  - emerald_block 32
  - ""
  - emerald_block 32
  - ""
```

### Result

`result` is the item the recipe produces, looked up through the Item Lookup System, with an optional amount after a space.

```yaml
result: ecoitems:enchanted_emerald 9 # The item to give, with an optional amount
```

### Recipe

The `recipe` list is the crafting grid, read left to right, top to bottom. Each entry is an ingredient, optionally with an amount, and `""` marks an empty slot. Set `shapeless: true` to ignore slot positions and match on the ingredients alone.

```yaml
shapeless: false # Optional; whether the recipe is shapeless, defaults to false
recipe: # Ingredients: https://plugins.auxilor.io/the-item-lookup-system/recipes
  - ""
  - emerald_block 32
  - ""
  - emerald_block 32
  - emerald_block 32
  - emerald_block 32
  - ""
  - emerald_block 32
  - ""
```

:::tip
EcoItems supports both shaped and shapeless recipes. See [Recipes](https://plugins.auxilor.io/the-item-lookup-system/recipes) for the full format.
:::

### Permission

`permission` is optional; set it to require a permission node before a player can craft the recipe.

```yaml
permission: "ecoitems.craft.enchanted_emerald_block_craft" # Optional; permission required to craft this recipe
```

:::tip Troubleshooting
- **Recipe not crafting?** Check the ingredient and result IDs resolve in the Item Lookup System.
- **Recipe won't load?** Check the ID rules above; the file name must be lowercase with no spaces or hyphens.
- **Players can't craft it?** A `permission` is set; grant it or remove the line.
:::

<hr/>

## Where to go next

- **Recipe format:** [Recipes](https://plugins.auxilor.io/the-item-lookup-system/recipes) for shaped, shapeless, and ingredient syntax.
- **Make an item:** [How to make an Item](../how-to-make-a-custom-item/how-to-make-a-custom-item) to craft your own EcoItems directly.