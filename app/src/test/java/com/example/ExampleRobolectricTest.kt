package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CommandRepository
import com.example.data.FlashFileProcessor
import com.example.model.ActionGroup
import com.example.model.ServiceBrand
import com.example.model.ToolScreenMode
import com.example.ui.ServiceViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `verify app name and native command repository`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FastDeck OTG", appName)

        assertTrue(CommandRepository.allActions.size >= 65)
        assertTrue(
            CommandRepository.allActions.all {
                it.command.isNotBlank() && it.title.isNotBlank() && it.description.isNotBlank()
            }
        )
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
    fun `verify viewModel strict mode isolation and brand filtering`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = ServiceViewModel(app)

        // Fastboot tab must contain ONLY Fastboot actions (!isAdb)
        vm.navigateToMode(ToolScreenMode.FASTBOOT_ONECLICK)
        vm.setBrand(ServiceBrand.XIAOMI)
        vm.setActionGroup(ActionGroup.BRAND_OEM)
        val xiaomiFbActions = vm.getAvailableActions()
        assertTrue(xiaomiFbActions.isNotEmpty())
        assertTrue(xiaomiFbActions.all { it.brand == ServiceBrand.XIAOMI && !it.isAdb })

        // ADB tab must contain ONLY ADB actions (isAdb)
        vm.navigateToMode(ToolScreenMode.ADB_ONECLICK)
        vm.setBrand(ServiceBrand.XIAOMI)
        val xiaomiAdbActions = vm.getAvailableActions()
        assertTrue(xiaomiAdbActions.isNotEmpty())
        assertTrue(xiaomiAdbActions.all { it.brand == ServiceBrand.XIAOMI && it.isAdb })

        vm.setBrand(ServiceBrand.HUAWEI)
        val huaweiAdbActions = vm.getAvailableActions()
        assertTrue(huaweiAdbActions.isNotEmpty())
        assertTrue(huaweiAdbActions.all { it.brand == ServiceBrand.HUAWEI && it.isAdb })
    }

    @Test
    fun `verify isolated cli terminal buffer starts empty without console logs`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = ServiceViewModel(app)

        vm.navigateToMode(ToolScreenMode.CMD_TERMINAL)
        // Terminal must start completely empty (no INIT / NAV / USB logs)
        assertTrue(vm.uiState.value.terminalLines.isEmpty())
        assertTrue(vm.uiState.value.consoleLogs.isNotEmpty())

        vm.submitTerminalCommand("help")
        assertTrue(vm.uiState.value.terminalLines.size >= 2)
        assertEquals("~ $ help", vm.uiState.value.terminalLines.first().text)

        vm.submitTerminalCommand("clear")
        assertTrue(vm.uiState.value.terminalLines.isEmpty())
    }
}
