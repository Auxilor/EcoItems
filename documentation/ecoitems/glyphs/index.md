---
title: "Glyphs"
sidebar_position: 1
---

Glyphs are custom characters — emojis, chat tags, icons — added to Minecraft's font through the resource pack. Players type placeholders like `:heart:` in chat, and they work in any config text too (item lore, GUI titles, any eco plugin). Glyphs are a paid-version feature, since they're delivered through the [resource pack](../resource-packs/index).

The paid version ships example glyphs: `:heart:` (also `<3`), an animated `:spinner:`, eight chat rank tags (`:king:`, `:hero:`, `:god:`, ...) locked behind permissions, and the `items_gui` GUI background.

The paid defaults also include the `items_gui` Eco Workshop background glyph. `%ecoitems_glyph_items_gui%` inserts it manually, while `/ecoitems gui` positions it automatically. Its editable texture is `plugins/EcoItems/pack/assets/ecoitems/textures/glyph/gui/items.png`.

## Making a glyph

1. Put the texture at `plugins/EcoItems/pack/assets/ecoitems/textures/glyph/<name>.png`.
2. Create a config in `plugins/EcoItems/glyphs/`:

```yaml
# glyphs/coin.yml
texture: glyph/coin # pack/assets/ecoitems/textures/glyph/coin.png
ascent: 8 # How far above the text baseline the glyph sits
height: 8 # Rendered height in pixels
placeholders:
  - ":coin:"
permission: "" # Optional; empty = everyone
tab-complete: true # Offer :coin: as a chat completion
colorable: false # false renders the glyph white; true lets text color tint it
```

3. Run `/ecoitems reload`.

`texture` is a `[namespace:]path` relative to `textures/`, without the file extension — the namespace defaults to `ecoitems`, and the shipped glyphs live under `textures/glyph/`.

## Where glyphs work

- **All eco-formatted text**: item lore, display names, GUI titles, messages — any config string in any eco plugin. Placeholders there are not permission-checked (the server admin wrote them).
- **Chat**: players with the glyph's permission can type `:coin:` or paste placeholders; without permission the placeholder stays literal, and pasting the raw glyph character renders as a scrambled box. Type `\:coin:` to write the placeholder literally.
- **Signs**: same behaviour as chat.
- **Placeholders**: `%ecoitems_glyph_<id>%` inserts a glyph and `%ecoitems_shift_<pixels>%` moves text horizontally (see below), everywhere eco placeholders (and PlaceholderAPI) work.

Chat, sign, and tab-complete formatting can each be toggled in [`pack.yml`](../resource-packs/configuration) under `glyphs:`.

## Shifts

Shifts move text horizontally by an exact pixel amount — the building block for custom GUI titles and HUDs. Use the placeholder form anywhere:

```yaml
# 20px right, then a glyph, then 8px back-left
title: "%ecoitems_shift_20%%ecoitems_glyph_coin%%ecoitems_shift_-8%"
```

Shifts range from -2047 to +2047 pixels. For plugins building Adventure components directly, the shift characters also live in the standalone `ecoitems:shift` font.

## Stable characters

Each glyph is assigned a codepoint automatically, remembered in `plugins/EcoItems/glyph-codepoints.yml` so glyphs keep rendering in old chat messages, signs, and item lore across reloads and config changes. Deleting that file re-flows the assignments. To pin a glyph to a specific character, set `char:` in its config.

<hr/>

## Where to go next

- **Animated glyphs:** [Animated Glyphs](animated-glyphs) for sprite-sheet animations.
- **The pack:** [Resource Packs](../resource-packs/index) for how glyph textures are delivered.

## Sprite sheets (multi-bitmap glyphs)

When you have many small glyphs, keep them in one png and let each glyph reference its cell. Glyphs sharing a sheet become a single font provider:

```yaml
# glyphs/emoji/heart_gold.yml
bitmap:
  texture: glyph/hearts_sheet # the shared png
  rows: 2 # how the sheet is divided
  columns: 2
  row: 0 # this glyph's cell, zero-indexed
  column: 1
ascent: 8
height: 8
placeholders: [":heart_gold:"]
```

All glyphs on one sheet should share `ascent` and `height` (differing values split them into separate providers). Cells no glyph claims stay empty. The shipped `heart_gold`/`heart_blue`/`heart_green` emoji demonstrate a 2×2 sheet.
