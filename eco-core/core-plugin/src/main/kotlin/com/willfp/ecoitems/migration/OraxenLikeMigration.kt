package com.willfp.ecoitems.migration

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.blocks.BlockBacking
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

enum class Dialect(val folder: String, val itemRef: String) {
    ORAXEN("Oraxen", "oraxen_item"),
    NEXO("Nexo", "nexo_item")
}

/**
 * Imports an Oraxen or Nexo setup. Nexo is an Oraxen fork, so the shapes are
 * shared; the dialect handles the renamed keys and Nexo's renumbered block
 * variations (which get translated through the blockstate into ours, keeping
 * already-placed world blocks working).
 */
class OraxenLikeMigration(
    private val plugin: EcoItemsPlugin,
    private val dialect: Dialect
) {
    private val result = MigrationResult(plugin)
    private val source = resolveRoot(
        plugin,
        plugin.dataFolder.resolve("migrations/${dialect.folder}"),
        "items", "pack", "glyphs"
    )
    private val itemIds = mutableSetOf<String>()
    private val equipmentAssets = mutableSetOf<String>()

    fun run(): MigrationResult {
        val items = loadItems()
        itemIds.addAll(items.keys)

        val recipes = loadShapedRecipes()

        for ((id, section) in items) {
            convertItem(id, section, recipes[id])
        }

        convertGlyphs()
        convertSounds()
        copyPack()

        return result
    }

    // ------------------------------------------------------------------ items

    /** All item sections across the items folder, with template inheritance applied. */
    private fun loadItems(): Map<String, ConfigurationSection> {
        val raw = mutableMapOf<String, ConfigurationSection>()
        for (file in source.resolve("items").walkTopDown().filter { it.extension == "yml" }) {
            val yaml = YamlConfiguration.loadConfiguration(file)
            for (id in yaml.getKeys(false)) {
                yaml.getConfigurationSection(id)?.let { raw[id] = it }
            }
        }

        val resolved = mutableMapOf<String, ConfigurationSection>()
        for ((id, section) in raw) {
            if (section.getBoolean("template")) {
                continue
            }

            val templateId = section.getString("template")
            val template = templateId?.let { raw[it] }
            if (templateId != null && template == null) {
                result.warn("Item $id references missing template '$templateId'")
            }

            resolved[id] = if (template != null) merged(template, section) else section
        }

        return resolved
    }

    private fun merged(base: ConfigurationSection, over: ConfigurationSection): ConfigurationSection {
        val out = YamlConfiguration()
        for (key in base.getKeys(true)) {
            if (!base.isConfigurationSection(key)) out.set(key, base.get(key))
        }
        for (key in over.getKeys(true)) {
            if (!over.isConfigurationSection(key)) out.set(key, over.get(key))
        }
        return out
    }

    private fun convertItem(id: String, section: ConfigurationSection, recipe: List<String>?) {
        val out = YamlConfiguration()

        out.set("item.item", (section.getString("material") ?: "PAPER").lowercase())

        (section.getString("itemname") ?: section.getString("displayname"))?.let {
            out.set("item.name", it)
        }
        if (section.isList("lore")) {
            out.set("item.lore", section.getStringList("lore"))
        }

        convertPack(id, section.getConfigurationSection("Pack"), out)
        convertComponents(id, section, out)

        recipe?.let {
            out.set("item.craftable", true)
            out.set("item.recipe", it)
            result.recipes++
        }

        section.getConfigurationSection("Mechanics")?.let { mechanics ->
            for (mechanic in mechanics.getKeys(false)) {
                val config = mechanics.getConfigurationSection(mechanic) ?: continue
                when (mechanic) {
                    "noteblock", "stringblock", "chorusblock", "block", "custom_block" ->
                        convertBlock(id, mechanic, config, section.getConfigurationSection("Pack"), out)

                    "furniture" -> convertFurniture(id, config, section.getConfigurationSection("Pack"), out)

                    else -> result.warn("Item $id: mechanic '$mechanic' is not supported and was skipped")
                }
            }
        }

        out.set("effects", emptyList<String>())
        out.set("conditions", emptyList<String>())

        val file = plugin.dataFolder.resolve("items/imported/${dialect.folder.lowercase()}/$id.yml")
        if (writeConverted(result, file, out)) {
            result.items++
        }
    }

    private fun convertPack(id: String, pack: ConfigurationSection?, out: YamlConfiguration) {
        if (pack == null) return

        val model = pack.getString("model")
        if (!model.isNullOrEmpty()) {
            out.set("item.model", assetRef(model))
        } else if (pack.isConfigurationSection("textures")) {
            // Map-form textures belong to a block model; convertBlock picks
            // them up. Non-block items fall back to the first entry.
            val map = pack.getConfigurationSection("textures")!!
            map.getKeys(false).firstOrNull()?.let { key ->
                out.set("item.texture", assetRef(map.getString(key)!!))
            }
        } else {
            textureList(pack, "textures", "texture").firstOrNull()?.let {
                out.set("item.texture", assetRef(it))
            }
            pack.getString("parent_model")?.takeIf { it.startsWith("item/") }?.let {
                out.set("item.texture-parent", it.removePrefix("item/"))
            }
        }

        // State models map 1:1 onto our convenience keys.
        for (state in listOf("pulling", "charged", "firework", "blocking", "cast", "damaged")) {
            for (kind in listOf("model", "texture", "models", "textures")) {
                val key = "${state}_$kind"
                if (!pack.contains(key)) continue
                val ours = "item.$state-$kind"
                if (pack.isList(key)) {
                    out.set(ours, pack.getStringList(key).map { assetRef(it) })
                } else {
                    pack.getString(key)?.let { out.set(ours, assetRef(it)) }
                }
            }
        }
    }

    private fun convertComponents(id: String, section: ConfigurationSection, out: YamlConfiguration) {
        val components = mutableMapOf<String, Any>()

        section.getConfigurationSection("Enchantments")?.let { enchants ->
            components["minecraft:enchantments"] = enchants.getKeys(false).associate { key ->
                (if (":" in key) key else "minecraft:$key") to enchants.getInt(key)
            }
        }

        if (section.getBoolean("unbreakable")) {
            components["minecraft:unbreakable"] = emptyMap<String, Any>()
        }

        // Leather-dye tint (LEATHER_* base + "color"); without it the tinted
        // model renders white.
        section.getString("color")?.let { raw ->
            val rgb = parseDyeColor(raw)
            if (rgb != null) {
                components["minecraft:dyed_color"] = rgb
            } else {
                result.warn("Item $id: couldn't parse color '$raw' - dye tint not converted")
            }
        }

        section.getConfigurationSection("Components")?.let { comps ->
            for (key in comps.getKeys(false)) {
                val value = plain(comps.get(key)) ?: continue
                when (key) {
                    "durability" -> components["minecraft:max_damage"] =
                        (value as? Map<*, *>)?.get("value") ?: value

                    "fire_resistant" -> components["minecraft:damage_resistant"] =
                        mapOf("types" to "#minecraft:is_fire")

                    // Oraxen wraps the song key in a map; vanilla wants the string.
                    "jukebox_playable" -> components["minecraft:jukebox_playable"] =
                        (value as? Map<*, *>)?.get("song_key") ?: value

                    // Enum-style slot names (LEGS) and the legacy model key
                    // don't parse as the vanilla component.
                    "equippable" -> components["minecraft:equippable"] =
                        (value as? Map<*, *>)?.entries?.associate { (k, v) ->
                            val name = if (k == "model" && "asset_id" !in value) "asset_id" else k.toString()
                            if (name == "asset_id" && ":" in v.toString()) {
                                equipmentAssets += v.toString()
                            }
                            name to if (name == "slot") v.toString().lowercase() else v
                        } ?: value

                    else -> components[if (":" in key) key else "minecraft:$key"] = value
                }
            }
        }

        section.getConfigurationSection("AttributeModifiers")?.let { attributes ->
            val list = attributes.getKeys(false).mapNotNull { name ->
                val attr = attributes.getConfigurationSection(name) ?: return@mapNotNull null
                val type = attr.getString("attribute") ?: return@mapNotNull null
                mapOf(
                    "id" to "ecoitems:$name",
                    "type" to "minecraft:" + type.lowercase().removePrefix("generic_"),
                    "amount" to attr.getDouble("amount"),
                    "operation" to (attr.getString("operation") ?: "ADD_NUMBER").lowercase(),
                    "slot" to (attr.getString("slot") ?: "any").lowercase()
                )
            }
            if (list.isNotEmpty()) {
                components["minecraft:attribute_modifiers"] = list
            }
        }
        if (section.isList("AttributeModifiers")) {
            result.warn("Item $id: legacy list-style AttributeModifiers are not converted")
        }

        if (section.contains("ItemFlags")) {
            result.warn("Item $id: ItemFlags are not converted (use minecraft:tooltip_display components)")
        }
        if (section.contains("PotionEffects")) {
            result.warn("Item $id: PotionEffects are not converted")
        }

        for ((key, value) in components) {
            out.set("item.components.$key", value)
        }
    }

    // ----------------------------------------------------------------- blocks

    /**
     * Positional texture-list ordering for parent models where a Nexo/Oraxen
     * dialect gives `textures:` as a plain list instead of a keyed map.
     * Only includes parents we have confirmed real-world ordering for -
     * unlisted parents with a list-form `textures:` are left for the model
     * fallback rather than guessed at and silently mismatched.
     */
    private val LIST_TEXTURE_KEYS = mapOf(
        "cube_all" to listOf("all"),
        "cross" to listOf("cross"),
        "cube_column" to listOf("end", "side") // Nexo list order: [top, sides]
    )

    /**
     * Nexo/Oraxen key a pillar's shared top-and-bottom texture as vanilla's
     * `end` (from block/cube_column); our generated block models key faces
     * top/bottom/side, so split `end` across both faces - the same path
     * twice - and move the parent to the equivalent cube_bottom_top.
     */
    private fun normalizeTextureKeys(textures: Map<String, String>): Map<String, String> {
        val end = textures["end"] ?: return textures

        val normalized = linkedMapOf(
            "top" to (textures["top"] ?: end),
            "bottom" to (textures["bottom"] ?: end)
        )

        for ((key, texture) in textures) {
            if (key !in normalized && key != "end") {
                normalized[key] = texture
            }
        }

        return normalized
    }

    private fun normalizeTextureParent(parent: String?, textures: Map<String, String>): String? =
        if (parent == "cube_column" && "end" in textures) "cube_bottom_top" else parent

    /** Oraxen forks vary between snake_case and kebab-case keys. */
    private fun ConfigurationSection.any(key: String): Any? =
        get(key) ?: get(key.replace("_", "-"))

    private fun ConfigurationSection.anyString(key: String): String? = any(key)?.toString()

    private fun ConfigurationSection.anySection(key: String): ConfigurationSection? =
        (any(key) as? ConfigurationSection)

    private fun convertBlock(
        id: String,
        mechanic: String,
        config: ConfigurationSection,
        pack: ConfigurationSection?,
        out: YamlConfiguration
    ) {
        val backing = when {
            mechanic == "noteblock" -> BlockBacking.NOTEBLOCK
            mechanic == "stringblock" -> BlockBacking.STRINGBLOCK
            mechanic == "chorusblock" -> BlockBacking.CHORUS
            else -> when (config.getString("type")?.uppercase()) {
                "FULL", "NOTEBLOCK" -> BlockBacking.NOTEBLOCK
                "STRING", "STRINGBLOCK" -> BlockBacking.STRINGBLOCK
                "CHORUS", "CHORUSBLOCK" -> BlockBacking.CHORUS
                else -> {
                    result.warn("Item $id: custom block type '${config.getString("type")}' is not supported")
                    return
                }
            }
        }

        out.set("block.type", backing.id)

        (config.any("custom_variation") as? Number)?.let { raw ->
            val variation = translateVariation(backing, raw.toInt())
            if (variation != null) {
                out.set("block.variation", variation)
            } else {
                result.warn(
                    "Item $id: variation $raw doesn't translate; " +
                        "auto-assigning (already-placed blocks of it will look wrong)"
                )
            }
        }

        // The block's look: a map of textures (generated model, like our
        // block.textures), a positional textures list keyed by parent_model,
        // an explicit model, or the item's single texture - which then
        // belongs on the block, not the item.
        val textureMap = pack?.getConfigurationSection("textures")
        val listTextures = pack?.let { textureList(it, "textures", "texture") } ?: emptyList()
        val parentModel = pack?.getString("parent_model")?.removePrefix("block/")
        val listKeys = parentModel?.let { LIST_TEXTURE_KEYS[it] }
        // Mechanics-level `model` is often just Nexo's internal model name
        // (e.g. "andesite_bricks"), not an asset path - only trust it as a
        // literal override when it looks like a real location.
        val model = (config.anyString("model") ?: config.anySection("appearance")?.anyString("model"))
            ?.takeIf { "/" in it || ":" in it }
        when {
            textureMap != null -> {
                val raw = textureMap.getKeys(false).associateWith { textureMap.getString(it)!! }
                for ((key, texture) in normalizeTextureKeys(raw)) {
                    out.set("block.textures.$key", assetRef(texture))
                }
                normalizeTextureParent(parentModel, raw)?.let { out.set("block.texture-parent", it) }
                out.set("item.texture", null)
            }

            listKeys != null && listKeys.size == listTextures.size && listTextures.size > 1 -> {
                val raw = listKeys.zip(listTextures).toMap()
                for ((key, texture) in normalizeTextureKeys(raw)) {
                    out.set("block.textures.$key", assetRef(texture))
                }
                out.set("block.texture-parent", normalizeTextureParent(parentModel, raw))
                out.set("item.texture", null)
            }

            model != null && pack?.getBoolean("generate_model") != true ->
                out.set("block.texture", assetRef(model))

            out.contains("item.texture") -> {
                out.set("block.texture", out.getString("item.texture"))
                out.set("item.texture", null)
                out.set("item.texture-parent", null)
            }

            model != null -> out.set("block.texture", assetRef(model))
        }

        // Modern Nexo directional is a single config, like ours; the old
        // Oraxen parent/child form needs manual collapsing.
        config.getConfigurationSection("directional")?.let { directional ->
            val type = directional.getString("type")
            val oldForm = directional.getKeys(false).any { it == "parent_block" || it.endsWith("_block") }
            when {
                oldForm -> result.warn(
                    "Item $id: old-style directional (parent/child items) needs manual conversion - " +
                        "use one item with block.directional and delete the children"
                )

                type != null && type.uppercase() in setOf("LOG", "FURNACE", "DROPPER") -> {
                    out.set("block.directional", type.lowercase())
                    if (out.contains("block.variation")) {
                        out.set("block.variation", null)
                        result.warn("Item $id: directional blocks re-assign their states; placed ones may change look")
                    }
                }

                else -> result.warn("Item $id: directional type '$type' is not supported")
            }
        }

        if (config.contains("hardness")) out.set("block.hardness", config.getDouble("hardness"))
        if (config.contains("light")) out.set("block.light", config.getInt("light"))
        if (config.getBoolean("is_falling")) out.set("block.falling", true)

        config.anySection("drop")?.let { drop ->
            if (drop.contains("silktouch")) out.set("block.drops.silk-touch", drop.getBoolean("silktouch"))
            if (drop.contains("fortune")) out.set("block.drops.fortune", drop.getBoolean("fortune"))
            drop.anyString("minimal_type")?.let {
                out.set("block.minimum-tier", if (it.equals("wood", true)) "wooden" else it.lowercase())
            }
            val tools = drop.getStringList("best_tools") + drop.getStringList("best_tool")
            if (tools.isNotEmpty()) out.set("block.correct-tools", tools.map { it.uppercase() })

            val loots = drop.getMapList("loots").mapNotNull { loot -> convertLoot(loot) }
            if (loots.isNotEmpty()) out.set("block.drops.items", loots)
        }

        // Newer forks replace drop/hardness with a breaking: rule list; take
        // what maps cleanly from the first rule.
        (config.getMapList("breaking").firstOrNull())?.let { rule ->
            (rule["hardness"] as? Number)?.let { out.set("block.hardness", it.toDouble()) }
            (rule["when"] as? List<*>)?.let { tools ->
                val categories = tools.mapNotNull {
                    it.toString().substringAfterLast(":").substringAfterLast("_").uppercase()
                        .takeIf { t -> t in setOf("PICKAXE", "AXE", "SHOVEL", "HOE", "SWORD", "SHEARS") }
                }.distinct()
                if (categories.isNotEmpty()) out.set("block.correct-tools", categories)
            }
            result.warn("Item $id: breaking: rules partially converted - review hardness/drops")
        }

        config.anySection("block_sounds")?.let { convertBlockSounds(it, out, "block") }

        for (unsupported in listOf("storage", "logStrip", "farmblock", "sapling", "limited_placing")) {
            if (config.contains(unsupported)) {
                result.warn("Item $id: block option '$unsupported' is not supported and was skipped")
            }
        }

        result.blocks++
    }

    /**
     * Oraxen variations are our variations (same state math). Nexo renumbered
     * to instrument*50+note+powered*25 for note blocks and reordered the
     * tripwire property bits - translate through the actual blockstate.
     */
    private fun translateVariation(backing: BlockBacking, variation: Int): Int? {
        if (dialect == Dialect.ORAXEN) {
            return variation.takeIf { it in backing.variations }
        }

        return when (backing) {
            BlockBacking.NOTEBLOCK -> {
                val instrument = variation / 50
                val rest = variation % 50
                val note = rest % 25
                val powered = rest >= 25
                (instrument * 25 + note + (if (powered) 400 else 0) - 26).takeIf { it in backing.variations }
            }

            BlockBacking.STRINGBLOCK -> {
                // Nexo bits: N1 S2 E4 W8; ours: E1 W2 S4 N8. 16/32/64 match.
                val north = variation and 1 != 0
                val south = variation and 2 != 0
                val east = variation and 4 != 0
                val west = variation and 8 != 0
                var ours = variation and (16 or 32 or 64)
                if (east) ours = ours or 1
                if (west) ours = ours or 2
                if (south) ours = ours or 4
                if (north) ours = ours or 8
                ours.takeIf { it in backing.variations }
            }

            BlockBacking.CHORUS -> variation.takeIf { it in backing.variations }

            // Oraxen/Nexo have no mushroom mechanic; nothing maps here.
            BlockBacking.MUSHROOM, BlockBacking.MUSHROOM_RED, BlockBacking.MUSHROOM_STEM -> null
        }
    }

    // -------------------------------------------------------------- furniture

    private fun convertFurniture(
        id: String,
        config: ConfigurationSection,
        pack: ConfigurationSection?,
        out: YamlConfiguration
    ) {
        config.getString("type")?.let {
            if (it != "DISPLAY_ENTITY") {
                result.warn("Item $id: $it furniture converts, but EcoItems furniture is display-entity based - re-check its look")
            }
        }

        out.set(
            "furniture.rotation",
            if (config.getString("restricted_rotation") == "VERY_STRICT") "4-way" else "8-way"
        )

        // Barriers: Oraxen's map/keyword forms and Nexo's string lists.
        val barriers = mutableListOf<String>()
        if (config.getBoolean("barrier")) barriers += "0,0,0"
        for (entry in config.getList("barriers").orEmpty()) {
            when (entry) {
                is String -> barriers += if (entry == "origin") "0,0,0" else entry
                is Map<*, *> -> barriers += "${entry["x"] ?: 0},${entry["y"] ?: 0},${entry["z"] ?: 0}"
            }
        }
        barriers += config.getStringList("hitbox.barriers")
        if (barriers.isNotEmpty()) out.set("furniture.barriers", barriers.distinct())

        // Interaction hitboxes: "x,y,z w,h" both dialects, ours uses WxH.
        val hitboxes = mutableListOf<String>()
        config.getConfigurationSection("hitbox")?.let { hitbox ->
            if (hitbox.contains("width") || hitbox.contains("height")) {
                hitboxes += "0,0,0 ${hitbox.getDouble("width", 1.0)}x${hitbox.getDouble("height", 1.0)}"
            }
        }
        for (raw in config.getStringList("hitboxes") + config.getStringList("hitbox.interactions")) {
            val parts = raw.trim().split(" ", limit = 2)
            val size = parts.getOrNull(1)?.replace(",", "x") ?: "1x1"
            hitboxes += "${parts[0]} $size"
        }
        if (hitboxes.isNotEmpty()) out.set("furniture.hitboxes", hitboxes)

        // Seats. Seat vectors share our convention (y = 0 sits at natural
        // chair height) and pass through verbatim; Oraxen's legacy scalar
        // height is measured from the block bottom with 0.6 as the standard
        // chair. Rebasing on 0.6 alone sinks the player a full cell, so +1.0.
        val seats = mutableListOf<String>()
        config.getConfigurationSection("seat")?.let {
            val yaw = if (it.contains("yaw")) " ${it.getDouble("yaw")}" else ""
            val y = Math.round((it.getDouble("height") - 0.6 + 1.0) * 100) / 100.0
            seats += "0,$y,0$yaw"
        }
        seats += config.getStringList("seats")
        if (seats.isNotEmpty()) out.set("furniture.seats", seats)

        // Lights.
        val lights = mutableListOf<String>()
        if (config.contains("light") && !config.isConfigurationSection("light")) {
            lights += "0,1,0 ${config.getInt("light")}"
            result.warn("Item $id: furniture light moved to the cell above the origin - adjust furniture.lights if needed")
        }
        lights += config.getStringList("lights")
        lights += config.getStringList("lights.lights")
        if (lights.isNotEmpty()) out.set("furniture.lights", lights)
        if (config.getBoolean("lights.toggleable")) out.set("furniture.toggleable-lights", true)

        config.getConfigurationSection("limited_placing")?.let { placing ->
            val floor = placing.getBoolean("floor", true)
            val wall = placing.getBoolean("wall")
            val roof = placing.getBoolean("roof")
            out.set("furniture.placement.floor", floor)
            out.set("furniture.placement.wall", wall)
            out.set("furniture.placement.ceiling", roof)

            // Oraxen/Nexo wall models face out of the wall; our wall placement
            // orients them the opposite way, so imports land 180 backwards.
            // Spin the display to compensate. Floor/ceiling already face the
            // player, so only pure wall furniture can take a blanket flip.
            if (wall && !floor && !roof) {
                out.set("furniture.display.y-rotation", 180)
            } else if (wall) {
                result.warn(
                    "Item $id: imported wall furniture can render 180 backwards, but it " +
                        "also allows floor/ceiling placement, so no automatic rotation was " +
                        "applied - set furniture.display.y-rotation manually if it looks wrong"
                )
            }
        }

        // Display properties (Oraxen display_entity_properties / Nexo properties).
        val display = config.getConfigurationSection("display_entity_properties")
            ?: config.getConfigurationSection("properties")
        display?.let {
            // Oraxen bakes furniture against the FIXED (item-frame) context,
            // which tilts geometry flat. Our furniture is a freestanding entity
            // wanting the raw upright geometry (NONE), so FIXED would lay it on
            // its side - drop it and forward only genuinely different contexts.
            it.getString("display_transform")?.let { t ->
                if (!t.equals("FIXED", ignoreCase = true)) {
                    out.set("furniture.display.transform", t.lowercase())
                } else {
                    result.warn("Item $id: furniture display_transform FIXED is Oraxen-specific and was skipped - EcoItems renders the model upright by default")
                }
            }
            it.getString("tracking_rotation")?.let { t -> out.set("furniture.display.billboard", t.lowercase()) }
            if (it.contains("view_range")) out.set("furniture.display.view-range", it.getDouble("view_range"))
            if (it.contains("brightness.block_light")) {
                out.set("furniture.display.brightness", it.getInt("brightness.block_light"))
            }
            it.getConfigurationSection("scale")?.let { scale ->
                out.set(
                    "furniture.display.scale",
                    "${scale.getDouble("x", 1.0)},${scale.getDouble("y", 1.0)},${scale.getDouble("z", 1.0)}"
                )
            }
            when {
                it.isConfigurationSection("translation") -> it.getConfigurationSection("translation")?.let { t ->
                    out.set(
                        "furniture.display.translation",
                        "${t.getDouble("x")},${t.getDouble("y")},${t.getDouble("z")}"
                    )
                }

                it.isString("translation") -> out.set("furniture.display.translation", it.getString("translation"))
            }
        }

        // Tall furniture (poles, canopies) often has model elements below y=0,
        // which clip into the floor under NONE-transform raw rendering. Nudge
        // the display up by that reach so the base rests on y=0.
        if (!out.isSet("furniture.display.transform")) {
            furnitureFloorOffset(pack)?.let { offset ->
                val existing = out.getString("furniture.display.translation")
                    ?.split(",")?.mapNotNull { it.trim().toDoubleOrNull() }
                val x = existing?.getOrNull(0) ?: 0.0
                val y = Math.round(((existing?.getOrNull(1) ?: 0.0) + offset) * 1000) / 1000.0
                val z = existing?.getOrNull(2) ?: 0.0
                out.set("furniture.display.translation", "$x,$y,$z")
                result.warn(
                    "Item $id: furniture model reaches ${Math.round(offset * 100) / 100.0} blocks below its " +
                        "origin - added a matching vertical display offset so it doesn't clip into the floor"
                )
            }
        }

        config.getConfigurationSection("drop")?.let { drop ->
            val loots = drop.getMapList("loots").mapNotNull { loot -> convertLoot(loot) }
            if (loots.isNotEmpty()) out.set("furniture.drops.items", loots)
        }

        config.getConfigurationSection("block_sounds")?.let { convertBlockSounds(it, out, "furniture") }

        for (unsupported in listOf("storage", "jukebox", "evolution", "stages", "modelengine_id", "text_entity", "connectable", "door", "beds")) {
            if (config.contains(unsupported)) {
                result.warn("Item $id: furniture option '$unsupported' is not supported and was skipped")
            }
        }

        result.furniture++
    }

    /** How far (in blocks) the furniture's model reaches below its origin, if any. */
    private fun furnitureFloorOffset(pack: ConfigurationSection?): Double? {
        if (pack == null || pack.getBoolean("generate_model")) return null
        val modelRef = pack.getString("model") ?: return null
        val relative = modelRef.removeSuffix(".json").replace("\\", "/").substringAfter(":")

        val modelsDirs = listOf(source.resolve("pack/models"), source.resolve("pack/assets/minecraft/models")) +
            source.resolve("pack/assets").listFiles().orEmpty()
                .filter { it.isDirectory }
                .map { it.resolve("models") }
        val file = modelsDirs.map { it.resolve("$relative.json") }.firstOrNull { it.isFile } ?: return null

        // Model files are JSON, not YAML - Bukkit's SnakeYAML chokes on them,
        // so parse with Gson (the JSON parser we already depend on).
        val elements = runCatching {
            file.bufferedReader().use { JsonParser.parseReader(it).asJsonObject }
                .getAsJsonArray("elements")
        }.getOrNull() ?: return null

        val minY = elements.mapNotNull { element ->
            runCatching {
                val obj = element.asJsonObject
                minOf(
                    obj.getAsJsonArray("from").get(1).asDouble,
                    obj.getAsJsonArray("to").get(1).asDouble
                )
            }.getOrNull()
        }.minOrNull() ?: return null

        return (-minY / 16.0).takeIf { it > 0.01 }
    }

    // ----------------------------------------------------------------- shared

    /** Parse an Oraxen color ("r, g, b" or "#rrggbb") into packed RGB. */
    private fun parseDyeColor(raw: String): Int? {
        val s = raw.trim()
        if (s.startsWith("#")) return s.removePrefix("#").toIntOrNull(16)
        val parts = s.split(",").map { it.trim().toIntOrNull() ?: return null }
        if (parts.size != 3) return null
        val (r, g, b) = parts
        return (r shl 16) or (g shl 8) or b
    }

    private fun convertLoot(loot: Map<*, *>): Map<String, Any>? {
        val item = loot[dialect.itemRef]?.toString()?.let { "ecoitems:$it" }
            ?: loot["minecraft_type"]?.toString()?.lowercase()
            ?: return null

        val converted = mutableMapOf<String, Any>("item" to item)
        loot["probability"]?.let { converted["chance"] = it }
        loot["max_amount"]?.let { converted["amount"] = "1-$it" }
        return converted
    }

    private fun convertBlockSounds(sounds: ConfigurationSection, out: YamlConfiguration, root: String) {
        for ((theirs, ours) in mapOf(
            "place_sound" to "place", "break_sound" to "break",
            "hit_sound" to "hit", "step_sound" to "step", "fall_sound" to "fall"
        )) {
            sounds.anyString(theirs)?.let { out.set("$root.sounds.$ours", it) }
        }
        if (sounds.contains("volume")) out.set("$root.sounds.volume", sounds.getDouble("volume"))
        if (sounds.contains("pitch")) out.set("$root.sounds.pitch", sounds.getDouble("pitch"))
    }

    /**
     * Oraxen's generated assets live in the minecraft namespace (its hybrid
     * pack/textures + pack/models folders); Nexo refs are already namespaced.
     */
    private fun assetRef(raw: String): String {
        val path = raw.removeSuffix(".png").removeSuffix(".json").replace("\\", "/")
        return if (":" in path) path else "minecraft:$path"
    }

    private fun textureList(pack: ConfigurationSection, vararg keys: String): List<String> {
        for (key in keys) {
            if (pack.isList(key)) return pack.getStringList(key)
            pack.getString(key)?.let { return listOf(it) }
        }
        return emptyList()
    }

    /** ConfigurationSections nested in values -> plain maps for out.set(). */
    private fun plain(value: Any?): Any? = when (value) {
        is ConfigurationSection -> value.getKeys(false).associateWith { plain(value.get(it)) }
        is Map<*, *> -> value.entries.associate { it.key.toString() to plain(it.value) }
        is List<*> -> value.map { plain(it) }
        else -> value
    }

    // ---------------------------------------------------------------- recipes

    /** Shaped recipes keyed by result item id, as our 9-slot lookup lists. */
    private fun loadShapedRecipes(): Map<String, List<String>> {
        val recipes = mutableMapOf<String, List<String>>()
        val file = source.resolve("recipes/shaped.yml")
        if (!file.exists()) {
            for (other in listOf("shapeless", "furnace", "blasting", "smoking", "campfire", "stonecutting")) {
                if (source.resolve("recipes/$other.yml").exists()) {
                    result.warn("Recipes: $other recipes are not supported and were skipped")
                }
            }
            return recipes
        }

        val yaml = YamlConfiguration.loadConfiguration(file)
        for (name in yaml.getKeys(false)) {
            val recipe = yaml.getConfigurationSection(name) ?: continue
            val resultRef = recipe.getConfigurationSection("result") ?: continue
            val resultId = resultRef.getString(dialect.itemRef) ?: run {
                result.warn("Recipe $name: only recipes resulting in a converted item are supported")
                continue
            }

            val shape = recipe.getStringList("shape").map { it.padEnd(3) }.take(3)
            val rows = shape + List(3 - shape.size) { "   " }
            val slots = rows.flatMap { row -> row.toCharArray().take(3).map { it } }

            val lookups = slots.map { char ->
                if (char == ' ' || char == '_') return@map "air"
                val ingredient = recipe.getConfigurationSection("ingredients.$char") ?: return@map "air"
                ingredient.getString(dialect.itemRef)?.let { "ecoitems:$it" }
                    ?: ingredient.getString("minecraft_type")?.lowercase()
                    ?: "air"
            }

            recipes[resultId] = lookups
        }

        return recipes
    }

    // ----------------------------------------------------------------- glyphs

    private fun convertGlyphs() {
        val folder = source.resolve("glyphs")
        if (!folder.isDirectory) return

        // Oraxen names it font.yml, some forks fonts.yml.
        val bitmaps = listOf("font.yml", "fonts.yml")
            .map { YamlConfiguration.loadConfiguration(source.resolve(it)) }
            .firstNotNullOfOrNull { it.getConfigurationSection("bitmaps") }

        for (file in folder.walkTopDown().filter { it.extension == "yml" }) {
            if (file.name in setOf("required.yml", "interface.yml", "shifts.yml")) continue

            val yaml = YamlConfiguration.loadConfiguration(file)
            for (id in yaml.getKeys(false)) {
                val glyph = yaml.getConfigurationSection(id) ?: continue
                val out = YamlConfiguration()

                val bitmapRef = glyph.getConfigurationSection("bitmap")
                if (bitmapRef != null) {
                    val sheet = bitmaps?.getConfigurationSection(bitmapRef.getString("id") ?: "")
                    if (sheet == null) {
                        result.warn("Glyph $id: bitmap sheet '${bitmapRef.getString("id")}' not found in fonts.yml")
                        continue
                    }
                    out.set("bitmap.texture", assetRef(sheet.getString("texture")!!))
                    out.set("bitmap.rows", sheet.getInt("rows"))
                    out.set("bitmap.columns", sheet.getInt("columns"))
                    out.set("bitmap.row", (bitmapRef.getInt("row") - 1).coerceAtLeast(0))
                    out.set("bitmap.column", (bitmapRef.getInt("column") - 1).coerceAtLeast(0))
                    out.set("ascent", sheet.getInt("ascent", 8))
                    out.set("height", sheet.getInt("height", 8))
                } else {
                    out.set("texture", assetRef(glyph.getString("texture") ?: continue))
                    out.set("ascent", glyph.getInt("ascent", 8))
                    out.set("height", glyph.getInt("height", 8))
                }

                glyph.getString("char")?.let { out.set("char", it) }
                val placeholders = glyph.getStringList("chat.placeholders")
                if (placeholders.isNotEmpty()) out.set("placeholders", placeholders)
                glyph.getString("chat.permission")?.takeIf { it.isNotEmpty() }?.let { out.set("permission", it) }
                if (glyph.getBoolean("chat.tabcomplete")) out.set("tab-complete", true)

                val target = plugin.dataFolder.resolve("glyphs/imported/${dialect.folder.lowercase()}/$id.yml")
                if (writeConverted(result, target, out)) {
                    result.glyphs++
                }
            }
        }
    }

    // ----------------------------------------------------------------- sounds

    private fun convertSounds() {
        when (dialect) {
            Dialect.ORAXEN -> {
                // Some forks name it sounds.yml and/or use the Nexo-style
                // list; classic entries use dotted ids, which yaml parses as
                // nesting - walk the tree.
                val file = listOf("sound.yml", "sounds.yml")
                    .map { source.resolve(it) }
                    .firstOrNull { it.exists() } ?: return
                val yaml = YamlConfiguration.loadConfiguration(file)

                if (yaml.isList("sounds")) {
                    for (entry in yaml.getMapList("sounds")) {
                        val id = entry["id"]?.toString() ?: continue
                        val section = YamlConfiguration()
                        for ((key, value) in entry) section.set(key.toString(), value)
                        convertSound(id, section)
                    }
                    return
                }

                walkSounds(yaml.getConfigurationSection("sounds") ?: yaml, "")
            }

            Dialect.NEXO -> {
                val file = source.resolve("sounds.yml")
                if (!file.exists()) return
                for (entry in YamlConfiguration.loadConfiguration(file).getMapList("sounds")) {
                    val id = entry["id"]?.toString() ?: continue
                    val section = YamlConfiguration()
                    for ((key, value) in entry) section.set(key.toString(), value)
                    convertSound(id, section)
                }
            }
        }
    }

    private var warnedRemapEntries = false

    private fun walkSounds(section: ConfigurationSection, path: String) {
        for (key in section.getKeys(false)) {
            val child = section.getConfigurationSection(key) ?: continue
            val childPath = if (path.isEmpty()) key else "$path.$key"

            val isEntry = child.contains("sound") || child.contains("sounds") ||
                child.contains("replace") || child.contains("jukebox")
            if (!isEntry) {
                if (childPath != "settings") {
                    walkSounds(child, childPath)
                }
                continue
            }

            convertSound(child.getString("id") ?: childPath, child)
        }
    }

    private fun convertSound(rawId: String, sound: ConfigurationSection) {
        // block.* / required.* entries are the wood/stone sound remap
        // machinery - EcoItems generates its own.
        val plainId = rawId.substringAfter(":")
        if (plainId.startsWith("block.") || plainId.startsWith("required.")) {
            if (!warnedRemapEntries) {
                warnedRemapEntries = true
                result.warn("Skipped the source's block./required. sound remap entries (EcoItems generates its own)")
            }
            return
        }

        val id = plainId.replace(Regex("[^a-z0-9_]"), "_").lowercase()
        val out = YamlConfiguration()

        sound.getString("category")?.let { out.set("category", it.lowercase()) }
        sound.getString("subtitle")?.let { out.set("subtitle", it) }

        val files = when {
            sound.isList("sounds") -> sound.getStringList("sounds")
            else -> listOfNotNull(sound.getString("sound") ?: sound.getString("sounds"))
        }
        if (files.isEmpty()) {
            result.warn("Sound $rawId has no sound files; skipped")
            return
        }
        out.set("sounds", files.map { raw ->
            val name = raw.removeSuffix(".ogg").replace("\\", "/")
            if (":" in name) name else "minecraft:$name"
        })

        val jukebox = sound.getConfigurationSection("jukebox") ?: sound.getConfigurationSection("jukebox_playable")
        jukebox?.let {
            it.getString("description")?.let { d -> out.set("jukebox.description", d) }
            val duration = it.get("duration") ?: it.get("length_in_seconds")
            (duration as? Number)?.let { d -> out.set("jukebox.length-seconds", d.toDouble()) }
            if (it.contains("comparator_output")) out.set("jukebox.comparator-output", it.getInt("comparator_output"))
            if (it.contains("range")) out.set("jukebox.range", it.getDouble("range"))
        }

        if (rawId != id && rawId.substringBefore(":") != "minecraft") {
            result.warn("Sound $rawId now plays as ecoitems:$id - update references")
        }

        val target = plugin.dataFolder.resolve("sounds/imported/${dialect.folder.lowercase()}/$id.yml")
        if (writeConverted(result, target, out)) {
            result.sounds++
        }
    }

    // ------------------------------------------------------------------- pack

    private fun copyPack() {
        val pack = source.resolve("pack")
        val target = plugin.dataFolder.resolve("pack")

        // Some setups keep external-pack folders at the source root instead
        // of under pack/external_packs - anything with an assets/ tree counts.
        val configDirs = setOf("items", "pack", "glyphs", "recipes", "messages", "sounds", "schematics")
        for (dir in source.listFiles().orEmpty()) {
            if (!dir.isDirectory || dir.name in configDirs || dir.name.startsWith("__")) continue
            val looseAssets = dir.resolve("assets")
            if (looseAssets.isDirectory) {
                result.assets += copyTree(looseAssets, target.resolve("assets"))
            }
        }

        if (!pack.isDirectory) {
            generateEquipment(target)
            return
        }

        when (dialect) {
            Dialect.ORAXEN -> {
                // Hybrid layout: the shortcut folders are minecraft-namespaced.
                for (root in listOf("textures", "models", "sounds", "font", "lang")) {
                    result.assets += copyTree(pack.resolve(root), target.resolve("assets/minecraft/$root"))
                }
                result.assets += copyTree(pack.resolve("assets"), target.resolve("assets"))
            }

            Dialect.NEXO -> {
                result.assets += copyTree(pack.resolve("assets"), target.resolve("assets"))
            }
        }

        // External packs holding the assets item configs reference get
        // flattened into the main pack (so generated models find their
        // files); standalone zips stay separate as imports.
        for (external in pack.resolve("external_packs").listFiles().orEmpty()) {
            val externalAssets = external.resolve("assets")
            when {
                externalAssets.isDirectory ->
                    result.assets += copyTree(externalAssets, target.resolve("assets"))

                external.isDirectory -> result.assets += copyTree(external, target.resolve("imports/${external.name}"))

                external.extension == "zip" -> {
                    val zipTarget = target.resolve("imports/${external.name}")
                    if (!zipTarget.exists() && !isJunk(external, pack)) {
                        external.copyTo(zipTarget)
                        result.assets++
                    }
                }
            }
        }

        generateEquipment(target)
    }

    /**
     * The source plugins generate the equipment assets for wearable armor at
     * pack build time from raw <set>_layer_1/2.png textures; without this,
     * equippable items reference equipment that doesn't exist and armor
     * renders invisible when worn.
     */
    private fun generateEquipment(target: File) {
        if (equipmentAssets.isEmpty()) {
            return
        }

        val assets = target.resolve("assets")
        val layerOnes = assets.walkTopDown()
            .filter { it.isFile && it.name.endsWith("_layer_1.png") }
            .toList()

        for (assetId in equipmentAssets.sorted()) {
            val namespace = assetId.substringBefore(':')
            val name = assetId.substringAfter(':').substringAfterLast('/')

            val definition = assets.resolve("$namespace/equipment/$name.json")
            if (definition.exists()) {
                continue
            }

            val layerOne = layerOnes.firstOrNull {
                it.name == "${name}_armor_layer_1.png" || it.name == "${name}_layer_1.png"
            }
            if (layerOne == null) {
                result.warn("Armor asset $assetId: no ${name}_layer_1.png texture found - the armor will be invisible when worn")
                continue
            }

            // The layer textures keep the namespace they were copied into.
            val textureNamespace = layerOne.relativeTo(assets).invariantSeparatorsPath.substringBefore('/')

            val humanoid = assets.resolve("$textureNamespace/textures/entity/equipment/humanoid/$name.png")
            humanoid.parentFile.mkdirs()
            layerOne.copyTo(humanoid, overwrite = true)
            result.assets++

            val layerTwo = File(layerOne.path.replace("_layer_1.png", "_layer_2.png"))
            if (layerTwo.exists()) {
                val leggings = assets.resolve("$textureNamespace/textures/entity/equipment/humanoid_leggings/$name.png")
                leggings.parentFile.mkdirs()
                layerTwo.copyTo(leggings, overwrite = true)
                result.assets++
            } else {
                result.warn("Armor asset $assetId: no ${name}_layer_2.png - leggings will render invisible")
            }

            val json = JsonObject()
            val layers = JsonObject()
            for (layerKey in listOf("humanoid", "humanoid_leggings")) {
                val entry = JsonObject()
                entry.addProperty("texture", "$textureNamespace:$name")
                val array = JsonArray()
                array.add(entry)
                layers.add(layerKey, array)
            }
            json.add("layers", layers)

            definition.parentFile.mkdirs()
            definition.writeText(
                GsonBuilder().setPrettyPrinting().create().toJson(json) + "\n"
            )
        }
    }
}
