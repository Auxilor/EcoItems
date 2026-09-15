---
title: "How to Make a HUD"
sidebar_position: 14
---

HUDs are persistent **text displays**, such as balances, stats, and server info, shown in the **action bar** or a **boss bar** and refreshed automatically. Combined with [glyphs](how-to-make-a-glyph) and pixel shifts, they can look like fully custom interface elements, including image bars for mana, thirst, or cooldowns. This page covers making a HUD and every option it supports.

:::info
HUDs are part of the resource pack system, which requires the paid version of EcoItems.
:::

## Quick start

1. Open `/plugins/EcoItems/huds/`.
2. Copy `_example.yml` and rename it to your HUD's ID, e.g. `balance.yml`.
3. Set the `text` and the `type` (`action-bar` or `boss-bar`).
4. Set `enabled-by-default: true` so players see it without toggling.
5. Run `/ecoitems reload` to rebuild the pack.
6. Check your action bar (or boss bar) to confirm it shows, and toggle it with `/ecoitems hud toggle balance`.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real HUD. The example needs PlaceholderAPI, Vault, and a `coin` glyph for its balance placeholder.
:::

## Naming and IDs

The file name without `.yml` is the HUD's ID, used in `/ecoitems hud toggle <id>`.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the HUD will not load.
:::

## The structure of a HUD

| Part | What it controls |
| --- | --- |
| **Text** | What the HUD shows |
| **Type** | Action bar or boss bar |
| **Positioning** | The vertical offset of the text |
| **Refresh** | How often it updates |
| **Visibility** | Who sees it by default, and permission and condition gates |
| **Boss bar** | The bar's colour, style, and progress |

```yaml
# === Text: what it shows ===
text: "%ecoitems_shift_100%&6$%vault_eco_balance% :coin:"

# === Type: where it shows ===
type: action-bar # action-bar or boss-bar

# === Positioning: vertical offset ===
text-ascent: -13 # Optional; vanilla is 7, lower moves down, maximum 8

# === Refresh ===
update-ticks: 40 # Optional; defaults to 40

# === Visibility: who sees it ===
enabled-by-default: true # If it shows for players who haven't toggled it
permission: "" # Optional; required to see the HUD
conditions: [] # Optional; libreforge conditions checked every update

# === Boss bar: only used when type is boss-bar ===
boss-bar:
  color: white # white, pink, blue, red, green, yellow, or purple
  style: progress # progress, notched_6, notched_10, notched_12, or notched_20
  progress: 1.0 # How full the bar is, 0.0 to 1.0
```

### Text

The `text` goes through the full EcoItems text pipeline on every update, per player: color codes, eco and PlaceholderAPI placeholders, `:glyph:` placeholders, and `%ecoitems_shift_<pixels>%` for horizontal positioning.

### Type

- **`action-bar`**: players have **one** action bar HUD active at a time. The active HUD is the first `enabled-by-default` one, until the player picks another with the toggle command.
- **`boss-bar`**: rendered as a boss bar at the top of the screen. Multiple boss bar HUDs can show at once, each toggled independently, and the `boss-bar` section styles them.

### Positioning

Normally the action bar renders in its vanilla spot. Setting `text-ascent` re-renders the HUD's dynamic text at a different height: EcoItems generates a dedicated font for the HUD (the vanilla ASCII page at your chosen ascent, plus all your glyphs and shifts), so placeholders and ordinary text move while glyphs keep their own configured ascents. The vanilla ascent is `7`; lower values move text **down** (`-13` sits just above the hotbar), and `8` is the maximum.

:::caution
Only basic ASCII characters are covered by the offset font, so accented or non-Latin text will not render inside an offset HUD.
:::

### Refresh

`update-ticks` is how often the HUD refreshes, and defaults to 40. Set it much higher and the action bar starts fading between updates.

### Visibility

Players toggle HUDs with `/ecoitems hud toggle <id>` (permission `ecoitems.command.hud.toggle`). For an action bar HUD this selects it or turns it off; for a boss bar HUD it flips that bar on or off. Choices persist across relogs. A `permission` additionally gates who can see the HUD at all.

`conditions` accepts [libreforge conditions](https://hub.auxilor.io/wiki/libreforge/configuring-a-condition), checked on every update:

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

### Boss bar

The `boss-bar` section sets the `color`, `style`, and `progress` of a boss bar HUD, and is ignored for action bars:

```yaml
# huds/server_info.yml
text: "&fWelcome to &6Your Server&7: &f%server_online% online"
type: boss-bar
boss-bar:
  color: yellow
  style: progress
  progress: 1.0
```

## Frame bars (mana, thirst, cooldowns)

A `frames:` section turns a HUD into a value-driven image bar. EcoItems evaluates a numeric expression per player, works out where it sits between `min` and `max`, and renders the matching frame. Each frame is a line of text, usually a single [glyph](how-to-make-a-glyph) drawn as one bar state.

```yaml
# huds/mana_bar.yml
type: action-bar
enabled-by-default: true
update-ticks: 5
frames:
  value: "%libreforge_points_mana%" # Any placeholder or math expression
  min: 0
  max: 100
  frames: # Lowest value first
    - ":mana_bar_0:"
    - ":mana_bar_1:"
    - ":mana_bar_2:"
    - ":mana_bar_3:"
    - ":mana_bar_4:"
```

With ten frames, a value 40% of the way from `min` to `max` shows frame 4, so make as many frames as you want granularity. `text` is optional for frame HUDs; give it a `%frame%` placeholder to mix the bar with other text (`text: "&bMana %frame%"`).

Any placeholder works as the `value`, such as a Vault balance, PlaceholderAPI, or EcoSkills stats. For custom per-player values like mana, thirst, or quest progress, use **libreforge points**, which every eco plugin shares:

- Effects like `give_points` and `set_points` change them, e.g. on a trigger or from an [item's effects](how-to-make-an-item).
- `/libreforge points give <player> <type> <amount>` changes them from the console.
- Conditions like `points_above` gate on them.
- `%libreforge_points_<type>%` reads them, which is exactly what `value:` wants.

Points are saved with the player automatically.

:::tip Sharing the action bar
When another plugin writes to the action bar (an item pickup message, another plugin's notification), EcoItems yields for a few seconds and then resumes, so transient messages show cleanly. Two plugins that both keep a *persistent* action bar will still compete, so pick one.
:::

<hr/>

## Where to go next

- **Glyphs:** [How to Make a Glyph](how-to-make-a-glyph) for the icons, bar segments, and pixel shifts HUDs are built from.
- **Commands:** [Commands and Permissions](commands-and-permissions) for the toggle command.
- **The pack:** [Resource Packs](resource-packs) for how HUD fonts reach players.
