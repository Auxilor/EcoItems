---
title: Furniture
sidebar_position: 1
---

# Furniture

:::info
Furniture models are part of the resource pack system, which requires the paid version of EcoItems.
:::

Add a `furniture:` section to any item config and the item places itself into the world as a display entity — with optional solid collision, sittable seats, and toggleable lights. The furniture shows the item itself, so its look comes from the item's normal `texture`/`model`/`definition`. Use a non-placeable base item (paper) to avoid client-side ghost flicker, and note vanilla *item* models are flat sprites — reference a block model instead, like the shipped `table_lamp` example (`model: "minecraft:block/lantern"`).

```yaml
# items/park_bench.yml
item:
  item: paper
  display-name: "&6Park Bench"
  model: furniture/park_bench

furniture:
  rotation: 4-way
  barriers:
    - "0..1,0,0"       # collision: origin cell + one east
  seats:
    - "0,0.55,0"
    - "1,0.55,0"
```

## How it works

- **Placement** snaps to 8 directions by default (`rotation: 4-way` or `none` to change it; multi-cell furniture always snaps to quarter turns). `placement: {floor, wall, ceiling}` controls which surfaces accept it.
- **Breaking**: punch the furniture (its hitbox or a collision barrier — barriers insta-break for furniture) and it drops its item, or a custom `drops:` table, same format as blocks, through the eco drop queue.
- **Seats** (`seats:` offsets): right-click to sit, sneak-dismount as usual.
- **Lights** (`lights: ["x,y,z level"]`): real light blocks placed with the furniture; `toggleable-lights: true` lets players right-click to switch them.
- **Hitboxes**: clicks are caught by interaction entities — a 1×1 box by default, or `hitboxes: ["x,y,z widthxheight"]` for custom shapes.
- **Display tuning** under `display:` — scale, translation, transform, billboard, fixed brightness, view range.
- **Protection plugins** see real place/break events, so WorldGuard regions and friends work unchanged. Furniture is naturally blast-proof (barrier blocks and display entities are immune to explosions).

See `items/_example_furniture.yml` for every option with comments.

:::caution
Furniture entities are real persistent entities. Don't `/kill @e` — you'd strip furniture displays and leave their barriers behind (breakable by ops).
:::
