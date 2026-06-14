package com.fontlens.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Persists FontItem metadata to a JSON file so the list loads instantly on reopen.
 * Typeface objects are NOT cached here — they are built lazily in TypefaceLoader.
 */
object FontCache {

    private const val FILE_NAME = "font_cache.json"
    private val gson = Gson()

    // Serializable version of FontItem (Uri stored as string)
    data class CachedFont(
        val id: String,
        val displayName: String,
        val uriString: String,
        val addedAt: Long,
        val folderPath: String,
        val meta: FontMeta
    )

    fun save(context: Context, fonts: List<FontItem>) {
        try {
            val cached = fonts.map { f ->
                CachedFont(
                    id = f.id,
                    displayName = f.displayName,
                    uriString = f.uri.toString(),
                    addedAt = f.addedAt,
                    folderPath = f.folderPath,
                    meta = f.meta
                )
            }
            val json = gson.toJson(cached)
            context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
                it.write(json.toByteArray())
            }
        } catch (_: Exception) {}
    }

    fun load(context: Context): List<FontItem> {
        return try {
            val json = context.openFileInput(FILE_NAME).use {
                it.readBytes().toString(Charsets.UTF_8)
            }
            val type = object : TypeToken<List<CachedFont>>() {}.type
            val cached: List<CachedFont> = gson.fromJson(json, type) ?: return emptyList()
            cached.map { c ->
                FontItem(
                    id = c.id,
                    displayName = c.displayName,
                    uri = android.net.Uri.parse(c.uriString),
                    addedAt = c.addedAt,
                    folderPath = c.folderPath,
                    meta = c.meta
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    fun clear(context: Context) {
        try { context.deleteFile(FILE_NAME) } catch (_: Exception) {}
    }
}
