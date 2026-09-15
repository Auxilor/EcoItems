---
title: "How to Make a Block"
sidebar_position: 5
---

A custom block is a normal EcoItem with a `block:` section: placing the item puts a real block in the world, with its own **texture**, **hardness**, **drops**, and **interaction effects**. EcoItems retextures unused vanilla blockstates, and [migrated setups](migrating-to-ecoitems) keep working because the state encoding is compatible with other custom item plugins. This page covers making a block from scratch and every option it supports.

:::info
Block textures are part of the resource pack system, which requires the paid version of EcoItems.
:::

## Quick start

1. On Paper, set the `block-updates` flags described in [Paper flags](#paper-flags) and restart the server.
2. Open `/plugins/EcoItems/items/` and create your block's file, e.g. `ruby_block.yml`. `items/examples/_example_block.yml` lists every option if you'd rather copy it.
3. Add an `item:` section as you would for any item, using a non-placeable base item such as `paper`.
4. Add a `block:` section with a `type` and a `texture`.
5. Put the texture at `pack/assets/ecoitems/textures/block/ruby_block.png` inside the EcoItems plugin folder.
6. Run `/ecoitems reload` to rebuild the pack.
7. Give yourself the block with `/ecoitems give <player> ruby_block`, then place and break it to confirm the texture and drops.

:::tip
Files starting with `_` are **never loaded**, so the example stays a reference. You can also organise blocks into subfolders inside `items/`, and they'll still load.
:::

## Naming and IDs

A block shares its item's ID: the file name without `.yml`. Other plugins can target it as `ecoitems:<id>` anywhere eco block lookups work, such as EcoSkills mining XP, libreforge `blocks` filters, and effect whitelists. WorldEdit and FAWE accept it in block patterns too (`//set ecoitems:ruby_block`, masks, replacements), with tab suggestions. WorldEdit operations bypass drops, like vanilla.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the block will not load.
:::

## The structure of a block

| Part | What it controls |
| --- | --- |
| **Item** | The placer item, identical to any EcoItem |
| **Backing** | Which vanilla blockstates carry the block |
| **Look** | The texture or model of the placed block |
| **Mining** | Hardness, the tools that get drops, and blast resistance |
| **Drops** | Items and XP from breaking it |
| **Behaviour** | Optional extras: orientation, gravity, stripping, saplings, stacking, sounds |
| **Effects** | libreforge effects run when players interact with the block |

```yaml
# === Item: the placer item ===
item:
  item: paper # A non-placeable base item
  name: "&cRuby Block"

block:
  # === Backing: which vanilla blockstates carry the block ===
  type: noteblock # noteblock (default), stringblock, chorus, mushroom, mushroom_red, or mushroom_stem

  # === Look: the placed block's model ===
  texture: block/ruby_block # pack/assets/ecoitems/textures/block/ruby_block.png

  # === Mining: how it breaks ===
  hardness: 3 # Like vanilla hardness; 0 breaks instantly
  correct-tools: [PICKAXE] # Tools that get drops
  minimum-tier: iron # The lowest tool tier that gets drops
  blast-resistant: false # Optional; if it survives explosions

  # === Drops: what breaking it gives ===
  drops: # Optional; without it, the block drops its own item
    silk-touch: true # Silk touch drops the block item itself
    fortune: true # Fortune multiplies loot
    items:
      - item: ecoitems:ruby # Any eco item lookup
        chance: 1.0
        amount: 2-4
    xp: 1-3

  # === Behaviour: optional extras ===
  sounds: # Optional; defaults to wood sounds
    place: block.stone.place
    break: block.stone.break

  # === Effects: run on interaction ===
  effects:
    right-click:
      - id: send_message
        args:
          message: "&aHello!"
```

### Item

The `item:` section is the same as any other EcoItem; see [How to Make an Item](how-to-make-an-item). The placer item automatically uses the block model as its icon unless the item defines its own texture.

### Backing

| `type` | Capacity | Collision | Notes |
| --- | --- | --- | --- |
| `noteblock` (default) | 774 | Solid | Full hardness support; the workhorse. |
| `stringblock` | 127 | None | For plants and decorations; always breaks instantly. |
| `chorus` | 63 | Solid | Corrupts how *natural* chorus plants render, so avoid it if your players visit the End. |
| `mushroom` | 63 | Solid | Brown mushroom block backing. Full hardness support. |
| `mushroom_red` | 63 | Solid | Red mushroom block backing. |
| `mushroom_stem` | 63 | Solid | Mushroom stem backing. |

The three mushroom backings add 189 more solid states with no block-update physics to fight. The trade-off is worldgen: naturally generated giant mushrooms are built from the same states, so pieces of them can render as your custom blocks in mushroom-heavy biomes (bonemeal-grown mushrooms are protected automatically). Avoid them if your worlds feature mushroom fields; plain `noteblock` has no such conflict.

Vanilla note blocks still work: they keep their default look, and EcoItems replays the right instrument sounds. Note blocks tuned *before* EcoItems was installed may show a custom texture, though, because the states they were tuned into now belong to custom blocks.

:::danger State assignments are permanent
Each block gets a vanilla blockstate permutation, saved in `block-variations.yml`. The state *is* the block's identity in the world, so never edit or delete entries there once blocks have been placed. Migrated configs can pin numbers with `variation:`.
:::

### Look

A single `texture` generates a `cube_all` model. A `textures:` map (e.g. `top`, `bottom`, `side`) picks the parent automatically (`cube_bottom_top`, `cube_column`, `cross`, or `orientable`), or you can set `texture-parent` yourself. `model:` points at your own model json instead.

### Mining

`hardness` works like vanilla hardness: `0` breaks instantly, and leaving it unset uses the backing block's vanilla hardness. Stringblock blocks always break instantly. `correct-tools` and `minimum-tier` gate drops only; any tool still breaks the block eventually. `blast-resistant: true` makes the block survive explosions.

### Drops

Without a `drops:` section, the block drops its own item. With one, `items` rolls each entry's `chance` and `amount`, `xp` adds experience, `silk-touch` drops the block item itself, and `fortune` multiplies loot.

### Behaviour

- **`directional: log|furnace|dropper`** (noteblock only) places the block oriented like logs, furnaces, or droppers, using one extra state per orientation.
- **`falling: true`** (noteblock only) makes the block fall like sand when unsupported.
- **`strips-to: <block id>`** converts the block into another custom block when right-clicked with an axe, costing 1 axe durability and respecting Unbreaking, exactly like vanilla log stripping. Orientation carries over when both blocks share the same directional type.
- **`sounds:`** sets `place`, `break`, `step`, `hit`, and `fall` sounds (vanilla or custom `ecoitems:<sound>` IDs), plus `volume` and `pitch`. With `blocks.custom-sounds` on in `pack.yml` (the default), the pack silences the vanilla wood sound events that the note block backing would trigger and the server replays them, so custom blocks get their configured sounds and real wooden blocks still sound normal.
- **`blocks.worlds`** in `config.yml` limits which worlds custom blocks and crops can be *placed* in (glob patterns, `!` excludes). Already-placed blocks keep working everywhere.

Stringblock blocks can also be **saplings** that grow into pasted WorldEdit schematics. Put `.schem` files in `plugins/EcoItems/schematics/`:

```yaml
block:
  type: stringblock
  texture: block/pine_sapling
  sapling:
    schematics: # Picked at random, by weight
      - schematic: pine.schem
        weight: 10
      - schematic: pine_large.schem
        weight: 2
    growth-time: 600 # Seconds; 0 = bonemeal only
    bonemeal: true
    min-light: 9
    require-space: true # Only grow when nothing solid is in the way
```

Copy the tree with its origin at the trunk base (stand where the sapling would be when you `//copy`), because the schematic pastes with its origin at the sapling. Custom EcoItems blocks inside schematics survive the round trip, since the blockstate is the identity. Growth needs WorldEdit or FAWE installed; a blocked or dark sapling tries again later.

Stringblock blocks can also **stack** like sea pickles: right-clicking with more of the same item grows the stack, and breaking drops one set of loot per stacked item:

```yaml
block:
  type: stringblock
  stackable:
    textures: # One texture (or models:) per stack count
      - block/berries_1
      - block/berries_2
      - block/berries_3
```

### Effects

Blocks can run [libreforge effects](https://hub.auxilor.io/wiki/libreforge/configuring-an-effect) when players interact with them, configured per event inside `block.effects`. The events are `punch`, `shift-punch`, `right-click`, `shift-right-click`, `place`, and `break`; sneaking fires only the `shift-` variant. The full libreforge toolbox works, including chances, cooldowns, conditions, filters, and command execution. With WorldGuard installed, the `ecoitems-block-interact` region flag can deny these per region.

## Paper flags

`disable-noteblock-updates` (and the tripwire, chorus, and mushroom equivalents if you use those backings), under `block-updates` in `config/paper-global.yml`, are required for custom blocks to reliably keep their textures. Without them, vanilla recalculates a note block's instrument whenever the block above or below it changes, which erases the custom state.

EcoItems never edits `paper-global.yml` for you, because modifying server config from a plugin is against Paper's terms. It checks on startup and logs a warning naming the flags you need to set; set them yourself, then restart the server. On Spigot, or until the flags are set, event listeners act as a partial fallback: they repair what they can see but can't intercept every update path, so some blocks will still lose their textures.

:::tip Troubleshooting
- **Block loses its texture after nearby blocks change?** Set the Paper flags above and restart.
- **Texture missing?** `texture` is relative to `pack/assets/ecoitems/textures/`, with no `.png`. A `model:` pointing at a file that doesn't exist logs a console warning on reload.
- **No drops?** `correct-tools` and `minimum-tier` gate drops, so check the tool you're breaking it with.
:::

<hr/>

## Where to go next

- **Crops:** [How to Make a Crop](how-to-make-a-crop) for staged plants built on the same block system.
- **Furniture:** [How to Make Furniture](how-to-make-furniture) for decorations with seats, storage, and collision that aren't full blocks.
- **The pack:** [Resource Packs](resource-packs) for how textures and models reach players.
- **Migrating:** [Migrating to EcoItems](migrating-to-ecoitems) to bring blocks over from Oraxen, Nexo, or ItemsAdder.
