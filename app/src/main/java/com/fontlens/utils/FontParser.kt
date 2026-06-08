package com.fontlens.utils

import com.fontlens.data.FontMeta
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object FontParser {

    fun parse(stream: InputStream): FontMeta {
        return try {
            val bytes = stream.readBytes()
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            parseBuffer(buf, bytes)
        } catch (e: Exception) {
            FontMeta()
        }
    }

    private fun parseBuffer(buf: ByteBuffer, raw: ByteArray): FontMeta {
        buf.position(0)
        val numTables = buf.getShort(4).toInt() and 0xFFFF
        val tableMap = mutableMapOf<String, Pair<Int, Int>>() // tag -> (offset, length)
        for (i in 0 until numTables) {
            val base = 12 + i * 16
            val tag = String(byteArrayOf(raw[base], raw[base+1], raw[base+2], raw[base+3]))
            val offset = buf.getInt(base + 8)
            val length = buf.getInt(base + 12)
            tableMap[tag] = Pair(offset, length)
        }

        var family = ""; var subfamily = ""; var fullName = ""; var version = ""
        var postscript = ""; var manufacturer = ""; var designer = ""; var description = ""
        var trademark = ""; var license = ""; var licenseURL = ""; var vendorURL = ""
        var designerURL = ""; var sampleText = ""

        tableMap["name"]?.let { (nameOff, _) ->
            val count = buf.getShort(nameOff + 2).toInt() and 0xFFFF
            val strOff = nameOff + (buf.getShort(nameOff + 4).toInt() and 0xFFFF)
            val nameMap = mutableMapOf<Int, String>()
            for (i in 0 until count) {
                val base = nameOff + 6 + i * 12
                val platformID = buf.getShort(base).toInt() and 0xFFFF
                val encodingID = buf.getShort(base + 2).toInt() and 0xFFFF
                val nameID    = buf.getShort(base + 6).toInt() and 0xFFFF
                val len       = buf.getShort(base + 8).toInt() and 0xFFFF
                val off       = buf.getShort(base + 10).toInt() and 0xFFFF
                if (nameMap.containsKey(nameID)) continue
                val strBytes = raw.copyOfRange(strOff + off, strOff + off + len)
                val str = if (platformID == 3 || (platformID == 0 && encodingID != 6)) {
                    String(strBytes, Charsets.UTF_16BE)
                } else {
                    String(strBytes, Charsets.ISO_8859_1)
                }
                if (str.isNotBlank()) nameMap[nameID] = str.trim()
            }
            family      = nameMap[1]  ?: ""
            subfamily   = nameMap[2]  ?: ""
            fullName    = nameMap[4]  ?: ""
            version     = nameMap[5]  ?: ""
            postscript  = nameMap[6]  ?: ""
            trademark   = nameMap[7]  ?: ""
            manufacturer= nameMap[8]  ?: ""
            designer    = nameMap[9]  ?: ""
            description = nameMap[10] ?: ""
            vendorURL   = nameMap[11] ?: ""
            designerURL = nameMap[12] ?: ""
            license     = nameMap[13] ?: ""
            licenseURL  = nameMap[14] ?: ""
            sampleText  = nameMap[19] ?: ""
        }

        var weight = 400; var boldSupport = false; var italicSupport = false
        var condensedSupport = false; var extendedSupport = false
        var isBold = false; var isItalic = false; var isRegular = true

        tableMap["OS/2"]?.let { (o, _) ->
            weight = buf.getShort(o + 4).toInt() and 0xFFFF
            val fsSelection = buf.getShort(o + 62).toInt() and 0xFFFF
            isBold    = (fsSelection and 0x20) != 0
            isItalic  = (fsSelection and 0x01) != 0
            isRegular = (fsSelection and 0x40) != 0
        }

        var italicAngle = 0f; var isFixedPitch = false
        tableMap["post"]?.let { (o, _) ->
            italicAngle  = buf.getInt(o + 4).toFloat() / 65536f
            isFixedPitch = buf.getInt(o + 12) != 0
        }

        var unitsPerEm = 1000; var macStyle = 0
        tableMap["head"]?.let { (o, _) ->
            unitsPerEm = buf.getShort(o + 18).toInt() and 0xFFFF
            macStyle   = buf.getShort(o + 44).toInt() and 0xFFFF
            boldSupport      = (macStyle and 0x01) != 0
            italicSupport    = (macStyle and 0x02) != 0
            condensedSupport = (macStyle and 0x20) != 0
            extendedSupport  = (macStyle and 0x40) != 0
        }

        var numGlyphs = 0
        tableMap["maxp"]?.let { (o, _) ->
            numGlyphs = buf.getShort(o + 4).toInt() and 0xFFFF
        }

        val supportedChars = parseCmap(buf, raw, tableMap)

        val weightName = mapOf(
            100 to "Thin", 200 to "ExtraLight", 300 to "Light", 400 to "Regular",
            500 to "Medium", 600 to "SemiBold", 700 to "Bold", 800 to "ExtraBold", 900 to "Black"
        )[weight] ?: "Weight $weight"

        return FontMeta(
            family = family, subfamily = subfamily, fullName = fullName, version = version,
            postscript = postscript, manufacturer = manufacturer, designer = designer,
            description = description, trademark = trademark, license = license,
            licenseURL = licenseURL, vendorURL = vendorURL, designerURL = designerURL,
            sampleText = sampleText, numGlyphs = numGlyphs, weight = weight,
            weightName = weightName, unitsPerEm = unitsPerEm, italicAngle = italicAngle,
            isFixedPitch = isFixedPitch, boldSupport = boldSupport, italicSupport = italicSupport,
            condensedSupport = condensedSupport, extendedSupport = extendedSupport,
            isBold = isBold, isItalic = isItalic, isRegular = isRegular,
            tables = tableMap.keys.toList(), supportedChars = supportedChars
        )
    }

    private fun parseCmap(buf: ByteBuffer, raw: ByteArray, tableMap: Map<String, Pair<Int, Int>>): List<Int> {
        val cmapOff = tableMap["cmap"]?.first ?: return emptyList()
        val numSubtables = buf.getShort(cmapOff + 2).toInt() and 0xFFFF
        var bestOff = -1; var bestPriority = -1
        for (i in 0 until numSubtables) {
            val base = cmapOff + 4 + i * 8
            val platformID = buf.getShort(base).toInt() and 0xFFFF
            val encodingID = buf.getShort(base + 2).toInt() and 0xFFFF
            val subOff     = cmapOff + (buf.getInt(base + 4))
            val format     = buf.getShort(subOff).toInt() and 0xFFFF
            val priority = when {
                platformID == 3 && encodingID == 10 && format == 12 -> 4
                platformID == 0 && encodingID == 6  && format == 12 -> 3
                platformID == 3 && encodingID == 1  && format == 4  -> 2
                platformID == 0 && format == 4                       -> 1
                else -> 0
            }
            if (priority > bestPriority) { bestPriority = priority; bestOff = subOff }
        }
        if (bestOff < 0) return emptyList()
        val format = buf.getShort(bestOff).toInt() and 0xFFFF
        val chars = mutableSetOf<Int>()
        try {
            when (format) {
                4 -> {
                    val segCount = (buf.getShort(bestOff + 6).toInt() and 0xFFFF) / 2
                    val endOff   = bestOff + 14
                    val startOff = endOff + segCount * 2 + 2
                    for (i in 0 until segCount - 1) {
                        val end   = buf.getShort(endOff   + i * 2).toInt() and 0xFFFF
                        val start = buf.getShort(startOff + i * 2).toInt() and 0xFFFF
                        for (c in start..end) if (c < 0xFFFF) chars.add(c)
                    }
                }
                12 -> {
                    val nGroups = buf.getInt(bestOff + 12)
                    for (i in 0 until nGroups) {
                        val startChar = buf.getInt(bestOff + 16 + i * 12)
                        val endChar   = buf.getInt(bestOff + 16 + i * 12 + 4)
                        for (c in startChar..endChar) {
                            if (chars.size >= 10000) break
                            if (c >= 32) chars.add(c)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return chars.sorted()
    }
}
