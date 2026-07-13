---
title: "How to Make an Item"
sidebar_position: 1
---

An EcoItem is a custom item defined in its own config file: you set its **display**, its **components**, its **recipe**, and the **effects** it runs while held or worn. This page takes you from an empty file to a working item you can give yourself in-game.

## Quick start

1. Open the `/plugins/EcoItems/items/` folder.
2. Copy `_example.yml` and rename it to your item's ID, e.g. `mithril_sword.yml`.
3. Edit the `item:` section to set the base item, display name, and lore.
4. Set the `slot:` and any `effects:` you want.
5. Run `/ecoitems reload`.
6. Give yourself the item with `/ecoitems give <you> mithril_sword` and confirm it appears with your display name and lore.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real item. You can also organise items into subfolders inside `items/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the item's ID. That ID is what you use in commands and in the [Item Lookup System](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system), so `mithril_sword.yml` has the ID `mithril_sword`.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the item will not load.
:::

## The structure of an item

| Part | What it controls |
| --- | --- |
| **Item** | The base item, display name, lore, components, and crafting recipe |
| **Slot and rarity** | The slot the item works in and its rarity |
| **Effects** | The effects and conditions that run while the item is active |

```yaml
# === Item: what the player gets ===
item:
  item: iron_sword hide_attributes # The base item: https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system
  name: "<g:#f953c6>Mithril Sword</g:#b91d73>" # The item name (the item_name component)
  lore: # The item lore, one entry per line
    - "&7Damage: &c12❤"
    - "&7Attack Speed: &c1.5"
    - ""
    - "<g:#f953c6>MITHRIL BONUS</g:#b91d73>"
    - "&8» &#f953c6Deal 50% more damage in the nether"
  components: # Optional; vanilla item components: https://minecraft.wiki/w/Data_component_format
    "minecraft:attribute_modifiers":
      - type: "minecraft:attack_damage"
        id: "ecoitems:damage"
        amount: 11
        operation: "add_value"
        slot: "mainhand"
  craftable: true # If the item can be crafted
  crafting-permission: ecoitems.craft.custom_item # Optional; permission required to craft the item
  shapeless: false # Optional; whether the recipe is shapeless, defaults to false
  recipe-give-amount: 1 # Optional; how many items the recipe gives, defaults to 1
  recipe: # The recipe: https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system/recipes
    - ""
    - ecoitems:mithril 2
    - ""
    - ""
    - ecoitems:mithril 2
    - ""
    - ""
    - stick
    - ""

# === Behaviour: where it works ===
slot: mainhand # The slot the item must be in to activate; defaults to mainhand
rarity: rare # Optional; the item rarity from the rarities folder

# === Effects: what it does ===
effects:
  - id: damage_multiplier # The effect to run
    args:
      multiplier: 1.5
    triggers:
      - melee_attack # When the effect fires
conditions:
  - id: in_world # A condition that must hold for the effects to run
    args:
      world: world_the_nether
```

### Item

The `item:` section defines the base item players receive and how it's crafted. The base item is an [Item Lookup](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system) string, so modifiers like `hide_attributes`, `glint`, `unbreakable`, and `max_damage:4096` all work inline.

:::tip
EcoItems supports both shaped and shapeless recipes. See [Recipes](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system/recipes) for the full format.
:::

### Components

`item.components` lets you set **any vanilla item component** on the item, using the same structure as vanilla commands - see the [Data component format](https://minecraft.wiki/w/Data_component_format) for every component and its fields. This is how you set combat stats, food behaviour, tool rules, cooldowns, equippability, and anything else the game itself supports:

```yaml
item:
  components:
    max_stack_size: 1
    use_cooldown:
      seconds: 2.5
    attribute_modifiers:
      - type: "minecraft:attack_damage"
        id: "ecoitems:damage"
        amount: 11 # On top of the player's base 1 damage, so 12 total
        operation: "add_value"
        slot: "mainhand"
```

Component keys without a namespace are treated as `minecraft:` components, so no quoting is needed; write `"somemod:component"` in quotes for anything namespaced. Invalid components are skipped with a warning in the console telling you what's wrong.

:::tip Names
`item.name` is shorthand for the `minecraft:item_name` component - the modern way to name items (no italics, no anvil-rename look). The older `item.display-name` still works and sets a custom name through the display system instead, which supports placeholders that update per-player.
:::

:::info Migrating from older versions
The old `base-damage`, `base-attack-speed`, `base-attack-range`, and `effective-durability` options have been replaced by components: use `minecraft:attribute_modifiers` for combat stats (as above) and `max_damage:<amount>` on the base item lookup for durability. The old `food:` and `tool:` sections are now the `minecraft:food`, `minecraft:consumable`, and `minecraft:tool` components - see [Custom Foods](custom-foods) and [Custom Tools](custom-tools).
:::

### Texture (paid version)

Give the item a custom texture with `item.texture`, pointing at a `.png` under `plugins/EcoItems/pack/assets/ecoitems/textures/item/` - the path is relative to `textures/`, without the extension. The resource pack is generated and delivered automatically - see [Resource Packs](../resource-packs/index).

```yaml
item:
  texture: item/mithril_sword # pack/assets/ecoitems/textures/item/mithril_sword.png
```

### Slot and rarity

`slot` accepts `mainhand`, `offhand`, `hands`, `helmet`, `chestplate`, `leggings`, `boots`, `armor`, `any`, a number from 0-40 for an exact slot, or a list like `"9, 10, 11, mainhand"`. `rarity` is optional and references a rarity from the `rarities/` folder.

### Effects

The `effects:` and `conditions:` sections are where the item gets its functionality: effects run on a trigger, and conditions gate when they're allowed to run.

```yaml
effects:
  - id: damage_multiplier # The effect to run
    args:
      multiplier: 1.5
    triggers:
      - melee_attack # When the effect fires
conditions:
  - id: in_world # A condition that must hold for the effects to run
    args:
      world: world_the_nether
```

:::danger Effects are their own system
Effects and conditions are a shared eco system with their own documentation. To configure them:

- [Configuring an Effect](https://hub.auxilor.io/wiki/libreforge/configuring-an-effect)
- [Configuring an Effect Chain](https://hub.auxilor.io/wiki/libreforge/configuring-a-chain)
:::

:::tip Troubleshooting
- **Item won't load?** Check the ID rules above; capitals, spaces, or hyphens in the file name stop it loading.
- **Effects do nothing?** The item only activates in its `slot`; a `mainhand` item does nothing in your inventory.
- **Recipe doesn't work?** Make sure `craftable: true` is set and the ingredient IDs resolve in the Item Lookup System.
- **Changes not showing?** Run `/ecoitems reload`, then get a fresh copy of the item; existing items in inventories are not updated.
:::

<hr/>

## Where to go next

- **Foods and tools:** [Custom Foods](custom-foods) and [Custom Tools](custom-tools) add eating and mining behaviour to an item.
- **Rarities:** [Item Rarity](../additional-configuration-options/item-rarity) for the rarity tags used by `rarity:`.
- **Textures:** [Resource Packs](../resource-packs/index) for custom item textures and models (paid).
- **Default configs:** the shipped items live [here](https://github.com/Auxilor/EcoItems/blob/master/eco-core/core-plugin/src/main/resources/items/), and you can find community items on [lrcdb](https://lrcdb.auxilor.io/).
