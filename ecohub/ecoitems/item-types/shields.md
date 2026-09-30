---
title: "Custom Shields"
sidebar_position: 5
---

A custom shield is a normal EcoItem with the vanilla **blocks_attacks** component, which lets any item **block attacks** when used: you set how much **damage it blocks**, from which **angles** and **damage types**, how much **durability** blocking costs, and its **sounds**. This page covers adding that blocking behaviour on top of a standard item.

## Quick start

1. Open `/plugins/EcoItems/items/` and create your shield's file, e.g. `ruby_shield.yml`.
2. Set up the `item:` section as you would for any item (see [How to Make an Item](how-to-make-an-item)).
3. Add the `minecraft:blocks_attacks` component.
4. Run `/ecoitems reload`.
5. Give yourself the shield with `/ecoitems give <player> ruby_shield`, hold right-click, and let a mob hit you from the front to confirm it blocks.

:::tip
Base the item on `shield` to keep its offhand equipping and banner support, or put `minecraft:blocks_attacks` on any other item (a sword, a book) to make it block too.
:::

## The structure of a shield

| Part | What it controls |
| --- | --- |
| **Item** | The base item, display, and recipe, identical to any EcoItem |
| **`minecraft:blocks_attacks`** | How blocking works: delay, damage blocked, durability cost, and sounds |
| **`minecraft:max_damage`** | Durability |
| **Blocking model** | An optional different look while raised (paid version) |

```yaml
# === Item: a normal EcoItem ===
item:
  item: shield
  name: "&cRuby Shield"
  texture: item/ruby_shield # Paid version
  blocking-texture: item/ruby_shield_blocking # Optional; the look while raised

  # === The blocking behaviour, as a vanilla component ===
  components:
    "minecraft:max_damage": 672 # Durability; vanilla shields have 336

    # Read here: https://minecraft.wiki/w/Data_component_format#blocks_attacks
    # Values shown are the vanilla shield's.
    "minecraft:blocks_attacks":
      block_delay_seconds: 0.25 # Optional; seconds held before it blocks, defaults to 0
      disable_cooldown_scale: 1.0 # Optional; scales how long axes disable it, defaults to 1
      damage_reductions: # Optional; how much damage is blocked
        - horizontal_blocking_angle: 90 # Optional; degrees from the front it blocks, defaults to 90
          base: 0 # Damage always blocked
          factor: 1 # Fraction of the damage blocked
      item_damage: # Optional; durability lost when blocking
        threshold: 3 # Hits below this much damage cost nothing
        base: 1
        factor: 1 # Durability lost = base + factor × damage, rounded down
      bypassed_by: "#minecraft:bypasses_shield" # Optional; damage types it can't block
      block_sound: "minecraft:item.shield.block" # Optional
      disabled_sound: "minecraft:item.shield.break" # Optional; played when an axe disables it
```

### Item

The `item:` section is the same as any other EcoItem; see [How to Make an Item](how-to-make-an-item). A shield is still a full EcoItem, so the top-level `effects:` and `conditions:` work too.

### Damage reductions

Each entry in `damage_reductions` blocks `base + factor × damage` of an incoming hit, capped at the hit's damage, when the attacker is within `horizontal_blocking_angle` degrees of where the player faces. Entries add together, so the vanilla `base: 0` and `factor: 1` block everything from the front.

An entry can also be limited to certain damage types with `type`. For example, a buckler that blocks half of melee damage but all projectiles:

```yaml
"minecraft:blocks_attacks":
  damage_reductions:
    - type: "#minecraft:is_projectile"
      base: 0
      factor: 1
    - type: "minecraft:player_attack"
      base: 0
      factor: 0.5
```

### Item damage

`item_damage` sets how much durability a blocked hit costs: nothing below `threshold` damage, otherwise `base + factor × damage`, rounded down. Set `factor: 0` for a flat cost per block.

### Blocking model

On the paid version, `blocking-texture` or `blocking-model` changes the shield's look while it's raised; see [Item States](item-types-item-states).

:::tip Troubleshooting
- **Shield doesn't block?** Check the attacker is inside `horizontal_blocking_angle`, and that `block_delay_seconds` has passed since raising it.
- **Component rejected?** Check the console on reload; invalid fields are reported with the reason. `blocks_attacks` needs 1.21.5 or newer.
:::

<hr/>

## Where to go next

- **Weapons:** [Custom Weapons](item-types-weapons) for axes that disable shields with `disable_blocking_for_seconds`.
- **Item states:** [Item States](item-types-item-states) for the raised shield model.
- **Repairing:** [Repairable Items](item-types-repairing) to let players repair shields at an anvil.
