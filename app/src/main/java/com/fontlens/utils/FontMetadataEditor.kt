package com.fontlens.utils

import android.content.Context
import android.net.Uri
import com.fontlens.data.FontMeta
import org.apache.fontbox.ttf.TTFParser
import java.io.ByteArrayInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object FontMetadataEditor {

    private val fieldToNameId = mapOf(
        "family" to 1,
        "subfamily" to 2,
        "fullName" to 4,
        "version" to 5,
        "postscript" to 6,
        "trademark" to 7,
        "manufacturer" to 8,
        "designer" to 9,
        "description" to 10,
        "vendorURL" to 11,
        "designerURL" to 12,
        "license" to 13,
        "licenseURL" to 14,
        "sampleText" to 19
    )

    private const val WINDOWS_PLATFORM = 3
    private const val WINDOWS_ENCODING = 1
    private const val WINDOWS_LANGUAGE = 0x0409

    private const val MAC_PLATFORM = 1
    private const val MAC_ENCODING = 0
    private const val MAC_LANGUAGE = 0x0000

    private const val FONT_CHECKSUM_MAGIC = 0xB1B0AFBAL
    private const val SFNT_HEADER_SIZE = 12
    private const val TABLE_DIR_ENTRY_SIZE = 16

    private data class TableEntry(
        val tag: String,
        val checksum: Int,
        val offset: Int,
        val length: Int
    )

    private data class NameRecordEntry(
        val platformId: Int,
        val encodingId: Int,
        val languageId: Int,
        val nameId: Int,
        val value: String
    )

    suspend fun writeMetadata(
        context: Context,
        uri: Uri,
        updates: Map<String, String>
    ): FontMeta? {
        if (updates.isEmpty()) return null

        // Only true sfnt fonts are handled here.
        val lower = (uri.lastPathSegment ?: "").lowercase()
        if (!(lower.endsWith(".ttf") || lower.endsWith(".otf"))) return null

        val original = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null

        val rewritten = try {
            rewriteSfntFont(original, updates)
        } catch (_: Exception) {
            return null
        }

        val written = writeBytesBack(context, uri, rewritten)
        if (!written) return null

        return FontParser.parse(ByteArrayInputStream(rewritten))
    }

    private fun writeBytesBack(context: Context, uri: Uri, bytes: ByteArray): Boolean {
        return try {
            context.contentResolver.openFileDescriptor(uri, "rwt")?.use { pfd ->
                FileOutputStream(pfd.fileDescriptor).channel.use { channel ->
                    channel.truncate(0)
                    channel.position(0)
                    channel.write(ByteBuffer.wrap(bytes))
                    channel.force(true)
                }
            } != null
        } catch (_: Exception) {
            try {
                context.contentResolver.openOutputStream(uri, "rwt")?.use { out ->
                    out.write(bytes)
                    out.flush()
                } != null
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun rewriteSfntFont(raw: ByteArray, updates: Map<String, String>): ByteArray {
        val tables = parseTableDirectory(raw)
        val nameTable = tables["name"] ?: error("Missing name table")
        val headTable = tables["head"] ?: error("Missing head table")

        val originalNameRecords = parseNameTable(raw, nameTable)
        val rebuiltNameTable = buildNameTable(originalNameRecords, updates)

        val tableBytes = mutableMapOf<String, ByteArray>()

        // Replace name table.
        tableBytes["name"] = rebuiltNameTable

        // Copy all other tables unchanged.
        for ((tag, entry) in tables) {
            if (tag == "name") continue
            val slice = raw.copyOfRange(entry.offset, entry.offset + entry.length)
            tableBytes[tag] = slice
        }

        // Set head.checkSumAdjustment = 0 first.
        val headZero = tableBytes.getValue("head").copyOf()
        putUInt32(headZero, 8, 0L)
        tableBytes["head"] = headZero

        // First pass: build with head adjustment = 0, compute checksum adjustment.
        val firstPass = assembleSfnt(raw.copyOfRange(0, SFNT_HEADER_SIZE), tables.values.toList(), tableBytes)
        val sum = sfntChecksum(firstPass)
        val adjustment = (FONT_CHECKSUM_MAGIC - sum) and 0xFFFFFFFFL

        // Final head table with correct adjustment.
        val headFinal = headZero.copyOf()
        putUInt32(headFinal, 8, adjustment)
        tableBytes["head"] = headFinal

        return assembleSfnt(raw.copyOfRange(0, SFNT_HEADER_SIZE), tables.values.toList(), tableBytes)
    }

    private fun parseTableDirectory(raw: ByteArray): LinkedHashMap<String, TableEntry> {
        val buf = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN)
        val numTables = u16(buf, 4)
        val out = LinkedHashMap<String, TableEntry>(numTables)

        for (i in 0 until numTables) {
            val base = SFNT_HEADER_SIZE + i * TABLE_DIR_ENTRY_SIZE
            val tag = String(byteArrayOf(raw[base], raw[base + 1], raw[base + 2], raw[base + 3]), Charsets.US_ASCII)
            val checksum = buf.getInt(base + 4)
            val offset = buf.getInt(base + 8)
            val length = buf.getInt(base + 12)
            out[tag] = TableEntry(tag, checksum, offset, length)
        }
        return out
    }

    private fun parseNameTable(raw: ByteArray, entry: TableEntry): List<NameRecordEntry> {
        val buf = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN)
        val count = u16(buf, entry.offset + 2)
        val stringOffset = u16(buf, entry.offset + 4)

        val records = ArrayList<NameRecordEntry>(count)
        for (i in 0 until count) {
            val base = entry.offset + 6 + i * 12
            val platformId = u16(buf, base)
            val encodingId = u16(buf, base + 2)
            val languageId = u16(buf, base + 4)
            val nameId = u16(buf, base + 6)
            val length = u16(buf, base + 8)
            val offset = u16(buf, base + 10)

            val start = entry.offset + stringOffset + offset
            val end = minOf(start + length, raw.size)
            val bytes = if (start in 0 until raw.size && start < end) raw.copyOfRange(start, end) else ByteArray(0)

            val value = decodeNameString(bytes, platformId, encodingId)
            records += NameRecordEntry(platformId, encodingId, languageId, nameId, value)
        }
        return records
    }

    private fun buildNameTable(
        originalRecords: List<NameRecordEntry>,
        updates: Map<String, String>
    ): ByteArray {
        val updateIds = updates.keys.mapNotNull { fieldToNameId[it] }.toSet()

        val rebuiltRecords = mutableListOf<NameRecordEntry>()

        // Put replacement Windows records first so parsers pick the edited value.
        for ((field, value) in updates) {
            val nameId = fieldToNameId[field] ?: continue
            rebuiltRecords += NameRecordEntry(
                platformId = WINDOWS_PLATFORM,
                encodingId = WINDOWS_ENCODING,
                languageId = WINDOWS_LANGUAGE,
                nameId = nameId,
                value = value
            )

            // Optional Mac record, only if representable in Latin-1.
            if (canEncodeLatin1(value)) {
                rebuiltRecords += NameRecordEntry(
                    platformId = MAC_PLATFORM,
                    encodingId = MAC_ENCODING,
                    languageId = MAC_LANGUAGE,
                    nameId = nameId,
                    value = value
                )
            }
        }

        // Keep every untouched original record.
        rebuiltRecords += originalRecords.filter { it.nameId !in updateIds }

        val encodedStrings = ArrayList<ByteArray>(rebuiltRecords.size)
        var stringSize = 0
        for (r in rebuiltRecords) {
            val bytes = encodeNameString(r.value, r.platformId, r.encodingId)
            encodedStrings += bytes
            stringSize += bytes.size
        }

        val stringOffset = 6 + rebuiltRecords.size * 12
        val out = ByteBuffer.allocate(stringOffset + stringSize).order(ByteOrder.BIG_ENDIAN)

        out.putShort(0) // format = 0
        out.putShort(rebuiltRecords.size.toShort())
        out.putShort(stringOffset.toShort())

        var cursor = 0
        for ((index, record) in rebuiltRecords.withIndex()) {
            val bytes = encodedStrings[index]
            out.putShort(record.platformId.toShort())
            out.putShort(record.encodingId.toShort())
            out.putShort(record.languageId.toShort())
            out.putShort(record.nameId.toShort())
            out.putShort(bytes.size.toShort())
            out.putShort(cursor.toShort())
            cursor += bytes.size
        }

        for (bytes in encodedStrings) out.put(bytes)
        return out.array()
    }

    private fun assembleSfnt(
        header: ByteArray,
        tableOrder: List<TableEntry>,
        tableBytes: Map<String, ByteArray>
    ): ByteArray {
        val placed = mutableListOf<PlacedTable>()
        var cursor = SFNT_HEADER_SIZE + tableOrder.size * TABLE_DIR_ENTRY_SIZE

        for (entry in tableOrder) {
            val data = tableBytes[entry.tag] ?: error("Missing table bytes for ${entry.tag}")
            cursor = align4(cursor)
            placed += PlacedTable(entry.tag, data, cursor, data.size)
            cursor += align4(data.size)
        }

        val out = ByteArray(cursor)
        System.arraycopy(header, 0, out, 0, SFNT_HEADER_SIZE)

        val dir = ByteBuffer.wrap(out).order(ByteOrder.BIG_ENDIAN)
        dir.position(SFNT_HEADER_SIZE)

        for (p in placed) {
            dir.put(p.tag.toByteArray(Charsets.US_ASCII).copyOf(4))
            dir.putInt(tableChecksum(p.data).toInt())
            dir.putInt(p.offset)
            dir.putInt(p.length)
            System.arraycopy(p.data, 0, out, p.offset, p.length)
        }

        return out
    }

    private data class PlacedTable(
        val tag: String,
        val data: ByteArray,
        val offset: Int,
        val length: Int
    )

    private fun tableChecksum(data: ByteArray): Long {
        val padded = if (data.size % 4 == 0) data else data + ByteArray(4 - (data.size % 4))
        val buf = ByteBuffer.wrap(padded).order(ByteOrder.BIG_ENDIAN)
        var sum = 0L
        while (buf.remaining() >= 4) {
            sum = (sum + (buf.int.toLong() and 0xFFFFFFFFL)) and 0xFFFFFFFFL
        }
        return sum
    }

    private fun sfntChecksum(data: ByteArray): Long {
        val padded = if (data.size % 4 == 0) data else data + ByteArray(4 - (data.size % 4))
        val buf = ByteBuffer.wrap(padded).order(ByteOrder.BIG_ENDIAN)
        var sum = 0L
        while (buf.remaining() >= 4) {
            sum = (sum + (buf.int.toLong() and 0xFFFFFFFFL)) and 0xFFFFFFFFL
        }
        return sum
    }

    private fun decodeNameString(bytes: ByteArray, platformId: Int, encodingId: Int): String {
        return try {
            if (platformId == 0 || platformId == 3 || encodingId == 1 || encodingId == 10) {
                String(bytes, Charsets.UTF_16BE).trim('\u0000', ' ')
            } else {
                String(bytes, Charsets.ISO_8859_1).trim('\u0000', ' ')
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun encodeNameString(value: String, platformId: Int, encodingId: Int): ByteArray {
        return if (platformId == 0 || platformId == 3 || encodingId == 1 || encodingId == 10) {
            value.toByteArray(Charsets.UTF_16BE)
        } else {
            value.toByteArray(Charsets.ISO_8859_1)
        }
    }

    private fun canEncodeLatin1(value: String): Boolean = value.all { it.code <= 0xFF }

    private fun u16(buf: ByteBuffer, offset: Int): Int =
        buf.getShort(offset).toInt() and 0xFFFF

    private fun putUInt32(array: ByteArray, offset: Int, value: Long) {
        val v = value and 0xFFFFFFFFL
        array[offset] = ((v shr 24) and 0xFF).toByte()
        array[offset + 1] = ((v shr 16) and 0xFF).toByte()
        array[offset + 2] = ((v shr 8) and 0xFF).toByte()
        array[offset + 3] = (v and 0xFF).toByte()
    }

    private fun align4(value: Int): Int = (value + 3) and -4
}
