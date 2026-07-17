package com.willfp.ecoitems.pack.blocks

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.blocks.BlockBacking
import com.willfp.ecoitems.blocks.EcoBlock
import com.willfp.ecoitems.blocks.EcoBlocks
import com.willfp.ecoitems.blocks.StackableBlock
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.pack.PackLocation
import com.willfp.ecoitems.pack.PackSettings

/**
 * Custom block pack assets: the hijacked vanilla blockstate files, generated
 * block models, item definitions for placer items without their own assets,
 * and the wood sound silence/replay entries.
 */
object BlockAssetGenerator {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    private val INSTRUMENTS = listOf(
        "harp", "basedrum", "snare", "hat", "bass", "flute", "bell", "guitar",
        "chime", "xylophone", "iron_xylophone", "cow_bell", "didgeridoo", "bit", "banjo", "pling"
    )

    fun generate(plugin: EcoItemsPlugin, settings: PackSettings, entries: MutableMap<String, ByteArray>) {
        val blocks = EcoBlocks.values()
        if (blocks.isEmpty()) {
            return
        }

        val models = blocks.associateWith { resolveModels(plugin, it, entries) }

        for (backing in BlockBacking.entries) {
            val forBacking = blocks.filter { it.backing == backing && models[it] != null }
            if (forBacking.isEmpty()) {
                continue
            }

            val variants = JsonObject()

            // The reserved default state keeps vanilla blocks looking vanilla.
            when (backing) {
                BlockBacking.NOTEBLOCK -> variants.add(
                    "instrument=harp,powered=false",
                    variant("minecraft:block/note_block")
                )

                BlockBacking.STRINGBLOCK -> variants.add(
                    "attached=false,disarmed=false,east=false,north=false,powered=false,south=false,west=false",
                    variant("minecraft:block/barrier")
                )

                BlockBacking.CHORUS -> variants.add(
                    "down=false,east=false,north=false,south=false,up=false,west=false",
                    variant("minecraft:block/chorus_plant")
                )

                // The player-placed all-faces state; the vanilla model file
                // shares the material's name.
                BlockBacking.MUSHROOM, BlockBacking.MUSHROOM_RED, BlockBacking.MUSHROOM_STEM -> variants.add(
                    "down=true,east=true,north=true,south=true,up=true,west=true",
                    variant("minecraft:block/${backing.material.key.key}")
                )
            }

            for (block in forBacking.sortedBy { it.id }) {
                // One model per orientation for stackables, else one for all.
                val modelList = models.getValue(block)!!
                for ((orientation, variation) in EcoBlocks.variations(block).withIndex()) {
                    val key = block.orientations[orientation]
                    val (x, y) = ROTATIONS[key] ?: (0 to 0)
                    val model = modelList.getOrElse(orientation) { modelList.last() }
                    variants.add(variantName(backing, variation), variant(model, x, y))
                }
            }

            val json = JsonObject()
            json.add("variants", variants)
            entries["assets/minecraft/blockstates/${backing.material.key.key}.json"] =
                gson.toJson(json).encodeToByteArray()
        }

        generateItemDefinitions(blocks, models, entries)

        if (settings.customBlockSounds && blocks.any { it.backing == BlockBacking.NOTEBLOCK }) {
            generateSoundRemap(entries)
        }
    }

    private fun variant(model: String, x: Int = 0, y: Int = 0): JsonObject = JsonObject().apply {
        addProperty("model", model)
        if (x != 0) addProperty("x", x)
        if (y != 0) addProperty("y", y)
    }

    // Baked model rotations per directional orientation, matching vanilla's
    // log/furnace/dropper blockstates.
    private val ROTATIONS = mapOf(
        "" to (0 to 0),
        "y" to (0 to 0), "x" to (90 to 90), "z" to (90 to 0),
        "north" to (0 to 0), "east" to (0 to 90), "south" to (0 to 180), "west" to (0 to 270),
        "up" to (270 to 0), "down" to (90 to 0)
    )

    private fun variantName(backing: BlockBacking, variation: Int): String = when (backing) {
        BlockBacking.NOTEBLOCK -> {
            val raw = variation + 26
            "instrument=${INSTRUMENTS[(raw % 400) / 25]},note=${raw % 25},powered=${raw >= 400}"
        }

        BlockBacking.STRINGBLOCK ->
            "attached=${variation and 16 != 0},disarmed=${variation and 32 != 0}," +
                "east=${variation and 1 != 0},north=${variation and 8 != 0}," +
                "powered=${variation and 64 != 0},south=${variation and 4 != 0}," +
                "west=${variation and 2 != 0}"

        BlockBacking.CHORUS,
        BlockBacking.MUSHROOM,
        BlockBacking.MUSHROOM_RED,
        BlockBacking.MUSHROOM_STEM ->
            "down=${variation and 32 != 0},east=${variation and 4 != 0}," +
                "north=${variation and 1 != 0},south=${variation and 2 != 0}," +
                "up=${variation and 16 != 0},west=${variation and 8 != 0}"
    }

    /**
     * The model references for a block, one per orientation slot. Stackable
     * blocks resolve one model per stack count; everything else resolves a
     * single model shared by all orientations.
     */
    private fun resolveModels(
        plugin: EcoItemsPlugin,
        block: EcoBlock,
        entries: MutableMap<String, ByteArray>
    ): List<String>? {
        val stackable = block.stackable
        if (stackable != null && block.orientations.firstOrNull()?.startsWith("stack") == true) {
            return resolveStackModels(plugin, block, stackable, entries)
        }

        return resolveModel(plugin, block, entries)?.let { listOf(it) }
    }

    private fun resolveStackModels(
        plugin: EcoItemsPlugin,
        block: EcoBlock,
        stackable: StackableBlock,
        entries: MutableMap<String, ByteArray>
    ): List<String>? {
        if (stackable.models.isNotEmpty()) {
            return stackable.models.map { raw ->
                val location = PackLocation.parse(raw) ?: run {
                    warned(plugin, block, "stackable model '$raw' is not a valid location")
                    return null
                }
                if (location.namespace != "minecraft" &&
                    !location.file(plugin, "models", "json").exists() &&
                    location.entry("models", "json") !in entries
                ) {
                    plugin.logger.warning("Block ${block.id}: model file ${location.entry("models", "json")} does not exist")
                }
                "${location.namespace}:${location.path}"
            }
        }

        return stackable.textures.map { raw ->
            val location = PackLocation.parse(raw) ?: run {
                warned(plugin, block, "stackable texture '$raw' is not a valid location")
                return null
            }
            checkTexture(plugin, block, location)

            val parent = block.config.getStringOrNull("texture-parent") ?: "cross"
            val textureKey = if (parent == "cube_all") "all" else "cross"
            writeModel(plugin, location, mapOf(textureKey to location.key), parent, entries)
            "${location.namespace}:${location.path}"
        }
    }

    /**
     * The model reference for a block, generating one from its texture(s)
     * when no explicit json shadows it. Null when the block has no assets.
     */
    private fun resolveModel(
        plugin: EcoItemsPlugin,
        block: EcoBlock,
        entries: MutableMap<String, ByteArray>
    ): String? {
        block.config.getStringOrNull("model")?.let { raw ->
            val location = PackLocation.parse(raw) ?: return warned(plugin, block, "model '$raw' is not a valid location")
            if (location.namespace != "minecraft" &&
                !location.file(plugin, "models", "json").exists() &&
                location.entry("models", "json") !in entries
            ) {
                plugin.logger.warning("Block ${block.id}: model file ${location.entry("models", "json")} does not exist")
            }
            return "${location.namespace}:${location.path}"
        }

        block.config.getStringOrNull("texture")?.let { raw ->
            val location = PackLocation.parse(raw) ?: return warned(plugin, block, "texture '$raw' is not a valid location")
            checkTexture(plugin, block, location)

            val parent = block.config.getStringOrNull("texture-parent") ?: "cube_all"
            val textureKey = when (parent) {
                "cube_all" -> "all"
                "cross" -> "cross"
                else -> return warned(plugin, block, "texture-parent '$parent' needs a textures: map (all/cross work with a single texture)")
            }

            writeModel(plugin, location, mapOf(textureKey to location.key), parent, entries)
            return "${location.namespace}:${location.path}"
        }

        if (block.config.has("textures")) {
            val section = block.config.getSubsection("textures")
            val textures = mutableMapOf<String, String>()
            for (key in section.getKeys(false)) {
                val location = PackLocation.parse(section.getString(key))
                    ?: return warned(plugin, block, "texture '$key' is not a valid location")
                checkTexture(plugin, block, location)
                textures[key] = location.key
            }

            val parent = block.config.getStringOrNull("texture-parent") ?: when (textures.keys.sorted()) {
                listOf("all") -> "cube_all"
                listOf("bottom", "side", "top") -> "cube_bottom_top"
                listOf("end", "side") -> "cube_column"
                listOf("cross") -> "cross"
                listOf("front", "side", "top") -> "orientable"
                else -> return warned(
                    plugin, block,
                    "can't guess a model parent for textures ${textures.keys}; set texture-parent"
                )
            }

            val location = PackLocation.parse("block/${block.id}")!!
            writeModel(plugin, location, textures, parent, entries)
            return "${location.namespace}:${location.path}"
        }

        return null
    }

    private fun writeModel(
        plugin: EcoItemsPlugin,
        location: PackLocation,
        textures: Map<String, String>,
        parent: String,
        entries: MutableMap<String, ByteArray>
    ) {
        val path = location.entry("models", "json")
        // An explicit model json in the pack folder shadows generation.
        if (location.file(plugin, "models", "json").exists()) {
            return
        }

        val json = JsonObject()
        json.addProperty("parent", "minecraft:block/$parent")
        val texturesJson = JsonObject()
        for ((key, value) in textures.toSortedMap()) {
            texturesJson.addProperty(key, value)
        }
        json.add("textures", texturesJson)

        entries[path] = gson.toJson(json).encodeToByteArray()
    }

    private fun checkTexture(plugin: EcoItemsPlugin, block: EcoBlock, location: PackLocation) {
        if (location.namespace == "minecraft") {
            return
        }
        if (!location.file(plugin, "textures", "png").exists()) {
            plugin.logger.warning(
                "Block ${block.id}: texture file ${location.entry("textures", "png")} does not exist"
            )
        }
    }

    private fun warned(plugin: EcoItemsPlugin, block: EcoBlock, message: String): String? {
        plugin.logger.warning("Block ${block.id}: $message")
        return null
    }

    /**
     * Placer items without their own texture/model/definition show the block
     * model as their icon.
     */
    private fun generateItemDefinitions(
        blocks: Collection<EcoBlock>,
        models: Map<EcoBlock, List<String>?>,
        entries: MutableMap<String, ByteArray>
    ) {
        for (item in EcoItems.values()) {
            val block = item.block ?: item.crop?.block ?: continue
            // Seeds show their crop's final stage; blocks show their model.
            val model = models[block]?.let { if (item.crop != null) it.last() else it.first() } ?: continue

            val itemConfig = item.config.getSubsection("item")
            if (itemConfig.has("texture") || itemConfig.has("model") || itemConfig.has("definition")) {
                continue
            }

            val definition = JsonObject()
            val node = JsonObject()
            node.addProperty("type", "minecraft:model")
            node.addProperty("model", model)
            definition.add("model", node)

            entries["assets/ecoitems/items/${item.id.key}.json"] = gson.toJson(definition).encodeToByteArray()
        }
    }

    /**
     * The note block backing plays wood sounds the client triggers itself -
     * silence the vanilla events and define replayable copies the server
     * plays instead (for custom blocks and real wooden blocks alike).
     */
    private fun generateSoundRemap(entries: MutableMap<String, ByteArray>) {
        val minecraft = JsonObject()
        for (event in listOf("break", "fall", "hit", "place", "step")) {
            val silenced = JsonObject()
            silenced.addProperty("replace", true)
            silenced.add("sounds", com.google.gson.JsonArray())
            minecraft.add("block.wood.$event", silenced)
        }

        // Merge over anything already there (imports).
        entries["assets/minecraft/sounds.json"]?.let { existing ->
            runCatching { JsonParser.parseString(existing.decodeToString()).asJsonObject }
                .getOrNull()?.entrySet()?.forEach { (key, value) ->
                    if (!minecraft.has(key)) minecraft.add(key, value)
                }
        }
        entries["assets/minecraft/sounds.json"] = gson.toJson(minecraft).encodeToByteArray()

        val ecoitems = JsonObject()
        val samples = mapOf(
            "place" to (1..4).map { "minecraft:dig/wood$it" },
            "break" to (1..4).map { "minecraft:dig/wood$it" },
            "step" to (1..6).map { "minecraft:step/wood$it" },
            "hit" to (1..6).map { "minecraft:step/wood$it" },
            "fall" to (1..6).map { "minecraft:step/wood$it" }
        )
        for ((event, sounds) in samples) {
            val definition = JsonObject()
            val array = com.google.gson.JsonArray()
            sounds.forEach { array.add(it) }
            definition.add("sounds", array)
            ecoitems.add("required.wood.$event", definition)
        }

        // SoundAssetGenerator runs later and merges this entry (it wins on
        // its own keys, ours stay).
        entries["assets/ecoitems/sounds.json"]?.let { existing ->
            runCatching { JsonParser.parseString(existing.decodeToString()).asJsonObject }
                .getOrNull()?.entrySet()?.forEach { (key, value) ->
                    if (!ecoitems.has(key)) ecoitems.add(key, value)
                }
        }
        entries["assets/ecoitems/sounds.json"] = gson.toJson(ecoitems).encodeToByteArray()
    }
}
