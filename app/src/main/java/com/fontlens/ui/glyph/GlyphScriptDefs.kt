package com.fontlens.ui.glyph

/**
 * Fine-grained Unicode block definitions for the Glyph Map viewer.
 * Each block is a separate "page" — even if two blocks belong to the same script
 * (e.g. Latin, Latin Extended-A, Latin Extended-B) they are shown separately.
 *
 * Only blocks that have at least one glyph in the font are shown.
 */
object GlyphScriptDefs {

    data class UnicodeBlock(
        val name: String,          // Display name shown as page header
        val range: IntRange        // Codepoint range (inclusive)
    )

    val ALL_BLOCKS: List<UnicodeBlock> = listOf(

        // ── Latin ──────────────────────────────────────────────────────────
        UnicodeBlock("Basic Latin",               0x0020..0x007F),
        UnicodeBlock("Latin-1 Supplement",        0x0080..0x00FF),
        UnicodeBlock("Latin Extended-A",          0x0100..0x017F),
        UnicodeBlock("Latin Extended-B",          0x0180..0x024F),
        UnicodeBlock("IPA Extensions",            0x0250..0x02AF),
        UnicodeBlock("Spacing Modifier Letters",  0x02B0..0x02FF),
        UnicodeBlock("Combining Diacritical",     0x0300..0x036F),

        // ── Greek & Coptic ──────────────────────────────────────────────────
        UnicodeBlock("Greek & Coptic",            0x0370..0x03FF),

        // ── Cyrillic ───────────────────────────────────────────────────────
        UnicodeBlock("Cyrillic",                  0x0400..0x04FF),
        UnicodeBlock("Cyrillic Supplement",       0x0500..0x052F),

        // ── Armenian / Hebrew / Arabic ─────────────────────────────────────
        UnicodeBlock("Armenian",                  0x0530..0x058F),
        UnicodeBlock("Hebrew",                    0x0590..0x05FF),
        UnicodeBlock("Arabic",                    0x0600..0x06FF),
        UnicodeBlock("Syriac",                    0x0700..0x074F),
        UnicodeBlock("Arabic Supplement",         0x0750..0x077F),

        // ── Indic scripts ─────────────────────────────────────────────────
        UnicodeBlock("Devanagari",                0x0900..0x097F),
        UnicodeBlock("Bengali",                   0x0980..0x09FF),
        UnicodeBlock("Gurmukhi",                  0x0A00..0x0A7F),
        UnicodeBlock("Gujarati",                  0x0A80..0x0AFF),
        UnicodeBlock("Oriya / Odia",              0x0B00..0x0B7F),
        UnicodeBlock("Tamil",                     0x0B80..0x0BFF),
        UnicodeBlock("Telugu",                    0x0C00..0x0C7F),
        UnicodeBlock("Kannada",                   0x0C80..0x0CFF),
        UnicodeBlock("Malayalam",                 0x0D00..0x0D7F),
        UnicodeBlock("Sinhala",                   0x0D80..0x0DFF),

        // ── South-East Asian ──────────────────────────────────────────────
        UnicodeBlock("Thai",                      0x0E00..0x0E7F),
        UnicodeBlock("Lao",                       0x0E80..0x0EFF),
        UnicodeBlock("Tibetan",                   0x0F00..0x0FFF),
        UnicodeBlock("Myanmar",                   0x1000..0x109F),
        UnicodeBlock("Georgian",                  0x10A0..0x10FF),
        UnicodeBlock("Hangul Jamo",               0x1100..0x11FF),
        UnicodeBlock("Ethiopic",                  0x1200..0x137F),
        UnicodeBlock("Khmer",                     0x1780..0x17FF),
        UnicodeBlock("Mongolian",                 0x1800..0x18AF),

        // ── Punctuation / Symbols ─────────────────────────────────────────
        UnicodeBlock("General Punctuation",       0x2000..0x206F),
        UnicodeBlock("Superscripts & Subscripts", 0x2070..0x209F),
        UnicodeBlock("Currency Symbols",          0x20A0..0x20CF),
        UnicodeBlock("Letterlike Symbols",        0x2100..0x214F),
        UnicodeBlock("Number Forms",              0x2150..0x218F),
        UnicodeBlock("Arrows",                    0x2190..0x21FF),
        UnicodeBlock("Mathematical Operators",    0x2200..0x22FF),
        UnicodeBlock("Miscellaneous Technical",   0x2300..0x23FF),
        UnicodeBlock("Control Pictures",          0x2400..0x243F),
        UnicodeBlock("Box Drawing",               0x2500..0x257F),
        UnicodeBlock("Block Elements",            0x2580..0x259F),
        UnicodeBlock("Geometric Shapes",          0x25A0..0x25FF),
        UnicodeBlock("Miscellaneous Symbols",     0x2600..0x26FF),
        UnicodeBlock("Dingbats",                  0x2700..0x27BF),
        UnicodeBlock("Braille Patterns",          0x2800..0x28FF),

        // ── CJK ───────────────────────────────────────────────────────────
        UnicodeBlock("CJK Radicals Supplement",   0x2E80..0x2EFF),
        UnicodeBlock("Kangxi Radicals",           0x2F00..0x2FDF),
        UnicodeBlock("CJK Symbols & Punctuation", 0x3000..0x303F),
        UnicodeBlock("Hiragana",                  0x3040..0x309F),
        UnicodeBlock("Katakana",                  0x30A0..0x30FF),
        UnicodeBlock("Bopomofo",                  0x3100..0x312F),
        UnicodeBlock("Hangul Compatibility Jamo", 0x3130..0x318F),
        UnicodeBlock("CJK Unified Ideographs",    0x4E00..0x9FFF),
        UnicodeBlock("Hangul Syllables",          0xAC00..0xD7A3),

        // ── Alphabetic Presentation Forms ─────────────────────────────────
        UnicodeBlock("Alphabetic Presentation",   0xFB00..0xFB4F),
        UnicodeBlock("Arabic Presentation-A",     0xFB50..0xFDFF),
        UnicodeBlock("Arabic Presentation-B",     0xFE70..0xFEFF),

        // ── Halfwidth / Fullwidth ─────────────────────────────────────────
        UnicodeBlock("Halfwidth & Fullwidth",     0xFF00..0xFFEF),

        // ── Math / Supplemental ───────────────────────────────────────────
        UnicodeBlock("Mathematical Alphanumeric", 0x1D400..0x1D7FF),
        UnicodeBlock("Emoji & Pictographs",       0x1F300..0x1F9FF)
    )

    /**
     * Given a font's supported codepoints, returns only blocks that have
     * at least one supported codepoint, along with which of those codepoints
     * are present (for "show all" mode we include the full range).
     */
    data class BlockPage(
        val block: UnicodeBlock,
        /** Codepoints to display — either only present ones, or full range */
        val codepoints: List<Int>,
        /** Set of codepoints actually in the font (for shadow coloring) */
        val presentSet: Set<Int>
    )

    fun buildPages(
        supportedChars: Set<Int>,
        showAll: Boolean
    ): List<BlockPage> {
        val pages = mutableListOf<BlockPage>()
        for (block in ALL_BLOCKS) {
            val present = block.range.filter { supportedChars.contains(it) }
            if (present.isEmpty()) continue          // skip blocks with 0 glyphs

            val codepoints = if (showAll) block.range.toList() else present
            pages.add(BlockPage(block, codepoints, present.toSet()))
        }
        return pages
    }
}
