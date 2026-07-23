---
title: Custom Paintings
sidebar_position: 1
---

# Custom Paintings

:::info
Paintings are part of the resource pack system, which requires the paid version of EcoItems.
:::

EcoItems can add fully custom paintings: the artwork ships in the resource pack, and the painting registers through a datapack EcoItems generates in your main world.

## Creating a painting

Each painting is one config in the `paintings/` folder - the file name is the painting's ID. Drop the artwork at `pack/assets/ecoitems/textures/painting/<id>.png` (16 pixels per block, so a 2×2 painting wants a 32×32 texture):

```yaml
# paintings/starlight.yml
width: 2
height: 2
title: "Starlight"
author: "EcoItems"
```

:::caution Restart required
Paintings live in a data-driven registry that Minecraft only loads at startup. After adding or removing paintings, restart the server - the console tells you when a restart is pending, and items referencing unregistered paintings warn until then.
:::

## Using it

Give a placeable painting item via a custom item with the `painting/variant` component (see the shipped `starlight_painting` example):

```yaml
# items/starlight_painting.yml
item:
  item: painting
  name: "&9Starlight Painting"
  components:
    "minecraft:painting/variant": "ecoitems:starlight"
```

Or directly: `/give @s painting[painting/variant="ecoitems:starlight"]`.

Breaking a placed custom painting drops the item configured with that variant (or, if no item declares it, a painting item that keeps the variant) - vanilla on its own would drop a plain painting.

Animated paintings work like vanilla animated textures: put a `<texture>.png.mcmeta` next to the artwork.
