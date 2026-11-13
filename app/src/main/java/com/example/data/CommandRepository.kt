package com.example.data

import com.example.model.ActionGroup
import com.example.model.ServiceActionItem
import com.example.model.ServiceBrand

object CommandRepository {

    val supportedPartitions: List<String> = listOf(
        "Auto Detect (from file)",
        "boot (Kernel / Ramdisk)",
        "init_boot (Android 13–16 Ramdisk)",
        "vendor_boot (Android 11–16 Vendor)",
        "vbmeta (AVB 2.0 Verification)",
        "vbmeta_system (System AVB)",
        "vbmeta_vendor (Vendor AVB)",
        "super (Dynamic Partition A10–A16)",
        "recovery (Recovery Image)",
        "dtbo (Device Tree Overlay)",
        "system (Logical Partition)",
        "vendor (Logical Partition)",
        "product (Logical Partition)",
        "system_ext (Logical Partition)",
        "odm (Logical Partition)",
        "modem (Baseband Radio)",
        "persist (Sensor & MAC Persist)",
        "userdata (User Storage)",
        "metadata (FBE Encryption)"
    )

    val allActions: List<ServiceActionItem> = listOf(
        // ==================== UNIVERSAL FASTBOOT & FASTBOOTD (A10–A16) ====================
        ServiceActionItem(
            id = "uni_fb_getvar_all",
            title = "[Universal] Read All Fastboot Variables (getvar all)",
            command = "fastboot getvar all",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.INFO_GETVAR
        ),
        ServiceActionItem(
            id = "uni_fb_userspace",
            title = "[Universal] Check Fastbootd Userspace Status (is-userspace)",
            command = "fastboot getvar is-userspace",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.DYNAMIC_SLOT
        ),
        ServiceActionItem(
            id = "uni_fb_slot_check",
            title = "[Universal] Read Current Active Slot (current-slot)",
            command = "fastboot getvar current-slot",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.DYNAMIC_SLOT
        ),
        ServiceActionItem(
            id = "uni_fb_slot_a",
            title = "[Universal] Set Active Boot Slot -> SLOT A",
            command = "fastboot set_active a",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.DYNAMIC_SLOT
        ),
        ServiceActionItem(
            id = "uni_fb_slot_b",
            title = "[Universal] Set Active Boot Slot -> SLOT B",
            command = "fastboot set_active b",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.DYNAMIC_SLOT
        ),
        ServiceActionItem(
            id = "uni_fb_enter_fastbootd",
            title = "[Universal] Enter Android 10–16 Userspace Fastbootd",
            command = "fastboot reboot fastboot",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.DYNAMIC_SLOT
        ),
        ServiceActionItem(
            id = "uni_fb_snap_cancel",
            title = "[Universal] Cancel Virtual A/B OTA Snapshot (A11–A16)",
            command = "fastboot snapshot-update cancel",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.DYNAMIC_SLOT
        ),
        ServiceActionItem(
            id = "uni_fb_snap_merge",
            title = "[Universal] Force Merge Virtual A/B Snapshot (A11–A16)",
            command = "fastboot snapshot-update merge",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.DYNAMIC_SLOT
        ),
        ServiceActionItem(
            id = "uni_fb_create_logical",
            title = "[Universal] Create Logical Partition system_a (Fastbootd)",
            command = "fastboot create-logical-partition system_a 3221225472",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.DYNAMIC_SLOT
        ),
        ServiceActionItem(
            id = "uni_fb_resize_logical",
            title = "[Universal] Resize Logical Partition system_a (Fastbootd)",
            command = "fastboot resize-logical-partition system_a 3221225472",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.DYNAMIC_SLOT
        ),
        ServiceActionItem(
            id = "uni_fb_delete_logical",
            title = "[Universal] Delete Inactive Logical Partition product_b",
            command = "fastboot delete-logical-partition product_b",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.DYNAMIC_SLOT
        ),
        ServiceActionItem(
            id = "uni_fb_unlock",
            title = "[Universal] Bootloader Flashing Unlock (Standard A10–A16)",
            command = "fastboot flashing unlock",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.UNLOCK_LOCK
        ),
        ServiceActionItem(
            id = "uni_fb_unlock_crit",
            title = "[Universal] Bootloader Unlock Critical Partitions",
            command = "fastboot flashing unlock_critical",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.UNLOCK_LOCK
        ),
        ServiceActionItem(
            id = "uni_fb_lock",
            title = "[Universal] Relock Bootloader (flashing lock)",
            command = "fastboot flashing lock",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.UNLOCK_LOCK
        ),
        ServiceActionItem(
            id = "uni_fb_unlock_ability",
            title = "[Universal] Check OEM Unlock Ability Flag",
            command = "fastboot flashing get_unlock_ability",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.UNLOCK_LOCK
        ),
        ServiceActionItem(
            id = "uni_fb_wipe_all",
            title = "[Universal] Format Userdata + FBE Metadata (fastboot -w)",
            command = "fastboot -w",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.WIPE_ERASE
        ),
        ServiceActionItem(
            id = "uni_fb_erase_userdata",
            title = "[Universal] Erase Userdata Partition Only",
            command = "fastboot erase userdata",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.WIPE_ERASE
        ),
        ServiceActionItem(
            id = "uni_fb_erase_metadata",
            title = "[Universal] Erase Android 10–16 FBE Metadata Partition",
            command = "fastboot erase metadata",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.WIPE_ERASE
        ),
        ServiceActionItem(
            id = "uni_fb_erase_cache",
            title = "[Universal] Erase Cache Partition",
            command = "fastboot erase cache",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.WIPE_ERASE
        ),
        ServiceActionItem(
            id = "uni_fb_erase_persist",
            title = "[Universal] Erase Persist Partition",
            command = "fastboot erase persist",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.WIPE_ERASE
        ),
        ServiceActionItem(
            id = "uni_fb_erase_modemst",
            title = "[Universal] Erase Baseband NV (modemst1 & modemst2)",
            command = "fastboot erase modemst1",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.WIPE_ERASE
        ),
        ServiceActionItem(
            id = "uni_fb_reboot_sys",
            title = "[Universal] Fastboot Reboot to System",
            command = "fastboot reboot",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.REBOOT_MODES
        ),
        ServiceActionItem(
            id = "uni_fb_reboot_bl",
            title = "[Universal] Fastboot Reboot to Bootloader",
            command = "fastboot reboot-bootloader",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.REBOOT_MODES
        ),
        ServiceActionItem(
            id = "uni_fb_reboot_rec",
            title = "[Universal] Fastboot Reboot to Recovery",
            command = "fastboot reboot recovery",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.REBOOT_MODES
        ),
        ServiceActionItem(
            id = "uni_fb_continue",
            title = "[Universal] Fastboot Continue Boot",
            command = "fastboot continue",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.REBOOT_MODES
        ),

        // ==================== XIAOMI / REDMI / POCO SPECIFIC ====================
        ServiceActionItem(
            id = "xm_oem_info",
            title = "[Xiaomi] Read Bootloader Lock & Device Info (oem device-info)",
            command = "fastboot oem device-info",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "xm_anti_rollback",
            title = "[Xiaomi] Check Anti-Rollback Protection Index (getvar anti)",
            command = "fastboot getvar anti",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "xm_read_token",
            title = "[Xiaomi] Read Mi Unlock Bootloader Token (getvar token)",
            command = "fastboot getvar token",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "xm_product_codename",
            title = "[Xiaomi] Read Exact Board Codename (getvar product)",
            command = "fastboot getvar product",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "xm_oem_edl",
            title = "[Xiaomi] Switch Fastboot to Qualcomm EDL 9008 (oem edl)",
            command = "fastboot oem edl",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "xm_oem_lks",
            title = "[Xiaomi] Check Bootloader LKS Lock Status (oem lks)",
            command = "fastboot oem lks",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "xm_oem_poweroff",
            title = "[Xiaomi] Power Off Phone from Fastboot (oem poweroff)",
            command = "fastboot oem poweroff",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "xm_adb_miui_ver",
            title = "[Xiaomi ADB] Read MIUI / HyperOS Build Version",
            command = "getprop ro.miui.ui.version.name",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.BRAND_OEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "xm_adb_disable_msa",
            title = "[Xiaomi ADB] Disable MIUI System Ads Daemon (MSA)",
            command = "pm disable-user --user 0 com.miui.msa.global",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.BRAND_OEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "xm_adb_disable_analytics",
            title = "[Xiaomi ADB] Disable Xiaomi Analytics Tracker",
            command = "pm disable-user --user 0 com.miui.analytics",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.BRAND_OEM,
            isAdb = true
        ),

        // ==================== HUAWEI / HONOR SPECIFIC ====================
        ServiceActionItem(
            id = "hw_bootinfo",
            title = "[Huawei] Read Bootloader & FRP Lock Status (oem get-bootinfo)",
            command = "fastboot oem get-bootinfo",
            brand = ServiceBrand.HUAWEI,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "hw_build_num",
            title = "[Huawei] Read Firmware Build Version (oem get-build-number)",
            command = "fastboot oem get-build-number",
            brand = ServiceBrand.HUAWEI,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "hw_model",
            title = "[Huawei] Read Exact Product Model (oem get-product-model)",
            command = "fastboot oem get-product-model",
            brand = ServiceBrand.HUAWEI,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "hw_psid",
            title = "[Huawei] Read Serial & IMEI PSID (oem get-psid)",
            command = "fastboot oem get-psid",
            brand = ServiceBrand.HUAWEI,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "hw_custom_ver",
            title = "[Huawei] Read Vendor & Country Cust Info (oeminforead-CUSTOM_VERSION)",
            command = "fastboot oem oeminforead-CUSTOM_VERSION",
            brand = ServiceBrand.HUAWEI,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "hw_backdoor_info",
            title = "[Huawei] Read Factory Backdoor / FBlock State (oem backdoor info)",
            command = "fastboot oem backdoor info",
            brand = ServiceBrand.HUAWEI,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "hw_reboot_erecovery",
            title = "[Huawei] Reboot to Huawei eRecovery Mode",
            command = "fastboot oem reboot-erecovery",
            brand = ServiceBrand.HUAWEI,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "hw_adb_emui_ver",
            title = "[Huawei ADB] Read EMUI / HarmonyOS Version",
            command = "getprop ro.build.version.emui",
            brand = ServiceBrand.HUAWEI,
            group = ActionGroup.BRAND_OEM,
            isAdb = true
        ),

        // ==================== VIVO / IQOO SPECIFIC ====================
        ServiceActionItem(
            id = "vv_bbk_unlock",
            title = "[Vivo] BBK Bootloader Unlock (fastboot bbk unlock_vivo)",
            command = "fastboot bbk unlock_vivo",
            brand = ServiceBrand.VIVO,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "vv_bbk_lock",
            title = "[Vivo] BBK Bootloader Lock (fastboot bbk lock_vivo)",
            command = "fastboot bbk lock_vivo",
            brand = ServiceBrand.VIVO,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "vv_bsp_unlock",
            title = "[Vivo] Vivo BSP Bootloader Unlock (vivo_bsp unlock_vivo)",
            command = "fastboot vivo_bsp unlock_vivo",
            brand = ServiceBrand.VIVO,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "vv_bsp_lock",
            title = "[Vivo] Vivo BSP Bootloader Relock (vivo_bsp lock_vivo)",
            command = "fastboot vivo_bsp lock_vivo",
            brand = ServiceBrand.VIVO,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "vv_oem_edl",
            title = "[Vivo] Enter Qualcomm EDL Mode (oem reboot-edl)",
            command = "fastboot oem reboot-edl",
            brand = ServiceBrand.VIVO,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "vv_adb_os_ver",
            title = "[Vivo ADB] Read FuntouchOS / OriginOS Build ID",
            command = "getprop ro.vivo.os.build.display.id",
            brand = ServiceBrand.VIVO,
            group = ActionGroup.BRAND_OEM,
            isAdb = true
        ),

        // ==================== OPPO / REALME / ONEPLUS SPECIFIC ====================
        ServiceActionItem(
            id = "op_device_info",
            title = "[Oppo/OnePlus] Read OEM Bootloader State (oem device-info)",
            command = "fastboot oem device-info",
            brand = ServiceBrand.OPPO_ONEPLUS,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "op_poweroff",
            title = "[Oppo/OnePlus] Power Off from Bootloader (oem poweroff)",
            command = "fastboot oem poweroff",
            brand = ServiceBrand.OPPO_ONEPLUS,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "op_serial",
            title = "[Oppo/OnePlus] Read PCBA / Chip Serial (getvar serialno)",
            command = "fastboot getvar serialno",
            brand = ServiceBrand.OPPO_ONEPLUS,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "op_adb_coloros",
            title = "[Oppo/Realme ADB] Read ColorOS / OxygenOS Version",
            command = "getprop ro.build.version.oplusrom",
            brand = ServiceBrand.OPPO_ONEPLUS,
            group = ActionGroup.BRAND_OEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "op_adb_region",
            title = "[Oppo/Realme ADB] Read NV Pipeline Region Code",
            command = "getprop ro.oplus.pipeline.region",
            brand = ServiceBrand.OPPO_ONEPLUS,
            group = ActionGroup.BRAND_OEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "op_adb_debloat_heytap",
            title = "[Oppo/Realme ADB] Disable HeyTap Cloud System Bloat",
            command = "pm disable-user --user 0 com.heytap.cloud",
            brand = ServiceBrand.OPPO_ONEPLUS,
            group = ActionGroup.BRAND_OEM,
            isAdb = true
        ),

        // ==================== SAMSUNG SPECIFIC ====================
        ServiceActionItem(
            id = "sam_adb_download",
            title = "[Samsung ADB] Reboot to Odin Download Mode (adb reboot download)",
            command = "adb reboot download",
            brand = ServiceBrand.SAMSUNG,
            group = ActionGroup.BRAND_OEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "sam_adb_csc",
            title = "[Samsung ADB] Read Active CSC Country Code (ro.csc.sales_code)",
            command = "getprop ro.csc.sales_code",
            brand = ServiceBrand.SAMSUNG,
            group = ActionGroup.BRAND_OEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "sam_adb_bl_ver",
            title = "[Samsung ADB] Read Bootloader & Binary Bit (ro.boot.bootloader)",
            command = "getprop ro.boot.bootloader",
            brand = ServiceBrand.SAMSUNG,
            group = ActionGroup.BRAND_OEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "sam_adb_knox_bit",
            title = "[Samsung ADB] Check Knox Warranty Bit (0x0 / 0x1)",
            command = "getprop ro.boot.warranty_bit",
            brand = ServiceBrand.SAMSUNG,
            group = ActionGroup.BRAND_OEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "sam_adb_disable_ota",
            title = "[Samsung ADB] Disable Samsung Software Update (com.wssyncmldm)",
            command = "pm disable-user --user 0 com.wssyncmldm",
            brand = ServiceBrand.SAMSUNG,
            group = ActionGroup.BRAND_OEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "sam_fb_userspace",
            title = "[Samsung Fastbootd] Check Userspace Fastbootd Status",
            command = "fastboot getvar is-userspace",
            brand = ServiceBrand.SAMSUNG,
            group = ActionGroup.BRAND_OEM
        ),

        // ==================== MOTOROLA / GOOGLE PIXEL SPECIFIC ====================
        ServiceActionItem(
            id = "moto_unlock_data",
            title = "[Motorola] Read OEM Unlock Data String (oem get_unlock_data)",
            command = "fastboot oem get_unlock_data",
            brand = ServiceBrand.MOTO_PIXEL,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "moto_fb_mode_set",
            title = "[Motorola] Enable Fastboot Factory Mode (oem fb_mode_set)",
            command = "fastboot oem fb_mode_set",
            brand = ServiceBrand.MOTO_PIXEL,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "moto_fb_mode_clear",
            title = "[Motorola] Fix Fastboot Loop / Clear Factory Mode (oem fb_mode_clear)",
            command = "fastboot oem fb_mode_clear",
            brand = ServiceBrand.MOTO_PIXEL,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "moto_cid",
            title = "[Motorola] Read Carrier ID & SKU (getvar cid)",
            command = "fastboot getvar cid",
            brand = ServiceBrand.MOTO_PIXEL,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "pixel_bl_ver",
            title = "[Pixel] Read Bootloader & Baseband Version (getvar version-bootloader)",
            command = "fastboot getvar version-bootloader",
            brand = ServiceBrand.MOTO_PIXEL,
            group = ActionGroup.BRAND_OEM
        ),

        // ==================== INFINIX / TECNO / ITEL (TRANSSION) ====================
        ServiceActionItem(
            id = "trn_unlock_ability",
            title = "[Infinix/Tecno] Check Unlock Ability (flashing get_unlock_ability)",
            command = "fastboot flashing get_unlock_ability",
            brand = ServiceBrand.TRANSSION,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "trn_unlock",
            title = "[Infinix/Tecno] Unlock Bootloader (fastboot flashing unlock)",
            command = "fastboot flashing unlock",
            brand = ServiceBrand.TRANSSION,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "trn_fastbootd",
            title = "[Infinix/Tecno] Enter Fastbootd for Super Partition",
            command = "fastboot reboot fastboot",
            brand = ServiceBrand.TRANSSION,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "trn_adb_hios",
            title = "[Infinix/Tecno ADB] Read HiOS / XOS System Build",
            command = "getprop ro.build.display.id",
            brand = ServiceBrand.TRANSSION,
            group = ActionGroup.BRAND_OEM,
            isAdb = true
        ),

        // ==================== ASUS / SONY SPECIFIC ====================
        ServiceActionItem(
            id = "asus_device_info",
            title = "[Asus] Read Bootloader & Root Status (oem device-info)",
            command = "fastboot oem device-info",
            brand = ServiceBrand.ASUS_SONY,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "asus_go",
            title = "[Asus] Execute Asus OEM Unlock Trigger (oem asus-go)",
            command = "fastboot oem asus-go",
            brand = ServiceBrand.ASUS_SONY,
            group = ActionGroup.BRAND_OEM
        ),
        ServiceActionItem(
            id = "sony_baseband",
            title = "[Sony] Read Xperia Baseband & Bootloader Info",
            command = "fastboot getvar version-baseband",
            brand = ServiceBrand.ASUS_SONY,
            group = ActionGroup.BRAND_OEM
        ),

        // ==================== UNIVERSAL ADB SYSTEM & SERVICE ACTIONS ====================
        ServiceActionItem(
            id = "adb_devices",
            title = "[ADB] Check Connected ADB Devices (adb devices -l)",
            command = "adb devices -l",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.ADB_SYSTEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_getprop_all",
            title = "[ADB] Read All Android 10–16 System Properties (getprop)",
            command = "getprop",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.ADB_SYSTEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_avb_state",
            title = "[ADB] Check Verified Boot AVB 2.0 State (ro.boot.verifiedbootstate)",
            command = "getprop ro.boot.verifiedbootstate",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.ADB_SYSTEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_slot_suffix",
            title = "[ADB] Check Active A/B Boot Slot (ro.boot.slot_suffix)",
            command = "getprop ro.boot.slot_suffix",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.ADB_SYSTEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_dyn_parts",
            title = "[ADB] Check Dynamic Partitions Enabled (ro.boot.dynamic_partitions)",
            command = "getprop ro.boot.dynamic_partitions",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.ADB_SYSTEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_sec_patch",
            title = "[ADB] Read Android Security Patch Level",
            command = "getprop ro.build.version.security_patch",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.ADB_SYSTEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_battery",
            title = "[ADB] Read Battery Health, Level & Temperature (dumpsys battery)",
            command = "dumpsys battery",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.ADB_SYSTEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_dexopt",
            title = "[ADB] Run ART Speed Compiler Optimization (bg-dexopt-job)",
            command = "cmd package bg-dexopt-job",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.ADB_SYSTEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_pkg_all",
            title = "[ADB] List All Installed System & User Packages",
            command = "pm list packages",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.ADB_SYSTEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_pkg_3rd",
            title = "[ADB] List Third-Party Installed Apps Only (pm list packages -3)",
            command = "pm list packages -3",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.ADB_SYSTEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_pkg_disabled",
            title = "[ADB] List Disabled Packages (pm list packages -d)",
            command = "pm list packages -d",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.ADB_SYSTEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_disk_free",
            title = "[ADB] Inspect Partition Mounts & Free Space (df -h)",
            command = "df -h",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.ADB_SYSTEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_kernel_uname",
            title = "[ADB] Read Linux Kernel Version & Architecture (uname -a)",
            command = "uname -a",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.ADB_SYSTEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_wm_reset",
            title = "[ADB] Reset Screen DPI & Size to Factory Default",
            command = "wm density reset",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.ADB_SYSTEM,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_reboot_sys",
            title = "[ADB] Reboot Phone Normally (adb reboot)",
            command = "adb reboot",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.REBOOT_MODES,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_reboot_bl",
            title = "[ADB] Reboot to Fastboot Bootloader (adb reboot bootloader)",
            command = "adb reboot bootloader",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.REBOOT_MODES,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_reboot_fastbootd",
            title = "[ADB] Reboot to Android 10–16 Fastbootd (adb reboot fastboot)",
            command = "adb reboot fastboot",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.REBOOT_MODES,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_reboot_rec",
            title = "[ADB] Reboot to Recovery Mode (adb reboot recovery)",
            command = "adb reboot recovery",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.REBOOT_MODES,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_reboot_sideload",
            title = "[ADB] Reboot to OTA Sideload Mode (adb reboot sideload-auto-reboot)",
            command = "adb reboot sideload-auto-reboot",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.REBOOT_MODES,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_reboot_edl",
            title = "[ADB] Reboot to Qualcomm EDL 9008 Mode (adb reboot edl)",
            command = "adb reboot edl",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.REBOOT_MODES,
            isAdb = true
        )
    )
}
