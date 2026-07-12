---
title: "Custom Foods"
sidebar_position: 2
---

A custom food is a normal EcoItem with the vanilla **food** and **consumable** components that make it edible: you set its **nutrition**, **eat time**, and any **potion effects** it grants. This page covers adding that food behaviour on top of a standard item.

## Quick start

1. Open the `/plugins/EcoItems/items/` folder.
2. Copy `_example_food.yml` and rename it to your food's ID, e.g. `enchanted_steak.yml`.
3. Set up the `item:` section as you would for any item (see [How to make an Item](how-to-make-a-custom-item)).
4. Add the `minecraft:food` and `minecraft:consumable` components.
5. Run `/ecoitems reload`.
6. Give yourself the item with `/ecoitems give <you> enchanted_steak` and eat it to confirm the hunger and effects apply.

:::tip
`_example_food.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real food. You can also organise foods into subfolders inside `items/`, and they'll still load.
:::

## Naming and IDs

Foods live in the same `items/` folder as every other EcoItem, so the file name without `.yml` is the item's ID, used in commands and in the [Item Lookup System](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system).

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the food will not load.
:::

## The structure of a food

| Part | What it controls |
| --- | --- |
| **Item** | The base item, display, and recipe, identical to any EcoItem |
| **`minecraft:food`** | Nutrition, saturation, and whether it can always be eaten |
| **`minecraft:consumable`** | Eat time and effects granted on eating |

```yaml
# === Item: a normal EcoItem ===
item:
  item: cooked_beef glint item_name:"Enchanted Steak" # The base item
  lore: [] # The item lore

  # === The eating behaviour, as vanilla components ===
  components:
    # Read here: https://minecraft.wiki/w/Data_component_format#food
    "minecraft:food":
      nutrition: 12 # Hunger restored: https://minecraft.wiki/w/Hunger
      saturation: 2 # Saturation restored
      can_always_eat: false # Optional; if it can be eaten on a full hunger bar

    # Read here: https://minecraft.wiki/w/Data_component_format#consumable
    "minecraft:consumable":
      consume_seconds: 1 # Time in seconds it takes to eat
      on_consume_effects: # Optional; effects applied on eating
        - type: "minecraft:apply_effects"
          effects:
            - id: "minecraft:regeneration" # The potion effect ID
              duration: 40 # Duration in ticks
              amplifier: 0 # The effect level, starting at 0
          probability: 1.0 # Chance the effects are applied, 0.0 to 1.0

  craftable: true # If the item can be crafted
  recipe:
    - ""
    - cooked_beef 64
    - ""
    - cooked_beef 64
    - cooked_beef 64
    - cooked_beef 64
    - ""
    - cooked_beef 64
    - ""
```

### Item

The `item:` section is exactly the same as any other EcoItem. See [How to make an Item](how-to-make-a-custom-item) for the full breakdown of the base item, display, and recipe fields. A food is still a full EcoItem, so the top-level `effects:` and `conditions:` work too; a `consume` trigger pairs naturally with foods:

```yaml
effects:
  - id: give_xp
    args:
      amount: 10
    triggers:
      - consume
```

### The components

`minecraft:food` and `minecraft:consumable` are the vanilla components that control eating — the same ones vanilla items use, with every field documented in the [Data component format](https://minecraft.wiki/w/Data_component_format#food). The consumable component also supports `animation`, `sound`, teleport effects (chorus fruit style), and more.

:::info Food changes apply to new items only
Editing components does not update foods already in player inventories; only newly given or crafted copies pick up the change.
:::

:::tip Troubleshooting
- **Can't eat the item?** Check the console for component warnings on reload; a typo in a field name or value will report exactly what's wrong.
- **Potion effects not applying?** Confirm the effect `id` is a valid potion effect and `probability` is high enough.
- **Hunger doesn't change after editing?** Get a fresh copy; existing foods keep their old behaviour.
:::

<hr/>

## Where to go next

- **The base item:** [How to make an Item](how-to-make-a-custom-item) for the shared item, display, and recipe fields.
- **Custom tools:** [Custom Tools](custom-tools) for mining behaviour instead of eating.
