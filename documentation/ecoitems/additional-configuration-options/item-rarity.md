---
title: "Item Rarity"
sidebar_position: 1
---

A rarity is a category you attach to items to show how good or rare they are: each rarity adds a **lore** tag, has a **weight** that breaks ties, and can claim a list of **items**. This page covers creating a rarity and turning the system on.

## Quick start

1. Enable the rarity system: set `rarity.enabled: true` in `/plugins/EcoItems/config.yml` (see [Plugin Config](../plugin-config)).
2. Open the `/plugins/EcoItems/rarities/` folder.
3. Copy `_example.yml` and rename it to your rarity's ID, e.g. `legendary.yml`.
4. Set the `lore`, `weight`, and any `items` the rarity should apply to.
5. Run `/ecoitems reload`.
6. Give yourself an item using that rarity and confirm the rarity lore tag appears.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real rarity. You can also organise rarities into subfolders inside `rarities/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the rarity's ID. That ID is what you put in an item's `rarity:` field and in the [Item Lookup System](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system), so `legendary.yml` has the ID `legendary`.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the rarity will not load.
:::

## The structure of a rarity

| Part | What it controls |
| --- | --- |
| **Lore** | The lore tag added to items of this rarity |
| **Weight** | Which rarity wins when an item matches more than one |
| **Items** | The non-EcoItems items that should get this rarity |

```yaml
# === Lore: the tag shown on the item ===
lore:
  - "&a&lCOMMON" # One lore line per entry, added to the item

# === Weight: tie-breaking ===
weight: 1 # Higher weight wins when an item matches multiple rarities

# === Items: what gets this rarity ===
items: # Items from the Item Lookup System; EcoItems should set rarity in their own config instead
  - diamond
```

### Lore

The `lore` list is the tag appended to every item of this rarity. Each entry is one line of lore and supports colour codes.

```yaml
lore:
  - "&a&lCOMMON" # One lore line per entry, added to the item
```

### Weight

An item can match more than one rarity. `weight` decides which one is shown: the highest weight wins.

```yaml
weight: 1 # Higher weight wins when an item matches multiple rarities
```

### Items

The `items` list assigns this rarity to vanilla or other looked-up items. EcoItems should set their rarity in their own config with the `rarity:` field rather than being listed here.

```yaml
items: # Items from the Item Lookup System
  - diamond
```

:::tip Troubleshooting
- **No rarity tag showing?** The system is off by default; set `rarity.enabled: true` in `config.yml`.
- **Wrong rarity on an item?** Another rarity has a higher `weight`; raise this one or lower the other.
- **EcoItem ignoring a rarity listed here?** Set `rarity:` in the EcoItem's own config instead of adding it to `items`.
:::

<hr/>

## Where to go next

- **Plugin settings:** [Plugin Config](../plugin-config) for the global rarity options like the default rarity.
- **Make an item:** [How to make an Item](../how-to-make-a-custom-item/how-to-make-a-custom-item) to set a `rarity:` on your own items.