package com.fontlens.utils

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import com.fontlens.data.FontItem
import com.fontlens.data.FontRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FontLoader {

    private val typefaceCache = mutableMapOf<String, Typeface>()

    suspend fun loadFontsFromUris(context: Context, uris: List<Uri>): List<FontItem> =
        withContext(Dispatchers.IO) {
            val result = mutableListOf<FontItem>()
            for (uri in uris) {
                try {
                    val cr = context.contentResolver
                    val name = getFileName(context, uri)
                    val id = "${name}_${uri.hashCode()}"

                    // Parse metadata
                    val meta = cr.openInputStream(uri)?.use { FontParser.parse(it) }
                        ?: continue

                    val displayName = meta.family.ifEmpty {
                        name.substringBeforeLast(".")
                    }

                    val item = FontItem(id = id, displayName = displayName, uri = uri, meta = meta)
                    result.add(item)

                    // Build typeface and cache
                    cr.openFileDescriptor(uri, "r")?.use { pfd ->
                        val tf = Typeface.Builder(pfd.fileDescriptor).build()
                        typefaceCache[id] = tf
                    }
                } catch (_: Exception) {}
            }
            result
        }

    fun getTypeface(fontId: String): Typeface? = typefaceCache[fontId]

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "Unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
        }
        return name
    }
}
