package com.fontforge.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object FontRepository {

    private val fonts = mutableListOf<FontItem>()
    private val favorites = mutableSetOf<String>()
    var settings = AppSettings()

    private val gson = Gson()
    private const val PREFS = "fontforge_prefs"
    private const val KEY_SETTINGS = "settings"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_META_OVERRIDES = "meta_overrides"

    fun getAll(): List<FontItem> = fonts.toList()

    fun getFavorites(): List<FontItem> = fonts.filter { favorites.contains(it.id) }

    fun getById(id: String): FontItem? = fonts.find { it.id == id }

    fun addFonts(items: List<FontItem>) {
        val existingIds = fonts.map { it.id }.toSet()
        fonts.addAll(items.filter { it.id !in existingIds })
    }

    fun isFavorite(id: String) = favorites.contains(id)

    fun toggleFavorite(id: String, context: Context) {
        if (favorites.contains(id)) favorites.remove(id) else favorites.add(id)
        save(context)
    }

    fun saveMetaOverrides(fontId: String, overrides: Map<String, String>, context: Context) {
        fonts.find { it.id == fontId }?.let { it.metaOverrides = overrides }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val allOverrides = prefs.getString(KEY_META_OVERRIDES, "{}") ?: "{}"
        val type = object : TypeToken<MutableMap<String, Map<String, String>>>() {}.type
        val map: MutableMap<String, Map<String, String>> = gson.fromJson(allOverrides, type) ?: mutableMapOf()
        map[fontId] = overrides
        prefs.edit().putString(KEY_META_OVERRIDES, gson.toJson(map)).apply()
    }

    fun saveSettings(context: Context) = save(context)

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Settings
        val settingsJson = prefs.getString(KEY_SETTINGS, null)
        if (settingsJson != null) {
            try { settings = gson.fromJson(settingsJson, AppSettings::class.java) } catch (_: Exception) {}
        }
        // Favorites
        val favsJson = prefs.getString(KEY_FAVORITES, "[]") ?: "[]"
        try {
            val type = object : TypeToken<Set<String>>() {}.type
            val loaded: Set<String> = gson.fromJson(favsJson, type) ?: emptySet()
            favorites.clear(); favorites.addAll(loaded)
        } catch (_: Exception) {}
        // Meta overrides — reapply to already-loaded fonts if any
        val allOverrides = prefs.getString(KEY_META_OVERRIDES, "{}") ?: "{}"
        try {
            val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
            val map: Map<String, Map<String, String>> = gson.fromJson(allOverrides, type) ?: emptyMap()
            fonts.forEach { f -> map[f.id]?.let { f.metaOverrides = it } }
        } catch (_: Exception) {}
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
            SamplePriority.ALWAYS_USER     -> userText.ifEmpty { default }
            SamplePriority.ALWAYS_META     -> metaText.ifEmpty { default }
            SamplePriority.METADATA_FIRST  -> metaText.ifEmpty { userText.ifEmpty { default } }
            SamplePriority.USER_FIRST      -> userText.ifEmpty { metaText.ifEmpty { default } }
        }
    }
}
