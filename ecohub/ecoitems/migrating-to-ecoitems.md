---
title: "Migrating to EcoItems"
sidebar_position: 15
---

Switching to EcoItems doesn't mean rebuilding your setup by hand. EcoItems can import from **Oraxen**, **Nexo**, or **ItemsAdder**: items, custom blocks, furniture, glyphs, sounds, and resource pack assets. This page covers running a migration, what converts, and what carries over in your worlds.

## How to migrate

1. **Back up your server**, including worlds.
2. Copy the contents of the old plugin's folder into the matching subfolder of `plugins/EcoItems/migrations/`. These subfolders are created automatically on startup:
   - everything inside `plugins/Oraxen/` → `migrations/Oraxen/`
   - everything inside `plugins/Nexo/` → `migrations/Nexo/`
   - everything inside `plugins/ItemsAdder/` → `migrations/ItemsAdder/`

   Migration only reads **extracted folders**, not `.zip` archives, so unpack a zipped export into the subfolder first. A single wrapping folder inside (e.g. `migrations/Nexo/Nexo/...`) is handled automatically.
3. Remove the old plugin's jar, but keep its folder until you're happy with the result.
4. Run **`/ecoitems migrate <oraxen|nexo|itemsadder>`** from the console.
5. Read the console output, which lists everything that couldn't be converted.
6. Test your items and pack, then delete the contents of the migrations folder.

Converted configs land in `items/imported/<source>/`, `glyphs/imported/<source>/`, and `sounds/imported/<source>/`. They're normal EcoItems configs you can edit and reorganise freely. Running a migration again skips files that already exist, so you can safely re-run after deleting a bad conversion.

:::caution Migrate before configuring blocks
Block state numbers are first come, first served. Run the migration on a **fresh** EcoItems install (and delete the example block items in `items/examples/`) so imported blocks keep the state numbers your old worlds were built with. The console warns if a number was already taken.
:::

### Where pack assets go

Most textures, models, and sounds from the old plugin's pack are flattened straight into `pack/assets/` alongside your own files. The exception is **standalone external pack zips or folders** (e.g. Oraxen's `pack/external_packs/`) that the old plugin merged rather than flattened. Those are copied as-is into `pack/imports/`, the same folder used for [merging other packs](resource-packs-merging-packs).

`pack/imports/` isn't there by default. It's created the moment something needs it (the pack builder on its first run, or a migration that finds one of these standalone packs), so don't worry if it's missing before then.

## What converts

| | Oraxen | Nexo | ItemsAdder |
|---|---|---|---|
| Items (name, lore, material, components, enchants, attributes) | ✅ | ✅ | ✅ |
| Item textures and models, including bow, shield, and rod state models | ✅ | ✅ | ✅ (textures and models) |
| Custom blocks (noteblock, stringblock, chorus) | ✅ | ✅ | ✅ (`REAL_NOTE`, `REAL_WIRE`) |
| Furniture (hitboxes, seats, lights, placement, display) | ✅ | ✅ | ✅ |
| Glyphs, including bitmap sheets | ✅ | ✅ | ❌ |
| Sounds and jukebox songs | ✅ | ✅ | ❌ |
| Crafting recipes (shaped and shapeless) | ✅ | ✅ | ✅ |
| Furnace, blast furnace, smoker, and campfire recipes | ✅ | ✅ | ✅ |
| Stonecutter and brewing stand recipes | ✅ | ✅ | ✅ |
| Smithing table recipes | ❌ | ❌ | ✅ |
| Resource pack files | ✅ | ✅ | ✅ |

## World compatibility

**Custom blocks placed in your worlds keep working.** EcoItems uses the same blockstate encoding as Oraxen, and imported blocks pin their state numbers (`variation:`), so existing Oraxen worlds render and behave identically. Nexo and ItemsAdder use different numbering, which the migration translates through the actual blockstate, so their worlds carry over too.

**Placed furniture does not carry over.** Old furniture entities belong to the old plugin, so break them before migrating or clean them up afterwards (their display entities show the old item and can be removed).

## What doesn't convert

Anything skipped is logged with the item ID during migration. The notable ones:

- **Behaviour mechanics** (Oraxen's `commands` and `lifeleech`, ItemsAdder's `events`, and so on). Rebuild these with [libreforge effects](https://hub.auxilor.io/wiki/libreforge/configuring-an-effect), which are far more powerful.
- **Storage blocks and furniture, evolving crops, saplings, farmblocks, doors, beds, and connectables** from the old plugin's format. Rebuild them with EcoItems' own [furniture](how-to-make-furniture), [crops](how-to-make-a-crop), and [blocks](how-to-make-a-block).
- **Anvil repairs** (ItemsAdder). They restore durability rather than make an item, so there's nothing to convert them into.
- **Extra recipes for an item beyond the first.** An EcoItem has one recipe, so where several make the same item, the first is kept and the rest are logged. Recipes using an item **tag** as an ingredient are skipped too.
- **`ItemFlags`, `PotionEffects`, and legacy list-style attribute modifiers.**
- **Item frame or armor stand furniture**, which converts to display entity furniture, so check how it looks.
- **Messages and settings.** EcoItems' own config layout differs, so reconfigure `config.yml`, `lang.yml`, and `pack.yml` manually.

<hr/>

## Where to go next

- **Review the result:** check your converted items against [How to Make an Item](how-to-make-an-item).
- **Blocks and furniture:** [How to Make a Block](how-to-make-a-block) and [How to Make Furniture](how-to-make-furniture) for the options imported configs use.
- **The pack:** [Resource Packs](resource-packs) for where imported assets live and how the pack is delivered.
