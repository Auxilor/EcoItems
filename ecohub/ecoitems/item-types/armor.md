---
title: "Custom Armor"
sidebar_position: 3
---

Custom armor is a normal EcoItem with the vanilla **equippable** component, which makes any item wearable and renders a fully custom **texture on the body**, with no shaders, trims, or restrictions on the base material. The same component covers **elytra**, **mob armor**, **saddles**, and **harnesses**. This page covers adding that wearable behaviour on top of a standard item.

:::info
Armor textures are part of the resource pack system, which requires the paid version of EcoItems.
:::

## Quick start

1. Open `/plugins/EcoItems/items/` and create a file for each piece, e.g. `emerald_helmet.yml`.
2. Set up the `item:` section as you would for any item (see [How to Make an Item](how-to-make-an-item)), with a `texture` for the inventory icon.
3. Add the `minecraft:equippable` component with a `slot` and an `asset_id`, e.g. `ecoitems:emerald`.
4. Create the equipment asset at `pack/assets/ecoitems/equipment/emerald.json` inside the EcoItems plugin folder.
5. Put the layer textures in `pack/assets/ecoitems/textures/entity/equipment/`.
6. Run `/ecoitems reload` to rebuild the pack.
7. Give yourself the piece with `/ecoitems give <player> emerald_helmet` and wear it to confirm the texture renders.

:::tip
The shipped `ruby_wolf_armor`, `ruby_horse_armor`, `ruby_llama_carpet`, `ruby_saddle`, and `ruby_harness` items are working examples of the equippable component.
:::

## The structure of a piece of armor

| Part | What it controls |
| --- | --- |
| **Item** | The base item, inventory icon, and recipe, identical to any EcoItem |
| **`minecraft:equippable`** | The slot it's worn in and the equipment asset it renders |
| **`minecraft:attribute_modifiers`** | Armor points and other stats while worn |
| **Equipment asset** | Which textures render on the body |

```yaml
# === Item: a normal EcoItem ===
item:
  item: paper # Any base material works
  name: "<g:#89E59D>Emerald Helmet</g:#37C6BA>"
  texture: item/default/armors/emerald_helmet # The inventory icon

  # === The wearable behaviour, as vanilla components ===
  components:
    "minecraft:max_stack_size": 1
    "minecraft:max_damage": 437 # Durability
    "minecraft:equippable":
      slot: "head" # head, chest, legs, or feet
      asset_id: "ecoitems:emerald" # pack/assets/ecoitems/equipment/emerald.json
    "minecraft:attribute_modifiers":
      - type: "minecraft:armor"
        id: "ecoitems:emerald_helmet_armor"
        amount: 3 # Armor points
        operation: "add_value"
        slot: "head"

slot: helmet # The libreforge slot, for effects
```

### Item

The `item:` section is the same as any other EcoItem; see [How to Make an Item](how-to-make-an-item). Give pieces durability with `minecraft:max_damage`, plus `minecraft:max_stack_size: 1`. The top-level `slot` is the libreforge slot the item's effects run in.

### The components

`minecraft:equippable` is the vanilla component (1.21.2+) that makes an item wearable. Beyond `slot` and `asset_id`, it supports `equip_sound`, `dispensable`, `swappable`, `camera_overlay`, and `allowed_entities`, all in the vanilla [Data component format](https://minecraft.wiki/w/Data_component_format#equippable).

Armor values come from `minecraft:attribute_modifiers` on the matching slot, exactly like vanilla armor.

### Equipment asset

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

- `pack/assets/ecoitems/textures/entity/equipment/humanoid/emerald.png` for the helmet, chestplate, and boots (the classic `layer_1`)
- `pack/assets/ecoitems/textures/entity/equipment/humanoid_leggings/emerald.png` for the leggings (`layer_2`)

One asset covers the whole set, so every piece points at the same `asset_id`.

## Elytra

Elytra work the same way, with a `wings` layer in the equipment asset:

```json
// pack/assets/ecoitems/equipment/magic_elytra.json
{
  "layers": { "wings": [{ "texture": "ecoitems:magic_elytra" }] }
}
```

Give the elytra a `broken-texture` to change its icon when it runs out of durability; see [Item States](item-types-item-states).

## Mob armor, saddles, and harnesses

The same component covers gear for mobs, with different slots and layer types:

| Gear | `slot` | Layer type | Version |
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
  name: "<g:#FA7CBB>Ruby Wolf Armor</g:#F14658>"
  texture: item/mob/ruby_wolf_armor
  components:
    "minecraft:equippable":
      slot: "body"
      asset_id: "ecoitems:ruby_wolf"
      allowed_entities: "minecraft:wolf"
```

A custom saddle's equipment asset declares a layer per rideable entity (`pig_saddle`, `horse_saddle`, `camel_saddle`, and so on), all pointing at your texture. Copy the shipped `pack/assets/ecoitems/equipment/ruby_saddle.json` as a starting point.

:::tip Troubleshooting
- **Texture doesn't render when worn?** Check the `asset_id` matches the equipment asset's file name, and that the layer texture sits in the folder named after the layer type.
- **Can't equip it?** Check `slot` is valid for the wearer, and that `allowed_entities` (if set) includes it.
:::

<hr/>

## Where to go next

- **The base item:** [How to Make an Item](how-to-make-an-item) for the shared item, display, and recipe fields.
- **Item states:** [Item States](item-types-item-states) for broken elytra and other state models.
- **The pack:** [Resource Packs](resource-packs) for how textures reach players.
- **Repairing:** [Repairable Items](item-types-repairing) to let players repair armor at an anvil.
