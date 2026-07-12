---
title: Custom Sounds
sidebar_position: 1
---

# Custom Sounds

:::info
Custom sounds are part of the resource pack system, which requires the paid version of EcoItems.
:::

EcoItems can add fully custom sounds to your server: music, ambience, UI feedback, ability sounds — anything you can put in a `.ogg` file. Sounds are delivered through the [resource pack](../resource-packs/index.md) and play like any vanilla sound.

## Creating a sound

Each sound is one config in the `sounds/` folder — the file name is the sound's ID. Drop the audio file in `pack/sounds/` as a `.ogg`, and reference it from the config:

```yaml
# sounds/battle_horn.yml
category: player
subtitle: "Battle Horn"
sound: battle_horn # pack/sounds/battle_horn.ogg
```

After `/ecoitems reload`, the sound plays as `ecoitems:battle_horn`:

```
/playsound ecoitems:battle_horn player @a
```

Or from any libreforge effect config, with the [play_sound effect](https://hub.auxilor.io/wiki/libreforge/all-effects):

```yaml
effects:
  - id: play_sound
    args:
      sound: ecoitems:battle_horn
    triggers:
      - alt_click
```

## All options

```yaml
# (Optional) The client volume slider this sound belongs to. One of:
# master, music, record, weather, block, hostile, neutral, player,
# ambient, voice. Defaults to master.
category: master

# (Optional) The subtitle shown when the sound plays (with subtitles enabled).
subtitle: "Battle Horn"

# The sound files. One entry plays as-is; multiple entries are picked
# randomly, weighted by weight.
sounds:
  - sound: horns/battle_horn # pack/sounds/horns/battle_horn.ogg
    volume: 1.0 # (Optional) Playback volume, defaults to 1.0
    pitch: 1.0 # (Optional) Playback pitch, defaults to 1.0
    weight: 2 # (Optional) Random-pick weight, defaults to 1
    stream: false # (Optional) Stream from disk; use for long sounds like music
    attenuation-distance: 16 # (Optional) Audible range in blocks, defaults to 16
    preload: false # (Optional) Load when the pack loads instead of on demand
  - sound: horns/battle_horn_alt
```

Simple sounds can skip the list entirely with `sound: <name>`, and a plain list of names works too:

```yaml
sounds:
  - horns/battle_horn
  - horns/battle_horn_alt
```

An entry can also reference vanilla samples by namespaced key (e.g. `minecraft:dig/stone1`) instead of a file in `pack/sounds/`.

## Language files

The pack can also override the client's translation strings. Entries in `pack/lang/global.json` apply to **every** language at once; per-language files like `pack/lang/en_us.json` override it for that language:

```json
// pack/lang/global.json
{
  "menu.disconnect": "§7See you soon!"
}
```

```json
// pack/lang/en_us.json
{
  "menu.returnToGame": "Back to the EcoItems experience"
}
```

Values support `:glyph:` placeholders (see [Glyphs](../glyphs/index.md)) and § color codes. Keys starting with `_` are treated as comments. Any vanilla translation key can be overridden — the defaults EcoItems ships are just a starting point.
