package com.example

import android.app.Application
import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.example.data.CommandRepository
import com.example.data.FlashFileProcessor
import com.example.data.UsbPortController
import com.example.model.ActionGroup
import com.example.model.AppFontSize
import com.example.model.ServiceBrand
import com.example.model.ThemeDesign
import com.example.model.ThemeMode
import com.example.model.ToolScreenMode
import com.example.ui.ServiceViewModel
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import javax.crypto.Cipher

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `verify app name and native command repository`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FastDeck OTG", appName)

        assertTrue(CommandRepository.allActions.size >= 70)
        assertTrue(
            CommandRepository.allActions.all {
                it.command.isNotBlank() && it.title.isNotBlank() && it.description.isNotBlank()
            }
        )
        // Verify all 6 ADB/Fastboot service categories have commands
        assertTrue(CommandRepository.allActions.any { it.isAdb && it.group == ActionGroup.INFO })
        assertTrue(CommandRepository.allActions.any { it.isAdb && it.group == ActionGroup.UNLOCK })
        assertTrue(CommandRepository.allActions.any { it.isAdb && it.group == ActionGroup.SERVICE })
        assertTrue(CommandRepository.allActions.any { !it.isAdb && it.group == ActionGroup.INFO })
        assertTrue(CommandRepository.allActions.any { !it.isAdb && it.group == ActionGroup.UNLOCK })
        assertTrue(CommandRepository.allActions.any { !it.isAdb && it.group == ActionGroup.SERVICE })
    }

    @Test
    fun `verify partition inference for Android 10 to 16 images`() {
        assertEquals("init_boot", FlashFileProcessor.inferPartition("init_boot_magisk.img"))
        assertEquals("vendor_boot", FlashFileProcessor.inferPartition("vendor_boot.img"))
        assertEquals("vbmeta", FlashFileProcessor.inferPartition("vbmeta.img"))
        assertEquals("super", FlashFileProcessor.inferPartition("super_empty.img"))
        assertEquals("boot", FlashFileProcessor.inferPartition("boot_a.img"))
    }

    @Test
    fun `verify viewModel strict drawer mode isolation and brand filtering`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = ServiceViewModel(app)

        vm.navigateToMode(ToolScreenMode.FASTBOOT_INFO)
        vm.setBrand(ServiceBrand.XIAOMI)
        val xiaomiFbInfo = vm.getAvailableActions()
        assertTrue(xiaomiFbInfo.isNotEmpty())
        assertTrue(xiaomiFbInfo.all { it.brand == ServiceBrand.XIAOMI && !it.isAdb && it.group == ActionGroup.INFO })

        vm.navigateToMode(ToolScreenMode.FASTBOOT_UNLOCK)
        vm.setBrand(ServiceBrand.VIVO)
        val vivoFbUnlock = vm.getAvailableActions()
        assertTrue(vivoFbUnlock.isNotEmpty())
        assertTrue(vivoFbUnlock.all { it.brand == ServiceBrand.VIVO && !it.isAdb && it.group == ActionGroup.UNLOCK })

        vm.navigateToMode(ToolScreenMode.ADB_INFO)
        vm.setBrand(ServiceBrand.SAMSUNG)
        val samsungAdbInfo = vm.getAvailableActions()
        assertTrue(samsungAdbInfo.isNotEmpty())
        assertTrue(samsungAdbInfo.all { it.brand == ServiceBrand.SAMSUNG && it.isAdb && it.group == ActionGroup.INFO })

        vm.navigateToMode(ToolScreenMode.ADB_UNLOCK)
        vm.setBrand(ServiceBrand.UNIVERSAL)
        val uniAdbUnlock = vm.getAvailableActions()
        assertTrue(uniAdbUnlock.isNotEmpty())
        assertTrue(uniAdbUnlock.all { it.brand == ServiceBrand.UNIVERSAL && it.isAdb && it.group == ActionGroup.UNLOCK })

        vm.navigateToMode(ToolScreenMode.ADB_SERVICE)
        vm.setBrand(ServiceBrand.UNIVERSAL)
        val uniAdbService = vm.getAvailableActions()
        assertTrue(uniAdbService.isNotEmpty())
        assertTrue(uniAdbService.all { it.brand == ServiceBrand.UNIVERSAL && it.isAdb && it.group == ActionGroup.SERVICE })

        vm.navigateToMode(ToolScreenMode.SETTINGS)
        assertEquals(ToolScreenMode.SETTINGS, vm.uiState.value.screenMode)
        vm.navigateToMode(ToolScreenMode.ABOUT)
        assertEquals(ToolScreenMode.ABOUT, vm.uiState.value.screenMode)
    }

    @Test
    fun `verify isolated cli terminal buffer and clean console logs without noise`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = ServiceViewModel(app)

        // Switching drawer modes or brands must not spam the Live Log Console
        vm.navigateToMode(ToolScreenMode.ADB_INFO)
        vm.setBrand(ServiceBrand.XIAOMI)
        vm.navigateToMode(ToolScreenMode.CMD_TERMINAL)
        assertTrue(vm.uiState.value.terminalLines.isEmpty())
        assertTrue(vm.uiState.value.consoleLogs.isEmpty())

        vm.submitTerminalCommand("help")
        assertTrue(vm.uiState.value.terminalLines.size >= 2)
        assertEquals("~ $ help", vm.uiState.value.terminalLines.first().text)

        vm.submitTerminalCommand("clear")
        assertTrue(vm.uiState.value.terminalLines.isEmpty())
    }

    @Test
    fun `verify console logs never expose raw CLI command strings on action execution`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = ServiceViewModel(app)

        vm.navigateToMode(ToolScreenMode.FASTBOOT_INFO)
        vm.setBrand(ServiceBrand.UNIVERSAL)
        val firstAction = vm.getAvailableActions().first()
        vm.setSelectedAction(firstAction)
        vm.onStartPressed()

        // Allow coroutine to log start/fail cleanly
        Thread.sleep(150)
        val allMsgs = vm.uiState.value.consoleLogs.joinToString("\n") { it.message }
        assertTrue(allMsgs.contains(firstAction.title))
        assertTrue(!allMsgs.contains(firstAction.command))
    }

    @Test
    fun `verify theme mode, 3 theme designs, 3 font sizes, and sound toggle update cleanly`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = ServiceViewModel(app)

        vm.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, vm.uiState.value.themeMode)
        vm.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, vm.uiState.value.themeMode)

        vm.setThemeDesign(ThemeDesign.EMERALD_MATRIX)
        assertEquals(ThemeDesign.EMERALD_MATRIX, vm.uiState.value.themeDesign)
        vm.setThemeDesign(ThemeDesign.CRIMSON_FORGE)
        assertEquals(ThemeDesign.CRIMSON_FORGE, vm.uiState.value.themeDesign)
        vm.setThemeDesign(ThemeDesign.CYBER_NEON)
        assertEquals(ThemeDesign.CYBER_NEON, vm.uiState.value.themeDesign)

        vm.setFontSize(AppFontSize.COMPACT)
        assertEquals(AppFontSize.COMPACT, vm.uiState.value.fontSize)
        vm.setFontSize(AppFontSize.LARGE)
        assertEquals(AppFontSize.LARGE, vm.uiState.value.fontSize)

        assertTrue(vm.uiState.value.soundEnabled)
        vm.setSoundEnabled(false)
        assertTrue(!vm.uiState.value.soundEnabled)
        vm.setSoundEnabled(true)
        assertTrue(vm.uiState.value.soundEnabled)
    }

    @Test
    fun `verify ADB RSA public key encodes exact 524-byte struct and signs 20-byte token validly`() {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val kp = kpg.generateKeyPair()
        val pub = kp.public as RSAPublicKey

        val payload = UsbPortController.encodeAdbPublicKeyPayload(pub)
        assertEquals(0.toByte(), payload.last())
        val payloadStr = String(payload, 0, payload.size - 1, StandardCharsets.US_ASCII)
        assertTrue(payloadStr.endsWith(" fastdeck@android"))
        val b64 = payloadStr.substringBefore(" ")
        val decoded = Base64.decode(b64, Base64.NO_WRAP)
        assertEquals(524, decoded.size)

        val bb = ByteBuffer.wrap(decoded).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(64, bb.int)
        val n0inv = bb.int.toLong() and 0xFFFFFFFFL

        val nBytesLE = ByteArray(256).also { bb.get(it) }
        val rrBytesLE = ByteArray(256).also { bb.get(it) }
        val exponent = bb.int
        assertEquals(pub.publicExponent.toInt(), exponent)

        val nFromStruct = BigInteger(1, nBytesLE.reversedArray())
        val rrFromStruct = BigInteger(1, rrBytesLE.reversedArray())
        assertEquals(pub.modulus, nFromStruct)
        val expectedRr = BigInteger.ONE.shiftLeft(4096).mod(pub.modulus)
        assertEquals(expectedRr, rrFromStruct)

        val r32 = BigInteger.ONE.shiftLeft(32)
        val n0 = pub.modulus.mod(r32).toLong()
        val prodMod = (BigInteger.valueOf(n0).multiply(BigInteger.valueOf(n0inv))).mod(r32)
        assertEquals(r32.subtract(BigInteger.ONE), prodMod)

        val token = ByteArray(20) { (it + 7).toByte() }
        val sig = UsbPortController.signAdbTokenPayload(kp.private, token)
        assertEquals(256, sig.size)

        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, pub)
        val recovered = cipher.doFinal(sig)
        val expectedDigestInfo = byteArrayOf(
            0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2B, 0x0E,
            0x03, 0x02, 0x1A, 0x05, 0x00, 0x04, 0x14
        ) + token
        assertArrayEquals(expectedDigestInfo, recovered)
    }
}
