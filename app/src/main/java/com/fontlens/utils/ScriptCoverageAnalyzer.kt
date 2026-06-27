package com.fontlens.utils

/**
 * Computes which scripts a font supports based on its cmap glyph set.
 * Uses true script names (Latin, Devanagari, etc.) not language names.
 * Coverage = (matched glyphs in range) / (total range size).
 * If coverage >= threshold, the script is considered supported.
 */
object ScriptCoverageAnalyzer {

    data class ScriptDef(val code: String, val ranges: List<IntRange>)

    val SCRIPTS: List<ScriptDef> = listOf(

        // Latin — covers English, French, German, Spanish, Polish, Czech, etc.
        ScriptDef("latin", listOf(
            0x0041..0x005A,  // Basic Latin uppercase A-Z
            0x0061..0x007A,  // Basic Latin lowercase a-z
            0x00C0..0x00FF,  // Latin-1 Supplement (à é ñ ü…)
            0x0100..0x017F,  // Latin Extended-A (ą ę ó ž š…)
            0x0180..0x024F   // Latin Extended-B (broader European)
        )),

        // Devanagari — Hindi, Marathi, Nepali, Sanskrit, Maithili
        ScriptDef("devanagari", listOf(0x0900..0x097F)),

        // Bengali — Bengali, Assamese
        ScriptDef("bengali", listOf(0x0980..0x09FF)),

        // Arabic — Arabic, Urdu, Persian, Pashto, etc.
        ScriptDef("arabic", listOf(
            0x0600..0x06FF,
            0x0750..0x077F   // Arabic Supplement
        )),

        // Hebrew
        ScriptDef("hebrew", listOf(0x0590..0x05FF)),

        // Han / CJK — Chinese (Simplified & Traditional), Japanese Kanji, Korean Hanja
        ScriptDef("han", listOf(0x4E00..0x9FFF)),

        // Hiragana + Katakana — Japanese syllabaries
        ScriptDef("kana", listOf(
            0x3040..0x309F,  // Hiragana
            0x30A0..0x30FF   // Katakana
        )),

        // Hangul — Korean
        ScriptDef("hangul", listOf(0xAC00..0xD7A3)),

        // Tamil
        ScriptDef("tamil", listOf(0x0B80..0x0BFF)),

        // Telugu
        ScriptDef("telugu", listOf(0x0C00..0x0C7F)),

        // Kannada
        ScriptDef("kannada", listOf(0x0C80..0x0CFF)),

        // Malayalam
        ScriptDef("malayalam", listOf(0x0D00..0x0D7F)),

        // Gujarati
        ScriptDef("gujarati", listOf(0x0A80..0x0AFF)),

        // Gurmukhi — Punjabi
        ScriptDef("gurmukhi", listOf(0x0A00..0x0A7F)),

        // Odia (Oriya)
        ScriptDef("odia", listOf(0x0B00..0x0B7F)),

        // Sinhala
        ScriptDef("sinhala", listOf(0x0D80..0x0DFF)),

        // Thai
        ScriptDef("thai", listOf(0x0E00..0x0E7F)),

        // Khmer
        ScriptDef("khmer", listOf(0x1780..0x17FF)),

        // Myanmar — Burmese, Karen, Mon, etc.
        ScriptDef("myanmar", listOf(0x1000..0x109F)),

        // Georgian
        ScriptDef("georgian", listOf(0x10A0..0x10FF)),

        // Armenian
        ScriptDef("armenian", listOf(0x0530..0x058F)),

        // Ethiopic — Amharic, Tigrinya, Oromo, etc.
        ScriptDef("ethiopic", listOf(0x1200..0x137F)),

        // Cyrillic — Russian, Bulgarian, Serbian, Ukrainian, Macedonian, etc.
        ScriptDef("cyrillic", listOf(0x0400..0x04FF)),

        // Greek
        ScriptDef("greek", listOf(0x0370..0x03FF)),

        // Tibetan
        ScriptDef("tibetan", listOf(0x0F00..0x0FFF)),

        // Lao
        ScriptDef("lao", listOf(0x0E80..0x0EFF)),

        // Mongolian
        ScriptDef("mongolian", listOf(0x1800..0x18AF))
    )

    private val rangeSizes: Map<String, Int> = SCRIPTS.associate { def ->
        def.code to def.ranges.sumOf { it.last - it.first + 1 }
    }

    fun analyze(supportedChars: List<Int>, thresholdPercent: Int): List<String> {
        if (supportedChars.isEmpty() || thresholdPercent <= 0) return emptyList()
        val charSet = supportedChars.toHashSet()
        val result  = mutableListOf<String>()
        for (def in SCRIPTS) {
            val total = rangeSizes[def.code] ?: continue
            if (total == 0) continue
            var matched = 0
            for (range in def.ranges) {
                for (cp in range) { if (charSet.contains(cp)) matched++ }
            }
            if (def.code == "latin") {
                // Also qualify if ≥ 80% of just A-Z/a-z (52 chars) are present —
                // a Basic-Latin-only font is still a Latin font even with no extended chars.
                val basicMatched = (0x0041..0x005A).count { charSet.contains(it) } +
                                   (0x0061..0x007A).count { charSet.contains(it) }
                if (basicMatched * 100 / 52 >= 80 || matched * 100 / total >= thresholdPercent)
                    result.add("latin")
            } else {
                if (matched * 100 / total >= thresholdPercent) result.add(def.code)
            }
        }
        return result
    }
}
