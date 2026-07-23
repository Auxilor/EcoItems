---
title: Custom Blocks
sidebar_position: 1
---

# Custom Blocks

:::info
Block textures are part of the resource pack system, which requires the paid version of EcoItems.
:::

Add a `block:` section to any item config and the item places a real custom block. EcoItems retextures unused vanilla blockstates, and [migrated setups](../migrations/index.md) keep working because the state encoding is compatible with what other custom item plugins use.

## Backings

| `type` | Capacity | Collision | Notes |
|---|---|---|---|
| `noteblock` (default) | 774 | Solid | Full hardness support; the workhorse. |
| `stringblock` | 127 | None | For plants/decorations; always breaks instantly. |
| `chorus` | 63 | Solid | Corrupts how *natural* chorus plants render - avoid if your players visit the End. |
| `mushroom` | 63 | Solid | Brown mushroom block backing. Full hardness support. |
| `mushroom_red` | 63 | Solid | Red mushroom block backing. |
| `mushroom_stem` | 63 | Solid | Mushroom stem backing. |

The three mushroom backings add 189 more solid states with no block-update physics to fight. The trade-off is worldgen: naturally generated giant mushrooms are built from the same states, so pieces of them can render as your custom blocks in mushroom-heavy biomes (bonemeal-grown mushrooms are protected automatically). Avoid them if your worlds feature mushroom fields; plain `noteblock` has no such conflict.

```yaml
# items/ruby_block.yml
item:
  item: paper
  name: "&cRuby Block"

block:
  type: noteblock
  texture: block/ruby_block   # pack/assets/ecoitems/textures/block/ruby_block.png
  hardness: 3
  correct-tools: [PICKAXE]
  minimum-tier: iron
  drops:
    silk-touch: true
    fortune: true
    items:
      - item: ecoitems:ruby
        chance: 1.0
        amount: 2-4
    xp: 1-3
```

A single `texture` generates a `cube_all` model; a `textures:` map auto-picks the parent (`cube_bottom_top`, `cube_column`, `cross`, `orientable`) or set `texture-parent` yourself; `model:` points at your own model json. The placer item automatically uses the block model as its icon unless the item defines its own texture.

See `items/_example_block.yml` for every option (`directional: log|furnace|dropper`, `falling`, `blast-resistant`, `sounds`, per-drop chances).

Custom logs can be strippable: `strips-to: <block id>` converts the block into another custom block when right-clicked with an axe (costing 1 axe durability, respecting Unbreaking), exactly like vanilla log stripping. Directional orientation carries over when both blocks share the same directional type.

Stringblock-backed blocks can also be saplings that grow into pasted WorldEdit schematics (drop `.schem` files into `plugins/EcoItems/schematics/`):

```yaml
block:
  type: stringblock
  texture: block/pine_sapling
  sapling:
    schematics:               # weighted random pick
      - schematic: pine.schem
        weight: 10
      - schematic: pine_large.schem
        weight: 2
    growth-time: 600           # seconds; 0 = bonemeal only
    bonemeal: true
    min-light: 9
    require-space: true        # only grow when nothing solid is in the way
```

Copy the tree with its origin at the trunk base (stand where the sapling would be when you `//copy`) - the schematic pastes with its origin at the sapling. Custom EcoItems blocks inside schematics survive the round-trip, since the blockstate is the identity. Growth needs WorldEdit or FAWE installed; a blocked or dark sapling just tries again later.

Stringblock-backed blocks can stack like sea pickles - right-clicking with more of the same item grows the stack, and breaking drops one set of loot per stacked item:

```yaml
block:
  type: stringblock
  stackable:
    textures:            # one texture (or models:) per stack count
      - block/berries_1
      - block/berries_2
      - block/berries_3
```

## Interaction effects

Blocks can run [libreforge effects](https://hub.auxilor.io/wiki/libreforge/configuring-an-effect) when players interact with them, configured per event inside the `block:` section:

```yaml
block:
  effects:
    right-click:
      - id: send_message
        args:
          message: "&aHello!"
    break:
      - id: give_xp
        args:
          amount: 5
```

Events: `punch`, `shift-punch`, `right-click`, `shift-right-click`, `place`, `break`. Sneaking fires only the `shift-` variant. The full libreforge toolbox works - chances, cooldowns, conditions, filters, command execution, and so on.

## Things to know

- **State assignments are permanent.** Each block gets a vanilla blockstate permutation, persisted in `block-variations.yml`. The state *is* the block's identity in the world - never edit or delete entries there once blocks have been placed. Migrated configs can pin numbers with `variation:`.
- **Paper flags.** On Paper, set `disable-noteblock-updates` (and the tripwire/chorus equivalents if you use those backings) under `block-updates` in `config/paper-global.yml`. EcoItems works without them (and on Spigot) via event listeners, but the flags are faster and bulletproof.
- **Vanilla note blocks still work** - they keep their default look, and EcoItems replays the right instrument sounds. Tuned note blocks from *before* EcoItems was installed may show a custom texture, though: the states they were tuned into now belong to custom blocks.
- **Custom block sounds** (`blocks.custom-sounds` in pack.yml, on by default): the pack silences the vanilla wood sound events (which the note block backing would trigger) and the server replays them, so custom blocks get their configured `sounds:` and real wooden blocks still sound normal.
- **Other plugins can target your blocks** anywhere eco block lookups work - `ecoitems:ruby_block` in EcoSkills mining XP, libreforge `blocks` filters, effect whitelists, and so on.
- **Per-world gating**: `blocks.worlds` in config.yml limits where custom blocks and crops can be *placed* (glob patterns, `!` excludes). Already-placed blocks keep working everywhere.
- **WorldEdit/FAWE**: `ecoitems:<id>` works in block patterns - `//set ecoitems:ruby_block`, masks, replacements - with tab suggestions. WorldEdit operations bypass drops, like vanilla.
