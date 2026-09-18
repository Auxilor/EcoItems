---
title: "Custom Totems"
sidebar_position: 6
---

A custom totem is a normal EcoItem with the vanilla **death_protection** component, which makes any item **save the player from death** while held: the item is used up, the player survives, and the **effects** you choose are applied. This page covers adding that behaviour on top of a standard item.

## Quick start

1. Open `/plugins/EcoItems/items/` and create your totem's file, e.g. `totem_of_rebirth.yml`.
2. Set up the `item:` section as you would for any item (see [How to Make an Item](how-to-make-an-item)).
3. Add the `minecraft:death_protection` component with the `death_effects` you want.
4. Run `/ecoitems reload`.
5. Preview the use animation with `/ecoitems totem totem_of_rebirth`.
6. Give yourself the totem with `/ecoitems give <player> totem_of_rebirth`, hold it, and take lethal damage in survival to confirm it saves you.

## The structure of a totem

| Part | What it controls |
| --- | --- |
| **Item** | The base item, display, and recipe, identical to any EcoItem |
| **`minecraft:death_protection`** | The effects applied when it saves the player |
| **`minecraft:max_stack_size`** | How many can stack; vanilla totems don't stack |

```yaml
# === Item: a normal EcoItem ===
item:
  item: totem_of_undying
  name: "&6Totem of Rebirth"
  texture: item/totem_of_rebirth # Paid version; also shown in the use animation

  # === The death protection, as a vanilla component ===
  components:
    "minecraft:max_stack_size": 1

    # Read here: https://minecraft.wiki/w/Data_component_format#death_protection
    "minecraft:death_protection":
      death_effects: # Optional; applied in order when it saves the player
        - type: "minecraft:clear_all_effects" # Removes every effect first
        - type: "minecraft:apply_effects"
          effects:
            - id: "minecraft:regeneration" # The potion effect ID
              duration: 900 # Duration in ticks
              amplifier: 2 # The effect level, starting at 0
            - id: "minecraft:absorption"
              duration: 200
              amplifier: 3
            - id: "minecraft:fire_resistance"
              duration: 800
              amplifier: 0
          probability: 1.0 # Optional; chance the effects are applied, 0.0 to 1.0
```

### Item

The `item:` section is the same as any other EcoItem; see [How to Make an Item](how-to-make-an-item). The totem only works in the main hand or offhand, like a vanilla totem.

A totem is still a full EcoItem, so the top-level `effects:` and `conditions:` work too. For behaviour beyond potion effects, use the `resurrect` trigger, which fires when a totem saves the player:

```yaml
effects:
  - id: send_message
    args:
      message: "&6The Totem of Rebirth saved you!"
    triggers:
      - resurrect
```

### Death effects

`death_effects` takes the same consume effects as food, applied in order:

| `type` | What it does |
| --- | --- |
| `minecraft:apply_effects` | Applies `effects` (a list of potion effects) with a `probability` |
| `minecraft:remove_effects` | Removes the listed `effects` (effect IDs or a tag) |
| `minecraft:clear_all_effects` | Removes every effect |
| `minecraft:teleport_randomly` | Teleports the player randomly within `diameter` blocks |
| `minecraft:play_sound` | Plays a `sound` |

The vanilla totem clears all effects, then applies Regeneration II for 45 seconds, Absorption II for 5 seconds, and Fire Resistance for 40 seconds.

:::info
Death protection doesn't work against damage that bypasses invulnerability, such as falling into the void or `/kill`.
:::

:::tip Troubleshooting
- **Player still dies?** Check the totem is in the main hand or offhand, and that the damage doesn't bypass invulnerability.
- **Component rejected?** Check the console on reload; invalid fields are reported with the reason.
:::

<hr/>

## Where to go next

- **Item types:** [Custom Foods](item-types-foods) for the same consume effects on food.
- **Commands:** [Commands and Permissions](commands-and-permissions) for `/ecoitems totem`.
- **The base item:** [How to Make an Item](how-to-make-an-item) for the shared item, display, and recipe fields.
