package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.FlashFileType
import com.example.model.ParsedFlashStep
import com.example.model.SelectedFlashFile
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.zip.ZipInputStream

object FlashFileProcessor {

    fun inspectAndParseUri(context: Context, uri: Uri, defaultPartition: String): SelectedFlashFile {
        val resolver = context.contentResolver
        var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "firmware.bin"
        var fileSize = -1L

        try {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx >= 0) {
                        cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { displayName = it }
                    }
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                        fileSize = cursor.getLong(sizeIdx)
                    }
                }
            }
        } catch (_: Exception) {
        }

        if (fileSize <= 0L) {
            try {
                resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    if (pfd.statSize > 0L) {
                        fileSize = pfd.statSize
                    }
                }
            } catch (_: Exception) {
            }
        }

        val header = ByteArray(28)
        var headerRead = 0
        try {
            resolver.openInputStream(uri)?.use { stream ->
                while (headerRead < header.size) {
                    val r = stream.read(header, headerRead, header.size - headerRead)
                    if (r <= 0) break
                    headerRead += r
                }
            }
        } catch (_: Exception) {
        }

        val magicHex = if (headerRead > 0) {
            header.take(headerRead.coerceAtMost(8))
                .joinToString(" ") { String.format(Locale.US, "%02X", it) }
        } else {
            "EMPTY"
        }

        // Android Sparse Image Header check (magic 0xED26FF3A -> 3A FF 26 ED)
        val isSparse = headerRead >= 28 &&
            header[0] == 0x3A.toByte() &&
            header[1] == 0xFF.toByte() &&
            header[2] == 0x26.toByte() &&
            header[3] == 0xED.toByte()

        val sparseDetails = if (isSparse) {
            val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            bb.position(4)
            val major = bb.short.toInt() and 0xFFFF
            val minor = bb.short.toInt() and 0xFFFF
            bb.short // file_hdr_sz
            bb.short // chunk_hdr_sz
            val blkSz = bb.int.toLong() and 0xFFFFFFFFL
            val totalBlks = bb.int.toLong() and 0xFFFFFFFFL
            val totalChunks = bb.int.toLong() and 0xFFFFFFFFL
            "Sparse v$major.$minor (${blkSz}B blk, $totalBlks blks, $totalChunks chunks)"
        } else {
            "Raw Binary Stream"
        }

        // Check Huawei UPDATE.APP magic (55 AA 5A A5) in first 128 bytes
        val hasHuaweiMagic = checkUpdateAppMagic(context, uri)

        val lower = displayName.lowercase(Locale.US)
        val type = when {
            lower.endsWith(".app") || hasHuaweiMagic -> FlashFileType.UPDATE_APP
            lower.endsWith(".bat") || lower.endsWith(".cmd") || lower.endsWith(".sh") -> FlashFileType.SCRIPT
            lower.endsWith(".zip") -> FlashFileType.ARCHIVE
            lower.endsWith(".img") || lower.endsWith(".bin") || isSparse -> FlashFileType.IMAGE
            else -> FlashFileType.RAW
        }

        val steps = when (type) {
            FlashFileType.SCRIPT -> parseRealScriptFile(context, uri)
            FlashFileType.UPDATE_APP -> parseRealUpdateAppWithChannel(context, uri)
            FlashFileType.ARCHIVE -> parseRealZipEntries(context, uri)
            FlashFileType.IMAGE, FlashFileType.RAW -> {
                val inferred = inferPartition(lower, defaultPartition)
                listOf(
                    ParsedFlashStep(
                        id = "img_1",
                        order = 1,
                        command = "fastboot flash $inferred $displayName",
                        targetPartition = inferred,
                        sourceName = displayName,
                        dataOffset = 0L,
                        dataSize = fileSize.coerceAtLeast(0L)
                    )
                )
            }
        }

        return SelectedFlashFile(
            uriString = uri.toString(),
            fileName = displayName,
            fileSizeBytes = fileSize.coerceAtLeast(0L),
            formattedSize = formatSize(fileSize),
            fileType = type,
            magicHeaderHex = magicHex,
            isSparseImage = isSparse,
            sparseInfo = sparseDetails,
            steps = steps
        )
    }

    private fun checkUpdateAppMagic(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val probe = ByteArray(128)
                val read = stream.read(probe)
                if (read < 4) return false
                for (i in 0..(read - 4)) {
                    if (probe[i] == 0x55.toByte() &&
                        probe[i + 1] == 0xAA.toByte() &&
                        probe[i + 2] == 0x5A.toByte() &&
                        probe[i + 3] == 0xA5.toByte()
                    ) {
                        return true
                    }
                }
                false
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Uses FileChannel O(1) seeking on ParcelFileDescriptor to accurately parse multi-gigabyte
     * Huawei UPDATE.APP packages without stream buffer drift.
     */
    private fun parseRealUpdateAppWithChannel(context: Context, uri: Uri): List<ParsedFlashStep> {
        val steps = mutableListOf<ParsedFlashStep>()
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).channel.use { channel ->
                    val fileSize = channel.size()
                    var pos = 0L
                    val buf = ByteBuffer.allocate(96).order(ByteOrder.LITTLE_ENDIAN)
                    var order = 1

                    // Huawei UPDATE.APP starts with 92 bytes of 0x00 padding before first 55 AA 5A A5
                    while (pos + 96 <= fileSize && steps.size < 64) {
                        buf.clear()
                        val read = channel.read(buf, pos)
                        if (read < 96) break
                        buf.flip()

                        var magicIdx = -1
                        for (i in 0..92) {
                            if (buf.get(i) == 0x55.toByte() &&
                                buf.get(i + 1) == 0xAA.toByte() &&
                                buf.get(i + 2) == 0x5A.toByte() &&
                                buf.get(i + 3) == 0xA5.toByte()
                            ) {
                                magicIdx = i
                                break
                            }
                        }

                        if (magicIdx < 0) {
                            pos += 4L
                            continue
                        }
                        if (magicIdx > 0) {
                            pos += magicIdx.toLong()
                            continue
                        }

                        val headerSize = buf.getInt(4).toLong() and 0xFFFFFFFFL
                        val dataSize = buf.getInt(24).toLong() and 0xFFFFFFFFL

                        val nameBytes = ByteArray(32)
                        buf.position(60)
                        buf.get(nameBytes)
                        val rawName = String(nameBytes, Charsets.US_ASCII)
                            .substringBefore('\u0000')
                            .trim()
                            .lowercase(Locale.US)

                        if (headerSize in 92..65536 &&
                            rawName.isNotEmpty() &&
                            rawName.all { it.isLetterOrDigit() || it == '_' || it == '-' }
                        ) {
                            val dataOffset = pos + headerSize
                            if (dataOffset + dataSize <= fileSize) {
                                steps.add(
                                    ParsedFlashStep(
                                        id = "uapp_$order",
                                        order = order,
                                        command = "fastboot flash $rawName",
                                        targetPartition = rawName,
                                        sourceName = "${rawName.uppercase(Locale.US)}.IMG (@0x${dataOffset.toString(16)})",
                                        dataOffset = dataOffset,
                                        dataSize = dataSize
                                    )
                                )
                                order++
                                val packetTotal = headerSize + dataSize
                                val rem = packetTotal % 4L
                                val alignedAdvance = if (rem == 0L) packetTotal else packetTotal + (4L - rem)
                                pos += alignedAdvance
                                continue
                            }
                        }
                        pos += 4L
                    }
                }
            }
        } catch (_: Exception) {
        }
        return steps
    }

    private fun parseRealScriptFile(context: Context, uri: Uri): List<ParsedFlashStep> {
        val result = mutableListOf<ParsedFlashStep>()
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).useLines { lines ->
                    var order = 1
                    lines.forEach { rawLine ->
                        val line = rawLine.trim()
                            .replace("%~dp0", "")
                            .replace("\$ANDROID_PRODUCT_OUT/", "")
                            .replace("\"${'$'}FASTBOOT\"", "fastboot")
                        if (line.isNotEmpty() &&
                            !line.startsWith("::") &&
                            !line.startsWith("#") &&
                            !line.startsWith("REM ", ignoreCase = true) &&
                            !line.startsWith("@echo", ignoreCase = true) &&
                            (line.contains("fastboot", ignoreCase = true) || line.contains("adb", ignoreCase = true))
                        ) {
                            val cleanCmd = line
                                .replace(Regex("^.*fastboot(\\.exe)?", RegexOption.IGNORE_CASE), "fastboot")
                                .substringBefore("||")
                                .substringBefore("&&")
                                .trim()
                            val parts = cleanCmd.split(Regex("\\s+"))
                            val flashIdx = parts.indexOfFirst { it.equals("flash", ignoreCase = true) }
                            val eraseIdx = parts.indexOfFirst { it.equals("erase", ignoreCase = true) }

                            val targetPart = when {
                                flashIdx >= 0 && flashIdx + 1 < parts.size -> parts[flashIdx + 1]
                                eraseIdx >= 0 && eraseIdx + 1 < parts.size -> parts[eraseIdx + 1]
                                else -> "device"
                            }
                            val srcFile = if (flashIdx >= 0 && flashIdx + 2 < parts.size) {
                                parts[flashIdx + 2]
                            } else {
                                cleanCmd
                            }

                            result.add(
                                ParsedFlashStep(
                                    id = "script_$order",
                                    order = order,
                                    command = cleanCmd,
                                    targetPartition = targetPart,
                                    sourceName = srcFile,
                                    dataOffset = 0L,
                                    dataSize = 0L
                                )
                            )
                            order++
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return result
    }

    private fun parseRealZipEntries(context: Context, uri: Uri): List<ParsedFlashStep> {
        val steps = mutableListOf<ParsedFlashStep>()
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ZipInputStream(stream).use { zis ->
                    var entry = zis.nextEntry
                    var order = 1
                    while (entry != null && order <= 40) {
                        if (!entry.isDirectory) {
                            val name = entry.name.substringAfterLast('/')
                            val lower = name.lowercase(Locale.US)
                            if (lower.endsWith(".img") || lower.endsWith(".bin") || lower == "payload.bin") {
                                val part = inferPartition(lower, "super")
                                val cmd = if (lower == "payload.bin") {
                                    "adb sideload $name"
                                } else {
                                    "fastboot flash $part $name"
                                }
                                steps.add(
                                    ParsedFlashStep(
                                        id = "zip_$order",
                                        order = order,
                                        command = cmd,
                                        targetPartition = part,
                                        sourceName = entry.name,
                                        dataOffset = 0L,
                                        dataSize = entry.size.coerceAtLeast(0L)
                                    )
                                )
                                order++
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (_: Exception) {
        }
        return steps
    }

    fun inferPartition(lowerName: String, fallback: String = "boot"): String {
        return when {
            lowerName.contains("init_boot") -> "init_boot"
            lowerName.contains("vendor_boot") -> "vendor_boot"
            lowerName.contains("boot") -> "boot"
            lowerName.contains("vbmeta_system") -> "vbmeta_system"
            lowerName.contains("vbmeta_vendor") -> "vbmeta_vendor"
            lowerName.contains("vbmeta") -> "vbmeta"
            lowerName.contains("recovery") -> "recovery"
            lowerName.contains("super") -> "super"
            lowerName.contains("system_ext") -> "system_ext"
            lowerName.contains("system") -> "system"
            lowerName.contains("vendor") -> "vendor"
            lowerName.contains("product") -> "product"
            lowerName.contains("dtbo") -> "dtbo"
            lowerName.contains("modem") || lowerName.contains("non-hlos") -> "modem"
            lowerName.contains("persist") -> "persist"
            lowerName.contains("userdata") -> "userdata"
            lowerName.contains("metadata") -> "metadata"
            else -> fallback
        }
    }

    fun formatSize(bytes: Long): String {
        if (bytes < 0L) return "Unknown"
        if (bytes == 0L) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.2f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.1f KB", bytes / kb)
            else -> "$bytes B"
        }
    }
}
