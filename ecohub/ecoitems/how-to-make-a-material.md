---
title: "How to Make a Material"
sidebar_position: 13
---

A material is what **parts** are made from. A part remembers its material, and a [modular item](how-to-make-a-modular-item) remembers the material in each of its slots, so its **name, stats, effects and look** all come from them. One material config works in every part and modular item. This page covers making one from scratch.

:::info
Materials, parts and modular items require the paid version of EcoItems. On the free version, material configs are not loaded, and the console lists them after each reload.
:::

## Quick start

1. Open `/plugins/EcoItems/materials/` and create a file, e.g. `cobalt.yml`. `materials/_example.yml` lists every option if you'd rather copy it.
2. Set its `name`, `color` and the `items` that count as it.
3. Under `slots`, add what it gives in each slot of your modular items, using the same slot IDs.
4. Run `/ecoitems reload`.
5. Make a part from it at an anvil, or give one with `/ecoitems give <player> <part> 1 material:cobalt`.

:::tip
Files starting with `_` are **never loaded**, so the example stays a reference. You can also organise materials into subfolders inside `materials/`, and they'll still load.
:::

## Naming and IDs

A material's ID is its file name without `.yml`. It's what `material:<id>` and `<slot>:<id>` take in lookups and commands.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the material will not load.
:::

## The structure of a material

| Part | What it controls |
| --- | --- |
| **Display** | The name, tint colour and palette |
| **Items** | Which items count as the material when making a part |
| **Slots** | What it gives in each slot of a modular item |
| **Traits** | Effects that work in any slot |

```yaml
# === Display: how the material looks in names and textures ===
name: "&9Cobalt" # Used in %material% and %material_<slot>%.
lore: # Shown through %materials_lore% in a modular item's lore.
  - "&9Cobalt: &7Mine faster"
color: "#2376DD" # The fallback tint, and the base for a generated palette.
palette: # 8 colours, darkest to lightest, replacing the 8 texture greys.
  - "#071338"
  - "#0D2466"
  - "#14389A"
  - "#1C4FC0"
  - "#2766D9"
  - "#3E86EC"
  - "#6FAEF7"
  - "#B4DCFF"

# === Items: what counts as this material ===
items:
  - ecoitems:cobalt_ingot

# === Slots: what it gives in each slot of a modular item ===
slots:
  head:
    overlays: # Drawn on the item when this material is in this slot.
      - ecoitems:item/part/cobalt_gem
    mining-speed: 14 # Used if this slot is the item's mining slot.
    mining-tier: diamond # Which blocks drop: wooden, stone, iron, diamond or netherite.
    components:
      max_damage: 800 # Added up across slots.
      attribute_modifiers: # Added up across slots.
        - type: "minecraft:attack_damage"
          id: "ecoitems:cobalt_head_damage"
          amount: 4
          operation: "add_value"
          slot: "mainhand"
    effects: [ ] # Effects only in this slot.
  handle:
    durability-multiplier: 1.75 # Multiplies the final durability.

# === Traits: effects in any slot ===
traits:
  - id: movement_speed_multiplier
    args:
      multiplier: 1.1
conditions: [ ]
```

### Display

`name` goes into part and modular item names through `%material%` and `%material_<slot>%`. `lore` lists what the material gives, for example its traits, and shows wherever a modular item's lore has a `%materials_lore%` line. The resource pack recolours part textures with the `palette`: each of the 8 greys a part texture is drawn with is swapped for the palette colour in the same position, the same way vanilla colours armor trims. Leave `palette` out and one is made from `color`.

### Items

Any [item lookup](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system) works, including other EcoItems. When a part recipe takes `material`, the first material whose items match is used.

### Slots

The keys are the slot IDs your modular items use. A slot can give:

| Key | Effect |
| --- | --- |
| `components` | Vanilla components. `attribute_modifiers` and `max_damage` add up across slots; for any other component the item's own config wins, then the first slot that sets it |
| `durability-multiplier` | Multiplies the item's durability after every slot is added |
| `mining-speed`, `mining-tier` | The mining speed and tier, read only from the item's mining slot |
| `effects` | [Effects](https://hub.auxilor.io/wiki/libreforge/configuring-an-effect) that only work in this slot |
| `lore` | Lines shown through `%materials_lore%` only while the material is in this slot |
| `overlays` | Textures drawn on the modular item when this material is in this slot, never recoloured. Parts on their own don't show them |

A slot that isn't listed gives nothing from this material.

### Traits

`traits` work in any slot, while the modular item is in its own `slot`. `conditions` apply to both traits and slot effects.

:::tip Troubleshooting
- **Part shows the wrong colours?** Its texture uses greys outside 34, 62, 92, 122, 152, 186, 220 and 250. The console warns about this on reload.
- **Material can't make a part?** None of its `items` match the item in the recipe, or there's not enough of it for the part's `cost`.
- **Stats missing in a slot?** The key under `slots` doesn't match the modular item's slot ID.
:::

<hr/>

## Where to go next

- **Modular items:** [How to Make a Modular Item](how-to-make-a-modular-item) to build parts and items from materials.
- **Effects:** the full effect, condition, and trigger reference at [Configuring an Effect](https://hub.auxilor.io/wiki/libreforge/configuring-an-effect).
- **Resource packs:** [Resource Packs](resource-packs) for how the pack is built and delivered.
- **Defaults:** the shipped example lives [here](https://github.com/Auxilor/EcoItems/tree/master/eco-core/core-plugin/src/main/resources/materials).
