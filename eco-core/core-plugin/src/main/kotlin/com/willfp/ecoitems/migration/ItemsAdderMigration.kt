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
            out.set("item.display-name", it)
        }
        if (section.isList("lore")) out.set("item.lore", section.getStringList("lore"))

        resource?.let {
            val modelPath = it.getString("model_path")
            val texture = it.getStringList("textures").firstOrNull() ?: it.getString("texture")
            when {
                modelPath != null -> out.set("item.model", "$namespace:${strip(modelPath)}")
                texture != null -> out.set("item.texture", "$namespace:${strip(texture)}")
            }
        }

        convertComponents(id, section, out)

        section.getConfigurationSection("behaviours")?.let { behaviours ->
            val block = behaviours.getConfigurationSection("block")
                ?: section.getConfigurationSection("specific_properties.block")
            block?.let { convertBlock(namespace, rawId, id, it, out) }

            behaviours.getConfigurationSection("furniture")?.let {
                convertFurniture(id, it, behaviours.getConfigurationSection("furniture_sit"), out)
            }

            for (unsupported in behaviours.getKeys(false)) {
                if (unsupported !in setOf("block", "furniture", "furniture_sit")) {
                    result.warn("Item $id: behaviour '$unsupported' is not supported and was skipped")
                }
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
        out: YamlConfiguration
    ) {
        val type = block.getString("placed_model.type") ?: block.getString("placed.type") ?: "REAL_NOTE"
        val backing = when (type.uppercase()) {
            "REAL_NOTE" -> BlockBacking.NOTEBLOCK
            "REAL_WIRE" -> BlockBacking.STRINGBLOCK
            else -> {
                result.warn("Item $id: IA block type '$type' has no EcoItems equivalent; block section skipped")
                return
            }
        }

        out.set("block.type", backing.id)

        // IA assigns state ids at runtime and caches them; those ids use the
        // same legacy encoding as Oraxen, so they pin directly.
        val cache = if (backing == BlockBacking.NOTEBLOCK) noteIds else wireIds
        val cached = cache["$namespace:$rawId"]
        if (cached != null && cached in backing.variations) {
            out.set("block.variation", cached)
        } else if (cache.isNotEmpty()) {
            result.warn("Item $id: no usable cached state id - auto-assigning (already-placed blocks of it will look wrong)")
        }

        // The block shows the item's texture; nothing extra to wire.
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
            out.set("furniture.seats", listOf("0,${sit.getDouble("sit_height", 0.5)},0"))
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

        writeNamespaceAtlas(namespace, target.resolve("textures"))
    }

    /** IA textures sit at arbitrary roots; atlas them like the Oraxen path. */
    private fun writeNamespaceAtlas(namespace: String, textures: File) {
        val roots = textures.listFiles()
            ?.filter { it.isDirectory && it.name !in setOf("item", "block", "font", "gui") }
            ?.map { it.name }
            .orEmpty()
        if (roots.isEmpty()) return

        val atlas = plugin.dataFolder.resolve("pack/assets/minecraft/atlases/blocks.json")
        val existing = if (atlas.exists()) atlas.readText() else """{"sources": []}"""

        val json = com.google.gson.JsonParser.parseString(existing).asJsonObject
        val sources = json.getAsJsonArray("sources") ?: com.google.gson.JsonArray().also { json.add("sources", it) }
        val present = sources.mapNotNull { entry ->
            entry.asJsonObject.let { "${it.get("source")?.asString}@${it.get("prefix")?.asString}" }
        }.toSet()

        for (root in roots) {
            if ("$root@$root/" in present) continue
            val source = com.google.gson.JsonObject()
            source.addProperty("type", "directory")
            source.addProperty("source", root)
            source.addProperty("prefix", "$root/")
            sources.add(source)
        }

        atlas.parentFile.mkdirs()
        atlas.writeText(com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json))
    }

    private fun strip(path: String): String =
        path.removeSuffix(".png").removeSuffix(".json").replace("\\", "/")
}
