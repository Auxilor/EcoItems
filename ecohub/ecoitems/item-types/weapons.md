---
title: "Custom Weapons"
sidebar_position: 4
---

A custom weapon is a normal EcoItem with the vanilla **combat** components: **attribute modifiers** set its damage and attack speed, the **weapon** component controls durability loss and shield disabling, and on 1.21.11+ the **spear** components add charged, kinetic attacks. This page covers building swords, axes, and spears on top of a standard item.

## Quick start

1. Open `/plugins/EcoItems/items/` and create your weapon's file, e.g. `mithril_sword.yml`.
2. Set up the `item:` section as you would for any item (see [How to Make an Item](how-to-make-an-item)).
3. Add `minecraft:attribute_modifiers` for `attack_damage` and `attack_speed`.
4. Add the `minecraft:weapon` component.
5. Run `/ecoitems reload`.
6. Give yourself the weapon with `/ecoitems give <player> mithril_sword` and hover it to confirm the damage and speed in the tooltip.

:::tip
Base the item on the vanilla weapon closest to what you want (`iron_sword`, `diamond_axe`, `iron_spear`). It keeps every component you don't override, including mining rules and animations, so you only configure what changes.
:::

## The structure of a weapon

| Part | What it controls |
| --- | --- |
| **Item** | The base item, display, and recipe, identical to any EcoItem |
| **`minecraft:attribute_modifiers`** | Attack damage and attack speed while held |
| **`minecraft:weapon`** | Durability lost per hit, and how long hits disable shields |
| **`minecraft:max_damage`** | Durability |

```yaml
# === Item: a normal EcoItem ===
item:
  item: iron_sword # The closest vanilla weapon
  name: "&bMithril Sword"
  texture: item/mithril_sword

  # === The combat behaviour, as vanilla components ===
  components:
    "minecraft:max_damage": 1200 # Durability

    # Read here: https://minecraft.wiki/w/Data_component_format#attribute_modifiers
    "minecraft:attribute_modifiers":
      - type: "minecraft:attack_damage"
        id: "minecraft:base_attack_damage" # The base ID shows it as the weapon's own stat
        amount: 7 # Added to the player's base 1, so 8 damage
        operation: "add_value"
        slot: "mainhand"
      - type: "minecraft:attack_speed"
        id: "minecraft:base_attack_speed"
        amount: -2.4 # Added to the player's base 4, so 1.6 attacks per second
        operation: "add_value"
        slot: "mainhand"

    # Read here: https://minecraft.wiki/w/Data_component_format#weapon
    "minecraft:weapon":
      item_damage_per_attack: 1 # Optional; durability lost per hit, defaults to 1
      disable_blocking_for_seconds: 0 # Optional; how long a hit disables shields, defaults to 0
```

### Item

The `item:` section is the same as any other EcoItem; see [How to Make an Item](how-to-make-an-item). A weapon is still a full EcoItem, so the top-level `effects:` and `conditions:` work too, e.g. running an effect on a `melee_attack` trigger.

### Attribute modifiers

Players have a base attack damage of 1 and a base attack speed of 4, and modifiers add to those. Vanilla values for reference:

| Weapon | `attack_damage` amount | `attack_speed` amount |
| --- | --- | --- |
| Iron sword | 5 | -2.4 |
| Diamond sword | 6 | -2.4 |
| Iron axe | 8 | -3.1 |

Using the IDs `minecraft:base_attack_damage` and `minecraft:base_attack_speed` makes the tooltip show the values as the weapon's own stats, like vanilla weapons. Configured modifiers replace the base item's default for the same attribute and slot, and any defaults you don't mention are kept.

### Weapon

`minecraft:weapon` is what makes an item lose durability when it hits. `disable_blocking_for_seconds` makes hits disable a blocking shield; vanilla axes use `5`.

## Spears

Spears (1.21.11+) have two attacks: a normal **stab**, and a **charged** attack that deals damage based on how fast the player is moving. They're built from several components, all of which a vanilla spear already has, so base a custom spear on one and override only what you change.

```yaml
item:
  item: iron_spear
  name: "&bMithril Spear"
  texture: item/mithril_spear
  components:
    "minecraft:max_damage": 1200
    "minecraft:attribute_modifiers":
      - type: "minecraft:attack_damage"
        id: "minecraft:base_attack_damage"
        amount: 4 # Iron spear: 2
        operation: "add_value"
        slot: "mainhand"

    # The charged attack. Values shown are the iron spear's.
    "minecraft:kinetic_weapon":
      delay_ticks: 12 # Ticks before the charge starts
      contact_cooldown_ticks: 10 # Ticks before the same target can be hit again
      damage_conditions: # When the charge deals damage
        max_duration_ticks: 225
        min_relative_speed: 4.6
      knockback_conditions: # When it knocks back
        max_duration_ticks: 135
        min_speed: 5.1
      dismount_conditions: # When it dismounts the target
        max_duration_ticks: 50
        min_speed: 11.0
      forward_movement: 0.38 # Iron spear: 0.38
      damage_multiplier: 1.2 # Charge damage = the base attack damage + relative speed × this; iron spear: 0.95
      sound: "minecraft:item.spear.use"
      hit_sound: "minecraft:item.spear.hit"
```

| Component | What it controls |
| --- | --- |
| `minecraft:kinetic_weapon` | The charged attack: its delay, the speed conditions for damage, knockback, and dismounting, and the damage multiplier |
| `minecraft:piercing_weapon` | The stab: whether it knocks back (`deals_knockback`) or dismounts (`dismounts`), and its sounds |
| `minecraft:attack_range` | Reach: `min_reach` and `max_reach` (iron spear: 2 to 4.5), creative reach, `hitbox_margin`, and `mob_factor` |
| `minecraft:minimum_attack_charge` | How charged the attack cooldown must be to attack, from 0 to 1 (spears: 1) |
| `minecraft:attack_animation` | The swing animation: `type` (`whack`, `stab`, or `none`) and `duration` in ticks |
| `minecraft:use_effects` | Whether the player can sprint while charging (`can_sprint`) and their movement speed (`speed_multiplier`) |
| `minecraft:damage_type` | The damage type dealt, e.g. `minecraft:spear` |

:::caution Renamed in 26.3
`minecraft:attack_animation` was called `minecraft:swing_animation` before 26.3. Use the name that matches your server version.
:::

Every component can also go on a non-spear item. For example, `minecraft:attack_range` alone gives a sword extra reach, and `minecraft:attack_animation` with `type: stab` makes any weapon thrust instead of swing.

:::tip Troubleshooting
- **Stats not changing?** Check the console for component warnings on reload; a typo in a field name or value reports exactly what's wrong.
- **Tooltip shows two damage lines?** Use the `minecraft:base_attack_damage` ID so your modifier replaces the base item's, instead of a new ID that adds on top.
- **Spear components rejected?** They need 1.21.11 or newer, and `attack_animation` needs 26.3 or newer.
:::

<hr/>

## Where to go next

- **Shields:** [Custom Shields](item-types-shields) to block what your weapons deal.
- **Repairing:** [Repairable Items](item-types-repairing) to let players repair weapons at an anvil.
- **The base item:** [How to Make an Item](how-to-make-an-item) for the shared item, display, and recipe fields.
