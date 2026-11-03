---
title: "How to Make a Modular Item"
sidebar_position: 14
---

A modular item is built from **parts**, one in each of its **slots**, and each part is made from a [material](how-to-make-a-material). The item remembers the material in every slot, so **one item config covers every combination**: three slots and 20 materials is 8,000 different items from one file. This page covers making parts and a modular item from scratch.

:::info
Recoloured, layered textures are part of the resource pack system, which requires the paid version of EcoItems. On the free version, parts and modular items use their base item's look.
:::

## Quick start

1. Make the [materials](how-to-make-a-material) you'll use, e.g. `cobalt`, `wood` and `iron`.
2. In `/plugins/EcoItems/items/`, create one item per part shape, e.g. `pickaxe_head.yml`, with a `part:` section. `items/_example_part.yml` lists every option.
3. Create the modular item, e.g. `modular_pickaxe.yml`, with a `modular:` section naming its slots. `items/_example_modular.yml` lists every option.
4. Draw one greyscale texture per part shape and put it in `pack/assets/<namespace>/textures/`.
5. Run `/ecoitems reload`.
6. Give one with `/ecoitems give <player> modular_pickaxe 1 head:cobalt handle:wood binding:iron`, or make the parts at an anvil and put them together at a crafting table.

:::tip
Files starting with `_` are **never loaded**, so the examples stay a reference. You can also organise items into subfolders inside `items/`, and they'll still load.
:::

## Naming and IDs

Parts and modular items are normal EcoItems, so their ID is the file name without `.yml`. Slot IDs and part shapes are your own names.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the item will not load.
:::

## The structure of a part

| Part | What it controls |
| --- | --- |
| **Recipe** | Where the part is made, and from what |
| **Shape** | Which slots of modular items it fits |
| **Display** | Its name and greyscale texture |

```yaml
# === Recipe: made at an anvil or a smithing table ===
item:
  item: flint
  recipes:
    type: anvil
    base: ecoitems:pickaxe_head_pattern # The left slot.
    material: material # The right slot: any material.
    repair-cost: 1 # The level cost.

part:
  # === Shape: which slots it fits ===
  shape: pickaxe_head
  cost: 2 # How many of the material's items it takes.

  # === Display: name and texture ===
  name: "%material% Pickaxe Head"
  texture: ecoitems:item/part/pickaxe_head # Greyscale, recoloured per material.
```

### Recipe

`material` accepts any material: the part is made from whichever material's item is used. It works as an anvil's `material` and a smithing table's `addition`. At an anvil, `cost` of the material is used up.

### Texture

Draw the texture with only these 8 greys, darkest to lightest: 34, 62, 92, 122, 152, 186, 220 and 250. The resource pack makes a copy for each material by swapping each grey for that material's palette colour, the way vanilla colours armor trims. Draw each part where it sits on the finished item, since the item stacks the part textures on top of each other.

## The structure of a modular item

| Part | What it controls |
| --- | --- |
| **Recipe** | How the parts are put together |
| **Slots** | Which part shape goes in each slot, and the drawing order |
| **Display** | Name and lore, from the slot materials |
| **Swapping** | Replacing one part at an anvil |
| **Mining** | Which blocks it mines, and at what speed and tier |
| **Animation** | Bow and crossbow pull states |

```yaml
# === Recipe: parts of any material fit ===
item:
  item: iron_pickaxe
  recipes:
    type: crafting_table
    recipe:
      - ""
      - ecoitems:pickaxe_head
      - ""
      - ""
      - ecoitems:binding
      - ""
      - ""
      - ecoitems:tool_rod
      - ""

modular:
  # === Slots: slot ID to the part shape it takes ===
  slots:
    head: pickaxe_head
    handle: tool_rod
    binding: binding
  layers: # The texture order, bottom to top.
    - handle
    - head
    - binding
  overlays: # Drawn on top, never recoloured.
    - ecoitems:item/part/pickaxe_shine

  # === Display: name and lore ===
  name: "%material_head% Pickaxe"
  lore:
    - "&7Head: %material_head%"

  # === Swapping: replace a part at an anvil ===
  swappable: true
  swap-cost: 1 # The level cost.
  swap-returns-part: false # Give the replaced part back.

  # === Mining: optional ===
  mining:
    blocks: "#minecraft:mineable/pickaxe"
    from: head # The slot whose material gives the speed and tier.

slot: mainhand
```

### Layers and overlays

The slot textures are drawn in `layers` order, bottom to top. Above them come any overlays the slots' materials add (see [How to Make a Material](how-to-make-a-material)), then the item's own `overlays`. Overlays are never recoloured, and every item from this config shows its `overlays` whatever its materials.

### Recipe

Each part ingredient matches that part made from any material. When the item is crafted, each slot takes the first part in the grid with its shape.

### Stats

The item's stats are built from its slots: attribute modifiers and durability add up, then each `durability-multiplier` is applied. See [How to Make a Material](how-to-make-a-material) for what a slot can give. The item's own `components` and `effects` still apply on top.

### Animation

Bows and crossbows change their look while pulled. Set `animation: bow` or `animation: crossbow` on the modular item, and give each part a texture per state under `states`:

```yaml
# In the part:
part:
  texture: ecoitems:item/part/bow_limb
  states:
    pulling_0: ecoitems:item/part/bow_limb_pulling_0
    pulling_1: ecoitems:item/part/bow_limb_pulling_1
    pulling_2: ecoitems:item/part/bow_limb_pulling_2

# In the modular item:
modular:
  animation: bow
  state-overlays: # Drawn on top in one state only, never recoloured.
    pulling_2:
      - ecoitems:item/arrow_nocked
```

A bow has the states `pulling_0`, `pulling_1` and `pulling_2`. A crossbow also has `arrow` and `rocket`, for when it is loaded. A part with no texture for a state uses its `texture`. The thresholds match vanilla, and the item's base should be a `bow` or `crossbow` so it can be drawn.

### Swapping

With `swappable: true`, put the item in the left slot of an anvil and a part in the right. The part replaces the one in the first slot of its shape that has a different material, for `swap-cost` levels. The replaced part is used up, unless `swap-returns-part: true`, in which case it is given back once the new item is taken. If more than one part item has that shape, the one whose ID is the shape name is given back.

## Internal placeholders

| Placeholder | Value |
| --- | --- |
| `%material%` | In a part's `name`: the name of its material |
| `%material_<slot>%` | In a modular item's `name` and `lore`: the name of the material in that slot |

Lookups and `/ecoitems give` take materials as arguments: `material:<id>` for a part, and `<slot>:<id>` for each slot of a modular item.

:::tip Troubleshooting
- **Crafting gives nothing?** A part in the grid doesn't match a slot's shape, or a slot has no part.
- **Item keeps its base look?** A slot has no material, or the server is on the free version.
- **Anvil doesn't offer a swap?** The item isn't `swappable`, or the part's material is already in every slot of its shape.
:::

<hr/>

## Where to go next

- **Materials:** [How to Make a Material](how-to-make-a-material) for what each slot gives.
- **Recipes:** [Workstation Recipes](workstation-recipes) for anvil and smithing table recipes.
- **The base item:** [How to Make an Item](how-to-make-an-item) for the item's other fields.
- **Defaults:** the shipped examples live [here](https://github.com/Auxilor/EcoItems/tree/master/eco-core/core-plugin/src/main/resources/items).
