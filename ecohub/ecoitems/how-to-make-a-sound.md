---
title: "How to Make a Sound"
sidebar_position: 13
---

A custom sound is a config in the `sounds/` folder that adds a new **sound event** from your own `.ogg` files: music, ambience, UI feedback, ability sounds, or anything else. Sounds are delivered through the [resource pack](resource-packs) and play like any vanilla sound, and can double as **music discs**. This page covers adding a sound, playing it, and overriding the client's language strings.

:::info
Custom sounds are part of the resource pack system, which requires the paid version of EcoItems.
:::

## Quick start

1. Open `/plugins/EcoItems/sounds/`.
2. Copy `_example.yml` and rename it to your sound's ID, e.g. `battle_horn.yml`.
3. Put the audio at `pack/assets/ecoitems/sounds/battle_horn.ogg` inside the EcoItems plugin folder.
4. Set `sound: battle_horn` to point at the file.
5. Run `/ecoitems reload` to rebuild the pack.
6. Run `/playsound ecoitems:battle_horn player @a` to confirm it plays.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real sound. The shipped `welcome` sound and `welcome_disc` item are a working music disc.
:::

## Naming and IDs

The file name without `.yml` is the sound's ID. The sound plays as `ecoitems:<id>`, which is what `/playsound`, effects, and jukebox items reference.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the sound will not load.
:::

## The structure of a sound

| Part | What it controls |
| --- | --- |
| **Category** | The client volume slider the sound belongs to |
| **Subtitle** | The text shown when the sound plays with subtitles on |
| **Sound files** | The `.ogg` files played, and how each one plays |
| **Jukebox** | Optional registration as a music disc song |

```yaml
# === Category: the volume slider ===
category: player # Optional; defaults to master

# === Subtitle ===
subtitle: "Battle Horn" # Optional

# === Sound files: what plays ===
sounds: # Multiple entries are picked at random, by weight
  - sound: horns/battle_horn # pack/assets/ecoitems/sounds/horns/battle_horn.ogg
    volume: 1.0 # Optional; defaults to 1.0
    pitch: 1.0 # Optional; defaults to 1.0
    weight: 2 # Optional; random-pick weight, defaults to 1
    stream: false # Optional; stream from disk, for long sounds like music
    attenuation-distance: 16 # Optional; audible range in blocks, defaults to 16
    preload: false # Optional; load when the pack loads instead of on demand
  - sound: horns/battle_horn_alt

# === Jukebox: optional music disc song ===
jukebox:
  description: "Battle Horn" # The disc tooltip and now-playing text
  length-seconds: 10 # How long the jukebox stays busy
  comparator-output: 3 # Redstone comparator level, 1-15
  range: 48 # Optional; audible range in blocks
```

### Category

`category` is the client volume slider the sound belongs to: `master`, `music`, `record`, `weather`, `block`, `hostile`, `neutral`, `player`, `ambient`, or `voice`. It defaults to `master`.

### Subtitle

`subtitle` is shown when the sound plays for players with subtitles enabled.

### Sound files

Each entry is a `[namespace:]path` relative to `sounds/`, with no extension and the namespace defaulting to `ecoitems`. One entry plays as-is; multiple entries are picked at random, weighted by `weight`. An entry can also reference a vanilla sample by namespaced key (e.g. `minecraft:dig/stone1`) instead of a file in your pack.

Simple sounds can skip the list with `sound: <name>`, and a plain list of names works too:

```yaml
sounds:
  - horns/battle_horn
  - horns/battle_horn_alt
```

### Jukebox

A `jukebox` section registers the sound as a jukebox song through a generated datapack, which **needs a server restart**; the console tells you when. Any item with a `jukebox_playable` component pointing at the sound then becomes a working music disc:

```yaml
item:
  item: paper
  components:
    "minecraft:max_stack_size": 1
    "minecraft:jukebox_playable": "ecoitems:welcome"
```

## Playing a sound

After `/ecoitems reload`, play the sound with its namespaced ID:

```text
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

## Language files

The pack can also override the client's translation strings, with lang files at their normal vanilla location. Entries in `pack/assets/minecraft/lang/global.json` apply to **every** language at once, and per-language files like `pack/assets/minecraft/lang/en_us.json` override it for that language:

```json
// pack/assets/minecraft/lang/global.json
{
  "menu.disconnect": "§7See you soon!"
}
```

```json
// pack/assets/minecraft/lang/en_us.json
{
  "menu.returnToGame": "Back to the EcoItems experience"
}
```

Values support `:glyph:` placeholders (see [How to Make a Glyph](how-to-make-a-glyph)) and § color codes, and keys starting with `_` are treated as comments. Any vanilla translation key can be overridden; the defaults EcoItems ships are only a starting point.

:::tip Troubleshooting
- **Sound doesn't play?** The console logs a warning on reload when a sound file doesn't exist in `pack/assets/`; check the path has no `.ogg` extension.
- **Music disc does nothing?** Jukebox songs register at startup, so restart the server after adding a `jukebox` section.
:::

<hr/>

## Where to go next

- **Effects:** the [play_sound effect](https://hub.auxilor.io/wiki/libreforge/all-effects) to play sounds from items and triggers.
- **Blocks:** [How to Make a Block](how-to-make-a-block) to use custom sounds for placing and breaking blocks.
- **The pack:** [Resource Packs](resource-packs) for how sounds reach players.
