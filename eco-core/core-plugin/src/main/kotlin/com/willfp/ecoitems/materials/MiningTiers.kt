package com.willfp.ecoitems.materials

/** The vanilla mining tiers, lowest first, as named in the incorrect_for_<tier>_tool block tags. */
object MiningTiers {
    private val ORDER = listOf("wooden", "stone", "iron", "diamond", "netherite")

    /** The higher of two tiers; an unknown or missing tier counts as lower than any known one. */
    fun higher(first: String?, second: String?): String? =
        listOfNotNull(first, second).maxByOrNull { ORDER.indexOf(it) }
}
