---
title: Custom Crops
sidebar_position: 1
---

# Custom Crops

:::info
Crop stage textures are part of the resource pack system, which requires the paid version of EcoItems.
:::

Add a `crop:` section to any item and the item becomes a **seed**: right-click farmland to plant a staged crop that grows over time, can be bonemealed, and drops configured loot when harvested fully grown.

```yaml
# items/golden_wheat.yml
item:
  item: wheat_seeds
  name: "&6Golden Wheat Seeds"
  texture: item/golden_wheat_seeds

crop:
  stages:                        # lowest to highest; each needs a texture (or model)
    - texture: crop/golden_wheat_0
    - texture: crop/golden_wheat_1
    - texture: crop/golden_wheat_2
    - texture: crop/golden_wheat_3
  growth-time: 1200              # seconds from planted to fully grown
  requires-farmland: true        # false = plants on any solid block
  min-light: 9                   # growth pauses below this light level
  bonemeal: true                 # bonemeal advances one stage
  rain-multiplier: 1.5           # optional weather growth speeds (1 = neutral,
  thunder-multiplier: 2.0        # above 1 = faster, below 1 = slower)
  snow-multiplier: 0.5           # applies instead of rain in cold biomes
  drops:                         # broken fully grown; without this, drops the seed
    items:
      - item: ecoitems:golden_wheat
        amount: 1-3
      - item: ecoitems:golden_wheat_seeds_item   # seeds back
        amount: 1-2
    xp: 0-2
  immature-drops:                # broken early; default = the seed item
    items:
      - item: ecoitems:golden_wheat
        chance: 0.1
```

## How growth works

- Crops occupy stringblock (tripwire) states - one state per stage - so they render as cross-shaped plants with no collision and break instantly, like vanilla crops. Stage state assignments persist in `block-variations.yml` like any custom block.
- Growth runs on a timer over loaded chunks. Time keeps counting while a chunk is unloaded, so crops catch up the moment the chunk loads.
- If the block below stops being farmland (trampled, broken), the crop pops with its immature drops, vanilla-style. Water flowing into a crop breaks it too.
- Breaking honors the same drop pipeline as custom blocks (fortune/silk options in the `drops:` table, eco DropQueue with telekinesis support), and protection plugins see real place/break events.

:::tip
Give the seed item its own `texture:`; without one, the seed icon falls back to the crop's final-stage model.
:::
