---
title: "Custom Tools"
sidebar_position: 3
---

A custom tool is a normal EcoItem with an added **tool** section that controls mining: you set its **mining speed**, **durability cost**, and per-block **rules**. This page covers adding that tool behaviour on top of a standard item.

## Quick start

1. Open the `/plugins/EcoItems/items/` folder.
2. Copy `_example.yml` and rename it to your tool's ID, e.g. `hardened_netherite_pickaxe.yml`.
3. Set up the `item:` section as you would for any item (see [How to make an Item](how-to-make-a-custom-item)).
4. Add a `tool:` section with the mining speed and any block rules.
5. Run `/ecoitems reload`.
6. Give yourself the item with `/ecoitems give <you> hardened_netherite_pickaxe` and mine a block to confirm the speed and drops behave as set.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real tool. You can also organise tools into subfolders inside `items/`, and they'll still load.
:::

## Naming and IDs

Tools live in the same `items/` folder as every other EcoItem, so the file name without `.yml` is the item's ID, used in commands and in the [Item Lookup System](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system).

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the tool will not load.
:::

## The structure of a tool

| Part | What it controls |
| --- | --- |
| **Item** | The base item, display, and recipe, identical to any EcoItem |
| **Tool** | The mining behaviour: speed, durability cost, and block rules |

```yaml
# === Item: a normal EcoItem ===
item:
  item: netherite_pickaxe glint max_damage:4096 item_name:"Hardened Netherite Pickaxe" # The base item
  lore: [] # The item lore
  craftable: true # If the item can be crafted
  crafting-permission: ecoitems.craft.custom_item # Optional; permission required to craft the item
  recipe-give-amount: 1 # Optional; how many items the recipe gives, defaults to 1
  recipe: # The recipe: https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system/recipes
    - netherite_ingot
    - netherite_ingot
    - netherite_ingot
    - netherite_ingot
    - netherite_pickaxe
    - netherite_ingot
    - netherite_ingot
    - netherite_ingot
    - netherite_ingot

# === Tool: the mining behaviour ===
tool:
  mining-speed: 1.0 # Default mining speed, used when no rule matches
  damage-per-block: 1 # Durability removed each time a block is mined
  rules: # Optional; per-block overrides, first match wins
    - blocks:
        - "#mineable_pickaxe" # Block names or tags; tags start with #
      speed: 45.8 # Mining speed for these blocks
    - blocks:
        - "#incorrect_for_netherite_tool"
      speed: 1
      drops: false # Optional; if mined blocks drop items with this tool
```

### Item

The `item:` section is exactly the same as any other EcoItem. See [How to make an Item](how-to-make-a-custom-item) for the full breakdown of the base item, display, and recipe fields. A tool is still a full EcoItem, so the top-level `effects:` and `conditions:` work too, e.g. running an effect on a `mine_block` trigger alongside the `tool:` mining rules.

### Tool

The `tool:` section turns the item into a mining tool. Block lists accept material names or tags; a tag starts with `#`. Material names are listed in the Bukkit `Material` javadoc and tags in the Bukkit `Tag` javadoc.

```yaml
tool:
  mining-speed: 1.0 # Default mining speed, used when no rule matches
  damage-per-block: 1 # Durability removed each time a block is mined
  rules: # Optional; per-block overrides, first match wins
    - blocks:
        - "#mineable_pickaxe" # Block names or tags; tags start with #
      speed: 45.8 # Mining speed for these blocks
    - blocks:
        - "#incorrect_for_netherite_tool"
      speed: 1
      drops: false # Optional; if mined blocks drop items with this tool
```

:::info Tool changes apply to new items only
Editing the `tool:` section does not update tools already in player inventories; only newly given or crafted copies pick up the change.
:::

:::tip Troubleshooting
- **Mining speed feels wrong?** A higher `speed` is faster; the `mining-speed` default applies only when no rule matches the block.
- **Blocks not dropping?** A matching rule with `drops: false` suppresses drops; remove it or set `drops: true`.
- **Rule not matching?** Tags must start with `#`; otherwise use exact material names.
:::

<hr/>

## Where to go next

- **The base item:** [How to make an Item](how-to-make-a-custom-item) for the shared item, display, and recipe fields.
- **Custom foods:** [Custom Foods](custom-foods) for eating behaviour instead of mining.