---
title: Migrating to EcoItems
sidebar_position: 1
---

# Migrating to EcoItems

EcoItems can import your setup from **Oraxen**, **Nexo**, or **ItemsAdder** — items, custom blocks, furniture, glyphs, sounds, and resource pack assets.

## How to migrate

1. **Back up your server**, including worlds.
2. Copy the contents of the old plugin's folder into the matching subfolder of `plugins/EcoItems/migrations/`:
   - everything inside `plugins/Oraxen/` → `migrations/Oraxen/`
   - everything inside `plugins/Nexo/` → `migrations/Nexo/`
   - everything inside `plugins/ItemsAdder/` → `migrations/ItemsAdder/`
3. Remove the old plugin's jar (keep its folder until you're happy with the result).
4. Run **`/ecoitems migrate <oraxen|nexo|itemsadder>`** from the console.
5. Read the console output — everything that couldn't be converted is listed there.
6. Test your items and pack, then delete the migrations folder contents.

Converted configs land in `items/imported/<source>/`, `glyphs/imported/<source>/` and `sounds/imported/<source>/` — normal EcoItems configs you can edit and reorganize freely. Pack assets are copied into `pack/`. Running a migration again skips files that already exist, so you can safely re-run after deleting a bad conversion.

## What converts

| | Oraxen | Nexo | ItemsAdder |
|---|---|---|---|
| Items (name, lore, material, components, enchants, attributes) | ✅ | ✅ | ✅ |
| Item textures/models incl. bow/shield/rod state models | ✅ | ✅ | ✅ (textures/models) |
| Custom blocks (noteblock/stringblock/chorus) | ✅ | ✅ | ✅ (`REAL_NOTE`/`REAL_WIRE`) |
| Furniture (hitboxes, seats, lights, placement, display) | ✅ | ✅ | ✅ |
| Glyphs (incl. bitmap sheets) | ✅ | ✅ | — |
| Sounds + jukebox songs | ✅ | ✅ | — |
| Shaped recipes | ✅ | ✅ | — |
| Resource pack files | ✅ | ✅ | ✅ |

## World compatibility

**Custom blocks placed in your worlds keep working.** EcoItems uses the same blockstate encoding as Oraxen, and imported blocks pin their state numbers (`variation:`), so existing Oraxen worlds render and behave identically. Nexo and ItemsAdder use different numbering — the migration translates it through the actual blockstate, so their worlds carry over too.

**Placed furniture does not carry over.** Old furniture entities belong to the old plugin; break them before migrating or clean them up afterwards (their display entities show the old item and can be removed).

:::caution Migrate before configuring blocks
Block state numbers are first-come-first-served. Run the migration on a **fresh** EcoItems install (and delete the example block items in `items/examples/`) so imported blocks keep the state numbers your old worlds were built with — the console warns if a number was already taken.
:::

## What doesn't convert

Anything skipped is logged with the item id during migration. The notable ones:

- **Behavior mechanics** (Oraxen's `commands`, `lifeleech`, IA's `events`, etc.) — rebuild these with [libreforge effects](https://plugins.auxilor.io/effects), which are far more powerful.
- **Storage blocks/furniture, evolving crops, saplings, farmblocks, doors, beds, connectables** — not yet supported in EcoItems.
- Shapeless/furnace recipes, `ItemFlags`, `PotionEffects`, legacy list-style attribute modifiers.
- Item-frame or armor-stand furniture converts to display-entity furniture — check how it looks.
- Messages/settings — EcoItems' own config layout differs; reconfigure `config.yml`/`lang.yml`/`pack.yml` manually.
