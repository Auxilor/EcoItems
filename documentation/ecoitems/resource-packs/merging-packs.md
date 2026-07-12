---
title: Merging Other Packs
sidebar_position: 4
---

# Merging Other Packs

Large servers usually have several plugins that need a resource pack — MythicMobs model packs, CustomNameplates, hand-made map packs. Since a client can only comfortably use one server pack, EcoItems can merge them all into the pack it generates and delivers.

## How to use it

Drop resource packs into `plugins/EcoItems/pack/imports/` — as `.zip` files or as plain folders — and `/ecoitems reload`. That's it: their contents are merged into the EcoItems pack and delivered to players.

Wrapper folders inside zips are handled automatically (a zip containing `MyPack/assets/...` works), as is a zip containing several packs. macOS/Windows junk files (`.DS_Store`, `__MACOSX`, `Thumbs.db`) are ignored.

## Merge order and priority

- Imports load **first**, as the lowest-priority layer: anything EcoItems generates (item models, fonts, HUD fonts, sounds) and anything you put in the [`pack/` folder](index#the-pack-folder) (itself a vanilla-structured pack) wins on collision.
- Between imports, packs load in **name order** and later packs win — prefix names like `10_mythicmobs.zip`, `20_nameplates.zip` to control priority. Overrides are logged.

Some files are **merged instead of replaced**, so packs cooperate rather than fight:

| File | Merge behavior |
| --- | --- |
| `assets/*/font/*.json` | Font providers combined (later pack's win per character) |
| `assets/*/sounds.json` | Sound events combined (later pack wins per event) |
| `assets/*/lang/*.json` | Translation keys combined (later pack wins per key) |
| `assets/*/atlases/*.json` | Atlas sources concatenated |

Imported `pack.mcmeta` files aren't copied, but their **overlay entries** carry over into the final pack.mcmeta, so version-dependent packs keep working.

## Glyph collision safety

Imported packs often define their own font characters in the same private-use unicode ranges EcoItems assigns [glyphs](../glyphs/index.md) from (imported Oraxen packs in particular). EcoItems reads every character the imported fonts define and assigns its own glyphs **around** them, so new glyphs never collide. If a glyph was assigned *before* the import was added (or uses an explicit `char:`) and now collides, you'll get a console warning — the imported pack's character wins in that case, and the warning tells you how to re-assign the glyph.

## Caveats

- If you use [animated glyphs](../glyphs/animated-glyphs.md), EcoItems overrides the vanilla text core shaders. An imported pack that also ships text shaders (some nameplate/text-effect packs do) will have those replaced — you'll get a console warning when this happens.
- Imported packs are included as-is; EcoItems doesn't rewrite their custom model data or item definitions. Plugins that reference their own pack's assets keep working because paths are preserved.
