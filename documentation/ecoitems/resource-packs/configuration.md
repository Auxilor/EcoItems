---
title: "Pack Configuration"
sidebar_position: 2
---

The pack system is configured in `plugins/EcoItems/pack.yml`. After changing anything here, run `/ecoitems reload` to apply it.

## Default pack.yml

```yaml
# If the resource pack system is enabled.
enabled: true

# The pack description shown in the pack menu.
description: "EcoItems Server Pack"

# Strip whitespace from json files when zipping (smaller downloads).
minify-json: true

# Client-side UI tweaks baked into the pack as shader patches. Text stays
# visible - only the dark backgrounds go.
interface:
  hide-scoreboard-background: false
  hide-tablist-background: false

delivery:
  # How the pack reaches players:
  # hosted      - Upload to a packhost instance (packs.auxilor.io by default)
  # self-hosted - Serve the pack over HTTP from this server
  # external    - Export the pack to a folder for you to host yourself
  # none        - Build the pack but don't deliver it
  mode: hosted

  # The message shown on the pack prompt.
  prompt: "&fThis server uses a resource pack for custom items"

  # If players must accept the pack to play.
  required: false

  # If players who decline a required pack should be kicked.
  kick-on-decline: false

  # If the pack should be sent to players when they join.
  send-on-join: true

  # Ticks to wait after join before sending the pack.
  join-delay-ticks: 0

  # If the pack should be re-sent to online players after a reload.
  send-on-reload: true

  hosted:
    # The packhost instance to upload to. See the packhost README to host your own.
    url: "https://packs.auxilor.io"

  self-hosted:
    # The address and port to serve the pack on.
    bind: "0.0.0.0"
    port: 8163

    # The address players download from, e.g. "http://203.0.113.4:8163".
    public-url: ""

  external:
    # Where to export the pack, relative to /plugins/EcoItems.
    directory: "pack-export"

    # The public URL of the exported pack.zip. If empty, the pack is only exported.
    url: ""

glyphs:
  # If glyph placeholders (like :heart:) should be replaced in chat messages.
  format-chat: true

  # If glyph placeholders should be replaced on signs.
  format-signs: true

  # If glyph placeholders should be offered as chat tab-completions.
  tab-complete: true
```

If you disable the pack system (or a publish fails), players simply aren't sent a pack - items keep working, textured items just render with their base item's look for players without the pack.

<hr/>

## Where to go next

- **Delivery:** [Delivery Modes](delivery-modes) explains the four modes in detail.
- **Textures:** [Resource Packs](index) covers giving items textures and models.
