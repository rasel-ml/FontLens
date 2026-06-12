package com.fontlens.utils

import android.content.Context
import android.net.Uri
import com.fontlens.data.FontItem
import com.fontlens.data.FontMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Parses font metadata only. Typeface loading is handled by TypefaceLoader separately.
 */
object FontLoader {

    suspend fun loadFontsFromUris(
        context: Context,
        uris: List<Uri>,
        folderPath: String = "",
        onProgress: ((loaded: Int, total: Int) -> Unit)? = null
    ): List<FontItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<FontItem>()
        val total = uris.size
        uris.forEachIndexed { index, uri ->
            try {
                val cr = context.contentResolver
                val name = getFileName(context, uri)
                val id = "${name}_${uri.hashCode()}"
                val meta = cr.openInputStream(uri)?.use { FontParser.parse(it) } ?: return@forEachIndexed
                val displayName = meta.family.ifEmpty { name.substringBeforeLast(".") }
                result.add(FontItem(
                    id = id, displayName = displayName, uri = uri, meta = meta,
                    addedAt = System.currentTimeMillis(), folderPath = folderPath
                ))
            } catch (_: Exception) {}
            onProgress?.invoke(index + 1, total)
        }
        result
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "Unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
        }
        return name
    }
}
