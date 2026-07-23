---
title: Loot Injection
sidebar_position: 1
---

# Loot Injection

Loot configs drop custom items from **vanilla** gameplay - breaking vanilla blocks, killing mobs, and fishing - without touching datapacks. Each config in the `loots/` folder is one loot entry; the file name is its ID.

```yaml
# loots/ruby_from_stone.yml
type: block          # block, mob, or fishing
targets:             # block lookups (or entity types for mob loot)
  - stone
  - deepslate
chance: 0.02         # chance for the loot to roll at all
fortune: true        # fortune on the tool multiplies amounts (block only)
items:
  - item: ecoitems:ruby
    chance: 1.0      # per-item chance within a successful roll
    amount: 1        # or a range like 1-3
xp: 1-3              # bonus xp
```

```yaml
# loots/rare_catch.yml
type: fishing
chance: 0.05
items:
  - item: ecoitems:enchanted_boot
biomes:              # optional filters; also worlds:
  - ocean
  - deep_ocean
```

## How each type behaves

- **`block`** - rolls when a player breaks a matching block (survival mode, and not when another plugin already cancelled the drops). Drops go through eco's DropQueue, so telekinesis and similar mechanics work.
- **`mob`** - rolls when a player kills a matching entity; items join the death drops at the mob's location.
- **`fishing`** - rolls on a successful catch; the first rolled item **replaces** the vanilla catch. `targets` is ignored.

## Filters and conditions

`biomes:` and `worlds:` limit where the loot applies (biome keys like `deep_dark`, lowercase world names). A `conditions:` block accepts [libreforge conditions](https://hub.auxilor.io/wiki/libreforge/configuring-a-condition) checked on the player - permissions, skill levels, anything the toolbox offers.

:::tip Player-placed blocks count
Block loot doesn't distinguish naturally generated blocks from player-placed ones. Keep chances low for renewable blocks, or gate them with conditions.
:::
