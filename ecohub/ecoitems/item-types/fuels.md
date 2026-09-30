---
title: "Custom Fuels"
sidebar_position: 7
---

From 26.3, fuel is a vanilla component, so any EcoItem can be **furnace fuel**, **brewing stand fuel**, or **compostable**: you set how long it **burns**, how many **brews** it powers, how much it **speeds up** the workstation, and how much it fills a **composter**. This page covers adding that behaviour on top of a standard item, plus the older option for earlier versions.

:::info
The `cooking_fuel`, `brewing_fuel`, and `compostable` components need a **26.3** server or newer. On older versions, see [Before 26.3](#before-263).
:::

## Quick start

1. Open `/plugins/EcoItems/items/` and create your fuel's file, e.g. `star_coal.yml`.
2. Set up the `item:` section as you would for any item (see [How to Make an Item](how-to-make-an-item)).
3. Add the `minecraft:cooking_fuel` component with a `burn_time` and `speed_multiplier`.
4. Run `/ecoitems reload`.
5. Give yourself the item with `/ecoitems give <player> star_coal`, put it in a furnace's fuel slot, and smelt something to confirm it burns.

## The structure of a fuel

| Part | What it controls |
| --- | --- |
| **Item** | The base item, display, and recipe, identical to any EcoItem |
| **`minecraft:cooking_fuel`** | Burn time and cooking speed in furnaces, smokers, and blast furnaces |
| **`minecraft:brewing_fuel`** | Brews powered and brewing speed in a brewing stand |
| **`minecraft:compostable`** | Layers added to a composter |

```yaml
# === Item: a normal EcoItem ===
item:
  item: paper # Any base item works
  name: "&eStar Coal"
  texture: item/star_coal # Paid version

  # === The fuel behaviour, as vanilla components ===
  components:
    # Read here: https://minecraft.wiki/w/Data_component_format#cooking_fuel
    "minecraft:cooking_fuel":
      burn_time: 3200 # Ticks the furnace stays lit; coal is 1600
      speed_multiplier: 2.0 # Cooking speed while it burns; 2.0 halves cook time

    # Read here: https://minecraft.wiki/w/Data_component_format#brewing_fuel
    "minecraft:brewing_fuel":
      uses: 40 # Brews per item; blaze powder is 20
      speed_multiplier: 1.5 # Brewing speed; 1.5 turns the 20-second brew into about 13

    # Read here: https://minecraft.wiki/w/Data_component_format#compostable
    "minecraft:compostable":
      layers: 1 # Layers added to a composter per item
```

### Item

The `item:` section is the same as any other EcoItem; see [How to Make an Item](how-to-make-an-item). Unlike the older option, the base item doesn't need to be a vanilla fuel: the component alone makes any item fit the fuel slot.

### Cooking fuel

`burn_time` is how many ticks the furnace stays lit, and `speed_multiplier` divides the cook time of whatever is smelting while the fuel burns. Both fields are required.

Each field takes a plain number, or the ID of one of vanilla's built-in number providers. A plain number is the same in every furnace, while the vanilla providers change with the block. For example, coal uses these to burn half as long but cook twice as fast in blast furnaces and smokers:

```yaml
"minecraft:cooking_fuel":
  burn_time: "minecraft:cooking/time_coal" # 1600 ticks, or 800 in fast furnaces
  speed_multiplier: "minecraft:cooking/speed_default" # 1.0, or 2.0 in fast furnaces
```

Vanilla ships providers for its own fuels, such as `minecraft:cooking/time_blaze_rod`, `minecraft:cooking/time_coal_block`, and `minecraft:cooking/time_lava_bucket`.

### Brewing fuel

`uses` is how many brews one item powers, and `speed_multiplier` divides the 400-tick (20-second) brew time. Both fields are required. Blaze powder uses `minecraft:brewing/uses_default` (20) and `minecraft:brewing/speed_default` (1.0).

### Compostable

`layers` is how many layers one item adds to a composter. It also takes a vanilla provider, such as `minecraft:compostable/medium`, which adds a layer 65% of the time (always, into an empty composter), like most crops.

## Before 26.3

On older versions, an item based on a vanilla fuel can override how long it burns with the `fuel` option, outside `components:`:

```yaml
item:
  item: coal # Must be a vanilla fuel
  name: "&eStar Coal"

fuel:
  burn-ticks: 3200 # 0 stops the item from being usable as fuel at all
```

Furnaces on older versions only accept items vanilla recognises as fuel, so base the item on one (`coal`, `blaze_rod`, ...). The option still works on 26.3, and if an item has both, `burn-ticks` sets the burn time.

:::tip Troubleshooting
- **Item won't go in the fuel slot?** Check the server is on 26.3 or newer, or use a vanilla fuel base with `fuel.burn-ticks`.
- **Component rejected?** Check the console on reload; both fields of `cooking_fuel` and `brewing_fuel` are required.
:::

<hr/>

## Where to go next

- **Recipes:** [Workstation Recipes](workstation-recipes) to make items at furnaces and brewing stands.
- **Item types:** [Custom Foods](item-types-foods) for items that are eaten instead of burned.
- **The base item:** [How to Make an Item](how-to-make-an-item) for the shared item, display, and recipe fields.
