---
title: "How to Make a Glyph"
sidebar_position: 9
---

Glyphs are **custom characters** added to Minecraft's font through the resource pack: emojis, chat tags, icons, and even GUI backgrounds. Players type **placeholders** like `:heart:` in chat, and glyphs work in any config text too, including item lore, GUI titles, and other eco plugins. Combined with **shifts**, they're the building blocks for custom interfaces. This page covers making a glyph, where glyphs work, and animating them.

:::info
Glyphs are delivered through the [resource pack](resource-packs), which requires the paid version of EcoItems.
:::

## Quick start

1. Open `/plugins/EcoItems/glyphs/`.
2. Copy `_example.yml` and rename it to your glyph's ID, e.g. `coin.yml`.
3. Put the texture at `pack/assets/ecoitems/textures/glyph/coin.png` inside the EcoItems plugin folder.
4. Set the `texture`, `ascent`, `height`, and `placeholders`.
5. Run `/ecoitems reload` to rebuild the pack.
6. Type `:coin:` in chat to confirm it renders.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real glyph. You can also organise glyphs into subfolders inside `glyphs/`, and they'll still load.
:::

The paid version ships working glyphs to learn from:

- **Emoji:** `:heart:` (also `<3`), plus `:heart_gold:`, `:heart_blue:`, and `:heart_green:` on a shared sprite sheet.
- **Animated:** `:spinner:` (also `:loading:`).
- **Tags and icons:** chat rank tags in `glyphs/tags/`, rarity tags in `glyphs/rarities/`, and `coins`, `crystals`, `f_button`, and `:logo:`. Glyphs without placeholders are inserted with `%ecoitems_glyph_<id>%`.
- **GUI background:** `items_gui`, the background for `/ecoitems gui`, which positions it automatically. `%ecoitems_glyph_items_gui%` inserts it manually, and its texture is `pack/assets/ecoitems/textures/glyph/gui/items.png`.

## Naming and IDs

The file name without `.yml` is the glyph's ID, used in `%ecoitems_glyph_<id>%` and `/ecoitems glyph <id>`.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the glyph will not load.
:::

## The structure of a glyph

| Part | What it controls |
| --- | --- |
| **Texture** | The image the glyph renders, or its cell on a sprite sheet |
| **Size** | How tall the glyph is and where it sits against the text |
| **Placeholders** | What players and configs type to insert it |
| **Access** | Who can use it in chat, and whether it's offered in completions and the picker |
| **Rendering** | Whether text colour tints it, and its fixed character |
| **Animation** | Optional sprite sheet animation |

```yaml
# === Texture: the image ===
texture: glyph/coin # pack/assets/ecoitems/textures/glyph/coin.png

# === Size: in pixels ===
ascent: 8 # How far above the text baseline it sits; must not exceed height
height: 8 # The rendered height

# === Placeholders: what inserts it ===
placeholders: # Optional
  - ":coin:"

# === Access: who can use it ===
permission: "" # Optional; required in chat and on signs, empty = everyone
tab-complete: true # Optional; offer the placeholders as chat completions
show-in-picker: true # Optional; offer it in the /ecoitems glyphs book

# === Rendering ===
colorable: false # Optional; false renders it white, true lets text colour tint it
# char: "ꐐ" # Optional; a fixed character instead of an automatic one

# === Animation: optional ===
# animation:
#   frames: 16
#   fps: 16
```

### Texture

`texture` is a `[namespace:]path` relative to `textures/`, with no file extension. The namespace defaults to `ecoitems`, and the shipped glyphs live under `textures/glyph/`.

When you have many small glyphs, keep them in one png and give each glyph a `bitmap:` section pointing at its cell instead of a `texture`. Glyphs sharing a sheet become a single font provider:

```yaml
# glyphs/emoji/heart_gold.yml
bitmap:
  texture: glyph/hearts_sheet # The shared png
  rows: 2 # How the sheet is divided
  columns: 2
  row: 0 # This glyph's cell, zero-indexed
  column: 1
ascent: 8
height: 8
placeholders: [":heart_gold:"]
```

All glyphs on one sheet should share `ascent` and `height`, since differing values split them into separate providers. Cells no glyph claims stay empty. The shipped `heart`, `heart_gold`, `heart_blue`, and `heart_green` glyphs share a 2×2 sheet.

### Size

`height` is the rendered height in pixels, and `ascent` is how far above the text baseline the glyph sits. `ascent` must not exceed `height`.

### Placeholders

Each entry in `placeholders` inserts the glyph wherever it's typed, such as `:coin:` or `<3`. A glyph without placeholders can still be inserted with `%ecoitems_glyph_<id>%`.

### Access

`permission` gates using the glyph in chat and on signs; empty means everyone. It doesn't apply to config-authored text. `tab-complete` offers the placeholders as chat completions, and `show-in-picker: false` hides the glyph from the [glyph picker](#the-glyph-picker), which suits oversized glyphs like GUI backgrounds.

### Rendering

`colorable: false` (the default) renders the glyph white, as designed; `true` lets the surrounding text colour tint it.

Each glyph is assigned a codepoint automatically and remembered in `plugins/EcoItems/glyph-codepoints.yml`, so glyphs keep rendering in old chat messages, signs, and item lore across reloads and config changes. Deleting that file reassigns them. To pin a glyph to a specific character, set `char:`.

`/ecoitems glyph <id>` echoes a glyph's character back to you, which is handy in the console for copying the raw character into other plugins' configs.

## Where glyphs work

- **All eco-formatted text:** item lore, display names, GUI titles, messages, and any config string in any eco plugin. Placeholders there aren't permission-checked, since the server admin wrote them.
- **Chat:** players with the glyph's permission can type `:coin:` or paste placeholders. Without permission the placeholder stays literal, and pasting the raw glyph character renders as a scrambled box. Type `\:coin:` to write the placeholder literally.
- **Signs:** the same behaviour as chat.
- **Placeholders:** `%ecoitems_glyph_<id>%` inserts a glyph and `%ecoitems_shift_<pixels>%` moves text horizontally, everywhere eco placeholders (and PlaceholderAPI) work.

Chat, sign, and tab-complete formatting can each be toggled under `glyphs:` in [`pack.yml`](resource-packs-configuration).

## Shifts

Shifts move text horizontally by an exact number of pixels, which makes them the building block for custom GUI titles and [HUDs](how-to-make-a-hud). Use the placeholder form anywhere:

```yaml
# 20px right, then a glyph, then 8px back left
title: "%ecoitems_shift_20%%ecoitems_glyph_coin%%ecoitems_shift_-8%"
```

Shifts range from -2047 to +2047 pixels. For plugins building Adventure components directly, the shift characters also live in the standalone `ecoitems:shift` font.

## The glyph picker

`/ecoitems glyphs` (permission `ecoitems.command.glyphs`, everyone by default) opens a book of every glyph the player is allowed to use. Hover a glyph for its name and placeholders, and click to copy the placeholder to the clipboard; books can't type into chat, so paste it. Glyphs the player lacks permission for don't appear, and `show-in-picker: false` hides a glyph entirely.

## Animated glyphs

Glyphs can **animate** from a sprite sheet or a **GIF**. Animation is fully client-side, so glyphs keep animating in old chat messages, item lore, and on signs without the server re-sending anything. The shipped `:spinner:` glyph is a working example.

### Making an animated glyph

1. Point a glyph's `texture` at a sprite sheet with the frames stacked **vertically** (or in one horizontal row), or at a `.gif`.
2. Add an `animation:` section with the frame count and speed. GIFs can skip this.
3. Run `/ecoitems reload` to rebuild the pack.
4. Type the glyph's placeholder in chat to confirm it animates.

```yaml
# glyphs/spinner.yml
texture: glyph/spinner # pack/assets/ecoitems/textures/glyph/spinner.png, e.g. 8x128 = 16 frames of 8x8
ascent: 6
height: 7
placeholders:
  - ":spinner:"
animation:
  frames: 16 # Frames in the sheet (max 16)
  fps: 16 # Playback speed (1-127)
  loop: true # false plays once, synced to world time
  offset: 0 # Optional; extra pixels to advance after the glyph
```

### Straight from a GIF

Put a `.gif` at the texture path instead (`pack/assets/ecoitems/textures/glyph/spinner.gif`) and the whole `animation:` section becomes optional. The frame count and speed are read from the GIF, and the pack build converts it into the sprite sheet png next to it, regenerating it whenever the GIF is newer. An `animation:` section still works to override `fps`, `loop`, or `offset`. GIFs are capped at the same 16 frames.

### How it works

Every frame gets its own font character. The frames are stacked at the same position using negative font advances, and a small patch to Minecraft's text shaders (added to the generated pack automatically) shows only the frame matching the current game time. Because the shader does the work, the animation runs anywhere text renders.

- **16 frames and 127 fps maximum**, because the shader encodes frame data in the text colour channels.
- **Non-looping animations sync to world time**, not to when the text appeared.
- **The shader patch overrides the vanilla text shaders.** If you merge another resource pack that also overrides text shaders (via `pack/imports/` or files under `pack/assets/`), the two will conflict; see [Merging Other Packs](resource-packs-merging-packs).
- **The shaders are only added to the pack when at least one animated glyph exists.**

<hr/>

## Where to go next

- **HUDs:** [How to Make a HUD](how-to-make-a-hud) to build interface elements from glyphs and shifts.
- **Merging packs:** [Merging Other Packs](resource-packs-merging-packs) for using animated glyphs alongside shader-based packs.
- **The pack:** [Resource Packs](resource-packs) for how glyph textures are delivered.
