---
title: "Plugin Config"
sidebar_position: 6
---

It controls recipe discovery, the global rarity system, and the admin item GUI. The main plugin config, `config.yml`, is found at `/plugins/EcoItems/config.yml`. After changing anything here, run `/ecoitems reload` to apply it.

The paid resource-pack system has its own config at `/plugins/EcoItems/pack.yml`; see [Resource Packs](resource-packs/index).

## Default config.yml

```yaml
discover-recipes: true # If all EcoItems recipes should be automatically unlocked for players

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
  title: "EcoItems &7(%page%/%max_page%)"

  background:
    enabled: true
    glyph: items_gui

  mask:
    items:
      - black_stained_glass_pane
    pattern:
      - "111111111"
      - "100000001"
      - "100000001"
      - "100000001"
      - "100000001"
      - "111111111"

  empty-item: air

  page-change:
    sound:
      enabled: true
      sound: ui.button.click
      pitch: 1.0
      volume: 1.0
    backwards:
      item: arrow name:"&fPrevious Page"
      model: ecoitems:gui_previous
      row: 6
      column: 4
    forwards:
      item: arrow name:"&fNext Page"
      model: ecoitems:gui_next
      row: 6
      column: 6

  close-button:
    enabled: true
    item: barrier
    model: ecoitems:gui_close
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
