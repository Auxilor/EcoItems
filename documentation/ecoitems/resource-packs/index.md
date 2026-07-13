---
title: "Resource Packs"
sidebar_position: 1
---

The paid version of EcoItems can give items custom textures and models, build them into a resource pack, and deliver that pack to players automatically. The free version has no pack system; items without textures work identically in both.

The pack is rebuilt on every `/ecoitems reload`, so adding a texture is: drop in a `.png`, point the item at it, reload.

The paid version ships with around 30 example textured items (swords, hammers, wands, gems, and a custom food) in the `items/examples/` folder, with their textures and models in the `pack/` folder - give one a try with `/ecoitems give <you> legendary_hammer`. Delete the item configs if you don't want them; deleting the whole `pack/` folder restores the default assets on the next reload.

## Giving an item a texture

1. Put your texture at `plugins/EcoItems/pack/assets/ecoitems/textures/item/<name>.png` (subfolders work).
2. Reference it from the item config:

```yaml
item:
  item: iron_sword hide_attributes
  texture: item/mithril_sword # pack/assets/ecoitems/textures/item/mithril_sword.png
```

3. Run `/ecoitems reload`. The pack is rebuilt and re-sent to online players.

References are `[namespace:]path` - no file extension, path relative to `textures/`, namespace defaulting to `ecoitems`. Keep item textures under `textures/item/` (or `textures/block/`) so the block atlas stitches them; a console warning fires otherwise. EcoItems generates a simple model for the texture unless you put your own model JSON at the same path under `models/` (e.g. `pack/assets/ecoitems/models/item/mithril_sword.json`).

Flat textures render like a vanilla item. For handheld items (swords, tools) that should render at the classic angle in hand, set the model parent:

```yaml
  texture: item/mithril_sword
  texture-parent: handheld # generated (default), handheld, or any model key
```

## Using your own models

If you have a full model JSON (from Blockbench or similar), use `model` instead of `texture`:

```yaml
item:
  model: item/mithril_sword # pack/assets/ecoitems/models/item/mithril_sword.json
```

Model references use the same `[namespace:]path` form, relative to `models/`. Any textures your model references live at their normal vanilla paths under `pack/assets/`. You can also reference a model from another namespace directly, e.g. `model: "somepack:item/thing"` - `minecraft:`-namespaced references pass through without a file check.

## The pack folder

`plugins/EcoItems/pack/` **is** a resource pack - the folder is structured exactly like a pack you'd distribute by hand, and everything in it is copied into the generated pack as-is. If you know vanilla resource packs, you already know this folder: you can copy files straight in from any pack.

| Path | Purpose |
| --- | --- |
| `pack/assets/ecoitems/textures/` | Textures - items under `textures/item/`, [glyphs](../glyphs/index) under `textures/glyph/` |
| `pack/assets/ecoitems/models/` | Custom item models (`.json`), referenced by `model:` |
| `pack/assets/ecoitems/sounds/` | [Custom sound](../sounds/index) files (`.ogg`) |
| `pack/assets/...` | Fonts, lang files, equipment assets, shaders, vanilla overrides - anything, at its natural vanilla path |
| `pack/pack.png` | Optional icon override for the pack |
| `pack/pack.mcmeta` | Optional; only its `overlays` entries are used (description and formats are always generated) |
| `pack/imports/` | External packs (zips or folders) to [merge](merging-packs) |

The built pack is written to `plugins/EcoItems/pack.zip`. Where your files collide with generated or imported content, mergeable files (fonts, `sounds.json`, lang files, atlases) are merged instead of clobbered; everything else from the pack folder wins, so you can always override what EcoItems generates.

:::info Migrating from older versions
Older versions used special `pack/textures/`, `pack/models/`, `pack/glyphs/`, `pack/sounds/`, and `pack/lang/` folders. If any of those still exist, a console warning tells you to move their files into the matching locations under `pack/assets/`.
:::

## How it works

EcoItems uses the modern `minecraft:item_model` component rather than CustomModelData: every textured item gets an item model definition at `assets/ecoitems/items/<id>.json` in the pack, and the component is set on the item automatically. One pack covers all supported versions (1.21.8+).

<hr/>

## Where to go next

- **Delivery:** [Delivery Modes](delivery-modes) covers how the pack reaches players.
- **Configuration:** [Pack Configuration](configuration) lists every `pack.yml` option.
- **Self-hosting:** [Packhost](packhost) explains running your own pack host.
