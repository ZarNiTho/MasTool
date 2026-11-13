package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CommandRepository
import com.example.data.FlashFileProcessor
import com.example.model.ActionGroup
import com.example.model.ServiceBrand
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
        assertTrue(CommandRepository.allActions.all { it.command.isNotBlank() && it.title.isNotBlank() })
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
    fun `verify viewModel brand filtering and action selection`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = ServiceViewModel(app)

        vm.setBrand(ServiceBrand.XIAOMI)
        vm.setActionGroup(ActionGroup.BRAND_OEM)
        val xiaomiActions = vm.getAvailableActions()
        assertTrue(xiaomiActions.isNotEmpty())
        assertTrue(xiaomiActions.all { it.brand == ServiceBrand.XIAOMI })

        vm.setBrand(ServiceBrand.HUAWEI)
        val huaweiActions = vm.getAvailableActions()
        assertTrue(huaweiActions.isNotEmpty())
        assertTrue(huaweiActions.all { it.brand == ServiceBrand.HUAWEI })
    }
}
