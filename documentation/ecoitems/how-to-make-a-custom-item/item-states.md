---
title: "Item States"
sidebar_position: 5
---

# Item States

:::info
Item state models are part of the resource pack system, which requires the paid version of EcoItems.
:::

Some items change their model depending on what's happening: bows draw, crossbows charge, shields block, fishing rods cast, elytra break. EcoItems generates the vanilla item-definition trees for all of these from simple config keys.

## State keys

Every key goes under `item:` next to your base `texture`/`model`. Each state accepts a `-model`/`-models` form (locations under `models/`) or a `-texture`/`-textures` form (generates simple models, honoring `texture-parent`):

| Key | What it does | For |
| --- | --- | --- |
| `pulling-models` / `-textures` (list) | Draw stages while using; first entry is stage 0 | Bows, crossbows |
| `charged-model` / `-texture` | Loaded with an arrow | Crossbows |
| `firework-model` / `-texture` | Loaded with a firework | Crossbows |
| `blocking-model` / `-texture` | While raised | Shields |
| `cast-model` / `-texture` | While the bobber is out | Fishing rods |
| `broken-model` / `-texture` | Out of durability | Elytra |
| `damaged-models` / `-textures` (list) | Progressive damage tiers, evenly spread | Anything with durability |
| `throwing-model` / `-texture` | The model in flight | Tridents (see below) |

```yaml
# items/my_bow.yml
item:
  item: bow
  model: item/my_bow
  pulling-models:
    - item/my_bow_pulling_0
    - item/my_bow_pulling_1
    - item/my_bow_pulling_2
```

The generated trees match the vanilla items exactly (same thresholds and properties), so custom bows feel identical to vanilla ones.

## Custom tridents

Any trident-material item is throwable. Give it a `throwing-model` (or `-texture`) and the projectile renders that model in flight, reverting when it lands so pickup shows the normal item:

```yaml
# items/my_trident.yml
item:
  item: trident
  texture: item/my_trident # in hand and in GUIs
  throwing-model: item/my_trident_3d # in flight
```

The in-flight swap needs Paper; on Spigot thrown tridents keep the held model.

## The raw definition escape hatch

For anything the state keys don't cover — display-context switching, composite layering, time-based models, `has_component` conditions — write the vanilla [item definition format](https://minecraft.wiki/w/Items_model_definition) directly. It's passed through verbatim, exactly like `item.components`, so every present and future node type works:

```yaml
item:
  item: paper
  definition:
    type: "minecraft:condition"
    property: "minecraft:using_item"
    on_false: { type: "minecraft:model", model: "ecoitems:item/spyglass_idle" }
    on_true: { type: "minecraft:model", model: "ecoitems:item/spyglass_zoom" }
```

`definition` wins over all state keys. Model references inside it must be fully namespaced (`ecoitems:item/...`), and files they point at should exist in your pack folder — raw trees aren't validated.

## 2D player heads

EcoItems ships two ready-made item models that render any player head's skin as a flat 2D icon — no config needed:

```
/give @s player_head[item_model="ecoitems:2d_player_head",profile={name:"Notch"}]
/give @s player_head[item_model="ecoitems:2d_player_head_large",profile={name:"Notch"}]
```

Perfect for GUIs, leaderboards, and trophies. To bake one into an item, set the `minecraft:item_model` and `minecraft:profile` components — see `items/_example_2d_head.yml`.

:::caution Raw definitions with `special` nodes
If you write your own `special` head node in a raw `definition`, copy the `transformation` block from the built-in `ecoitems:2d_player_head` definition. 26.x clients read the skull's centring and flip from it — without it, heads render upside down and offset. Older clients ignore the field.
:::
