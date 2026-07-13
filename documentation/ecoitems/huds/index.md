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
