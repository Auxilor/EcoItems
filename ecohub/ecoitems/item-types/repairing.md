---
title: "Repairable Items"
sidebar_position: 8
---

Durability is set with vanilla components too, so any EcoItem can **wear out**, be **repaired at an anvil** with the materials you choose, be **enchanted**, or never break at all. This page covers giving an item durability and making it repairable, on top of a standard item.

## Quick start

1. Open your item's config in `/plugins/EcoItems/items/`.
2. Add `minecraft:max_damage` to give it durability, plus `minecraft:max_stack_size: 1`.
3. Add `minecraft:repairable` with the items that repair it.
4. Run `/ecoitems reload`.
5. Damage the item (or set its durability with `/ecoitems durability <remaining>`), then combine it with a repair item in an anvil to confirm it repairs.

## The structure of a repairable item

| Part | What it controls |
| --- | --- |
| **Item** | The base item, display, and recipe, identical to any EcoItem |
| **`minecraft:max_damage`** | Total durability |
| **`minecraft:repairable`** | The items that repair it in an anvil |
| **`minecraft:enchantable`** | How well it enchants at an enchanting table |
| **`minecraft:repair_cost`** | The starting anvil cost |
| **`minecraft:unbreakable`** | Whether it never loses durability |

```yaml
# === Item: a normal EcoItem ===
item:
  item: iron_pickaxe
  name: "&bMithril Pickaxe"

  # === Durability and repair, as vanilla components ===
  components:
    "minecraft:max_stack_size": 1 # Items with durability can't stack
    "minecraft:max_damage": 1200 # Total durability

    # Read here: https://minecraft.wiki/w/Data_component_format#repairable
    "minecraft:repairable":
      items: "#minecraft:diamond_tool_materials" # An item, a list of items, or a #tag

    # Read here: https://minecraft.wiki/w/Data_component_format#enchantable
    "minecraft:enchantable":
      value: 10 # Enchantability; iron is 14, diamond is 10, gold is 22

    "minecraft:repair_cost": 0 # Optional; extra anvil levels, grows with each use
```

### Item

The `item:` section is the same as any other EcoItem; see [How to Make an Item](how-to-make-an-item). Basing the item on a vanilla tool, weapon, or armor piece means it already has durability, repair materials, and enchantability, so you only override what changes.

### Durability

`minecraft:max_damage` is the item's total durability. It needs `minecraft:max_stack_size: 1`, since an item can't be both damageable and stackable. `minecraft:damage` sets how much durability a fresh copy has already lost.

To make an item that never wears out, add `minecraft:unbreakable` instead:

```yaml
components:
  "minecraft:unbreakable": {}
```

### Repairing

`minecraft:repairable` lists the items that repair it in an anvil. `items` takes a single item ID, a list of IDs, or an item tag starting with `#`, such as `#minecraft:iron_tool_materials`. Each repair item restores a quarter of the durability, like vanilla.

Players can also repair an item by combining two copies in an anvil, like vanilla tools. `/ecoitems repair` fully repairs the held item for operators.

:::caution Repair items match by base material
`repairable` only knows vanilla item types, so it can't point at another EcoItem. Listing `paper` accepts any paper, including every EcoItem built on `paper`. The same applies when combining two items: vanilla anvils only check the base material, so two different EcoItems on the same base can be combined. Plugins that replace anvil behaviour, such as EcoEnchants, may handle this differently. Base repairable items on a material no other EcoItem uses where that matters.
:::

### Enchanting

`minecraft:enchantable` sets the item's enchantability, which affects how good the enchantments from an enchanting table are. Which enchantments apply is decided by the base item's type and tags. `minecraft:repair_cost` is the extra level cost added in an anvil, which vanilla increases every time the item is worked on.

:::tip Troubleshooting
- **Anvil won't repair it?** Check the repair item's vanilla type is in `items`, and that the item has `max_damage`.
- **Component rejected?** Check the console on reload; `max_damage` must be at least 1.
:::

<hr/>

## Where to go next

- **Tools and weapons:** [Custom Tools](item-types-tools) and [Custom Weapons](item-types-weapons) for items that wear out as they're used.
- **Commands:** [Commands and Permissions](commands-and-permissions) for `/ecoitems repair` and `/ecoitems durability`.
- **The base item:** [How to Make an Item](how-to-make-an-item) for the shared item, display, and recipe fields.
