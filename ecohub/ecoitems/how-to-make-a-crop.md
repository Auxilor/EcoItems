---
title: "How to Make a Crop"
sidebar_position: 6
---

A custom crop is a normal EcoItem with a `crop:` section, which turns the item into a **seed**: right-click farmland to plant a staged crop that **grows over time**, can be bonemealed, and **drops loot** when harvested fully grown. This page covers making a crop from scratch and how growth works.

:::info
Crop stage textures are part of the resource pack system, which requires the paid version of EcoItems.
:::

## Quick start

1. Open `/plugins/EcoItems/items/` and create your seed's file, e.g. `golden_wheat_seeds.yml`. `items/examples/_example_crop.yml` lists every option if you'd rather copy it.
2. Add an `item:` section for the seed, with its own `texture`.
3. Add a `crop:` section with a list of `stages`, lowest first.
4. Put one texture per stage in `pack/assets/ecoitems/textures/crop/` inside the EcoItems plugin folder.
5. Run `/ecoitems reload` to rebuild the pack.
6. Give yourself the seed with `/ecoitems give <player> golden_wheat_seeds`, plant it on farmland, and bonemeal it to confirm each stage renders.

:::tip
Files starting with `_` are **never loaded**, so the example stays a reference. You can also organise crops into subfolders inside `items/`, and they'll still load.
:::

## Naming and IDs

A crop shares its seed item's ID: the file name without `.yml`. Drops reference items with the [Item Lookup System](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system), so `ecoitems:<id>` gives back seeds or other EcoItems.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the crop will not load.
:::

## The structure of a crop

| Part | What it controls |
| --- | --- |
| **Seed item** | The item players plant, identical to any EcoItem |
| **Stages** | The texture or model for each growth stage |
| **Growth** | How long it takes, where it can be planted, and what speeds it up |
| **Drops** | Loot from breaking it fully grown or early |

```yaml
# === Seed item: what players plant ===
item:
  item: wheat_seeds
  name: "&6Golden Wheat Seeds"
  texture: item/golden_wheat_seeds # The seed icon

crop:
  # === Stages: lowest to highest ===
  stages: # Each stage needs a texture (or model)
    - texture: crop/golden_wheat_0
    - texture: crop/golden_wheat_1
    - texture: crop/golden_wheat_2
    - texture: crop/golden_wheat_3

  # === Growth: timing and planting rules ===
  growth-time: 1200 # Average seconds from planted to fully grown
  requires-farmland: true # false = plants on any solid block
  min-light: 9 # Growth pauses below this light level
  bonemeal: true # If bonemeal advances one stage
  rain-multiplier: 1.5 # Optional; growth speed while raining (1 = neutral, above 1 = faster)
  thunder-multiplier: 2.0 # Optional; growth speed during thunderstorms
  snow-multiplier: 0.5 # Optional; applies instead of rain in cold biomes

  # === Drops: loot from breaking it ===
  drops: # Broken fully grown; without this, drops the seed
    items:
      - item: ecoitems:golden_wheat
        amount: 1-3
      - item: ecoitems:golden_wheat_seeds # Seeds back
        amount: 1-2
    xp: 0-2
  immature-drops: # Broken early; defaults to the seed item
    items:
      - item: ecoitems:golden_wheat
        chance: 0.1
```

### Seed item

The `item:` section is the same as any other EcoItem; see [How to Make an Item](how-to-make-an-item).

:::tip
Give the seed item its own `texture:`. Without one, the seed icon falls back to the crop's final-stage model.
:::

### Stages

Each stage takes a `texture` (a cross model is generated from it) or a `model`. Crops occupy stringblock (tripwire) states, one per stage, so they render as cross-shaped plants with no collision and break instantly, like vanilla crops. Stage state assignments are saved in `block-variations.yml` like any [custom block](how-to-make-a-block).

### Growth

- Growth runs on a timer over loaded chunks. Time keeps counting while a chunk is unloaded, so crops catch up the moment the chunk loads.
- If the block below stops being farmland (trampled or broken), the crop pops with its immature drops, like vanilla. Water flowing into a crop breaks it too.
- The weather multipliers are optional: `1` is neutral, above `1` grows faster, and below `1` grows slower.
- `blocks.worlds` in `config.yml` limits which worlds crops can be planted in.

### Drops

Breaking uses the same drop pipeline as [custom blocks](how-to-make-a-block): the fortune and silk touch options in `drops:`, and eco's drop queue with telekinesis support. `drops` applies when the crop is fully grown and defaults to the seed; `immature-drops` applies when it's broken early and also defaults to the seed. Protection plugins see real place and break events.

<hr/>

## Where to go next

- **Blocks:** [How to Make a Block](how-to-make-a-block) for the block system crops are built on.
- **Loot:** [Loot Injection](loot-injection) to drop your seeds from vanilla blocks, mobs, or fishing.
- **The base item:** [How to Make an Item](how-to-make-an-item) for the seed's display and recipe fields.
