---
title: HUDs
sidebar_position: 1
---

# HUDs

:::info
HUDs are part of the resource pack system, which requires the paid version of EcoItems.
:::

HUDs are persistent text displays - balances, stats, server info - shown in the action bar or a boss bar and refreshed automatically. Combined with [glyphs](../glyphs/index.md) and pixel shifts they can look like fully custom interface elements.

## Creating a HUD

Each HUD is one config in the `huds/` folder - the file name is the HUD's ID:

```yaml
# huds/balance.yml
text: "%ecoitems_shift_100%&6$%vault_eco_balance% :coin:"
type: action-bar
text-ascent: -13
enabled-by-default: true
```

The `text` goes through the full EcoItems text pipeline on every update, per player: color codes, eco and PlaceholderAPI placeholders, `:glyph:` placeholders, and `%ecoitems_shift_<pixels>%` for horizontal positioning.

## Positioning text with text-ascent

Normally the action bar renders in its vanilla spot. Setting `text-ascent` re-renders the HUD's dynamic text at a different height: EcoItems generates a dedicated font for the HUD (the vanilla ASCII page at your chosen ascent, plus all your glyphs and shifts), so placeholders and ordinary text move while glyphs keep their own configured ascents. The vanilla ascent is `7`; lower values move text **down** (`-13` sits just above the hotbar), and `8` is the maximum. Only the basic ASCII characters are covered - accented or non-Latin text will not render inside an offset HUD.

## Action bars and boss bars

- **`type: action-bar`** - players have **one** action bar HUD active at a time. The active HUD is the first `enabled-by-default` one until the player picks another with the toggle command.
- **`type: boss-bar`** - rendered as a boss bar at the top of the screen; multiple boss-bar HUDs can show at once, each independently toggleable. Style it with the `boss-bar` section (`color`, `style`, `progress`).

```yaml
# huds/server_info.yml
text: "&fWelcome to &6Your Server &7- &f%server_online% online"
type: boss-bar
boss-bar:
  color: yellow
  style: progress
  progress: 1.0
```

## Frame bars (mana, thirst, cooldowns...)

A `frames:` section turns a HUD into a value-driven image bar: EcoItems evaluates a numeric expression per player, works out where it sits between `min` and `max`, and renders the matching frame. Each frame is a line of text - usually a single [glyph](../glyphs/index.md) drawn as one bar segment state.

```yaml
# huds/mana_bar.yml
type: action-bar
enabled-by-default: true
update-ticks: 5
frames:
  value: "%libreforge_points_mana%"   # any placeholder/math expression
  min: 0
  max: 100
  frames:                              # lowest value first
    - ":mana_bar_0:"
    - ":mana_bar_1:"
    - ":mana_bar_2:"
    - ":mana_bar_3:"
    - ":mana_bar_4:"
```

With ten frames, a value 40% of the way from `min` to `max` shows frame 4 - make as many frames as you want granularity. `text` is optional for frame HUDs; give it a `%frame%` placeholder to mix the bar with other text (`text: "&bMana %frame%"`).

**Where does the value come from?** Any placeholder works (Vault balance, PlaceholderAPI, EcoSkills stats). For custom per-player values - mana, thirst, quest progress - use **libreforge points**, which every eco plugin shares: effects like `give_points`/`set_points` change them (e.g. on a trigger, or from an [item's effects](../how-to-make-a-custom-item/how-to-make-a-custom-item.md)), `/libreforge points give <player> <type> <amount>` changes them from the console, conditions like `points_above` gate on them, and `%libreforge_points_<type>%` reads them - exactly what `value:` wants. Points persist with the player automatically.

## Toggling

Players toggle HUDs with `/ecoitems hud toggle <id>` (permission `ecoitems.command.hud.toggle`). For an action-bar HUD this selects it or turns it off; for a boss-bar HUD it flips that bar on or off. Choices persist across relogs. A per-HUD `permission` key additionally gates who can see it at all.

## Conditions

Every HUD accepts a [libreforge conditions](https://hub.auxilor.io/wiki/libreforge/configuring-a-condition) block, checked on every update. Common recipes:

```yaml
# Hide underwater so the oxygen bar is readable:
conditions:
  - id: in_water
    inverted: true

# Only in one world:
conditions:
  - id: in_world
    args:
      world: world
```

## Playing nicely with other plugins

When another plugin writes to the action bar (an item pickup message, a different plugin's notification), EcoItems yields for a few seconds and then resumes - transient messages show cleanly. Two plugins that both maintain a *persistent* action bar will still compete; pick one.
