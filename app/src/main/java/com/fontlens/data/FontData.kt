package com.fontlens.data

import android.net.Uri

data class FontMeta(
    val family: String = "",
    val subfamily: String = "",
    val fullName: String = "",
    val version: String = "",
    val postscript: String = "",
    val manufacturer: String = "",
    val designer: String = "",
    val description: String = "",
    val trademark: String = "",
    val license: String = "",
    val licenseURL: String = "",
    val vendorURL: String = "",
    val designerURL: String = "",
    val sampleText: String = "",
    val numGlyphs: Int = 0,
    val weight: Int = 400,
    val weightName: String = "Regular",
    val unitsPerEm: Int = 1000,
    val italicAngle: Float = 0f,
    val isFixedPitch: Boolean = false,
    val boldSupport: Boolean = false,
    val italicSupport: Boolean = false,
    val condensedSupport: Boolean = false,
    val extendedSupport: Boolean = false,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isRegular: Boolean = true,
    val tables: List<String> = emptyList(),
    val supportedChars: List<Int> = emptyList()
)

data class FontItem(
    val id: String,
    val displayName: String,
    val uri: Uri,
    val meta: FontMeta,
    var metaOverrides: Map<String, String> = emptyMap(),
    val addedAt: Long = System.currentTimeMillis(),
    val folderPath: String = ""
) {
    val effectiveMeta: FontMeta get() = meta.copy(
        family       = metaOverrides["family"]       ?: meta.family,
        subfamily    = metaOverrides["subfamily"]    ?: meta.subfamily,
        fullName     = metaOverrides["fullName"]     ?: meta.fullName,
        version      = metaOverrides["version"]      ?: meta.version,
        postscript   = metaOverrides["postscript"]   ?: meta.postscript,
        manufacturer = metaOverrides["manufacturer"] ?: meta.manufacturer,
        designer     = metaOverrides["designer"]     ?: meta.designer,
        description  = metaOverrides["description"]  ?: meta.description,
        trademark    = metaOverrides["trademark"]    ?: meta.trademark,
        license      = metaOverrides["license"]      ?: meta.license,
        licenseURL   = metaOverrides["licenseURL"]   ?: meta.licenseURL,
        vendorURL    = metaOverrides["vendorURL"]    ?: meta.vendorURL,
        designerURL  = metaOverrides["designerURL"]  ?: meta.designerURL,
        sampleText   = metaOverrides["sampleText"]   ?: meta.sampleText
    )
}

data class AppSettings(
    val langSamples: Map<String, String> = mapOf(
        "English" to "The quick brown fox jumps over the lazy dog 0123456789",
        "Bengali" to "আমার সোনার বাংলা আমি তোমায় ভালোবাসি"
    ),
    val samplePriority: SamplePriority = SamplePriority.METADATA_FIRST,
    val glyphShowAll: Boolean = false,
    val defaultLang: String = "English",
    val folderRecursive: Boolean = true,
    val colorTheme: ColorTheme = ColorTheme.GREEN,
    val darkMode: Boolean = false,
    val followSystem: Boolean = false,
    // New fields
    val themeMode: ThemeMode = ThemeMode.DAY,   // replaces followSystem+darkMode
    val preferMetaSample: Boolean = true,        // replaces 4-option SamplePriority
    val showFullFontName: Boolean = false,        // new display option
    // Legacy (kept for Gson backward compat)
    val theme: AppTheme = AppTheme.DAY
)

enum class SamplePriority { METADATA_FIRST, USER_FIRST, ALWAYS_USER, ALWAYS_META }
enum class AppTheme { SYSTEM, DAY, NIGHT }
enum class ColorTheme { RED, YELLOW, GREEN, BLUE }
enum class ThemeMode { SYSTEM, DAY, NIGHT }
enum class SortOrder { NAME_ASC, NAME_DESC, DATE_ASC, DATE_DESC, FOLDER }

sealed class FontListItem {
    data class Font(val font: FontItem) : FontListItem()
    data class FolderHeader(val path: String) : FontListItem()
}
