---
title: "How to Make a Painting"
sidebar_position: 8
---

A custom painting is a config in the `paintings/` folder that adds a new **painting variant**: the **artwork** ships in the resource pack, and the variant registers through a datapack EcoItems generates in your main world. Players place it like any vanilla painting. This page covers adding a painting and giving players an item that places it.

:::info
Paintings are part of the resource pack system, which requires the paid version of EcoItems.
:::

## Quick start

1. Open `/plugins/EcoItems/paintings/`.
2. Copy `_example.yml` and rename it to your painting's ID, e.g. `starlight.yml`.
3. Set the `width` and `height` in blocks, plus an optional `title` and `author`.
4. Put the artwork at `pack/assets/ecoitems/textures/painting/starlight.png` inside the EcoItems plugin folder.
5. **Restart the server**, since paintings only register at startup.
6. Run `/give @s painting[painting/variant="ecoitems:starlight"]` and place it to confirm it works.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real painting. The shipped `starlight` and `tungtung` paintings, and their `starlight_painting` and `tung_tung_painting` items, are working examples.
:::

## Naming and IDs

The file name without `.yml` is the painting's ID. It registers as the variant `ecoitems:<id>`, which is what items and `/give` commands reference.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the painting will not load.
:::

## The structure of a painting

| Part | What it controls |
| --- | --- |
| **Artwork** | The texture the painting shows |
| **Size** | How many blocks wide and tall it is |
| **Tooltip** | The title and author shown when hovering the painting item |

```yaml
# === Artwork: the texture ===
texture: starlight # Optional; defaults to the painting's ID

# === Size: in blocks ===
width: 2 # 1 to 16
height: 2 # 1 to 16

# === Tooltip: shown on the painting item ===
title: "Starlight" # Optional
author: "EcoItems" # Optional
```

### Artwork

`texture` is a `[namespace:]path` relative to `textures/painting/`, and defaults to the painting's ID, so `starlight.yml` reads `pack/assets/ecoitems/textures/painting/starlight.png`. Use 16 pixels per block: a 2×2 painting wants a 32×32 texture.

Animated paintings work like vanilla animated textures: put a `<texture>.png.mcmeta` next to the artwork.

### Size

`width` and `height` are in blocks, from 1 to 16 each, and default to 1.

### Tooltip

`title` and `author` are optional, and appear when hovering the painting item.

:::caution Restart required
Paintings live in a data-driven registry that Minecraft only loads at startup. After adding or removing paintings, restart the server. The console tells you when a restart is pending, and items referencing unregistered paintings warn until then.
:::

## Painting items

To give players a placeable painting, make a custom item with the `painting/variant` component:

```yaml
# items/starlight_painting.yml
item:
  item: painting
  name: "&9Starlight Painting"
  components:
    "minecraft:painting/variant": "ecoitems:starlight"
```

Breaking a placed custom painting drops the item configured with that variant or, if no item declares it, a painting item that keeps the variant. Vanilla on its own would drop a plain painting.

<hr/>

## Where to go next

- **The item:** [How to Make an Item](how-to-make-an-item) for the painting item's display, recipe, and components.
- **The pack:** [Resource Packs](resource-packs) for how artwork reaches players.
