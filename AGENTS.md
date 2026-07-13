# EcoItems

A fully-featured custom content plugin for Minecraft 1.21.8 - 26.2: custom items, resource packs,
glyphs, sounds, HUDs, paintings, jukebox songs, item state models, pack merging, custom blocks,
furniture, and migration tools for importing setups from other custom item plugins.
Supports Spigot and Paper - functionality that requires (or is much nicer with) Paper
APIs may require Paper and refuse to work on Spigot, that is okay. It is however a requirement that
EcoItems works at least to some extent on Spigot, due to SpigotMC.org rules.

Built on [eco](https://github.com/Auxilor/eco) and [libreforge](https://github.com/Auxilor/libreforge).
Their full sources are at `~/IdeaProjects/eco` and `~/IdeaProjects/libreforge`. Sibling plugins
(`~/IdeaProjects/EcoScrolls`, `EcoArmor`, `EcoSkills`, `EcoEnchants`, `EcoMobs`) are the style
reference: **when unsure how to write something, find how a sibling plugin does it and match that.**

## The single most important rule

This codebase was once rewritten by an agent into 82k lines of staging queues, config freezing,
schema validation passes, ownership manifests, diagnostics files, capability negotiation, and
deprecated-on-arrival compat shims. All of it was deleted. Do not build infrastructure; build
features the way eco plugins build them. If a change needs a new abstraction layer, a queue, a
codegen step, a shell script, or a "manager", it is almost certainly wrong - simplify until it
doesn't.

## Repository layout

```
eco-core/core-plugin/
  src/main/kotlin/com/willfp/ecoitems/   # the plugin (free + paid)
    EcoItemsPlugin.kt                    # LibreforgePlugin entry point
    items/                               # EcoItem, EcoItems registry, recipes, listeners
    blocks/                              # custom blocks: state math, allocator, listeners
    furniture/                           # furniture: config, placement, listeners
    glyphs/, sounds/, huds/, paintings/  # data-driven content categories
    migration/                           # importers for other custom item plugins
    rarity/                              # Rarity, Rarities
    display/                             # DisplayModules (lore/name rendering)
    commands/, libreforge/, util/
    nms/ItemComponentsProxy.kt           # proxy interface + config->plain-value helper
    pack/PackFeature.kt                  # free/paid seam (interface + reflective loader)
  src/main/resources/                    # config.yml, lang.yml, items/, rarities/, recipes/
  src/paid/kotlin/com/willfp/ecoitems/   # paid-only source (folded into main unless -Pfree)
    pack/                                # pack build, publishers, delivery, defaults
  src/paid/resources/                    # pack.yml, items/{defaults,examples}/, glyphs/, sounds/,
                                         # pack/ (a vanilla-structured resource pack)
eco-core/core-nms/v1_21_8 ... v26_2/     # one paperweight module per supported version
packhost/                                # git submodule -> github.com/Auxilor/packhost
documentation/ecoitems/                  # user docs, pulled by an external tool (no build step)
plans/                                   # overhaul-plan.md is committed; plans/context/ NEVER is
```

`plans/context/` holds vendored third-party reference code and test fixtures for the migration
tools. It is gitignored and must never be committed or shipped.

**Licensing.** Everything under `src/paid/resources` ships to customers. Only add assets that are
original work, owner-licensed, or generated in-session (procedural textures/sounds are fine).
Never copy assets from other plugins or from the vanilla client into shipped resources.

## Conventions (the eco/libreforge way)

**Lifecycle.** `EcoItemsPlugin : LibreforgePlugin()`. Override `handleEnable`, `handleReload`,
`handleDisable`, and the `load*()` list providers (`loadConfigCategories`, `loadListeners`,
`loadPluginCommands`, `loadDisplayModules`). A top-level `internal lateinit var plugin` singleton is
set in `init {}` and used everywhere. Plugin constructors and constructor-registered `onLoad`
callbacks must use only `com.willfp.libreforge.loader/**`; the full libreforge runtime is installed
by the base `LibreforgePlugin` `onLoad(START)` callback, so anything touching other libreforge
packages belongs in `handleEnable()` or later. Category order matters: Sounds and Paintings load
before EcoItems so component warnings can recognise pending datapack registrations.

**Config-driven classes.** One yml file = one object. A class takes `(id: String, config: Config)`
and reads every field eagerly in its constructor/property initializers - no schemas, no validation
passes, no normalizers. Effects/conditions compile via `Effects.compile(config.getSubsections(...),
ViolationContext(plugin, "Item ID $id"))`; bad config surfaces as libreforge violations or
`plugin.logger.warning(...)` lines, nothing fancier. Registries are libreforge `ConfigCategory` /
`RegistrableCategory` objects (`EcoItems`, `Rarities`, `EcoItemsRecipes`, `Glyphs`, `Sounds`,
`Huds`, `Paintings`) that `clear()` and re-`register()` on every reload.

**Reloads are synchronous.** eco clears the categories, re-reads every yml, and calls
`handleReload()` - on the main thread, every time, including once during startup (so `handleReload`
runs at least twice on boot; make it idempotent). A reload lagspike is acceptable; reloads happen in
development. Never stage, queue, defer, or async a reload.

**Items and PDC.** Items carry exactly one persistent data key: `ecoitems:item` (STRING = item id),
via the `ItemStack.ecoItem` / `FastItemStack.ecoItem` extensions in `items/ItemUtils.kt`. Do not add
versions, revisions, hashes, or any other keys - extra PDC breaks stacking. (Furniture entities have
their own `ecoitems:furniture*` keys - that's entity PDC, not item PDC.) Each `EcoItem` registers
an eco `CustomItem` so `ecoitems:<id>` resolves in the item lookup system. `EcoItem : Holder`, and
`EcoItemFinder : ItemHolderFinder<EcoItem>` provides holders to libreforge. Display lore lines are
prefixed with `Display.PREFIX`. Config ids are `[a-z0-9_]`; `_`-prefixed yml files are examples and
never loaded. `item.name` is shorthand for the `minecraft:item_name` component; `item.display-name`
is the legacy custom-name path through the display system.

**Item components.** `item.components` in an item config accepts any vanilla data component in
vanilla command format; keys without a namespace are treated as `minecraft:`. Flow: `Config` ->
`toComponentValues()` (plain maps/lists/scalars) -> `ItemComponentsProxy.withComponents()` ->
per-version NMS impl resolves the `DataComponentType` from the registry, converts values to NBT,
parses through the vanilla codec, and sets it on the stack.
Never hand-implement a specific component (no FoodComponentHandler-style classes) - the codec path
supports every present and future component for free. Gotcha: wrapper configs (libreforge's
separator-ambivalent wrapper) do not implement `Config.toMap()` - it silently returns an empty map.
`toComponentValues()` roundtrips through `toPlaintext()` for this reason; don't "simplify" it away.

**NMS proxies.** Interface in `nms/` named `XxxProxy`; implementation named `Xxx` in
`com.willfp.ecoitems.proxy.<version>` in each `core-nms/<version>` module. Resolution is automatic
via `plugin.getProxy(XxxProxy::class.java)` + `proxy-package` in `eco.yml` + eco's
`ProxyConstants.NMS_VERSION`. There is no `bucket` property or any other self-identification - eco
handles it. The five modules are near-identical copies; the only real difference is
`ResourceLocation` (v1_21_8, v1_21_10) vs `Identifier` (v1_21_11+). Root gradle wires `reobf`
configuration for `v1_21_*` modules and `shadow` for `v26_*`.

## Custom blocks and furniture

**Blocks** (`blocks/`, main source) hijack unused vanilla blockstates: `noteblock` (774 states),
`stringblock`/tripwire (127), `chorus` (63). The state IS the block's identity - `BlockBacking`
holds the variation math, `BlockVariations` auto-assigns and persists numbers to
`block-variations.yml` (never edit once blocks are placed; explicit `variation:` pins are never
persisted). A `block:` section on any item config makes the item a placer. Each block registers an
eco `CustomBlock` + a `BlockProvider("ecoitems")` so `ecoitems:<id>` works in every plugin's block
lookups. Custom hardness uses the `BLOCK_BREAK_SPEED` attribute on `BlockDamageEvent` (string
blocks are always insta-break - client-side, nothing can slow a zero-hardness backing). Physics
suppression is listener-based and works on Spigot; on Paper the console recommends the
`block-updates` flags in paper-global.yml. Drops go through eco's `DropQueue` for player breaks.

**Furniture** (`furniture/`, main source) is an `ItemDisplay` showing the item itself, plus
`Interaction` hitboxes, real `BARRIER` collision blocks, invisible ArmorStand seats, and real
`LIGHT` blocks. All placement data lives in the base entity's PDC; barriers resolve to their
furniture by a nearby-entity scan. Furniture is naturally blast-proof (barriers and displays are
explosion-immune). Barrier mining insta-breaks via `BlockDamageEvent.setInstaBreak`.

Both support per-event libreforge effects (`effects.punch`, `right-click`, `shift-` variants,
`place`, `break`, furniture `sit`) via `libreforge/ContentEffects.kt` - compiled with
`Effects.compileChain`, dispatched with `TriggerData(...).dispatch(player.toDispatcher())`.

## Free/paid split

`src/paid/kotlin` and `src/paid/resources` are folded into the main compilation unless building
with `-Pfree`. The only generated code is `BuildConfig.FREE_VERSION` (a tiny gradle task - do not
add more codegen). Main code must never reference paid classes directly; the one seam is
`PackFeatures.instance` in `pack/PackFeature.kt`, which reflectively loads the paid
`EcoItemsPackFeature` object. Paid-only features hang off that interface. The free jar must always
compile and run: check `BuildConfig.FREE_VERSION` (e.g. EcoItem warns that textures need paid) and
keep paid resources (pack.yml, textures, default/example items) out of `src/main/resources`.

## The pack system (paid)

Flow, all in `src/paid/kotlin/.../pack/`, orchestrated by `EcoItemsPackFeature.handleReload`:

1. `PackDefaults.ensure` - extracts bundled `pack/**` jar entries to `plugins/EcoItems/pack/` on
   first run, and ships new bundled files to existing installs without overwriting edits
   (`pack/builtin/**` jar entries are feature assets injected at build time instead - fixes to
   those reach every install). The pack folder mirrors a vanilla resource pack:
   `pack/assets/<ns>/{textures,models,sounds,font,lang,...}` at their natural paths, optional
   `pack/pack.png`, optional `pack/pack.mcmeta` (only `overlays` entries used -
   description/formats are always generated). Legacy `pack/{textures,models,glyphs,sounds,lang}`
   folders trigger a console warning to move files into `pack/assets/`. External packs dropped
   in `pack/imports/` (zips or folders) merge in as the lowest-priority layer via `PackImports`
   - fonts/sounds/lang/atlases merge smartly, imported font codepoints are reserved so glyph
   auto-assignment routes around them, and imported pack.mcmeta overlay entries carry into the
   final mcmeta.
2. `ItemPackAsset.fromItem` per item - `item.texture` / `item.model` (and glyph/sound refs) are
   `[ns:]path` `PackLocation`s (default ns `ecoitems`, no extension) relative to
   `textures/` / `models/`: `texture: item/foo` reads `pack/assets/ecoitems/textures/item/foo.png`
   (optional `texture-parent: handheld`; a model is generated unless a json exists at the same
   path under `models/`), `minecraft:` model refs pass through without a file check.
3. `PackBuilder.build` - copies the vanilla-structured pack folder wholesale (merge-aware:
   fonts, sounds.json, lang, atlases merge with generated/imported content; everything else from
   the pack folder wins on collision), generates `assets/ecoitems/items/<id>.json` definitions
   (+ simple models for plain textures), block blockstates/models, and writes a sorted,
   fixed-timestamp zip to `plugins/EcoItems/pack.zip` (stable SHA-1 = dedup/re-download no-ops).
   One magic file: `pack/assets/minecraft/lang/global.json` applies its entries to every language
   (per-language files win per key, `_`-prefixed keys are comments, values support `:glyph:`
   placeholders).
4. A `PackPublisher` (`hosted` = POST to packhost, `self-hosted` = JDK HttpServer, `external` =
   export dir, `none`) returns a `PublishedPack`.
5. `PackDelivery` sends it (fixed pack UUID so re-sends replace, `send-on-join`/`send-on-reload`).

Items get their texture through the `minecraft:item_model` component set to `ecoitems:<id>` -
never CustomModelData. One pack covers all supported client versions; `PackMcmeta.MAX_FORMAT` needs
a bump each Minecraft release (cosmetic if stale - server-sent packs bypass the format gate).
Textures under `textures/item/**` and `textures/block/**` stitch automatically via the vanilla
blocks atlas; other roots need directory sources in a user atlas file (migrations write one), and
a console warning fires for uncovered item textures.

## Migration tools

`migration/` (main source) imports setups from Oraxen, Nexo, and ItemsAdder: users copy the old
plugin folder's contents into `migrations/<Source>/` and run `/ecoitems migrate <source>`.
Converted configs land in `items/imported/<source>/`, `glyphs/imported/`, `sounds/imported/`;
pack assets copy into `pack/`. World compatibility is the key invariant: our blockstate math
matches Oraxen's exactly (imported blocks pin `variation:` numbers), Nexo's renumbered variations
translate through the actual blockstate, and ItemsAdder pins from its storage caches. This is the
only place in the codebase that should know anything about those plugins' config formats. Test
fixtures for the converters live in `plans/context/` (gitignored).

## packhost

A deliberately minimal Bun/TypeScript service (own repo, submodule here). Anonymous
content-addressed uploads (SHA-1), S3 storage, Postgres metadata, per-IP daily quotas. Hard
constraints: zero npm runtime dependencies (Bun built-ins only), environment surface is exactly
`DATABASE_URL` + `S3_*` (+ `PORT`); caps/quotas are code constants, never env vars. No auth stacks,
signing, leases, or retention workers. It has a small `bun test` suite against in-memory fakes -
keep it passing (`bun test`, `bunx tsc --noEmit`). Work on packhost lands in its own repo; remember
to update the submodule pointer here.

## Building and verifying

- `./gradlew build` (paid) and `./gradlew build -Pfree` - both must stay green; jars land in `bin/`.
- Always run gradle from the repo root (shell cwd persists between commands and a wrong-dir
  `./gradlew` fails silently through grep filters). `bin/` accumulates old versioned jars - after
  building, check the jar's mtime/contents before deploying it anywhere.
- The free jar must contain no paid classes/resources (`unzip -l` it if in doubt).
- There are no plugin unit tests, by convention (sibling plugins have none). Verify by running:
  drop the jar + an eco jar (in `~/Downloads`) on a Paper 1.21.8 server (paper jars also in
  `~/Downloads`), boot, check the log, and use RCON for `/ecoitems reload` / `/ecoitems give`.
  A scratch server setup with an RCON script has been used before; item component errors show as
  `[EcoItems] Invalid component on item <id>: ...` warnings.
- Local packhost stack: `docker compose up` in the packhost repo (Postgres + MinIO), point
  `pack.yml` `delivery.hosted.url` at `http://127.0.0.1:3000`.

## Dos and don'ts

- **Do** read the sibling plugins before inventing anything.
- **Do** keep configs data-driven and read eagerly; report problems as console warnings.
- **Do** keep docs in `documentation/ecoitems/` in sync with config/behaviour changes.
- **Don't** add: reload queues/staging, config freezing/snapshots, schema validators, diagnostics
  files, capability systems, platform-adapter layers, ownership manifests, extra PDC keys, `bucket`
  properties on proxies, build-time codegen (beyond BuildConfig), shell scripts, gradle Exec tasks,
  or `@Deprecated` compat shims for unreleased code.
- **Don't** write per-component handlers - the generic codec path covers all components.
- **Don't** commit `plans/context/`, ever.
- **Don't** ship third-party assets; see the licensing note under the repository layout.
- **Don't** grow packhost's env or dependency surface.
- **Don't** touch `base-damage`/`effective-durability`-style derived mechanics - they were removed
  on purpose; vanilla components express these directly.

## Adding new content types

The established pattern: a new `ConfigCategory` + data-driven class (like `Rarities`/`Rarity` or
`Paintings`/`Painting`), a folder of yml configs, and - if it has pack assets - asset generation in
the pack build step alongside `ItemAssetGenerator`. Paid-only content hangs off the existing
`PackFeature` seam. No new frameworks.
