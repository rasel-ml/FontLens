package com.fontlens.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object FontRepository {

    private val fonts = mutableListOf<FontItem>()
    private val favorites = mutableSetOf<String>()
    var settings = AppSettings()

    // Keep overrides in memory so they survive font reloads
    private val overridesCache = mutableMapOf<String, Map<String, String>>()

    private val gson = Gson()
    private const val PREFS = "fontlens_prefs"
    private const val KEY_SETTINGS = "settings"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_META_OVERRIDES = "meta_overrides"

    fun getAll(): List<FontItem> = fonts.toList()

    fun getFavorites(): List<FontItem> = fonts.filter { favorites.contains(it.id) }

    fun getById(id: String): FontItem? = fonts.find { it.id == id }

    fun addFonts(items: List<FontItem>) {
        val existingIds = fonts.map { it.id }.toSet()
        val newFonts = items.filter { it.id !in existingIds }
        // Reapply any saved overrides for these fonts immediately
        newFonts.forEach { font ->
            overridesCache[font.id]?.let { font.metaOverrides = it }
        }
        fonts.addAll(newFonts)
    }

    fun isFavorite(id: String) = favorites.contains(id)

    fun toggleFavorite(id: String, context: Context) {
        if (favorites.contains(id)) favorites.remove(id) else favorites.add(id)
        save(context)
    }

    fun saveMetaOverrides(fontId: String, overrides: Map<String, String>, context: Context) {
        // Update in-memory font
        fonts.find { it.id == fontId }?.let { it.metaOverrides = overrides }
        // Update in-memory cache
        overridesCache[fontId] = overrides
        // Persist to SharedPreferences
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val allOverrides = prefs.getString(KEY_META_OVERRIDES, "{}") ?: "{}"
        val type = object : TypeToken<MutableMap<String, Map<String, String>>>() {}.type
        val map: MutableMap<String, Map<String, String>> =
            gson.fromJson(allOverrides, type) ?: mutableMapOf()
        map[fontId] = overrides
        prefs.edit().putString(KEY_META_OVERRIDES, gson.toJson(map)).apply()
    }

    fun getMetaOverrides(fontId: String): Map<String, String> =
        overridesCache[fontId] ?: emptyMap()

    fun saveSettings(context: Context) = save(context)

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // Settings
        prefs.getString(KEY_SETTINGS, null)?.let {
            try { settings = gson.fromJson(it, AppSettings::class.java) } catch (_: Exception) {}
        }

        // Favorites
        prefs.getString(KEY_FAVORITES, "[]")?.let {
            try {
                val type = object : TypeToken<Set<String>>() {}.type
                val loaded: Set<String> = gson.fromJson(it, type) ?: emptySet()
                favorites.clear()
                favorites.addAll(loaded)
            } catch (_: Exception) {}
        }

        // Load overrides into cache — applied to fonts in addFonts() when they load
        prefs.getString(KEY_META_OVERRIDES, "{}")?.let {
            try {
                val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
                val map: Map<String, Map<String, String>> = gson.fromJson(it, type) ?: emptyMap()
                overridesCache.clear()
                overridesCache.putAll(map)
                // Also reapply to any already-loaded fonts (e.g. after settings change)
                fonts.forEach { f -> overridesCache[f.id]?.let { ov -> f.metaOverrides = ov } }
            } catch (_: Exception) {}
        }
    }

    private fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SETTINGS, gson.toJson(settings))
            .putString(KEY_FAVORITES, gson.toJson(favorites))
            .apply()
    }

    fun getSampleText(font: FontItem): String {
        val s = settings
        val userText = s.langSamples[s.defaultLang] ?: ""
        val metaText = font.effectiveMeta.sampleText
        val default = "The quick brown fox jumps over the lazy dog 0123456789"
        return when (s.samplePriority) {
            SamplePriority.ALWAYS_USER    -> userText.ifEmpty { default }
            SamplePriority.ALWAYS_META    -> metaText.ifEmpty { default }
            SamplePriority.METADATA_FIRST -> metaText.ifEmpty { userText.ifEmpty { default } }
            SamplePriority.USER_FIRST     -> userText.ifEmpty { metaText.ifEmpty { default } }
        }
    }
}
