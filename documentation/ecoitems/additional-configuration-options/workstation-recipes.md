---
title: "Workstation Recipes"
sidebar_position: 2
---

An item's recipe goes in an `item.recipes` section. Set `type` in there to choose where it's made - a crafting table by default, or a furnace, smithing table, stonecutter, anvil, or brewing stand.

The recipe formats themselves are shared across eco plugins and documented in full on the [Recipes](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-recipes) page. This page covers how EcoItems uses them.

## Quick start

1. Open your item's config in `/plugins/EcoItems/items/`.
2. Add a `recipes:` section inside `item:`.
3. Set `type:` and that workstation's ingredient keys - for a furnace, just `input`.
4. Run `/ecoitems reload`.
5. Put the ingredient into the workstation in-game and confirm your item comes out.

```yaml
item:
  item: iron_ingot
  name: "&bPurified Iron"
  recipes:
    type: furnace
    input: raw_iron
    experience: 0.7
    cook-time: 200
```

:::tip
An item still only has **one** recipe. Setting `type` changes where that recipe is made, it doesn't add a second one.
:::

## The workstations

Everything goes in the item's `item.recipes` section, and the result is always the item itself. Ingredients are [Item Lookup](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system) strings, so EcoItems, other eco plugins, and vanilla items all work.

| `type` | Ingredient keys | Extra keys |
| --- | --- | --- |
| `crafting_table` *(default)* | `recipe` (9 slots) | `shapeless` |
| `furnace` | `input` | `experience`, `cook-time` |
| `blast_furnace` | `input` | `experience`, `cook-time` |
| `smoker` | `input` | `experience`, `cook-time` |
| `campfire` | `input` | `experience`, `cook-time` |
| `stonecutter` | `input` | |
| `smithing_table` | `template`, `base`, `addition` | |
| `anvil` | `base`, `material` *(optional)* | `repair-cost` |
| `brewing_stand` | `base`, `ingredient` | `brew-time` |

`give-amount` and `permission` work on every type. What each key does is covered on the [Recipes](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-recipes) page.

```yaml
# Crafting table - the default, so type can be left out
item:
  recipes:
    type: crafting_table
    shapeless: false # Optional; defaults to false
    permission: ecoitems.craft.mithril_sword # Optional
    give-amount: 1 # Optional; defaults to 1
    recipe: # Read left to right, top to bottom; "" is an empty slot
      - ""
      - ecoitems:mithril 2
      - ""
      - ""
      - ecoitems:mithril 2
      - ""
      - ""
      - stick
      - ""
```

```yaml
# Furnace, blast furnace, smoker, or campfire
item:
  recipes:
    type: blast_furnace
    input: raw_iron
    experience: 0.7 # Optional; XP dropped, defaults to 0
    cook-time: 100 # Optional; in ticks, defaults to the vanilla speed
```

```yaml
# Stonecutter
item:
  recipes:
    type: stonecutter
    input: ecoitems:marble_block
```

```yaml
# Smithing table - all three slots are required
item:
  recipes:
    type: smithing_table
    template: netherite_upgrade_smithing_template
    base: diamond_sword
    addition: ecoitems:mithril
```

```yaml
# Anvil - material is optional
item:
  recipes:
    type: anvil
    base: diamond_sword
    material: ecoitems:mithril # Optional
    repair-cost: 5 # Optional; in levels, defaults to 1
```

```yaml
# Brewing stand
item:
  recipes:
    type: brewing_stand
    base: potion
    ingredient: ecoitems:mithril
    brew-time: 400 # Optional; in ticks, defaults to 400
```

## Permissions

`permission` stops players without that node from taking the item out of the workstation:

```yaml
item:
  recipes:
    type: furnace
    input: raw_iron
    permission: ecoitems.craft.purified_iron
```

:::warning Campfires ignore permissions
A campfire drops its result on the ground rather than into a result slot, so there is no way to tell who it belongs to. `permission` has no effect on a `campfire` recipe.
:::

:::info Hoppers
Permissions apply to players. A hopper pulling an item out of a furnace or brewing stand isn't a player, so it isn't checked.
:::

:::tip Troubleshooting
- **Recipe not working?** Check the console on reload; a bad ingredient lookup skips the whole recipe and logs a warning.
- **Nothing happens at the workstation?** Check `type` is spelled as in the table above - an unknown value is logged and skipped.
- **Wrong workstation?** Villager trading and grindstones are [EcoCrafting](https://hub.auxilor.io/wiki/ecocrafting/how-to-make-a-recipe) features, not EcoItems ones.
:::

<hr/>

## Where to go next

- **Recipe formats:** [Recipes](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-recipes) for the full reference on every workstation and option.
- **Make an item:** [How to make an Item](../how-to-make-a-custom-item/how-to-make-a-custom-item) for everything else in the `item:` section.
