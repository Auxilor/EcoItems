---
title: Custom Blocks
sidebar_position: 1
---

# Custom Blocks

:::info
Block textures are part of the resource pack system, which requires the paid version of EcoItems.
:::

Add a `block:` section to any item config and the item places a real custom block. EcoItems retextures unused vanilla blockstates — the same technique (and the same state math) as Oraxen and Nexo, so existing worlds and configs can migrate.

## Backings

| `type` | Capacity | Collision | Notes |
|---|---|---|---|
| `noteblock` (default) | 774 | Solid | Full hardness support; the workhorse. |
| `stringblock` | 127 | None | For plants/decorations; always breaks instantly. |
| `chorus` | 63 | Solid | Corrupts how *natural* chorus plants render — avoid if your players visit the End. |

```yaml
# items/ruby_block.yml
item:
  item: paper
  display-name: "&cRuby Block"

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

## Things to know

- **State assignments are permanent.** Each block gets a vanilla blockstate permutation, persisted in `block-variations.yml`. The state *is* the block's identity in the world — never edit or delete entries there once blocks have been placed. Imported Oraxen/Nexo configs can pin numbers with `variation:`.
- **Paper flags.** On Paper, set `disable-noteblock-updates` (and the tripwire/chorus equivalents if you use those backings) under `block-updates` in `config/paper-global.yml`. EcoItems works without them (and on Spigot) via event listeners, but the flags are faster and bulletproof.
- **Vanilla note blocks still work** — they keep their default look, and EcoItems replays the right instrument sounds. Tuned note blocks from *before* EcoItems was installed may show a custom texture, though: the states they were tuned into now belong to custom blocks.
- **Custom block sounds** (`blocks.custom-sounds` in pack.yml, on by default): the pack silences the vanilla wood sound events (which the note block backing would trigger) and the server replays them, so custom blocks get their configured `sounds:` and real wooden blocks still sound normal.
- **Other plugins can target your blocks** anywhere eco block lookups work — `ecoitems:ruby_block` in EcoSkills mining XP, libreforge `blocks` filters, effect whitelists, and so on.
