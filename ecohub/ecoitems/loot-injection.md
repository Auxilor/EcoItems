---
title: "Loot Injection"
sidebar_position: 13
---

Loot configs drop custom items from **vanilla** gameplay: **breaking blocks**, **killing mobs**, and **fishing**, without touching datapacks. Each config in the `loots/` folder is one loot entry with its own chance, items, and filters. This page covers making a loot entry and how each type behaves.

## Quick start

1. Open `/plugins/EcoItems/loots/`.
2. Copy `_example_loot.yml` and rename it to your loot's ID, e.g. `ruby_from_stone.yml`.
3. Set the `type` and, for block or mob loot, the `targets`.
4. Set the `chance` and the `items` it drops.
5. Run `/ecoitems reload`.
6. Temporarily set `chance: 1.0`, then break a target block (or kill a target mob) in survival to confirm it drops.

:::tip
`_example_loot.yml` is included as a reference and is **never loaded**, so copy or rename it to make real loot. You can also organise loot into subfolders inside `loots/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the loot entry's ID.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the loot will not load.
:::

## The structure of a loot entry

| Part | What it controls |
| --- | --- |
| **Type** | Whether it rolls on blocks, mobs, or fishing |
| **Targets** | Which blocks or mobs it rolls on |
| **Rolls** | The chance to roll, the items, and bonus XP |
| **Filters** | Biomes, worlds, and conditions it applies in |

```yaml
# === Type: what triggers the roll ===
type: block # block, mob, or fishing

# === Targets: which blocks or mobs ===
targets: # Not used by fishing
  - stone
  - deepslate

# === Rolls: what it drops ===
chance: 0.02 # Chance for the loot to roll at all, 0-1
fortune: true # Fortune on the tool multiplies amounts (block only)
items:
  - item: ecoitems:ruby # Any eco item lookup
    chance: 1.0 # Per-item chance within a successful roll
    amount: 1 # Or a range like 1-3
xp: 1-3 # Bonus XP

# === Filters: where and for whom ===
biomes: [] # Optional; empty = everywhere
worlds: [] # Optional; empty = everywhere
conditions: [] # Optional; libreforge conditions checked on the player
```

### Type

- **`block`** rolls when a player breaks a matching block in survival, unless another plugin already cancelled the drops. It works on [custom blocks](how-to-make-a-block) too, on top of their own `drops:`.
- **`mob`** rolls when a player kills a matching entity; items join the death drops at the mob's location.
- **`fishing`** rolls on a successful catch, and the first rolled item **replaces** the vanilla catch. `targets` is ignored.

### Targets

Block loot takes any eco block lookup: materials, `#tags`, `ecoitems:my_block`, or another plugin's custom blocks. Mob loot takes any eco entity lookup: entity types, `#tags`, or custom entities.

### Rolls

`chance` is the chance for the whole entry to roll. When it does, each item in `items` rolls its own `chance`, with an `amount` that can be a number or a range. `fortune` multiplies amounts for block loot, and `xp` drops bonus experience.

For example, a rare fishing catch limited to oceans:

```yaml
type: fishing
chance: 0.05
items:
  - item: ecoitems:enchanted_boot
biomes:
  - ocean
  - deep_ocean
```

### Filters

`biomes:` and `worlds:` limit where the loot applies, using biome keys like `deep_dark` and lowercase world names. A `conditions:` block accepts [libreforge conditions](https://hub.auxilor.io/wiki/libreforge/configuring-a-condition) checked on the player, such as permissions or skill levels.

:::tip Player-placed blocks count
Block loot doesn't distinguish naturally generated blocks from player-placed ones. Keep chances low for renewable blocks, or gate them with conditions.
:::

<hr/>

## Where to go next

- **Make an item:** [How to Make an Item](how-to-make-an-item) to create the items your loot drops.
- **Conditions:** [libreforge conditions](https://hub.auxilor.io/wiki/libreforge/configuring-a-condition) to gate loot on the player.
