package com.fontlens.data

import com.fontlens.data.scriptDisplayName

/**
 * A single language definition.
 *
 * @param name          Display name, e.g. "Spanish"
 * @param isoCode       BCP-47 / ISO 639-1 code, e.g. "es"
 * @param scriptCode    Parent script code matching ScriptCoverageAnalyzer, e.g. "latin"
 * @param requiredChars Codepoints that MUST all be present for this language to show.
 *                      Empty list = only the base script coverage is required.
 */
data class LanguageDef(
    val name: String,
    val isoCode: String,
    val scriptCode: String,
    val requiredChars: List<Int>
)

/**
 * Master language list, grouped by script.
 * requiredChars contains only the characters BEYOND the base script range
 * that a font must also have to properly support this language.
 *
 * Strategy: keep requirements minimal and accurate — only glyphs that are
 * genuinely unique to the language and likely to be missing in a basic font.
 */
val ALL_LANGUAGES: List<LanguageDef> = listOf(

    // ── ANSI legacy fonts ─────────────────────────────────────────────────
    // Special entry: fonts that encode non-Latin scripts at ANSI/ASCII slots
    LanguageDef("ANSI", "ansi", "ansi", emptyList()),

    // ── Latin script ──────────────────────────────────────────────────────
    // English — only A-Z/a-z, covered by base Latin
    LanguageDef("English",    "en", "latin", emptyList()),

    // Spanish — ñ, Ñ, ¿, ¡, and accented vowels
    LanguageDef("Spanish",    "es", "latin", listOf(
        0x00F1, 0x00D1,  // ñ Ñ
        0x00E1, 0x00E9, 0x00ED, 0x00F3, 0x00FA, // á é í ó ú
        0x00C1, 0x00C9, 0x00CD, 0x00D3, 0x00DA  // Á É Í Ó Ú
    )),

    // French — accented chars + ç, œ, æ, «, »
    LanguageDef("French",     "fr", "latin", listOf(
        0x00E0, 0x00E2, 0x00E4, // à â ä
        0x00E8, 0x00E9, 0x00EA, 0x00EB, // è é ê ë
        0x00EE, 0x00EF,         // î ï
        0x00F4, 0x00F9, 0x00FB, 0x00FC, // ô ù û ü
        0x00E7, 0x00C7,         // ç Ç
        0x0153, 0x0152,         // œ Œ
        0x00AB, 0x00BB          // « »
    )),

    // German — umlauts + ß
    LanguageDef("German",     "de", "latin", listOf(
        0x00E4, 0x00F6, 0x00FC, // ä ö ü
        0x00C4, 0x00D6, 0x00DC, // Ä Ö Ü
        0x00DF                  // ß
    )),

    // Italian — accented vowels
    LanguageDef("Italian",    "it", "latin", listOf(
        0x00E0, 0x00E8, 0x00E9, 0x00EC, 0x00F2, 0x00F9, // à è é ì ò ù
        0x00C0, 0x00C8, 0x00C9, 0x00CC, 0x00D2, 0x00D9  // À È É Ì Ò Ù
    )),

    // Portuguese — tildes, cedilla, accented vowels
    LanguageDef("Portuguese", "pt", "latin", listOf(
        0x00E3, 0x00F5,         // ã õ
        0x00C3, 0x00D5,         // Ã Õ
        0x00E7, 0x00C7,         // ç Ç
        0x00E1, 0x00E9, 0x00ED, 0x00F3, 0x00FA, // á é í ó ú
        0x00C1, 0x00C9, 0x00CD, 0x00D3, 0x00DA  // Á É Í Ó Ú
    )),

    // Dutch — basic Latin covers most; ij ligature optional but ë matters
    LanguageDef("Dutch",      "nl", "latin", listOf(
        0x00EB, 0x00EF, 0x00FC  // ë ï ü (most common accented chars in NL)
    )),

    // Polish — ogonek, kreska, stroke
    LanguageDef("Polish",     "pl", "latin", listOf(
        0x0105, 0x0104, // ą Ą
        0x0107, 0x0106, // ć Ć
        0x0119, 0x0118, // ę Ę
        0x0142, 0x0141, // ł Ł
        0x0144, 0x0143, // ń Ń
        0x00F3, 0x00D3, // ó Ó
        0x015B, 0x015A, // ś Ś
        0x017A, 0x0179, // ź Ź
        0x017C, 0x017B  // ż Ż
    )),

    // Czech — háček chars
    LanguageDef("Czech",      "cs", "latin", listOf(
        0x010D, 0x010C, // č Č
        0x010F, 0x010E, // ď Ď
        0x011B, 0x011A, // ě Ě
        0x0148, 0x0147, // ň Ň
        0x0159, 0x0158, // ř Ř
        0x0161, 0x0160, // š Š
        0x0165, 0x0164, // ť Ť
        0x016F, 0x016E, // ů Ů
        0x017E, 0x017D  // ž Ž
    )),

    // Romanian — comma-below s/t
    LanguageDef("Romanian",   "ro", "latin", listOf(
        0x0219, 0x0218, // ș Ș (comma below)
        0x021B, 0x021A, // ț Ț (comma below)
        0x00E2, 0x00C2, // â Â
        0x00EE, 0x00CE, // î Î
        0x0103, 0x0102  // ă Ă
    )),

    // Hungarian — double acute
    LanguageDef("Hungarian",  "hu", "latin", listOf(
        0x0151, 0x0150, // ő Ő
        0x0171, 0x0170  // ű Ű
    )),

    // Turkish — dotted/dotless i, ş, ğ, ç
    LanguageDef("Turkish",    "tr", "latin", listOf(
        0x0131, 0x0130, // ı İ
        0x015F, 0x015E, // ş Ş
        0x011F, 0x011E, // ğ Ğ
        0x00E7, 0x00C7, // ç Ç
        0x00F6, 0x00D6, // ö Ö
        0x00FC, 0x00DC  // ü Ü
    )),

    // Swedish / Norwegian / Danish — å, æ/ä, ø/ö
    LanguageDef("Swedish",    "sv", "latin", listOf(
        0x00E5, 0x00C5, // å Å
        0x00E4, 0x00C4, // ä Ä
        0x00F6, 0x00D6  // ö Ö
    )),
    LanguageDef("Norwegian",  "no", "latin", listOf(
        0x00E6, 0x00C6, // æ Æ
        0x00F8, 0x00D8, // ø Ø
        0x00E5, 0x00C5  // å Å
    )),
    LanguageDef("Danish",     "da", "latin", listOf(
        0x00E6, 0x00C6, // æ Æ
        0x00F8, 0x00D8, // ø Ø
        0x00E5, 0x00C5  // å Å
    )),

    // Finnish — only ä ö beyond basic Latin
    LanguageDef("Finnish",    "fi", "latin", listOf(
        0x00E4, 0x00C4, // ä Ä
        0x00F6, 0x00D6  // ö Ö
    )),

    // Vietnamese — extensive diacritic stack
    LanguageDef("Vietnamese", "vi", "latin", listOf(
        0x1EA0, 0x1EA1, 0x1EA2, 0x1EA3, 0x1EA4, 0x1EA5, // Ạ ạ Ả ả Ấ ấ …
        0x0103, 0x0102, 0x01A1, 0x01A0, 0x01B0, 0x01AF, // ă Ă ơ Ơ ư Ư
        0x0111, 0x0110  // đ Đ
    )),

    // Indonesian / Malay — basic Latin suffices
    LanguageDef("Indonesian", "id", "latin", emptyList()),
    LanguageDef("Malay",      "ms", "latin", emptyList()),

    // Swahili / Tagalog — basic Latin
    LanguageDef("Swahili",    "sw", "latin", emptyList()),
    LanguageDef("Filipino",   "fil", "latin", listOf(0x00F1, 0x00D1)), // ñ Ñ

    // ── Cyrillic script ───────────────────────────────────────────────────
    // Russian — base Cyrillic (А-Я / а-я)
    LanguageDef("Russian",    "ru", "cyrillic", emptyList()),

    // Ukrainian — extra chars і, ї, є, ґ
    LanguageDef("Ukrainian",  "uk", "cyrillic", listOf(
        0x0456, 0x0406, // і І
        0x0457, 0x0407, // ї Ї
        0x0454, 0x0404, // є Є
        0x0491, 0x0490  // ґ Ґ
    )),

    // Bulgarian — base Cyrillic covers it
    LanguageDef("Bulgarian",  "bg", "cyrillic", emptyList()),

    // Serbian — ђ, ј, љ, њ, ћ, џ
    LanguageDef("Serbian",    "sr", "cyrillic", listOf(
        0x0452, 0x0402, // ђ Ђ
        0x0458,         // ј
        0x0459, 0x0409, // љ Љ
        0x045A, 0x040A, // њ Њ
        0x045B, 0x040B, // ћ Ћ
        0x045F, 0x040F  // џ Џ
    )),

    // Macedonian — same as Serbian set
    LanguageDef("Macedonian", "mk", "cyrillic", listOf(
        0x0452, 0x0402,
        0x0458,
        0x0459, 0x0409,
        0x045A, 0x040A,
        0x045C, 0x040C, // ќ Ќ
        0x0455, 0x0405  // ѕ Ѕ
    )),

    // Kazakh — additional Cyrillic letters
    LanguageDef("Kazakh",     "kk", "cyrillic", listOf(
        0x04D9, 0x04D8, // ә Ә
        0x0493, 0x0492, // ғ Ғ
        0x049B, 0x049A, // қ Қ
        0x04A3, 0x04A2, // ң Ң
        0x04E9, 0x04E8, // ө Ө
        0x04B1, 0x04B0, // ұ Ұ
        0x04AF, 0x04AE, // ү Ү
        0x04BB, 0x04BA  // һ Һ
    )),

    // ── Arabic script ─────────────────────────────────────────────────────
    // Arabic (Standard) — base Arabic block
    LanguageDef("Arabic",     "ar", "arabic", emptyList()),

    // Persian / Farsi — extra chars پ چ ژ گ
    LanguageDef("Persian",    "fa", "arabic", listOf(
        0x067E, // پ
        0x0686, // چ
        0x0698, // ژ
        0x06AF  // گ
    )),

    // Urdu — same extra chars as Persian + ڈ ڑ ں
    LanguageDef("Urdu",       "ur", "arabic", listOf(
        0x067E, 0x0686, 0x0698, 0x06AF, // پ چ ژ گ
        0x0688, 0x0691, 0x06BA           // ڈ ڑ ں
    )),

    // ── Devanagari script ─────────────────────────────────────────────────
    // Hindi — base Devanagari
    LanguageDef("Hindi",      "hi", "devanagari", emptyList()),

    // Marathi — same block + ळ
    LanguageDef("Marathi",    "mr", "devanagari", listOf(0x0933)), // ळ

    // Nepali — base Devanagari covers it
    LanguageDef("Nepali",     "ne", "devanagari", emptyList()),

    // ── Han script ────────────────────────────────────────────────────────
    // Chinese (Simplified) — needs CJK Unified Ideographs
    LanguageDef("Chinese (Simplified)",  "zh-Hans", "han", emptyList()),

    // Chinese (Traditional) — same range; differentiated by variant glyphs
    // (we can't detect variant glyphs, so same requirement as Simplified)
    LanguageDef("Chinese (Traditional)", "zh-Hant", "han", emptyList()),

    // Japanese (Kanji) — shares CJK block; additional check via kana
    LanguageDef("Japanese",   "ja", "han", emptyList()),

    // Korean (Hanja) — shares CJK block
    LanguageDef("Korean",     "ko", "han", emptyList()),

    // ── Kana (Hiragana / Katakana) ────────────────────────────────────────
    LanguageDef("Japanese (Kana)", "ja-kana", "kana", emptyList()),

    // ── Hangul ───────────────────────────────────────────────────────────
    LanguageDef("Korean",     "ko-hang", "hangul", emptyList()),

    // ── Tamil ─────────────────────────────────────────────────────────────
    LanguageDef("Tamil",      "ta", "tamil", emptyList()),

    // ── Telugu ───────────────────────────────────────────────────────────
    LanguageDef("Telugu",     "te", "telugu", emptyList()),

    // ── Kannada ──────────────────────────────────────────────────────────
    LanguageDef("Kannada",    "kn", "kannada", emptyList()),

    // ── Malayalam ────────────────────────────────────────────────────────
    LanguageDef("Malayalam",  "ml", "malayalam", emptyList()),

    // ── Gujarati ─────────────────────────────────────────────────────────
    LanguageDef("Gujarati",   "gu", "gujarati", emptyList()),

    // ── Gurmukhi (Punjabi) ───────────────────────────────────────────────
    LanguageDef("Punjabi",    "pa", "gurmukhi", emptyList()),

    // ── Odia ─────────────────────────────────────────────────────────────
    LanguageDef("Odia",       "or", "odia", emptyList()),

    // ── Sinhala ──────────────────────────────────────────────────────────
    LanguageDef("Sinhala",    "si", "sinhala", emptyList()),

    // ── Thai ─────────────────────────────────────────────────────────────
    LanguageDef("Thai",       "th", "thai", emptyList()),

    // ── Khmer ────────────────────────────────────────────────────────────
    LanguageDef("Khmer",      "km", "khmer", emptyList()),

    // ── Myanmar ──────────────────────────────────────────────────────────
    LanguageDef("Burmese",    "my", "myanmar", emptyList()),

    // ── Georgian ─────────────────────────────────────────────────────────
    LanguageDef("Georgian",   "ka", "georgian", emptyList()),

    // ── Armenian ─────────────────────────────────────────────────────────
    LanguageDef("Armenian",   "hy", "armenian", emptyList()),

    // ── Ethiopic ─────────────────────────────────────────────────────────
    LanguageDef("Amharic",    "am", "ethiopic", emptyList()),

    // ── Greek ────────────────────────────────────────────────────────────
    LanguageDef("Greek",      "el", "greek", emptyList()),

    // ── Hebrew ───────────────────────────────────────────────────────────
    LanguageDef("Hebrew",     "he", "hebrew", emptyList()),

    // ── Tibetan ──────────────────────────────────────────────────────────
    LanguageDef("Tibetan",    "bo", "tibetan", emptyList()),

    // ── Lao ──────────────────────────────────────────────────────────────
    LanguageDef("Lao",        "lo", "lao", emptyList()),

    // ── Mongolian ────────────────────────────────────────────────────────
    LanguageDef("Mongolian",  "mn", "mongolian", emptyList()),

    // ── Bengali ──────────────────────────────────────────────────────────
    LanguageDef("Bengali",    "bn", "bengali", emptyList()),
    LanguageDef("Assamese",   "as", "bengali", listOf(0x09F0, 0x09F1)) // ৰ ৱ (Assamese-specific)
)

/** All languages for a given script code, preserving definition order. */
fun languagesForScript(scriptCode: String): List<LanguageDef> =
    ALL_LANGUAGES.filter { it.scriptCode == scriptCode }

/**
 * True when a script has exactly one language defined and the language name
 * matches the script display name (e.g. Thai/Thai, Greek/Greek).
 * Used to suppress "Language · Script" in favour of just "Language" in Settings.
 */
fun isSingleNameScript(scriptCode: String): Boolean {
    val langs = languagesForScript(scriptCode)
    if (langs.size != 1) return false
    return langs[0].name.equals(scriptDisplayName(scriptCode), ignoreCase = true)
}

/**
 * Returns languages supported by a font for a given script, in the user's
 * preferred order (langOrder). Filters by required glyph coverage.
 * Returns empty list if fewer than 2 languages pass (no language row shown).
 */
fun supportedLanguages(
    scriptCode: String,
    supportedChars: Set<Int>,
    langOrder: List<String>
): List<LanguageDef> {
    val candidates = languagesForScript(scriptCode)
    val charsPassing = candidates.filter { lang ->
        lang.requiredChars.all { cp -> supportedChars.contains(cp) }
    }
    // Re-order by langOrder, then append any not in langOrder
    val ordered = langOrder
        .mapNotNull { iso -> charsPassing.find { it.isoCode == iso } } +
        charsPassing.filter { it.isoCode !in langOrder }
    return if (ordered.size <= 1) emptyList() else ordered
}

/** Default sample texts keyed by ISO code. */
fun defaultLanguageSamples(): Map<String, String> = mapOf(
    "ansi"    to "ANSI Legacy Font — àáâãäåæçèéêëìíîï",
    "en"      to "The quick brown fox jumps over the lazy dog",
    "es"      to "El veloz murciélago hindú comía feliz cardillo y kiwi",
    "fr"      to "Portez ce vieux whisky au juge blond qui fume",
    "de"      to "Victor jagt zwölf Boxkämpfer quer über den großen Sylter Deich",
    "it"      to "Ma la volpe col suo balzo ha raggiunto il quieto Fido",
    "pt"      to "À noite, vovó Kowalsky vê o ímã cair no pé do pingüim",
    "nl"      to "Pa's wijze lynx bezag vroom het fikse aquaduct",
    "pl"      to "Pchnąć w tę łódź jeża lub ośm skrzyń fig",
    "cs"      to "Příliš žluťoučký kůň úpěl ďábelské ódy",
    "ro"      to "Muzicologă în bej vând whisky și tequila, extravagând",
    "hu"      to "Jött Áron győző túl a fák közé",
    "tr"      to "Pijamalı hasta yağız şoföre çabucak güvendi",
    "sv"      to "Flygande bäckasiner söka hwila på mjuka tuvor",
    "no"      to "Vår sære Zulu fra badeøya spilte jo qvart fux",
    "da"      to "Quizdeltagerne spiste jordbær med fløde",
    "fi"      to "Törkylempijävongahdus — wikin ääret",
    "vi"      to "Cánh đồng xanh mướt trải dài đến tận chân trời",
    "id"      to "Saya makan nasi goreng setiap pagi dengan lauk tempe",
    "ms"      to "Muzik jazz dan blues menjadi kegemaran ramai",
    "sw"      to "Mvulana mmoja alicheza mpira barabarani",
    "fil"     to "Ang mabilis na kayumanggi na lobo ay tumatalon",
    "ru"      to "Съешь же ещё этих мягких французских булок",
    "uk"      to "Жебракують філософи при ґанку церкви в Гадячі",
    "bg"      to "Ах, чудна българска земьо, полюшвай се нежно",
    "sr"      to "Фијуче ветар у шибљу, леди пасаже и кров",
    "mk"      to "Ѕвездана ноќ, тивка и мирна, покрива ги полињата",
    "kk"      to "Жылқы мінез — жер мінезі, ер мінезі",
    "ar"      to "الحروف العربية جميلة ومتنوعة في أشكالها",
    "fa"      to "پرواز را به خاطر بسپار، پرنده مردنی است",
    "ur"      to "اردو زبان دلوں کو چھو لینے والی ہے",
    "hi"      to "सभी मनुष्यों को गौरव और अधिकारों के मामले में स्वतन्त्रता",
    "mr"      to "महाराष्ट्र हे एक सुंदर राज्य आहे",
    "ne"      to "नेपाल एक सुन्दर देश हो",
    "zh-Hans" to "人人生而自由，在尊严和权利上一律平等",
    "zh-Hant" to "人人生而自由，在尊嚴和權利上一律平等",
    "ja"      to "人人生而自由，在尊嚴和權利上一律平等",
    "ko"      to "모든 사람은 태어날 때부터 자유로우며",
    "ja-kana" to "いろはにほへとちりぬるをわかよたれそ",
    "ko-hang" to "나랏말싸미 듕귁에 달아 문자와로 서로 사맛디",
    "ta"      to "அனைத்து மனிதர்களும் சுதந்திரமாகவே பிறக்கின்றனர்",
    "te"      to "అన్ని మానవులు స్వేచ్ఛగా జన్మిస్తారు",
    "kn"      to "ಎಲ್ಲಾ ಮಾನವರೂ ಸ್ವತಂತ್ರರಾಗಿ ಜನಿಸಿದ್ದಾರೆ",
    "ml"      to "എല്ലാ മനുഷ്യരും സ്വതന്ത്രരായി ജനിക്കുന്നു",
    "gu"      to "બધા મનુષ્યો સ્વતંત્ર અને સમાન ગૌરવ સાથે જન્મ્યા છે",
    "pa"      to "ਸਾਰੇ ਮਨੁੱਖ ਆਜ਼ਾਦ ਅਤੇ ਬਰਾਬਰ ਪੈਦਾ ਹੁੰਦੇ ਹਨ",
    "or"      to "ସମସ୍ତ ମଣିଷ ସ୍ୱାଧୀନ ଭାବରେ ଜନ୍ମ ଲାଭ କରନ୍ତି",
    "si"      to "සියලු මනුෂ්‍යයෝ නිදහස් ව උපත ලබති",
    "th"      to "มนุษย์ทุกคนเกิดมามีอิสระและเท่าเทียมกัน",
    "km"      to "មនុស្សទាំងអស់កើតមកមានសេរីភាព",
    "my"      to "လူသားအားလုံးသည် လွတ်လပ်စွာမွေးဖွားလာကြသည်",
    "ka"      to "ყველა ადამიანი იბადება თავისუფალი",
    "hy"      to "Բոլոր մարդիկ ծնվում են ազատ",
    "am"      to "ሁሉም ሰዎች ነፃ ሆነው ይወለዳሉ",
    "el"      to "Όλοι οι άνθρωποι γεννιούνται ελεύθεροι",
    "he"      to "כָּל בְּנֵי הָאָדָם נוֹלְדוּ בְּנֵי חוֹרִין",
    "bo"      to "མི་རིགས་ཀྱི་གོ་མཐོ་དང་ཐོབ་ཐང་གི་ཐད་ནས་རང་དབང་ཡོད།",
    "lo"      to "ມະນຸດທຸກຄົນເກີດມາມີສິດທິເທົ່າທຽມກັນ",
    "mn"      to "Хүн бүр төрөлхийн эрх чөлөөтэй",
    "bn"      to "আমার সোনার বাংলা আমি তোমায় ভালোবাসি",
    "as"      to "সকলো মানুহ স্বাধীনভাৱে জন্মগ্ৰহণ কৰে"
)
