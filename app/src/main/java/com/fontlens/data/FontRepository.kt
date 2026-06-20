package com.fontlens.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object FontRepository {

    private val fonts     = mutableListOf<FontItem>()
    private val tempFonts = mutableMapOf<String, FontItem>()
    private val favorites = mutableSetOf<String>()
    var settings = AppSettings()

    private val overridesCache   = mutableMapOf<String, Map<String, String>>()
    private val savedFolderUris  = mutableSetOf<String>()
    private val loadedFolderUris = mutableSetOf<String>()

    private val gson = Gson()
    private const val PREFS         = "fontlens_prefs"
    private const val KEY_SETTINGS  = "settings"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_OVERRIDES = "meta_overrides"
    private const val KEY_FOLDERS   = "folder_uris"

    // ── Library ───────────────────────────────────────────────────────────────
    fun getAll(): List<FontItem> = fonts.toList()
    fun getFavorites(): List<FontItem> = fonts.filter { favorites.contains(it.id) }
    fun getById(id: String): FontItem? = fonts.find { it.id == id } ?: tempFonts[id]

    fun addFonts(items: List<FontItem>) {
        // Deduplicate by ID AND by URI string to prevent re-adding same file
        // from a parent folder scan (fixes recursive duplicate bug)
        val existingIds  = fonts.map { it.id }.toSet()
        val existingUris = fonts.map { it.uri.toString() }.toSet()
        val newFonts = items.filter { it.id !in existingIds && it.uri.toString() !in existingUris }
        newFonts.forEach { font -> overridesCache[font.id]?.let { font.metaOverrides = it } }
        fonts.addAll(newFonts)
    }

    fun addFontsAndSave(items: List<FontItem>, context: Context) {
        addFonts(items)
        FontCache.save(context, fonts)
    }

    fun removeFont(id: String, context: Context) {
        fonts.removeAll { it.id == id }
        favorites.remove(id)
        overridesCache.remove(id)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val allOverrides = prefs.getString(KEY_OVERRIDES, "{}") ?: "{}"
        val type = object : TypeToken<MutableMap<String, Map<String, String>>>() {}.type
        val map: MutableMap<String, Map<String, String>> =
            gson.fromJson(allOverrides, type) ?: mutableMapOf()
        map.remove(id)
        prefs.edit().putString(KEY_OVERRIDES, gson.toJson(map)).apply()
        save(context)
        FontCache.save(context, fonts)
    }

    fun removeFontFromStorage(id: String, context: Context): Boolean {
        val font = fonts.find { it.id == id } ?: return false
        return try {
            context.contentResolver.delete(font.uri, null, null)
            removeFont(id, context); true
        } catch (_: Exception) {
            removeFont(id, context); false
        }
    }

    /** Remove all fonts belonging to a specific folder path — used when removing a folder */
    fun removeFontsByFolder(folderPath: String, context: Context) {
        val toRemove = fonts.filter { it.folderPath == folderPath }.map { it.id }
        toRemove.forEach { id ->
            fonts.removeAll { it.id == id }
            favorites.remove(id)
            overridesCache.remove(id)
        }
        if (toRemove.isNotEmpty()) {
            // Clean up overrides for removed fonts
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val allOverrides = prefs.getString(KEY_OVERRIDES, "{}") ?: "{}"
            val type = object : TypeToken<MutableMap<String, Map<String, String>>>() {}.type
            val map: MutableMap<String, Map<String, String>> =
                gson.fromJson(allOverrides, type) ?: mutableMapOf()
            toRemove.forEach { map.remove(it) }
            prefs.edit().putString(KEY_OVERRIDES, gson.toJson(map)).apply()
            save(context)
            FontCache.save(context, fonts)
        }
    }

    fun isInLibrary(id: String) = fonts.any { it.id == id }

    // ── Temp fonts ────────────────────────────────────────────────────────────
    fun addTempFont(font: FontItem) { tempFonts[font.id] = font }

    fun promoteToLibrary(id: String, context: Context): Boolean {
        val font = tempFonts.remove(id) ?: return false
        if (fonts.none { it.id == id }) fonts.add(font)
        FontCache.save(context, fonts)
        save(context)
        return true
    }

    // ── Favorites ─────────────────────────────────────────────────────────────
    fun isFavorite(id: String) = favorites.contains(id)

    fun toggleFavorite(id: String, context: Context) {
        if (favorites.contains(id)) favorites.remove(id) else favorites.add(id)
        save(context)
    }

    // ── Folders ───────────────────────────────────────────────────────────────
    fun saveFolderUri(uri: Uri, context: Context) {
        savedFolderUris.add(uri.toString()); save(context)
    }

    fun removeSavedFolder(uri: Uri, context: Context) {
        savedFolderUris.remove(uri.toString())
        loadedFolderUris.remove(uri.toString())
        // Also immediately remove all fonts from this folder
        val folderPath = "/" + (uri.lastPathSegment ?: "").substringAfter(":")
        removeFontsByFolder(folderPath, context)
        save(context)
    }

    fun getSavedFolderUris(): List<Uri> = savedFolderUris.map { Uri.parse(it) }
    fun isFolderLoaded(uri: Uri)     = loadedFolderUris.contains(uri.toString())
    fun markFolderLoaded(uri: Uri)   { loadedFolderUris.add(uri.toString()) }
    fun unmarkFolderLoaded(uri: Uri) { loadedFolderUris.remove(uri.toString()) }

    // ── Meta overrides ────────────────────────────────────────────────────────
    fun saveMetaOverrides(fontId: String, overrides: Map<String, String>, context: Context) {
        fonts.find { it.id == fontId }?.let { it.metaOverrides = overrides }
        overridesCache[fontId] = overrides
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val allOverrides = prefs.getString(KEY_OVERRIDES, "{}") ?: "{}"
        val type = object : TypeToken<MutableMap<String, Map<String, String>>>() {}.type
        val map: MutableMap<String, Map<String, String>> =
            gson.fromJson(allOverrides, type) ?: mutableMapOf()
        map[fontId] = overrides
        prefs.edit().putString(KEY_OVERRIDES, gson.toJson(map)).apply()
        FontCache.save(context, fonts)
    }

    fun getMetaOverrides(fontId: String): Map<String, String> =
        overridesCache[fontId] ?: emptyMap()
    fun updateFontMeta(fontId: String, newMeta: FontMeta, context: Context) {
        val idx = fonts.indexOfFirst { it.id == fontId }
        if (idx >= 0) {
            val old = fonts[idx]
            fonts[idx] = old.copy(
                meta = newMeta,
                metaOverrides = emptyMap(),
                displayName = newMeta.family.ifEmpty { old.displayName }
            )
            FontCache.save(context, fonts)
        }
    
        tempFonts[fontId]?.let { old ->
            tempFonts[fontId] = old.copy(
                meta = newMeta,
                metaOverrides = emptyMap(),
                displayName = newMeta.family.ifEmpty { old.displayName }
            )
        }
    }
    // ── Settings ──────────────────────────────────────────────────────────────
    fun saveSettings(context: Context) = save(context)

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_SETTINGS, null)?.let {
            try { settings = gson.fromJson(it, AppSettings::class.java) } catch (_: Exception) {}
        }
        prefs.getString(KEY_FAVORITES, "[]")?.let {
            try {
                val type = object : TypeToken<Set<String>>() {}.type
                val loaded: Set<String> = gson.fromJson(it, type) ?: emptySet()
                favorites.clear(); favorites.addAll(loaded)
            } catch (_: Exception) {}
        }
        prefs.getString(KEY_FOLDERS, "[]")?.let {
            try {
                val type = object : TypeToken<Set<String>>() {}.type
                val loaded: Set<String> = gson.fromJson(it, type) ?: emptySet()
                savedFolderUris.clear(); savedFolderUris.addAll(loaded)
            } catch (_: Exception) {}
        }
        // Load cached fonts instantly
        if (fonts.isEmpty()) {
            val cached = FontCache.load(context)
            if (cached.isNotEmpty()) {
                cached.forEach { font -> overridesCache[font.id]?.let { font.metaOverrides = it } }
                fonts.addAll(cached)
            }
        }
    }

    private fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_SETTINGS,  gson.toJson(settings))
            .putString(KEY_FAVORITES, gson.toJson(favorites))
            .putString(KEY_FOLDERS,   gson.toJson(savedFolderUris))
            .apply()
    }

    /**
     * Returns the best sample text for a font card / preview.
     * Priority:
     *   1. Font's built-in metadata sample text (if preferMetaSample ON)
     *   2. First script in scriptOrder that has a user sample and the font supports
     *   3. First script in scriptOrder that has a user sample (regardless of font support)
     *   4. Fallback
     */
    fun getSampleText(font: FontItem): String {
        val s = settings
        val metaText = font.effectiveMeta.sampleText
        if (s.preferMetaSample && metaText.isNotEmpty()) return metaText
        val fontCodes = font.effectiveMeta.scriptCodes.toSet()
        // Try highest-priority supported script first
        val fromSupported = s.scriptOrder.firstNotNullOfOrNull { code ->
            if (fontCodes.contains(code)) s.langSamples[code]?.ifEmpty { null } else null
        }
        if (fromSupported != null) return fromSupported
        // Fall back to any sample in order
        val fromOrder = s.scriptOrder.firstNotNullOfOrNull { code ->
            s.langSamples[code]?.ifEmpty { null }
        }
        return fromOrder ?: "The quick brown fox jumps over the lazy dog"
    }

    /** Returns sample text for a specific script code, or null if none exists. */
    fun getSampleForLang(langCode: String): String? =
        settings.langSamples[langCode]?.ifEmpty { null }

    /**
     * Returns script codes to display on a font card, ordered by user's scriptOrder,
     * limited to scripts the font actually supports.
     */
    fun orderedScriptCodesForFont(font: FontItem): List<String> {
        val s = settings
        val fontCodes = font.effectiveMeta.scriptCodes.toSet()
        return s.scriptOrder.filter { fontCodes.contains(it) }
    }
}
