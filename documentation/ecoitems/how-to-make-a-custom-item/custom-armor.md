---
title: "Custom Armor"
sidebar_position: 4
---

# Custom Armor

:::info
Armor textures are part of the resource pack system, which requires the paid version of EcoItems.
:::

Custom armor uses the vanilla `minecraft:equippable` component (1.21.2+): any item can become wearable, rendering a fully custom texture on the body — no shaders, no trims, no restrictions on the base material.

## Player armor

Each piece is a normal item with an `equippable` component pointing at an **equipment asset**:

```yaml
# items/emerald_helmet.yml
item:
  item: paper
  display-name: "<g:#89E59D>Emerald Helmet</g:#37C6BA>"
  texture: item/default/armors/emerald_helmet # the inventory icon
  components:
    "minecraft:max_stack_size": 1
    "minecraft:max_damage": 437
    "minecraft:equippable":
      slot: "head" # head, chest, legs, feet
      asset_id: "ecoitems:emerald"
    "minecraft:attribute_modifiers":
      - type: "minecraft:armor"
        id: "ecoitems:emerald_helmet_armor"
        amount: 3
        operation: "add_value"
        slot: "head"

slot: helmet # the libreforge slot, for effects
```

The equipment asset defines which textures render on the body. It lives at its natural vanilla location in the pack folder:

```json
// pack/assets/ecoitems/equipment/emerald.json
{
  "layers": {
    "humanoid": [{ "texture": "ecoitems:emerald" }],
    "humanoid_leggings": [{ "texture": "ecoitems:emerald" }]
  }
}
```

With the layer textures at:

- `pack/assets/ecoitems/textures/entity/equipment/humanoid/emerald.png` (helmet, chestplate, boots — the classic `layer_1`)
- `pack/assets/ecoitems/textures/entity/equipment/humanoid_leggings/emerald.png` (leggings — `layer_2`)

One asset covers the whole set; every piece points at the same `asset_id`. The shipped emerald, ruby, and obsidian sets are complete working examples.

## Elytra

Elytra work the same way with a `wings` layer — see the shipped `magic_elytra` example:

```json
// pack/assets/ecoitems/equipment/magic_elytra.json
{
  "layers": { "wings": [{ "texture": "ecoitems:magic_elytra" }] }
}
```

## Mob armor, saddles, and harnesses

The same component covers gear for mobs — just different slots and layer types. The shipped `ruby_wolf_armor`, `ruby_horse_armor`, `ruby_llama_carpet`, `ruby_saddle`, and `ruby_harness` examples cover every case:

| Gear | `slot` | Layer type | Notes |
| --- | --- | --- | --- |
| Wolf armor | `body` | `wolf_body` | 1.21.2+ |
| Horse armor | `body` | `horse_body` | 1.21.2+ |
| Llama carpet | `body` | `llama_body` | 1.21.2+ |
| Saddle | `saddle` | `<entity>_saddle` (one layer per rideable mob) | 1.21.5+ |
| Happy ghast harness | `body` | `happy_ghast_body` | 1.21.6+ |

Use `allowed_entities` to restrict what can wear it:

```yaml
# items/ruby_wolf_armor.yml
item:
  item: wolf_armor
  display-name: "<g:#FA7CBB>Ruby Wolf Armor</g:#F14658>"
  texture: item/mob/ruby_wolf_armor
  components:
    "minecraft:equippable":
      slot: "body"
      asset_id: "ecoitems:ruby_wolf"
      allowed_entities: "minecraft:wolf"
```

A custom saddle's equipment asset declares a layer per rideable entity (`pig_saddle`, `horse_saddle`, `camel_saddle`, ...) all pointing at your texture — copy the shipped `ruby_saddle.json` as a starting point.

## Tips

- The `equippable` component supports more options — `equip_sound`, `dispensable`, `swappable`, `camera_overlay` — all in vanilla component format.
- Give pieces durability with `minecraft:max_damage` (plus `minecraft:max_stack_size: 1`).
- Armor values come from `minecraft:attribute_modifiers` on the right slot, exactly like the vanilla items.
