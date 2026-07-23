package com.willfp.ecoitems.pack

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.pack.glyphs.GlyphText
import java.io.File

/**
 * Applies pack/assets/minecraft/lang/global.json to every language at once.
 *
 * global.json is not a real pack file: its entries are copied into every
 * vanilla language, with per-language files (from the pack folder or from
 * imports, already in the entries map) taking precedence. Values go through
 * the glyph replacer, so :glyph: placeholders work in lang entries. Keys
 * starting with _ are comments and skipped.
 */
object LangAssetGenerator {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    // Every language code the vanilla client ships.
    private val LANGUAGE_CODES = setOf(
        "af_za", "ar_sa", "ast_es", "az_az", "ba_ru",
        "bar", "be_by", "bg_bg", "br_fr", "brb", "bs_ba", "ca_es", "cs_cz",
        "cy_gb", "da_dk", "de_at", "de_ch", "de_de", "el_gr", "en_au", "en_ca",
        "en_gb", "en_nz", "en_pt", "en_ud", "en_us", "enp", "enws", "eo_uy",
        "es_ar", "es_cl", "es_ec", "es_es", "es_mx", "es_uy", "es_ve", "esan",
        "et_ee", "eu_es", "fa_ir", "fi_fi", "fil_ph", "fo_fo", "fr_ca", "fr_fr",
        "fra_de", "fur_it", "fy_nl", "ga_ie", "gd_gb", "gl_es", "haw_us", "he_il",
        "hi_in", "hr_hr", "hu_hu", "hy_am", "id_id", "ig_ng", "io_en", "is_is",
        "isv", "it_it", "ja_jp", "jbo_en", "ka_ge", "kk_kz", "kn_in", "ko_kr",
        "ksh", "kw_gb", "la_la", "lb_lu", "li_li", "lmo", "lol_us", "lt_lt",
        "lv_lv", "lzh", "mk_mk", "mn_mn", "ms_my", "mt_mt", "nah", "nds_de",
        "nl_be", "nl_nl", "nn_no", "no_no", "oc_fr", "ovd", "pl_pl", "pt_br",
        "pt_pt", "qya_aa", "ro_ro", "rpr", "ru_ru", "ry_ua", "se_no", "sk_sk",
        "sl_si", "so_so", "sq_al", "sr_sp", "sv_se", "sxu", "szl", "ta_in",
        "th_th", "tl_ph", "tlh_aa", "tok", "tr_tr", "tt_ru", "uk_ua", "val_es",
        "vec_it", "vi_vn", "yi_de", "yo_ng", "zh_cn", "zh_hk", "zh_tw", "zlm_arab"
    )

    const val GLOBAL_LANG = "assets/minecraft/lang/global.json"

    fun generate(plugin: EcoItemsPlugin, entries: MutableMap<String, ByteArray>) {
        val global = readLang(plugin, plugin.dataFolder.resolve("pack/$GLOBAL_LANG")) ?: JsonObject()

        // Languages to emit: all of them if global has entries, otherwise
        // just the ones something already contributed a file for.
        val existing = entries.keys
            .filter { it.startsWith("assets/minecraft/lang/") }
            .map { it.removePrefix("assets/minecraft/lang/").removeSuffix(".json") }

        val codes = if (global.size() > 0) LANGUAGE_CODES + existing else existing

        for (code in codes) {
            if (code !in LANGUAGE_CODES) {
                plugin.logger.warning("Language file $code.json is not a known language code; including it anyway")
            }

            val target = "assets/minecraft/lang/$code.json"
            val merged = JsonObject()

            // Global entries first; the per-language file (already merged
            // from imports and the pack folder) wins per key.
            copyInto(global, merged)

            entries[target]?.let { file ->
                runCatching { JsonParser.parseString(file.decodeToString()).asJsonObject }
                    .getOrNull()
                    ?.let { copyInto(it, merged) }
            }

            if (merged.size() > 0) {
                entries[target] = gson.toJson(merged).encodeToByteArray()
            }
        }
    }

    private fun copyInto(source: JsonObject, target: JsonObject) {
        for ((key, value) in source.entrySet()) {
            if (key.startsWith("_")) {
                continue
            }

            val processed = if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                // Resolve :glyph: placeholders at build time.
                JsonPrimitive(GlyphText.replaceLegacy(value.asString, null, false))
            } else {
                value
            }

            target.add(key, processed)
        }
    }

    private fun readLang(plugin: EcoItemsPlugin, file: File): JsonObject? {
        if (!file.isFile) {
            return null
        }

        return runCatching { JsonParser.parseString(file.readText()).asJsonObject }
            .onFailure { plugin.logger.warning("Could not parse ${file.name}: not a valid language file") }
            .getOrNull()
    }
}
