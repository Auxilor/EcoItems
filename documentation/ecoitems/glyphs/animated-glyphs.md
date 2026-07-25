---
title: "Animated Glyphs"
sidebar_position: 2
---

Glyphs can animate from a sprite sheet - fully client-side, so they keep animating in old chat messages, item lore, and on signs without the server re-sending anything. The shipped `:spinner:` glyph is an example.

## Setup

The texture is a sprite sheet with the frames stacked **vertically** (or in one horizontal row):

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
  loop: true # false plays once (synced to world time)
  offset: 0 # Optional extra pixels to advance after the glyph
```

## Straight from a GIF

Drop a `.gif` at the texture path instead (`pack/assets/ecoitems/textures/glyph/spinner.gif`) and the whole `animation:` section becomes optional: the frame count and speed are read from the GIF, and the pack build converts it into the sprite-sheet png next to it (regenerated whenever the gif is newer). An `animation:` section still works to override `fps`, `loop`, or `offset`. GIFs are capped at the same 16 frames.

## How it works (and its limits)

Every frame gets its own font character; the frames are stacked at the same position using negative font advances, and a small patch to Minecraft's text shaders (included in the generated pack automatically) shows only the frame matching the current game time. Because the shader does the work, the animation runs anywhere text renders.

Limits to be aware of:

- **16 frames and 127 fps maximum** - the shader encodes frame data in the text color channels.
- **Non-looping animations sync to world time**, not to when the text appeared.
- The shader patch overrides the vanilla text shaders. If you merge another resource pack that also overrides text shaders (via `pack/imports/` or files under `pack/assets/`), the two will conflict.
- The shaders are only added to the pack when at least one animated glyph exists.
