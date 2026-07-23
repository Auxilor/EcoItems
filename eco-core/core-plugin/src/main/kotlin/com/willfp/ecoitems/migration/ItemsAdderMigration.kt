package com.willfp.ecoitems.migration

import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.blocks.BlockBacking
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * Imports an ItemsAdder setup: contents/<pack>/ config ymls, the storage
 * caches (for stable block-state ids), and the per-namespace assets. IA
 * assets keep their own namespaces, so texture/model refs inside copied
 * model jsons stay valid.
 */
class ItemsAdderMigration(private val plugin: EcoItemsPlugin) {
    private val result = MigrationResult(plugin)
    private val source = resolveRoot(
        plugin,
        plugin.dataFolder.resolve("migrations/ItemsAdder"),
        "contents", "storage"
    )
    private val usedIds = mutableSetOf<String>()

    private val noteIds = loadCache("real_blocks_note_ids_cache.yml")
    private val wireIds = loadCache("real_wire_ids_cache.yml")

    fun run(): MigrationResult {
        val contents = source.resolve("contents")
        if (!contents.isDirectory) {
            result.warn("migrations/ItemsAdder/contents not found - copy the ItemsAdder folder contents in first")
            return result
        }

        for (pack in contents.listFiles().orEmpty().filter { it.isDirectory }) {
            for (file in pack.walkTopDown().filter { it.extension == "yml" }) {
                val yaml = YamlConfiguration.loadConfiguration(file)
                val items = yaml.getConfigurationSection("items") ?: continue
                val namespace = yaml.getString("info.namespace") ?: pack.name

                for (id in items.getKeys(false)) {
                    items.getConfigurationSection(id)?.let { convertItem(namespace, id, it) }
                }
            }

            copyAssets(pack)
        }

        return result
    }

    /** IA storage caches: maps of "namespace:item" -> state id. */
    private fun loadCache(name: String): Map<String, Int> {
        val file = source.resolve("storage/$name")
        if (!file.exists()) return emptyMap()

        val yaml = YamlConfiguration.loadConfiguration(file)
        return yaml.getKeys(true)
            .filter { !yaml.isConfigurationSection(it) }
            .associate { it.replace(".", ":") to yaml.getInt(it) }
    }

    private fun convertItem(namespace: String, rawId: String, section: ConfigurationSection) {
        var id = rawId
        if (!usedIds.add(id)) {
            id = "${namespace}_$rawId"
            result.warn("Duplicate item id $rawId - imported as $id")
            if (!usedIds.add(id)) return
        }

        val out = YamlConfiguration()

        val resource = section.getConfigurationSection("resource")
        out.set("item.item", (resource?.getString("material") ?: "PAPER").lowercase())

        (section.getString("display_name") ?: section.getString("name"))?.let {
            out.set("item.name", it)
        }
        if (section.isList("lore")) out.set("item.lore", section.getStringList("lore"))

        convertComponents(id, section, out)

        // Block/furniture behaviours live under behaviours: (modern IA) or
        // specific_properties: (legacy) - accept either regardless of which
        // top-level section exists.
        val behaviours = section.getConfigurationSection("behaviours")
        val blockConfig = behaviours?.getConfigurationSection("block")
            ?: section.getConfigurationSection("specific_properties.block")
        val furnitureConfig = behaviours?.getConfigurationSection("furniture")
            ?: section.getConfigurationSection("specific_properties.furniture")
        val sitConfig = behaviours?.getConfigurationSection("furniture_sit")
            ?: section.getConfigurationSection("specific_properties.furniture_sit")

        // A custom block renders as its generated block model, so its textures
        // belong on block.*, not as a flat item texture. Everything else (plain
        // items, furniture) keeps the item-level texture/model.
        val madeBlock = blockConfig != null && convertBlock(namespace, rawId, id, blockConfig, resource, out)
        if (!madeBlock) {
            applyItemResource(namespace, resource, out)
        }

        furnitureConfig?.let { convertFurniture(id, it, sitConfig, out) }

        behaviours?.getKeys(false)?.forEach { unsupported ->
            if (unsupported !in setOf("block", "furniture", "furniture_sit")) {
                result.warn("Item $id: behaviour '$unsupported' is not supported and was skipped")
            }
        }

        if (section.contains("events")) {
            result.warn("Item $id: IA events/actions are not converted - rebuild them as libreforge effects")
        }

        out.set("effects", emptyList<String>())
        out.set("conditions", emptyList<String>())

        val file = plugin.dataFolder.resolve("items/imported/itemsadder/$id.yml")
        if (writeConverted(result, file, out)) {
            result.items++
        }
    }

    private fun convertComponents(id: String, section: ConfigurationSection, out: YamlConfiguration) {
        val components = mutableMapOf<String, Any>()

        val enchants = section.getStringList("enchants")
        if (enchants.isNotEmpty()) {
            components["minecraft:enchantments"] = enchants.mapNotNull { raw ->
                val split = raw.split(":")
                val level = split.lastOrNull()?.toIntOrNull() ?: return@mapNotNull null
                val key = split.dropLast(1).joinToString(":").lowercase()
                (if (":" in key) key else "minecraft:$key") to level
            }.toMap()
        }

        if (section.contains("durability.max_durability")) {
            components["minecraft:max_damage"] = section.getInt("durability.max_durability")
        }
        if (section.getBoolean("durability.unbreakable")) {
            components["minecraft:unbreakable"] = emptyMap<String, Any>()
        }
        if (section.contains("max_stack_size")) {
            components["minecraft:max_stack_size"] = section.getInt("max_stack_size")
        }

        section.getConfigurationSection("attribute_modifiers")?.let { slots ->
            val list = mutableListOf<Map<String, Any>>()
            for (slot in slots.getKeys(false)) {
                val attributes = slots.getConfigurationSection(slot) ?: continue
                for (attribute in attributes.getKeys(false)) {
                    val snake = attribute.replace(Regex("([a-z])([A-Z])")) {
                        "${it.groupValues[1]}_${it.groupValues[2].lowercase()}"
                    }.lowercase()
                    list += mapOf(
                        "id" to "ecoitems:${slot}_$snake",
                        "type" to "minecraft:$snake",
                        "amount" to attributes.getDouble(attribute),
                        "operation" to "add_value",
                        "slot" to slot.lowercase()
                    )
                }
            }
            if (list.isNotEmpty()) components["minecraft:attribute_modifiers"] = list
        }

        if (section.contains("item_flags")) {
            result.warn("Item $id: item_flags are not converted")
        }

        for ((key, value) in components) {
            out.set("item.components.$key", value)
        }
    }

    private fun convertBlock(
        namespace: String,
        rawId: String,
        id: String,
        block: ConfigurationSection,
        resource: ConfigurationSection?,
        out: YamlConfiguration
    ): Boolean {
        val type = block.getString("placed_model.type") ?: block.getString("placed.type") ?: "REAL_NOTE"
        val backing = when (type.uppercase()) {
            "REAL_NOTE" -> BlockBacking.NOTEBLOCK
            "REAL_WIRE" -> BlockBacking.STRINGBLOCK
            "REAL" -> BlockBacking.MUSHROOM
            else -> {
                result.warn("Item $id: IA block type '$type' has no EcoItems equivalent; block section skipped")
                return false
            }
        }

        out.set("block.type", backing.id)

        // IA assigns state ids at runtime and caches them; note/wire ids use
        // the same legacy encoding as Oraxen, so they pin directly. Mushroom
        // (REAL) ids use an internal encoding we can't translate.
        val cache = when (backing) {
            BlockBacking.NOTEBLOCK -> noteIds
            BlockBacking.STRINGBLOCK -> wireIds
            else -> emptyMap()
        }
        val cached = cache["$namespace:$rawId"]
        if (cached != null && cached in backing.variations) {
            out.set("block.variation", cached)
        } else if (cache.isNotEmpty()) {
            result.warn("Item $id: no usable cached state id - auto-assigning (already-placed blocks of it will look wrong)")
        } else if (backing in BlockBacking.mushrooms) {
            result.warn(
                "Item $id: IA REAL (mushroom) state ids can't be translated - " +
                    "new placements work, but already-placed blocks of it will render wrong"
            )
        }

        // The placed block renders its own generated model, built from the
        // IA resource textures; fall back to a flat item texture only if there
        // were none to generate from.
        if (!applyBlockTextures(id, namespace, resource, out)) {
            applyItemResource(namespace, resource, out)
        }

        if (block.contains("hardness")) out.set("block.hardness", block.getDouble("hardness"))
        if (block.contains("light_level")) out.set("block.light", block.getInt("light_level"))
        if (block.getBoolean("no_explosion")) out.set("block.blast-resistant", true)

        val tools = block.getStringList("break_tools_whitelist")
            .mapNotNull { it.substringAfterLast("_").takeIf { t -> t.isNotEmpty() } }
            .distinct()
        if (tools.isNotEmpty()) out.set("block.correct-tools", tools)

        if (!block.getBoolean("drop_when_mined", true)) {
            out.set("block.drops.items", emptyList<Any>())
        }

        block.getConfigurationSection("sound")?.let { sound ->
            for (event in listOf("place", "break")) {
                sound.getString("$event.name")?.let {
                    // IA uses enum-ish names (BLOCK_WOOD_PLACE); best effort.
                    out.set("block.sounds.$event", it.lowercase().replace("_", "."))
                }
            }
        }

        result.blocks++
        return true
    }

    /** Item-level texture/model for plain items and furniture (first texture wins). */
    private fun applyItemResource(namespace: String, resource: ConfigurationSection?, out: YamlConfiguration) {
        resource ?: return
        val modelPath = resource.getString("model_path")
        val texture = resource.getStringList("textures").firstOrNull() ?: resource.getString("texture")
        when {
            modelPath != null -> out.set("item.model", "$namespace:${strip(modelPath)}")
            texture != null -> out.set("item.texture", "$namespace:${strip(texture)}")
        }
    }

    /**
     * Build our block texture keys from an IA `resource`. An explicit model_path
     * passes through; otherwise IA's `generate: true` texture list is collapsed
     * to the smallest vanilla parent that fits. IA lists a block's faces in the
     * fixed order down, east, north, south, up, west, so a single texture is an
     * all-faces cube, a uniform-sided list is a cube_bottom_top pillar, and
     * anything else is a full per-face cube. Returns false if there was nothing
     * to generate from (the caller falls back to an item texture).
     */
    private fun applyBlockTextures(
        id: String,
        namespace: String,
        resource: ConfigurationSection?,
        out: YamlConfiguration
    ): Boolean {
        resource ?: return false

        resource.getString("model_path")?.let {
            out.set("block.model", "$namespace:${strip(it)}")
            return true
        }

        val textures = resource.getStringList("textures").ifEmpty {
            listOfNotNull(resource.getString("texture"))
        }.map { "$namespace:${strip(it)}" }

        when (textures.size) {
            0 -> return false

            1 -> out.set("block.texture", textures[0])

            6 -> {
                val down = textures[0]; val east = textures[1]; val north = textures[2]
                val south = textures[3]; val up = textures[4]; val west = textures[5]
                val sides = listOf(east, north, south, west)
                when {
                    sides.distinct().size == 1 && up == down && up == east ->
                        out.set("block.texture", up)

                    sides.distinct().size == 1 -> {
                        out.set("block.textures.top", up)
                        out.set("block.textures.bottom", down)
                        out.set("block.textures.side", east)
                        out.set("block.texture-parent", "cube_bottom_top")
                    }

                    else -> {
                        out.set("block.textures.up", up)
                        out.set("block.textures.down", down)
                        out.set("block.textures.north", north)
                        out.set("block.textures.south", south)
                        out.set("block.textures.east", east)
                        out.set("block.textures.west", west)
                        out.set("block.texture-parent", "cube")
                    }
                }
            }

            else -> {
                out.set("block.texture", textures[0])
                result.warn(
                    "Item $id: block has ${textures.size} textures (expected 1 or 6) - " +
                        "only the first was used; set block.textures manually"
                )
            }
        }

        return true
    }

    private fun convertFurniture(
        id: String,
        furniture: ConfigurationSection,
        sit: ConfigurationSection?,
        out: YamlConfiguration
    ) {
        furniture.getString("entity")?.let {
            if (!it.equals("item_display", ignoreCase = true)) {
                result.warn("Item $id: IA $it furniture converts, but EcoItems furniture is display-entity based - re-check its look")
            }
        }

        if (furniture.getBoolean("solid")) {
            out.set("furniture.barriers", listOf("0,0,0"))
        }

        furniture.getConfigurationSection("hitbox")?.let { hitbox ->
            val width = hitbox.getDouble("width", 1.0)
            val height = hitbox.getDouble("height", 1.0)
            val x = hitbox.getDouble("width_offset")
            val y = hitbox.getDouble("height_offset")
            val z = hitbox.getDouble("length_offset")
            out.set("furniture.hitboxes", listOf("$x,$y,$z ${width}x$height"))
        }

        if (furniture.contains("light_level")) {
            out.set("furniture.lights", listOf("0,1,0 ${furniture.getInt("light_level").coerceIn(1, 15)}"))
        }

        if (sit?.getBoolean("enabled") == true || sit?.contains("sit_height") == true) {
            // IA sit_height is measured from the block bottom; our seat y is
            // relative to the natural chair height (0.6 above the bottom).
            val y = Math.round((sit.getDouble("sit_height", 0.5) - 0.6) * 100) / 100.0
            out.set("furniture.seats", listOf("0,$y,0"))
        }

        furniture.getConfigurationSection("placeable_on")?.let { placing ->
            out.set("furniture.placement.floor", placing.getBoolean("floor", true))
            out.set("furniture.placement.wall", placing.getBoolean("walls"))
            out.set("furniture.placement.ceiling", placing.getBoolean("ceiling"))
        }

        furniture.getConfigurationSection("display_transformation")?.let { display ->
            display.getString("transform")?.let { out.set("furniture.display.transform", it.lowercase()) }
            display.getConfigurationSection("scale")?.let {
                out.set(
                    "furniture.display.scale",
                    "${it.getDouble("x", 1.0)},${it.getDouble("y", 1.0)},${it.getDouble("z", 1.0)}"
                )
            }
            display.getConfigurationSection("translation")?.let {
                out.set(
                    "furniture.display.translation",
                    "${it.getDouble("x")},${it.getDouble("y")},${it.getDouble("z")}"
                )
            }
        }

        result.furniture++
    }

    /**
     * IA assets live per-pack in several possible layouts; copy them all into
     * pack/assets/<namespace>/ keeping the IA namespace.
     */
    private fun copyAssets(pack: File) {
        val namespace = pack.walkTopDown()
            .filter { it.extension == "yml" }
            .mapNotNull { YamlConfiguration.loadConfiguration(it).getString("info.namespace") }
            .firstOrNull() ?: pack.name

        val target = plugin.dataFolder.resolve("pack/assets/$namespace")

        for (root in listOf("textures", "models", "sounds", "font")) {
            result.assets += copyTree(pack.resolve(root), target.resolve(root))
            result.assets += copyTree(pack.resolve("resourcepack/$root"), target.resolve(root))
            result.assets += copyTree(pack.resolve("resourcepack/$namespace/$root"), target.resolve(root))
        }
        // Fully vanilla-structured variants copy as-is.
        result.assets += copyTree(pack.resolve("assets"), plugin.dataFolder.resolve("pack/assets"))
        result.assets += copyTree(pack.resolve("resourcepack/assets"), plugin.dataFolder.resolve("pack/assets"))

    }


    private fun strip(path: String): String =
        path.removeSuffix(".png").removeSuffix(".json").replace("\\", "/")
}
