package com.fontlens.utils

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads Typeface objects lazily, one by one sequentially.
 * Notifies via callback each time one completes so the UI can update that card.
 */
object TypefaceLoader {

    private val cache = mutableMapOf<String, Typeface>()
    private val loading = mutableSetOf<String>() // prevent duplicate loads

    fun getTypeface(fontId: String): Typeface? = cache[fontId]

    fun isLoaded(fontId: String) = cache.containsKey(fontId)

    /**
     * Load typefaces one by one sequentially.
     * onEach is called on the MAIN thread after each font loads.
     */
    suspend fun loadSequentially(
        context: Context,
        fonts: List<Pair<String, Uri>>, // id -> uri
        onEach: (fontId: String) -> Unit
    ) {
        for ((id, uri) in fonts) {
            if (cache.containsKey(id) || loading.contains(id)) continue
            loading.add(id)
            val tf = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        Typeface.Builder(pfd.fileDescriptor).build()
                    }
                } catch (_: Exception) { null }
            }
            loading.remove(id)
            if (tf != null) {
                cache[id] = tf
                withContext(Dispatchers.Main) { onEach(id) }
            }
        }
    }

    /**
     * Load a single typeface immediately (for preview screen).
     * Returns cached if already loaded, otherwise builds it now.
     */
    suspend fun loadSingle(context: Context, fontId: String, uri: Uri): Typeface? {
        cache[fontId]?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    Typeface.Builder(pfd.fileDescriptor).build().also { cache[fontId] = it }
                }
            } catch (_: Exception) { null }
        }
    }

    fun clear() { cache.clear(); loading.clear() }
}
