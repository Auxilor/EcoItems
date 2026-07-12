---
title: "Resource Packs"
sidebar_position: 1
---

The paid version of EcoItems can give items custom textures and models, build them into a resource pack, and deliver that pack to players automatically. The free version has no pack system; items without textures work identically in both.

The pack is rebuilt on every `/ecoitems reload`, so adding a texture is: drop in a `.png`, point the item at it, reload.

The paid version ships with around 30 example textured items (swords, hammers, wands, gems, and a custom food) in the `items/examples/` folder, with their textures and models in the `pack/` folder — give one a try with `/ecoitems give <you> legendary_hammer`. Delete the item configs if you don't want them; deleting the whole `pack/` folder restores the default assets on the next reload.

## Giving an item a texture

1. Put your texture at `plugins/EcoItems/pack/textures/<name>.png` (subfolders work).
2. Reference it from the item config:

```yaml
item:
  item: iron_sword hide_attributes
  texture: mithril_sword # pack/textures/mithril_sword.png
```

3. Run `/ecoitems reload`. The pack is rebuilt and re-sent to online players.

Flat textures render like a vanilla item. For handheld items (swords, tools) that should render at the classic angle in hand, set the model parent:

```yaml
  texture: mithril_sword
  texture-parent: handheld # generated (default), handheld, or any model key
```

## Using your own models

If you have a full model JSON (from Blockbench or similar), use `model` instead of `texture`:

```yaml
item:
  model: mithril_sword # pack/models/mithril_sword.json
```

Any textures your model references can be shipped in the `pack/assets/` overlay (see below). You can also reference a model from another namespace directly, e.g. `model: "somepack:item/thing"`.

## The pack folder

Everything pack-related lives in `plugins/EcoItems/pack/`:

| Path | Purpose |
| --- | --- |
| `pack/textures/` | Item textures (`.png`), referenced by `texture:` |
| `pack/models/` | Custom item models (`.json`), referenced by `model:` |
| `pack/assets/` | Copied into the pack verbatim as `assets/` — fonts, sounds, vanilla overrides, anything |
| `pack/pack.png` | Optional icon override for the pack |

Everything in `pack/textures/` and `pack/models/` is available inside the pack as `ecoitems:item/<path>`, so custom models can reference any texture you drop in there. The built pack is written to `plugins/EcoItems/pack.zip`. Files in `pack/assets/` win over generated files on collision, so you can always override what EcoItems generates.

## How it works

EcoItems uses the modern `minecraft:item_model` component rather than CustomModelData: every textured item gets an item model definition at `assets/ecoitems/items/<id>.json` in the pack, and the component is set on the item automatically. One pack covers all supported versions (1.21.8+).

<hr/>

## Where to go next

- **Delivery:** [Delivery Modes](delivery-modes) covers how the pack reaches players.
- **Configuration:** [Pack Configuration](configuration) lists every `pack.yml` option.
- **Self-hosting:** [Packhost](packhost) explains running your own pack host.
