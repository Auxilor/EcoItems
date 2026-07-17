package com.willfp.ecoitems.pack

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoitems.furniture.Furniture
import com.willfp.ecoitems.furniture.FurnitureState
import com.willfp.ecoitems.items.EcoItem

/**
 * A state's models: either explicit model locations or textures to generate
 * simple models from. Written as <state>-model(s) / <state>-texture(s).
 */
class StateModel(
    val locations: List<PackLocation>,
    val fromTextures: Boolean
) {
    companion object {
        fun parse(config: Config, state: String): Pair<StateModel?, String?> {
            val models = config.getStrings("$state-models")
                .ifEmpty { listOfNotNull(config.getStringOrNull("$state-model")) }
            val textures = config.getStrings("$state-textures")
                .ifEmpty { listOfNotNull(config.getStringOrNull("$state-texture")) }

            val (values, fromTextures) = when {
                models.isNotEmpty() -> models to false
                textures.isNotEmpty() -> textures to true
                else -> return null to null
            }

            val locations = values.map {
                PackLocation.parse(it) ?: return null to "$state value '$it' is not a valid location"
            }

            return StateModel(locations, fromTextures) to null
        }
    }
}

/**
 * The pack-facing part of an item config: the base texture/model, optional
 * state models (pulling, blocking, ...), and the raw definition escape hatch.
 */
class ItemPackAsset(
    val id: String,
    val texture: PackLocation?,
    val textureParent: String,
    val model: PackLocation?,
    val definition: Config?,
    val states: Map<String, StateModel>,
    val isCrossbow: Boolean,
    val invalid: String?
) {
    val throwing: StateModel?
        get() = states["throwing"]

    companion object {
        // The convenience state keys and how they compose is decided by
        // ItemDefinitionBuilder; this just parses them.
        private val STATES = listOf(
            "pulling", "charged", "firework", "blocking", "cast", "broken", "damaged", "throwing"
        )

        fun fromItem(item: EcoItem): ItemPackAsset? {
            val itemConfig = item.config.getSubsection("item")

            val texture = itemConfig.getStringOrNull("texture")
            val model = itemConfig.getStringOrNull("model")
            val definition = if (itemConfig.has("definition")) itemConfig.getSubsection("definition") else null

            val states = mutableMapOf<String, StateModel>()
            var invalid: String? = null

            for (state in STATES) {
                val (parsed, problem) = StateModel.parse(itemConfig, state)
                if (problem != null) {
                    invalid = invalid ?: problem
                }
                if (parsed != null) {
                    states[state] = parsed
                }
            }

            if (texture == null && model == null && definition == null && states.isEmpty()) {
                return null
            }

            val parsedTexture = texture?.let { PackLocation.parse(it) }
            val parsedModel = model?.let { PackLocation.parse(it) }

            invalid = when {
                texture != null && parsedTexture == null -> "texture '$texture' is not a valid location"
                model != null && parsedModel == null -> "model '$model' is not a valid location"
                texture == null && model == null && definition == null -> "state models need a base texture or model"
                else -> invalid
            }

            return ItemPackAsset(
                item.id.key,
                parsedTexture,
                itemConfig.getStringOrNull("texture-parent") ?: "generated",
                parsedModel,
                definition,
                states,
                itemConfig.getString("item").substringBefore(' ').equals("crossbow", ignoreCase = true),
                invalid
            )
        }

        /** A furniture state's alternative look, as its own item asset. */
        fun fromFurnitureState(furniture: Furniture, state: FurnitureState): ItemPackAsset? {
            if (!state.hasAssets) {
                return null
            }

            val texture = state.config.getStringOrNull("texture")
            val model = state.config.getStringOrNull("model")

            val parsedTexture = texture?.let { PackLocation.parse(it) }
            val parsedModel = model?.let { PackLocation.parse(it) }

            val invalid = when {
                texture != null && parsedTexture == null -> "texture '$texture' is not a valid location"
                model != null && parsedModel == null -> "model '$model' is not a valid location"
                else -> null
            }

            return ItemPackAsset(
                "${furniture.id}_state_${state.name}",
                parsedTexture,
                state.config.getStringOrNull("texture-parent") ?: "generated",
                parsedModel,
                null,
                emptyMap(),
                false,
                invalid
            )
        }
    }
}
