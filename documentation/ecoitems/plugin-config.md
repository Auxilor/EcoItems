---
title: "Plugin Config"
sidebar_position: 6
---

It controls recipe discovery, the global rarity system, and the admin item GUI. The main plugin config, `config.yml`, is found at `/plugins/EcoItems/config.yml`. After changing anything here, run `/ecoitems reload` to apply it.

The paid resource-pack system has its own config at `/plugins/EcoItems/pack.yml`; see [Resource Packs](resource-packs/index).

## Default config.yml

```yaml
discover-recipes: true # If all EcoItems recipes should be automatically unlocked for players

# If items already in circulation should be rebuilt from their current configs
# on join, on pickup, and after a reload. Durability, anvil renames, extra
# enchantments, and other plugins' item data are preserved.
auto-update-items: true

blocks:
  place-cooldown-ms: 200 # Milliseconds between custom block/furniture placements
  worlds: # Worlds where custom blocks/crops can be placed ("*", "world_*", "!creative_world")
    - "*"
  tool-speeds: # Mining speed per tool tier (hand = 1), used for custom block hardness
    wooden: 2
    stone: 4
    iron: 6
    diamond: 8
    netherite: 9
    golden: 12

rarity:
  enabled: false # If the rarity system should be enabled
  blank-lore-line: true # If a blank lore line should separate the rarity tag from the item lore
  default: common # The rarity given to items with no rarity specified
  display-default: true # If items with no rarity should be shown with the default rarity
```

## Admin item GUI

`/ecoitems gui` opens a paginated browser of every loaded EcoItem. It is intended for administrators: left-clicking an entry gives one copy. The menu follows eco's standard configurable GUI shape.

Paid builds apply `background.glyph` and each control's optional `model`; the free build ignores those presentation fields and uses the configured vanilla title and items.

```yaml
items-gui:
  rows: 6
  title: "&7EcoItems (%page%/%max_page%)"

  # Items in folders under items/ group into categories: the GUI opens with
  # a category menu and each folder becomes a sub-menu. With no folders, the
  # GUI is a flat item list.
  category-title: "&7%category% (%page%/%max_page%)"
  category-item:
    name: "&f%category%"
    lore:
      - "&7%count% items"
  # The category for items directly in items/ (only shown if any exist).
  root-category-name: "Other"

  back-button:
    enabled: true
    item: arrow
    model: ecoitems:back
    name: "&fBack"
    row: 6
    column: 1

  background:
    enabled: true
    glyph: items_gui

  mask:
    items: []
    pattern:
      - "000000000"
      - "000000000"
      - "000000000"
      - "000000000"
      - "000000000"
      - "000000000"

  empty-item: air

  page-change:
    sound:
      enabled: true
      sound: ui.button.click
      pitch: 1.0
      volume: 1.0
    backwards:
      item: arrow name:"&fPrevious Page"
      model: ecoitems:previous
      # Shown when there's no previous page; remove to hide the button instead.
      locked-model: ecoitems:previous_locked
      row: 6
      column: 4
    forwards:
      item: arrow name:"&fNext Page"
      model: ecoitems:next
      locked-model: ecoitems:next_locked
      row: 6
      column: 6

  close-button:
    enabled: true
    item: barrier
    model: ecoitems:close
    name: "&cClose"
    lore: []
    row: 6
    column: 5

  item-area:
    width: 7
    height: 4
    row: 2
    column: 2

  custom-slots: []
```

`rows`, `title`, `mask`, `item-area`, `page-change`, `close-button`, and `custom-slots` follow eco's GUI conventions. Omitting an inactive page item's configuration hides that control at the first or last page.

<hr/>

## Where to go next

- **Rarities:** [Item Rarity](additional-configuration-options/item-rarity) covers building the rarities the options above point to.
- **Make an item:** [How to make an Item](how-to-make-a-custom-item/how-to-make-a-custom-item) to start adding items.
- **Pack settings:** [Resource Packs](resource-packs/index) for the paid `pack.yml` options.
