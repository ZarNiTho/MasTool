package com.example.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.util.Base64
import com.example.model.LogLevel
import com.example.model.UsbPortInfo
import com.example.model.UsbProtocolType
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Locale
import javax.crypto.Cipher

class UsbPortController(private val context: Context) {

    companion object {
        const val ACTION_USB_PERMISSION = "com.aistudio.fastdeckotg.USB_PERMISSION"

        // ADB USB Protocol Constants
        private const val A_CNXN = 0x4E584E43
        private const val A_AUTH = 0x48545541
        private const val A_OPEN = 0x4E45504F
        private const val A_OKAY = 0x59414B4F
        private const val A_CLSE = 0x45534C43
        private const val A_WRTE = 0x45545257
        private const val A_VERSION = 0x01000000
        private const val MAX_ADB_DATA = 256 * 1024

        private const val ADB_AUTH_TOKEN = 1
        private const val ADB_AUTH_SIGNATURE = 2
        private const val ADB_AUTH_RSAPUBLICKEY = 3

        private const val ADB_RSA_KEY_BITS = 2048
        private const val ADB_RSA_MODULUS_WORDS = ADB_RSA_KEY_BITS / 32 // 64
        private const val ADB_RSA_STRUCT_BYTES = 4 + 4 + (ADB_RSA_MODULUS_WORDS * 4) * 2 + 4 // 524

        // ASN.1 SHA-1 DigestInfo prefix for 20-byte ADB auth token
        private val SHA1_DIGEST_INFO_PREFIX = byteArrayOf(
            0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2B, 0x0E,
            0x03, 0x02, 0x1A, 0x05, 0x00, 0x04, 0x14
        )

        // Fastboot Max Single Download Chunk (256 MB)
        private const val MAX_FASTBOOT_CHUNK = 256L * 1024L * 1024L

        /**
         * Encodes a 2048-bit RSAPublicKey into Android's exact 524-byte little-endian
         * RSAPublicKey binary struct (libcrypto_utils android_pubkey_encode) so adbd
         * properly verifies and persists it to /data/misc/adb/adb_keys when "Always allow" is checked.
         */
        internal fun encodeAdbPublicKeyStruct(pub: RSAPublicKey): ByteArray {
            val n = pub.modulus
            val r32 = BigInteger.ONE.shiftLeft(32)
            val mask32 = r32.subtract(BigInteger.ONE)
            val n0inv = r32.subtract(n.modInverse(r32)).and(mask32).toLong().toInt()
            val rr = BigInteger.ONE.shiftLeft(ADB_RSA_KEY_BITS * 2).mod(n)

            val buf = ByteBuffer.allocate(ADB_RSA_STRUCT_BYTES).order(ByteOrder.LITTLE_ENDIAN)
            buf.putInt(ADB_RSA_MODULUS_WORDS)
            buf.putInt(n0inv)
            for (i in 0 until ADB_RSA_MODULUS_WORDS) {
                buf.putInt(n.shiftRight(i * 32).and(mask32).toLong().toInt())
            }
            for (i in 0 until ADB_RSA_MODULUS_WORDS) {
                buf.putInt(rr.shiftRight(i * 32).and(mask32).toLong().toInt())
            }
            buf.putInt(pub.publicExponent.toInt())
            return buf.array()
        }

        internal fun encodeAdbPublicKeyPayload(
            pub: RSAPublicKey,
            comment: String = "fastdeck@android"
        ): ByteArray {
            val structBytes = encodeAdbPublicKeyStruct(pub)
            val base64Pub = Base64.encodeToString(structBytes, Base64.NO_WRAP)
            return "$base64Pub $comment\u0000".toByteArray(StandardCharsets.US_ASCII)
        }

        internal fun signAdbTokenPayload(privateKey: PrivateKey, token: ByteArray): ByteArray {
            val token20 = token.copyOf(20)
            val digestInfo = SHA1_DIGEST_INFO_PREFIX + token20

            try {
                val sig = Signature.getInstance("NONEwithRSA")
                sig.initSign(privateKey)
                sig.update(digestInfo)
                val out = sig.sign()
                if (out.size == 256) return out
            } catch (_: Exception) {
            }

            return try {
                val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
                cipher.init(Cipher.ENCRYPT_MODE, privateKey)
                cipher.doFinal(digestInfo)
            } catch (_: Exception) {
                ByteArray(256)
            }
        }
    }

    private val usbManager: UsbManager? =
        context.getSystemService(Context.USB_SERVICE) as? UsbManager

    fun scanPorts(): List<UsbPortInfo> {
        val manager = usbManager ?: return emptyList()
        val devices = manager.deviceList ?: return emptyList()
        return devices.values.map { dev -> inspectDevice(manager, dev) }
    }

    fun requestPermission(deviceName: String): Boolean {
        val manager = usbManager ?: return false
        val device = manager.deviceList[deviceName] ?: return false
        if (manager.hasPermission(device)) return true

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val intent = Intent(ACTION_USB_PERMISSION).apply {
            setPackage(context.packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)
        manager.requestPermission(device, pendingIntent)
        return true
    }

    /**
     * Executes one or more Fastboot wire commands over USB Bulk endpoints.
     */
    fun executeFastbootWireCommands(
        deviceName: String,
        wireCommands: List<String>,
        onLog: (String, LogLevel) -> Unit
    ) {
        val manager = usbManager ?: run {
            onLog("UsbManager service unavailable.", LogLevel.FAIL)
            return
        }
        val device = manager.deviceList[deviceName] ?: run {
            onLog("USB OTG device '$deviceName' not attached.", LogLevel.FAIL)
            return
        }
        if (!manager.hasPermission(device)) {
            onLog("USB permission required for ${device.deviceName}. Requesting permission...", LogLevel.WARN)
            requestPermission(deviceName)
            return
        }

        val triple = findBulkEndpoints(device, preferProtocol = 0x03) ?: run {
            onLog("No USB Bulk IN/OUT endpoints found on ${device.deviceName}.", LogLevel.FAIL)
            return
        }
        val (iface, epIn, epOut) = triple
        val connection = manager.openDevice(device) ?: run {
            onLog("Failed to open UsbDeviceConnection for ${device.deviceName}.", LogLevel.FAIL)
            return
        }

        try {
            if (!connection.claimInterface(iface, true)) {
                onLog("Failed to claim Fastboot USB interface #${iface.id}.", LogLevel.FAIL)
                return
            }

            for (wireCommand in wireCommands) {
                val payload = wireCommand.toByteArray(StandardCharsets.US_ASCII)
                val tx = connection.bulkTransfer(epOut, payload, payload.size, 4000)
                if (tx < 0) {
                    onLog("USB Bulk OUT write failed ($tx) for '$wireCommand'.", LogLevel.FAIL)
                    return
                }
                onLog("TX ($tx B) -> $wireCommand", LogLevel.USB)

                val timeoutMs = if (wireCommand.startsWith("erase:") || wireCommand.startsWith("flash:")) {
                    15000
                } else {
                    4000
                }
                val ok = readFastbootResponses(connection, epIn, timeoutMs, onLog)
                if (!ok) break
            }
        } catch (e: Exception) {
            onLog("USB Fastboot exception: ${e.message}", LogLevel.FAIL)
        } finally {
            try {
                connection.releaseInterface(iface)
                connection.close()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Streams binary image data from a Uri (or UPDATE.APP byte offset) using ParcelFileDescriptor / InputStream
     * over USB Bulk endpoints using Fastboot download + flash protocol.
     */
    fun flashBinaryStreamOverUsb(
        deviceName: String,
        uri: Uri,
        partition: String,
        dataOffset: Long,
        dataSize: Long,
        disableVerity: Boolean,
        onProgress: (Float) -> Unit,
        onLog: (String, LogLevel) -> Unit
    ) {
        val manager = usbManager ?: run {
            onLog("UsbManager unavailable.", LogLevel.FAIL)
            return
        }
        val device = manager.deviceList[deviceName] ?: run {
            onLog("USB OTG Fastboot device not attached.", LogLevel.FAIL)
            return
        }
        if (!manager.hasPermission(device)) {
            onLog("USB permission required for ${device.deviceName}.", LogLevel.WARN)
            requestPermission(deviceName)
            return
        }

        val triple = findBulkEndpoints(device, preferProtocol = 0x03) ?: run {
            onLog("No Bulk endpoints on ${device.deviceName}.", LogLevel.FAIL)
            return
        }
        val (iface, epIn, epOut) = triple
        val connection = manager.openDevice(device) ?: run {
            onLog("Unable to open USB connection.", LogLevel.FAIL)
            return
        }

        try {
            if (!connection.claimInterface(iface, true)) {
                onLog("Unable to claim USB interface #${iface.id}.", LogLevel.FAIL)
                return
            }

            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { fis ->
                    val channel = fis.channel
                    val fileTotal = pfd.statSize.takeIf { it > 0 } ?: channel.size()
                    val streamBytes = if (dataSize > 0L) {
                        dataSize.coerceAtMost(fileTotal - dataOffset)
                    } else {
                        (fileTotal - dataOffset).coerceAtLeast(0L)
                    }

                    if (streamBytes <= 0L) {
                        onLog("Selected file stream has 0 bytes to transfer.", LogLevel.FAIL)
                        return
                    }

                    channel.position(dataOffset)

                    val transferSize = streamBytes.coerceAtMost(MAX_FASTBOOT_CHUNK)
                    if (streamBytes > MAX_FASTBOOT_CHUNK) {
                        onLog(
                            "Image size (${FlashFileProcessor.formatSize(streamBytes)}) exceeds 256MB single buffer; transferring first 256MB chunk.",
                            LogLevel.WARN
                        )
                    }

                    val downloadCmd = String.format(Locale.US, "download:%08x", transferSize)
                    val cmdBytes = downloadCmd.toByteArray(StandardCharsets.US_ASCII)
                    val txCmd = connection.bulkTransfer(epOut, cmdBytes, cmdBytes.size, 4000)
                    if (txCmd < 0) {
                        onLog("Failed to send '$downloadCmd' ($txCmd).", LogLevel.FAIL)
                        return
                    }
                    onLog("TX -> $downloadCmd (${FlashFileProcessor.formatSize(transferSize)})", LogLevel.USB)

                    val respBuf = ByteArray(64)
                    val rx = connection.bulkTransfer(epIn, respBuf, respBuf.size, 4000)
                    if (rx <= 0) {
                        onLog("No response from bootloader to '$downloadCmd'.", LogLevel.FAIL)
                        return
                    }
                    val reply = String(respBuf, 0, rx, StandardCharsets.US_ASCII).trim()
                    onLog("RX <- $reply", LogLevel.USB)
                    if (!reply.startsWith("DATA")) {
                        onLog("Bootloader rejected download request: $reply", LogLevel.FAIL)
                        return
                    }

                    val chunk = ByteArray(16384)
                    var sentTotal = 0L
                    while (sentTotal < transferSize) {
                        val toRead = minOf(chunk.size.toLong(), transferSize - sentTotal).toInt()
                        val r = fis.read(chunk, 0, toRead)
                        if (r <= 0) break

                        // If patching vbmeta with disable-verity (offset 123 flags byte in AVBHeader)
                        if (sentTotal == 0L && disableVerity && partition.startsWith("vbmeta") && r >= 128) {
                            if (chunk[0] == 'A'.code.toByte() &&
                                chunk[1] == 'V'.code.toByte() &&
                                chunk[2] == 'B'.code.toByte() &&
                                chunk[3] == '0'.code.toByte()
                            ) {
                                chunk[123] = (chunk[123].toInt() or 0x03).toByte()
                                onLog("AVB0 Header detected: patched flags byte @offset 123 (disable-verity | disable-verification).", LogLevel.OKAY)
                            }
                        }

                        val written = connection.bulkTransfer(epOut, chunk, r, 6000)
                        if (written < 0) {
                            onLog("Bulk data write failed at offset $sentTotal ($written).", LogLevel.FAIL)
                            return
                        }
                        sentTotal += written
                        onProgress((sentTotal.toFloat() / transferSize.toFloat()).coerceIn(0f, 1f))
                    }

                    onLog("Transferred $sentTotal / $transferSize bytes to bootloader RAM.", LogLevel.OKAY)
                    if (!readFastbootResponses(connection, epIn, 8000, onLog)) {
                        return
                    }

                    val flashCmd = "flash:$partition"
                    val flashBytes = flashCmd.toByteArray(StandardCharsets.US_ASCII)
                    val txFlash = connection.bulkTransfer(epOut, flashBytes, flashBytes.size, 4000)
                    if (txFlash < 0) {
                        onLog("Failed to send '$flashCmd' ($txFlash).", LogLevel.FAIL)
                        return
                    }
                    onLog("TX -> $flashCmd", LogLevel.USB)
                    readFastbootResponses(connection, epIn, 25000, onLog)
                }
            } ?: onLog("Could not open ParcelFileDescriptor for selected file.", LogLevel.FAIL)
        } catch (e: Exception) {
            onLog("Flash error: ${e.message}", LogLevel.FAIL)
        } finally {
            try {
                connection.releaseInterface(iface)
                connection.close()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Native USB ADB Protocol Execution (A_CNXN -> A_AUTH RSA -> A_OPEN -> A_WRTE -> A_CLSE).
     * Executes real ADB reboot or shell commands over USB OTG when an ADB device is connected!
     */
    fun executeAdbOverUsbOtg(
        deviceName: String,
        command: String,
        onLog: (String, LogLevel) -> Unit
    ) {
        val manager = usbManager ?: return
        val device = manager.deviceList[deviceName] ?: run {
            onLog("No USB OTG device at $deviceName.", LogLevel.FAIL)
            return
        }
        if (!manager.hasPermission(device)) {
            onLog("USB permission required for $deviceName. Requesting permission...", LogLevel.WARN)
            requestPermission(deviceName)
            return
        }
        val triple = findBulkEndpoints(device, preferProtocol = 0x01) ?: run {
            onLog("No Bulk endpoints on $deviceName.", LogLevel.FAIL)
            return
        }
        val (iface, epIn, epOut) = triple
        val connection = manager.openDevice(device) ?: run {
            onLog("Failed to open USB connection for $deviceName.", LogLevel.FAIL)
            return
        }

        try {
            if (!connection.claimInterface(iface, true)) {
                onLog("Cannot claim ADB USB interface #${iface.id}.", LogLevel.FAIL)
                return
            }

            // Step 1: Send A_CNXN
            val banner = "host::features=shell_v2,cmd,stat_v2,abb_exec,fastbootd\u0000"
                .toByteArray(StandardCharsets.UTF_8)
            sendAdbPacket(connection, epOut, A_CNXN, A_VERSION, MAX_ADB_DATA, banner)
            onLog("ADB USB TX -> CNXN (${device.productName})", LogLevel.USB)

            // Step 2: Handle A_AUTH or A_CNXN response
            var connected = false
            var sentSignature = false
            var sentPublicKey = false
            var authWaitRetries = 0
            val keyPair = getOrCreateAdbRsaKeyPair()

            for (attempt in 0 until 12) {
                val readTimeout = if (sentPublicKey) 4000 else 3500
                val pkt = readAdbPacket(connection, epIn, readTimeout)
                if (pkt == null) {
                    if (sentPublicKey && authWaitRetries < 3) {
                        authWaitRetries++
                        continue
                    }
                    break
                }
                when (pkt.command) {
                    A_CNXN -> {
                        val remoteBanner = String(pkt.payload, StandardCharsets.UTF_8)
                            .trim { it <= ' ' || it == '\u0000' }
                        onLog("ADB USB Connected: $remoteBanner", LogLevel.OKAY)
                        connected = true
                        break
                    }
                    A_AUTH -> {
                        if (pkt.arg0 == ADB_AUTH_TOKEN) {
                            if (!sentSignature) {
                                val sig = signAdbToken(keyPair, pkt.payload)
                                sendAdbPacket(connection, epOut, A_AUTH, ADB_AUTH_SIGNATURE, 0, sig)
                                sentSignature = true
                                onLog("ADB USB TX -> AUTH_SIGNATURE (2048-bit RSA)", LogLevel.USB)
                            } else if (!sentPublicKey) {
                                val pubKeyBytes = encodeAdbPublicKey(keyPair)
                                sendAdbPacket(connection, epOut, A_AUTH, ADB_AUTH_RSAPUBLICKEY, 0, pubKeyBytes)
                                sentPublicKey = true
                                onLog(
                                    "ADB USB TX -> AUTH_RSAPUBLICKEY. Please tap 'Allow USB Debugging' on target phone!",
                                    LogLevel.WARN
                                )
                            }
                        }
                    }
                }
            }

            if (!connected) {
                onLog("ADB USB handshake waiting for target authorization or device not in ADB mode.", LogLevel.WARN)
                return
            }

            // Route ADB host/service commands cleanly without falling into remote /system/bin/sh
            val trimmedCmd = command.trim()
            val lower = trimmedCmd.lowercase(Locale.US)
            if (lower == "adb devices" || lower == "adb devices -l") {
                onLog("${device.serialNumber ?: device.deviceName}\tdevice product:${device.productName}", LogLevel.OKAY)
                return
            }
            if (lower == "adb get-state") {
                onLog("device", LogLevel.OKAY)
                return
            }
            if (lower == "adb get-serialno") {
                onLog(device.serialNumber ?: device.deviceName, LogLevel.OKAY)
                return
            }

            val serviceDest: String = when {
                lower == "adb" -> {
                    onLog("Usage: adb devices | adb shell <cmd> | adb reboot [bootloader|recovery]", LogLevel.INFO)
                    return
                }
                lower == "adb reboot" -> "reboot:\u0000"
                lower.startsWith("adb reboot ") -> {
                    val target = trimmedCmd.substringAfter("reboot ", "").trim()
                    "reboot:$target\u0000"
                }
                lower == "adb root" -> "root:\u0000"
                lower == "adb unroot" -> "unroot:\u0000"
                lower == "adb remount" -> "remount:\u0000"
                lower == "adb usb" -> "usb:\u0000"
                lower.startsWith("adb tcpip ") -> {
                    val port = trimmedCmd.substringAfter("tcpip ", "").trim()
                    "tcpip:$port\u0000"
                }
                lower == "adb shell" -> {
                    onLog("Usage: adb shell <command>", LogLevel.INFO)
                    return
                }
                lower.startsWith("adb shell ") -> {
                    val shellCmd = trimmedCmd.substring(10).trim()
                    "shell:$shellCmd\u0000"
                }
                lower.startsWith("adb ") -> {
                    val sub = trimmedCmd.substring(4).trim()
                    onLog(
                        "adb: unsupported subcommand '$sub'. Use 'adb shell <cmd>', 'adb reboot [target]', or 'adb devices'.",
                        LogLevel.FAIL
                    )
                    return
                }
                lower.startsWith("shell ") -> {
                    val shellCmd = trimmedCmd.substring(6).trim()
                    "shell:$shellCmd\u0000"
                }
                else -> "shell:$trimmedCmd\u0000"
            }

            val isRebootService = serviceDest.startsWith("reboot:")
            val localId = 1
            sendAdbPacket(
                connection,
                epOut,
                A_OPEN,
                localId,
                0,
                serviceDest.toByteArray(StandardCharsets.UTF_8)
            )
            onLog("ADB USB TX -> OPEN '${serviceDest.trimEnd('\u0000')}'", LogLevel.USB)

            // Step 4: Read A_OKAY / A_WRTE / A_CLSE stream output
            var remoteId = 0
            var streamOpened = false
            for (loop in 0 until 128) {
                val pktTimeout = if (isRebootService && streamOpened) 1200 else 3500
                val pkt = readAdbPacket(connection, epIn, pktTimeout) ?: break
                when (pkt.command) {
                    A_OKAY -> {
                        remoteId = pkt.arg0
                        streamOpened = true
                    }
                    A_WRTE -> {
                        remoteId = pkt.arg0
                        val text = String(pkt.payload, StandardCharsets.UTF_8)
                        text.lines().filter { it.isNotBlank() }.forEach { line ->
                            onLog(line, LogLevel.INFO)
                        }
                        sendAdbPacket(connection, epOut, A_OKAY, localId, remoteId, ByteArray(0))
                    }
                    A_CLSE -> {
                        sendAdbPacket(connection, epOut, A_CLSE, localId, remoteId, ByteArray(0))
                        onLog("ADB stream closed (OKAY).", LogLevel.OKAY)
                        break
                    }
                }
            }
            if (isRebootService && streamOpened) {
                onLog("Reboot command accepted by target device.", LogLevel.OKAY)
            }
        } catch (e: Exception) {
            onLog("ADB USB error: ${e.message}", LogLevel.FAIL)
        } finally {
            try {
                connection.releaseInterface(iface)
                connection.close()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Executes a real local shell command via ProcessBuilder when inspecting local host properties.
     */
    fun executeLocalShell(command: String, onLog: (String, LogLevel) -> Unit) {
        try {
            val clean = command
                .removePrefix("adb shell ")
                .removePrefix("shell ")
                .trim()
            val process = ProcessBuilder("sh", "-c", clean)
                .redirectErrorStream(true)
                .start()

            var lineCount = 0
            BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
                lines.take(100).forEach { line ->
                    if (line.isNotBlank()) {
                        onLog(line, LogLevel.INFO)
                        lineCount++
                    }
                }
            }
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                onLog("Local Shell Exit 0 ($lineCount lines)", LogLevel.OKAY)
            } else {
                onLog("Local Shell Exit $exitCode (Requires target OTG device or root permission)", LogLevel.WARN)
            }
        } catch (e: Exception) {
            onLog("Shell execution error: ${e.message}", LogLevel.FAIL)
        }
    }

    private fun readFastbootResponses(
        connection: UsbDeviceConnection,
        epIn: UsbEndpoint,
        timeoutMs: Int,
        onLog: (String, LogLevel) -> Unit
    ): Boolean {
        val buf = ByteArray(256)
        var polls = 0
        while (polls < 64) {
            polls++
            val rx = connection.bulkTransfer(epIn, buf, buf.size, timeoutMs)
            if (rx < 4) {
                onLog("Fastboot USB read timeout ($rx).", LogLevel.WARN)
                return false
            }
            val status = String(buf, 0, 4, StandardCharsets.US_ASCII)
            val body = if (rx > 4) {
                String(buf, 4, rx - 4, StandardCharsets.US_ASCII).trim()
            } else {
                ""
            }
            when (status) {
                "INFO" -> onLog("(bootloader) $body", LogLevel.INFO)
                "TEXT" -> onLog(body, LogLevel.INFO)
                "OKAY" -> {
                    onLog("OKAY ${if (body.isNotEmpty()) "[$body]" else ""}".trim(), LogLevel.OKAY)
                    return true
                }
                "FAIL" -> {
                    onLog("FAILED (remote: '$body')", LogLevel.FAIL)
                    return false
                }
                else -> {
                    onLog("RX: $status$body", LogLevel.USB)
                    return true
                }
            }
        }
        return false
    }

    private class AdbPacket(
        val command: Int,
        val arg0: Int,
        val arg1: Int,
        val payload: ByteArray
    )

    private fun sendAdbPacket(
        connection: UsbDeviceConnection,
        epOut: UsbEndpoint,
        command: Int,
        arg0: Int,
        arg1: Int,
        payload: ByteArray
    ) {
        var checksum = 0
        for (b in payload) {
            checksum += (b.toInt() and 0xFF)
        }
        val header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(command)
            putInt(arg0)
            putInt(arg1)
            putInt(payload.size)
            putInt(checksum)
            putInt(command xor -1)
        }.array()

        connection.bulkTransfer(epOut, header, header.size, 2500)
        if (payload.isNotEmpty()) {
            connection.bulkTransfer(epOut, payload, payload.size, 3000)
        }
    }

    private fun readAdbPacket(
        connection: UsbDeviceConnection,
        epIn: UsbEndpoint,
        timeoutMs: Int
    ): AdbPacket? {
        val hdr = ByteArray(24)
        var rx = -1
        for (zlpRetry in 0 until 3) {
            rx = connection.bulkTransfer(epIn, hdr, hdr.size, timeoutMs)
            if (rx != 0) break
        }
        if (rx != 24) return null

        val bb = ByteBuffer.wrap(hdr).order(ByteOrder.LITTLE_ENDIAN)
        val cmd = bb.int
        val arg0 = bb.int
        val arg1 = bb.int
        val len = bb.int
        bb.int // checksum
        bb.int // magic

        val payload = if (len in 1..MAX_ADB_DATA) {
            val data = ByteArray(len)
            var total = 0
            var zeroReads = 0
            while (total < len) {
                val r = connection.bulkTransfer(epIn, data, total, len - total, timeoutMs)
                if (r < 0) break
                if (r == 0) {
                    zeroReads++
                    if (zeroReads >= 3) break
                    continue
                }
                zeroReads = 0
                total += r
            }
            data
        } else {
            ByteArray(0)
        }
        return AdbPacket(cmd, arg0, arg1, payload)
    }

    private fun getOrCreateAdbRsaKeyPair(): KeyPair {
        val privFile = File(context.filesDir, "adb_rsa_priv.key")
        val pubFile = File(context.filesDir, "adb_rsa_pub.key")
        if (privFile.exists() && pubFile.exists()) {
            try {
                val kf = KeyFactory.getInstance("RSA")
                val priv = kf.generatePrivate(PKCS8EncodedKeySpec(privFile.readBytes()))
                val pub = kf.generatePublic(X509EncodedKeySpec(pubFile.readBytes()))
                if (pub is RSAPublicKey && priv is RSAPrivateKey && pub.modulus.bitLength() == ADB_RSA_KEY_BITS) {
                    return KeyPair(pub, priv)
                }
            } catch (_: Exception) {
            }
        }
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(ADB_RSA_KEY_BITS)
        val kp = kpg.generateKeyPair()
        try {
            privFile.writeBytes(kp.private.encoded)
            pubFile.writeBytes(kp.public.encoded)
        } catch (_: Exception) {
        }
        return kp
    }

    private fun signAdbToken(keyPair: KeyPair, token: ByteArray): ByteArray {
        return signAdbTokenPayload(keyPair.private, token)
    }

    private fun encodeAdbPublicKey(keyPair: KeyPair): ByteArray {
        val rsaPub = keyPair.public as? RSAPublicKey
            ?: throw IllegalStateException("ADB RSA public key required")
        return encodeAdbPublicKeyPayload(rsaPub)
    }

    private fun inspectDevice(manager: UsbManager, device: UsbDevice): UsbPortInfo {
        val hasPerm = try {
            manager.hasPermission(device)
        } catch (_: Exception) {
            false
        }

        var protoType = UsbProtocolType.VENDOR_PORT
        var ifaceId = 0
        var epInAddr = 0
        var epOutAddr = 0
        var maxPkt = 512

        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == 0xFF && iface.interfaceSubclass == 0x42) {
                if (iface.interfaceProtocol == 0x01) {
                    protoType = UsbProtocolType.ADB
                    ifaceId = iface.id
                } else if (iface.interfaceProtocol == 0x03) {
                    protoType = UsbProtocolType.FASTBOOT
                    ifaceId = iface.id
                }
            }
            for (e in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(e)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    maxPkt = ep.maxPacketSize
                    if (ep.direction == UsbConstants.USB_DIR_IN) epInAddr = ep.address
                    if (ep.direction == UsbConstants.USB_DIR_OUT) epOutAddr = ep.address
                }
            }
        }

        val prodName = try {
            device.productName ?: "USB Device"
        } catch (_: Exception) {
            "USB Device"
        }

        val serial = try {
            if (hasPerm) device.serialNumber ?: "N/A" else "Perm Required"
        } catch (_: Exception) {
            "Perm Required"
        }

        return UsbPortInfo(
            deviceName = device.deviceName,
            vendorId = device.vendorId,
            productId = device.productId,
            vendorHex = String.format(Locale.US, "0x%04X", device.vendorId),
            productHex = String.format(Locale.US, "0x%04X", device.productId),
            productName = prodName,
            serialNumber = serial,
            interfaceId = ifaceId,
            endpointInAddress = epInAddr,
            endpointOutAddress = epOutAddr,
            maxPacketSize = maxPkt,
            hasPermission = hasPerm,
            protocolType = protoType
        )
    }

    private fun findBulkEndpoints(
        device: UsbDevice,
        preferProtocol: Int
    ): Triple<UsbInterface, UsbEndpoint, UsbEndpoint>? {
        var fallback: Triple<UsbInterface, UsbEndpoint, UsbEndpoint>? = null
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            var epIn: UsbEndpoint? = null
            var epOut: UsbEndpoint? = null
            for (e in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(e)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (ep.direction == UsbConstants.USB_DIR_IN) epIn = ep
                    if (ep.direction == UsbConstants.USB_DIR_OUT) epOut = ep
                }
            }
            if (epIn != null && epOut != null) {
                if (iface.interfaceClass == 0xFF &&
                    iface.interfaceSubclass == 0x42 &&
                    iface.interfaceProtocol == preferProtocol
                ) {
                    return Triple(iface, epIn, epOut)
                }
                if (fallback == null) {
                    fallback = Triple(iface, epIn, epOut)
                }
            }
        }
        return fallback
    }
}
