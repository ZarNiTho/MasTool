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
        // ==================== UNIVERSAL FASTBOOT INFO ====================
        ServiceActionItem(
            id = "uni_fb_getvar_all",
            title = "Read All Bootloader Variables",
            command = "fastboot getvar all",
            description = "ဖုန်း၏ Bootloader အချက်အလက်အားလုံး (Partition, Slot, Lock state, Serial) ကို ဖတ်ယူသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.INFO
        ),
        ServiceActionItem(
            id = "uni_fb_userspace",
            title = "Check Fastbootd Userspace Status",
            command = "fastboot getvar is-userspace",
            description = "လက်ရှိ Mode သည် သာမန် Bootloader လား သို့မဟုတ် Android 10–16 Fastbootd လား စစ်ဆေးသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.INFO
        ),
        ServiceActionItem(
            id = "uni_fb_slot_check",
            title = "Read Current Active Boot Slot",
            command = "fastboot getvar current-slot",
            description = "A/B Partition ဖုန်းများတွင် လက်ရှိ အလုပ်လုပ်နေသော Slot (a သို့မဟုတ် b) ကို စစ်ဆေးသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.INFO
        ),
        ServiceActionItem(
            id = "uni_fb_unlock_ability",
            title = "Check OEM Unlock Permission Flag",
            command = "fastboot flashing get_unlock_ability",
            description = "Developer Options ထဲတွင် OEM Unlock ဖွင့်ထားခြင်း ရှိ/မရှိ စစ်ဆေးသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.INFO
        ),

        // ==================== UNIVERSAL FASTBOOT UNLOCK ====================
        ServiceActionItem(
            id = "uni_fb_unlock",
            title = "Unlock Bootloader (Standard A10–A16)",
            command = "fastboot flashing unlock",
            description = "Android 10 မှ 16 အထိ ဖုန်းများ၏ Bootloader ကို တရားဝင် Unlock ပြုလုပ်သည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.UNLOCK
        ),
        ServiceActionItem(
            id = "uni_fb_unlock_crit",
            title = "Unlock Critical Bootloader Partitions",
            command = "fastboot flashing unlock_critical",
            description = "Bootloader နှင့်အတူ Preloader/ABL စသည့် Critical Partition များကိုပါ Unlock လုပ်သည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.UNLOCK
        ),
        ServiceActionItem(
            id = "uni_fb_lock",
            title = "Relock Bootloader",
            command = "fastboot flashing lock",
            description = "Bootloader ပွင့်နေသော ဖုန်းကို မူလအတိုင်း ပြန်လည် Lock ခတ်သည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.UNLOCK
        ),
        ServiceActionItem(
            id = "uni_fb_erase_frp",
            title = "Erase FRP Lock Partition",
            command = "fastboot erase frp",
            description = "Fastboot မှတစ်ဆင့် Google Account (FRP) Partition ကို တိုက်ရိုက် ရှင်းလင်းသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.UNLOCK
        ),

        // ==================== UNIVERSAL FASTBOOT SERVICE ====================
        ServiceActionItem(
            id = "uni_fb_slot_a",
            title = "Set Active Boot Slot -> SLOT A",
            command = "fastboot set_active a",
            description = "ဖုန်း Boot တက်မည့် Active Slot ကို Slot A သို့ ပြောင်းလဲသတ်မှတ်သည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "uni_fb_slot_b",
            title = "Set Active Boot Slot -> SLOT B",
            command = "fastboot set_active b",
            description = "ဖုန်း Boot တက်မည့် Active Slot ကို Slot B သို့ ပြောင်းလဲသတ်မှတ်သည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "uni_fb_enter_fastbootd",
            title = "Switch to Userspace Fastbootd Mode",
            command = "fastboot reboot fastboot",
            description = "Dynamic Partition (super/system/vendor) များ ရေးသားနိုင်ရန် Fastbootd သို့ ပြောင်းသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "uni_fb_snap_cancel",
            title = "Cancel Virtual A/B OTA Snapshot",
            command = "fastboot snapshot-update cancel",
            description = "Android 11–16 တွင် OTA Update တန်းလန်းဖြစ်နေသော Virtual A/B Snapshot ကို ပယ်ဖျက်သည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "uni_fb_snap_merge",
            title = "Force Merge Virtual A/B Snapshot",
            command = "fastboot snapshot-update merge",
            description = "Android 11–16 Virtual A/B Snapshot Partition များကို အပြီးသတ် ပေါင်းစည်းပေးသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "uni_fb_create_logical",
            title = "Create Logical Partition system_a",
            command = "fastboot create-logical-partition system_a 3221225472",
            description = "Fastbootd အတွင်း၌ Dynamic Logical Partition အသစ် (system_a) တည်ဆောက်သည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "uni_fb_resize_logical",
            title = "Resize Logical Partition system_a",
            command = "fastboot resize-logical-partition system_a 3221225472",
            description = "Fastbootd အတွင်း၌ Logical Partition (system_a) အရွယ်အစားကို ပြန်ညှိသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "uni_fb_delete_logical",
            title = "Delete Inactive Logical Partition product_b",
            command = "fastboot delete-logical-partition product_b",
            description = "Super Partition နေရာလွတ်ရစေရန် မသုံးသော Logical Partition (product_b) ကို ဖျက်သည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "uni_fb_wipe_all",
            title = "Factory Reset (Wipe Userdata + Metadata)",
            command = "fastboot -w",
            description = "Userdata နှင့် Android 10–16 FBE Encryption Metadata နှစ်ခုလုံးကို အပြီးရှင်းလင်းသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "uni_fb_erase_userdata",
            title = "Erase Userdata Partition Only",
            command = "fastboot erase userdata",
            description = "ဖုန်းအတွင်းရှိ User Data ကိုသာ သီးသန့် ဖျက်ထုတ်သည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "uni_fb_erase_metadata",
            title = "Erase FBE Encryption Metadata",
            command = "fastboot erase metadata",
            description = "Android 10–16 File-Based Encryption (FBE) Key Metadata Partition ကို ဖျက်သည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "uni_fb_erase_cache",
            title = "Erase Cache Partition",
            command = "fastboot erase cache",
            description = "System Cache Partition ကို ရှင်းလင်းသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "uni_fb_erase_persist",
            title = "Erase Persist Sensor Partition",
            command = "fastboot erase persist",
            description = "Sensor နှင့် Account/Calibration သိမ်းဆည်းသော Persist Partition ကို ရှင်းလင်းသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "uni_fb_erase_modemst",
            title = "Reset Baseband NV (Modemst1 & Modemst2)",
            command = "fastboot erase modemst1",
            description = "Qualcomm Baseband EFS Cache (modemst1 နှင့် modemst2) ကို Reset ချသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "uni_fb_reboot_sys",
            title = "Reboot to Normal System",
            command = "fastboot reboot",
            description = "Fastboot Mode မှ သာမန်ဖုန်းမျက်နှာပြင်သို့ ပြန်လည် Reboot ချသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "uni_fb_reboot_bl",
            title = "Reboot Back to Bootloader",
            command = "fastboot reboot-bootloader",
            description = "Fastboot Bootloader Mode သို့ ပြန်လည် Reboot ချသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "uni_fb_reboot_rec",
            title = "Reboot to Recovery Mode",
            command = "fastboot reboot recovery",
            description = "Fastboot မှ Recovery Mode သို့ တိုက်ရိုက် ဝင်ရောက်သည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "uni_fb_continue",
            title = "Continue Normal System Boot",
            command = "fastboot continue",
            description = "ဖုန်းကို ပိတ်မချဘဲ လက်ရှိ Kernel မှ ဆက်လက် Boot တက်စေသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE
        ),

        // ==================== XIAOMI / REDMI / POCO SPECIFIC ====================
        ServiceActionItem(
            id = "xm_oem_info",
            title = "Read Bootloader Lock & Device Status",
            command = "fastboot oem device-info",
            description = "Xiaomi ဖုန်း၏ Bootloader Unlock ဖြစ်/မဖြစ် နှင့် Critical Lock အခြေအနေကို ဖတ်သည်။",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.INFO
        ),
        ServiceActionItem(
            id = "xm_anti_rollback",
            title = "Check Anti-Rollback Index",
            command = "fastboot getvar anti",
            description = "Xiaomi Firmware Downgrade လုပ်ရာတွင် Brick မဖြစ်စေရန် ARB Index နံပါတ်ကို စစ်သည်။",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.INFO
        ),
        ServiceActionItem(
            id = "xm_read_token",
            title = "Read Mi Unlock Bootloader Token",
            command = "fastboot getvar token",
            description = "Mi Unlock Tool နှင့် တွဲသုံးရသော Xiaomi Bootloader Token String ကို ထုတ်ယူသည်။",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.UNLOCK
        ),
        ServiceActionItem(
            id = "xm_product_codename",
            title = "Read Exact Board Codename",
            command = "fastboot getvar product",
            description = "Xiaomi ဖုန်း၏ အတိအကျ Model Codename (ဥပမာ marble, houji) ကို စစ်ဆေးသည်။",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.INFO
        ),
        ServiceActionItem(
            id = "xm_oem_lks",
            title = "Check MediaTek LKS Lock State",
            command = "fastboot oem lks",
            description = "MediaTek သုံး Xiaomi/Redmi ဖုန်းများတွင် Bootloader LKS Lock အခြေအနေကို စစ်သည်။",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.INFO
        ),
        ServiceActionItem(
            id = "xm_oem_edl",
            title = "Switch Fastboot to Qualcomm EDL 9008",
            command = "fastboot oem edl",
            description = "Fastboot မှ Qualcomm Emergency Download (9008) Mode သို့ တိုက်ရိုက် ပြောင်းသည်။",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "xm_oem_poweroff",
            title = "Power Off Phone from Fastboot",
            command = "fastboot oem poweroff",
            description = "Fastboot Mode အတွင်းမှ ဖုန်းပါဝါကို လုံးဝပိတ်ချသည်။",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "xm_adb_miui_ver",
            title = "Read MIUI / HyperOS Build Version",
            command = "getprop ro.miui.ui.version.name",
            description = "ADB မှတစ်ဆင့် Xiaomi ဖုန်း၏ MIUI သို့မဟုတ် HyperOS ဗားရှင်းကို ဖတ်ယူသည်။",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.INFO,
            isAdb = true
        ),
        ServiceActionItem(
            id = "xm_adb_region_check",
            title = "Read Xiaomi Region & Market Name",
            command = "getprop ro.miui.region",
            description = "Xiaomi ဖုန်း၏ Region (CN / Global / EU / IN) သတ်မှတ်ချက်ကို ဖတ်ယူသည်။",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.INFO,
            isAdb = true
        ),
        ServiceActionItem(
            id = "xm_adb_disable_finddevice",
            title = "Disable Xiaomi Find Device Service",
            command = "pm disable-user --user 0 com.xiaomi.finddevice",
            description = "Xiaomi Cloud Find Device Background Service ကို ယာယီပိတ်ပင်သည်။",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.UNLOCK,
            isAdb = true
        ),
        ServiceActionItem(
            id = "xm_adb_disable_msa",
            title = "Disable MIUI System Ads Daemon",
            command = "pm disable-user --user 0 com.miui.msa.global",
            description = "MIUI/HyperOS စနစ်အတွင်းရှိ ကြော်ငြာဝန်ဆောင်မှု (MSA) ကို ပိတ်ပင်သည်။",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.SERVICE,
            isAdb = true
        ),
        ServiceActionItem(
            id = "xm_adb_disable_analytics",
            title = "Disable Xiaomi Analytics Tracker",
            command = "pm disable-user --user 0 com.miui.analytics",
            description = "နောက်ခံတွင် အလုပ်လုပ်နေသော Xiaomi Analytics စနစ်ကို ပိတ်ပင်သည်။",
            brand = ServiceBrand.XIAOMI,
            group = ActionGroup.SERVICE,
            isAdb = true
        ),

        // ==================== HUAWEI / HONOR SPECIFIC ====================
        ServiceActionItem(
            id = "hw_bootinfo",
            title = "Read Bootloader & FRP Lock Status",
            command = "fastboot oem get-bootinfo",
            description = "Huawei/Honor ဖုန်း၏ Bootloader Lock နှင့် FRP Lock အခြေအနေကို စစ်ဆေးသည်။",
            brand = ServiceBrand.HUAWEI,
            group = ActionGroup.INFO
        ),
        ServiceActionItem(
            id = "hw_build_num",
            title = "Read Firmware Build Number",
            command = "fastboot oem get-build-number",
            description = "Huawei ဖုန်းတွင် တင်ထားသော လက်ရှိ Firmware Build ဗားရှင်းအပြည့်အစုံကို ဖတ်သည်။",
            brand = ServiceBrand.HUAWEI,
            group = ActionGroup.INFO
        ),
        ServiceActionItem(
            id = "hw_model",
            title = "Read Exact Product Model",
            command = "fastboot oem get-product-model",
            description = "Huawei/Honor ဖုန်း၏ မော်ဒယ်နံပါတ်အမှန်ကို Fastboot မှ ဖတ်ယူသည်။",
            brand = ServiceBrand.HUAWEI,
            group = ActionGroup.INFO
        ),
        ServiceActionItem(
            id = "hw_psid",
            title = "Read Serial & IMEI PSID Info",
            command = "fastboot oem get-psid",
            description = "ဖုန်း၏ Serial Number (SN) နှင့် IMEI/MEID PSID အချက်အလက်ကို ဖတ်ယူသည်။",
            brand = ServiceBrand.HUAWEI,
            group = ActionGroup.INFO
        ),
        ServiceActionItem(
            id = "hw_custom_ver",
            title = "Read Vendor & Country Cust Version",
            command = "fastboot oem oeminforead-CUSTOM_VERSION",
            description = "Firmware တင်ရာတွင် လိုအပ်သော Region/Cust Version (ဥပမာ C432, C636) ကို စစ်သည်။",
            brand = ServiceBrand.HUAWEI,
            group = ActionGroup.INFO
        ),
        ServiceActionItem(
            id = "hw_backdoor_info",
            title = "Read Factory FBlock / Backdoor State",
            command = "fastboot oem backdoor info",
            description = "HiSilicon Kirin ဖုန်းများတွင် FBlock (Factory Backdoor) ပွင့်/မပွင့် စစ်ဆေးသည်။",
            brand = ServiceBrand.HUAWEI,
            group = ActionGroup.UNLOCK
        ),
        ServiceActionItem(
            id = "hw_reboot_erecovery",
            title = "Reboot to Huawei eRecovery Mode",
            command = "fastboot oem reboot-erecovery",
            description = "Fastboot မှ Huawei eRecovery Mode သို့ တိုက်ရိုက် ဝင်ရောက်သည်။",
            brand = ServiceBrand.HUAWEI,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "hw_adb_emui_ver",
            title = "Read EMUI / HarmonyOS Version",
            command = "getprop ro.build.version.emui",
            description = "ADB မှတစ်ဆင့် Huawei/Honor ဖုန်း၏ EMUI သို့မဟုတ် MagicOS ဗားရှင်းကို ဖတ်သည်။",
            brand = ServiceBrand.HUAWEI,
            group = ActionGroup.INFO,
            isAdb = true
        ),
        ServiceActionItem(
            id = "hw_adb_chipset",
            title = "Read Kirin / SoC Platform Hardware",
            command = "getprop ro.hardware",
            description = "Huawei ဖုန်းတွင် သုံးထားသော HiSilicon Kirin သို့မဟုတ် Snapdragon Chip ကို စစ်သည်။",
            brand = ServiceBrand.HUAWEI,
            group = ActionGroup.INFO,
            isAdb = true
        ),
        ServiceActionItem(
            id = "hw_adb_disable_ota",
            title = "Disable Huawei System OTA Updater",
            command = "pm disable-user --user 0 com.huawei.android.hwouc",
            description = "Huawei System Software Update (HWOUC) အလိုအလျောက်တက်ခြင်းကို ပိတ်သည်။",
            brand = ServiceBrand.HUAWEI,
            group = ActionGroup.SERVICE,
            isAdb = true
        ),

        // ==================== VIVO / IQOO SPECIFIC ====================
        ServiceActionItem(
            id = "vv_bbk_unlock",
            title = "BBK Bootloader Unlock",
            command = "fastboot bbk unlock_vivo",
            description = "Vivo ဖုန်းများအား BBK OEM Command ဖြင့် Bootloader Unlock ပြုလုပ်သည်။",
            brand = ServiceBrand.VIVO,
            group = ActionGroup.UNLOCK
        ),
        ServiceActionItem(
            id = "vv_bbk_lock",
            title = "BBK Bootloader Relock",
            command = "fastboot bbk lock_vivo",
            description = "Vivo ဖုန်းများအား BBK OEM Command ဖြင့် Bootloader ပြန်လည် Lock ခတ်သည်။",
            brand = ServiceBrand.VIVO,
            group = ActionGroup.UNLOCK
        ),
        ServiceActionItem(
            id = "vv_bsp_unlock",
            title = "Vivo BSP Bootloader Unlock",
            command = "fastboot vivo_bsp unlock_vivo",
            description = "Vivo မော်ဒယ်အသစ်များတွင် vivo_bsp Command ဖြင့် Bootloader Unlock လုပ်သည်။",
            brand = ServiceBrand.VIVO,
            group = ActionGroup.UNLOCK
        ),
        ServiceActionItem(
            id = "vv_bsp_lock",
            title = "Vivo BSP Bootloader Relock",
            command = "fastboot vivo_bsp lock_vivo",
            description = "Vivo မော်ဒယ်အသစ်များတွင် vivo_bsp Command ဖြင့် Bootloader ပြန်ပိတ်သည်။",
            brand = ServiceBrand.VIVO,
            group = ActionGroup.UNLOCK
        ),
        ServiceActionItem(
            id = "vv_oem_edl",
            title = "Switch Fastboot to Qualcomm EDL Mode",
            command = "fastboot oem reboot-edl",
            description = "Vivo Fastboot Mode မှ Qualcomm 9008 EDL Mode သို့ ပြောင်းလဲသည်။",
            brand = ServiceBrand.VIVO,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "vv_adb_os_ver",
            title = "Read FuntouchOS / OriginOS Version",
            command = "getprop ro.vivo.os.build.display.id",
            description = "ADB မှတစ်ဆင့် Vivo ဖုန်း၏ FuntouchOS သို့မဟုတ် OriginOS Build ကို ဖတ်သည်။",
            brand = ServiceBrand.VIVO,
            group = ActionGroup.INFO,
            isAdb = true
        ),
        ServiceActionItem(
            id = "vv_adb_model_ver",
            title = "Read Vivo Hardware & Product Series",
            command = "getprop ro.vivo.product.model",
            description = "Vivo/iQOO ဖုန်း၏ အတွင်းပိုင်း Product Model အမှန်ကို ADB မှ စစ်ဆေးသည်။",
            brand = ServiceBrand.VIVO,
            group = ActionGroup.INFO,
            isAdb = true
        ),

        // ==================== OPPO / REALME / ONEPLUS SPECIFIC ====================
        ServiceActionItem(
            id = "op_device_info",
            title = "Read OEM Bootloader Lock State",
            command = "fastboot oem device-info",
            description = "Oppo/Realme/OnePlus ဖုန်း၏ Bootloader Lock အခြေအနေကို စစ်ဆေးသည်။",
            brand = ServiceBrand.OPPO_ONEPLUS,
            group = ActionGroup.INFO
        ),
        ServiceActionItem(
            id = "op_serial",
            title = "Read PCBA / Chip Serial Number",
            command = "fastboot getvar serialno",
            description = "ဖုန်း၏ Motherboard Serial Number ကို Fastboot မှ ဖတ်ယူသည်။",
            brand = ServiceBrand.OPPO_ONEPLUS,
            group = ActionGroup.INFO
        ),
        ServiceActionItem(
            id = "op_poweroff",
            title = "Power Off Phone from Bootloader",
            command = "fastboot oem poweroff",
            description = "Bootloader Mode မှ ဖုန်းပါဝါကို တိုက်ရိုက် ပိတ်ချသည်။",
            brand = ServiceBrand.OPPO_ONEPLUS,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "op_adb_coloros",
            title = "Read ColorOS / RealmeUI / OxygenOS Version",
            command = "getprop ro.build.version.oplusrom",
            description = "ADB မှတစ်ဆင့် Oplus ROM (ColorOS/OxygenOS) ဗားရှင်းကို ဖတ်ယူသည်။",
            brand = ServiceBrand.OPPO_ONEPLUS,
            group = ActionGroup.INFO,
            isAdb = true
        ),
        ServiceActionItem(
            id = "op_adb_region",
            title = "Read NV Pipeline Region Code",
            command = "getprop ro.oplus.pipeline.region",
            description = "Oppo/OnePlus ဖုန်း၏ NV Region သတ်မှတ်ချက်ကို စစ်ဆေးသည်။",
            brand = ServiceBrand.OPPO_ONEPLUS,
            group = ActionGroup.INFO,
            isAdb = true
        ),
        ServiceActionItem(
            id = "op_adb_debloat_heytap",
            title = "Disable HeyTap Cloud System Service",
            command = "pm disable-user --user 0 com.heytap.cloud",
            description = "HeyTap Cloud နောက်ခံ စနစ်ဝန်ဆောင်မှုကို ပိတ်ပင်သည်။",
            brand = ServiceBrand.OPPO_ONEPLUS,
            group = ActionGroup.SERVICE,
            isAdb = true
        ),

        // ==================== SAMSUNG SPECIFIC ====================
        ServiceActionItem(
            id = "sam_fb_userspace",
            title = "Check Samsung Fastbootd Status",
            command = "fastboot getvar is-userspace",
            description = "Samsung Recovery Fastbootd Mode အလုပ်လုပ်နေခြင်း ရှိ/မရှိ စစ်ဆေးသည်။",
            brand = ServiceBrand.SAMSUNG,
            group = ActionGroup.INFO
        ),
        ServiceActionItem(
            id = "sam_fb_reboot_rec",
            title = "Reboot from Fastbootd to Recovery",
            command = "fastboot reboot recovery",
            description = "Samsung Fastbootd Mode မှ Recovery Mode သို့ ပြန်ဝင်သည်။",
            brand = ServiceBrand.SAMSUNG,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "sam_adb_csc",
            title = "Read Active CSC Country Code",
            command = "getprop ro.csc.sales_code",
            description = "Samsung ဖုန်း၏ လက်ရှိ CSC Region Code (ဥပမာ THL, XXV, EUX) ကို စစ်ဆေးသည်။",
            brand = ServiceBrand.SAMSUNG,
            group = ActionGroup.INFO,
            isAdb = true
        ),
        ServiceActionItem(
            id = "sam_adb_bl_ver",
            title = "Read Bootloader & Binary Bit Version",
            command = "getprop ro.boot.bootloader",
            description = "Firmware တင်ရာတွင် အရေးကြီးသော Samsung Bootloader Binary (U1/U2/S3 စသည်) ကို စစ်သည်။",
            brand = ServiceBrand.SAMSUNG,
            group = ActionGroup.INFO,
            isAdb = true
        ),
        ServiceActionItem(
            id = "sam_adb_knox_bit",
            title = "Check Knox Warranty Bit Status",
            command = "getprop ro.boot.warranty_bit",
            description = "Samsung Knox Warranty Bit (0x0 မူရင်း သို့မဟုတ် 0x1 ပေါက်ပြီး) ကို စစ်ဆေးသည်။",
            brand = ServiceBrand.SAMSUNG,
            group = ActionGroup.INFO,
            isAdb = true
        ),
        ServiceActionItem(
            id = "sam_adb_download",
            title = "Reboot to Odin Download Mode",
            command = "adb reboot download",
            description = "ADB မှတစ်ဆင့် ခလုတ်နှိပ်စရာမလိုဘဲ Samsung Odin Download Mode သို့ တိုက်ရိုက်ဝင်သည်။",
            brand = ServiceBrand.SAMSUNG,
            group = ActionGroup.SERVICE,
            isAdb = true
        ),
        ServiceActionItem(
            id = "sam_adb_disable_ota",
            title = "Disable Samsung OTA Software Update",
            command = "pm disable-user --user 0 com.wssyncmldm",
            description = "Samsung အလိုအလျောက် System Update (OTA) တက်ခြင်းကို ပိတ်ပင်သည်။",
            brand = ServiceBrand.SAMSUNG,
            group = ActionGroup.SERVICE,
            isAdb = true
        ),

        // ==================== MOTOROLA / GOOGLE PIXEL SPECIFIC ====================
        ServiceActionItem(
            id = "moto_unlock_data",
            title = "Read Motorola OEM Unlock Data String",
            command = "fastboot oem get_unlock_data",
            description = "Motorola တရားဝင် Bootloader Unlock Key တောင်းရန် Unlock Data String ကို ထုတ်ယူသည်။",
            brand = ServiceBrand.MOTO_PIXEL,
            group = ActionGroup.UNLOCK
        ),
        ServiceActionItem(
            id = "moto_fb_mode_set",
            title = "Enable Fastboot Factory Mode",
            command = "fastboot oem fb_mode_set",
            description = "Motorola Firmware ရေးသားရာတွင် Fastboot Mode ၌ ဆက်နေစေရန် Factory Mode ဖွင့်သည်။",
            brand = ServiceBrand.MOTO_PIXEL,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "moto_fb_mode_clear",
            title = "Fix Fastboot Loop / Clear Factory Mode",
            command = "fastboot oem fb_mode_clear",
            description = "ဖုန်းဖွင့်တိုင်း Fastboot သို့သာ ပြန်ရောက်နေသော Motorola Fastboot Loop ကို ဖြေရှင်းသည်။",
            brand = ServiceBrand.MOTO_PIXEL,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "moto_cid",
            title = "Read Motorola Carrier ID & SKU",
            command = "fastboot getvar cid",
            description = "Motorola ဖုန်း၏ Carrier ID (CID) နှင့် Region အချက်အလက်ကို စစ်ဆေးသည်။",
            brand = ServiceBrand.MOTO_PIXEL,
            group = ActionGroup.INFO
        ),
        ServiceActionItem(
            id = "pixel_bl_ver",
            title = "Read Pixel Bootloader Version",
            command = "fastboot getvar version-bootloader",
            description = "Google Pixel ဖုန်း၏ Bootloader ဗားရှင်းအတိအကျကို စစ်ဆေးသည်။",
            brand = ServiceBrand.MOTO_PIXEL,
            group = ActionGroup.INFO
        ),
        ServiceActionItem(
            id = "moto_pixel_adb_carrier",
            title = "Read Carrier & Boot Hardware SKU",
            command = "getprop ro.boot.hardware.sku",
            description = "ADB မှတစ်ဆင့် Pixel သို့မဟုတ် Motorola ဖုန်း၏ Hardware SKU မော်ဒယ်ကို စစ်သည်။",
            brand = ServiceBrand.MOTO_PIXEL,
            group = ActionGroup.INFO,
            isAdb = true
        ),

        // ==================== INFINIX / TECNO / ITEL (TRANSSION) ====================
        ServiceActionItem(
            id = "trn_unlock_ability",
            title = "Check Bootloader Unlock Permission",
            command = "fastboot flashing get_unlock_ability",
            description = "Infinix/Tecno/Itel ဖုန်းတွင် OEM Unlock ခွင့်ပြုချက် ရှိ/မရှိ စစ်ဆေးသည်။",
            brand = ServiceBrand.TRANSSION,
            group = ActionGroup.INFO
        ),
        ServiceActionItem(
            id = "trn_unlock",
            title = "Unlock Transsion Bootloader",
            command = "fastboot flashing unlock",
            description = "Infinix/Tecno/Itel ဖုန်း၏ Bootloader ကို Unlock ပြုလုပ်သည်။",
            brand = ServiceBrand.TRANSSION,
            group = ActionGroup.UNLOCK
        ),
        ServiceActionItem(
            id = "trn_fastbootd",
            title = "Enter Fastbootd for Super Partition",
            command = "fastboot reboot fastboot",
            description = "Infinix/Tecno Super Partition ရေးသားနိုင်ရန် Fastbootd Mode သို့ ပြောင်းသည်။",
            brand = ServiceBrand.TRANSSION,
            group = ActionGroup.SERVICE
        ),
        ServiceActionItem(
            id = "trn_adb_hios",
            title = "Read HiOS / XOS System Build",
            command = "getprop ro.build.display.id",
            description = "ADB မှတစ်ဆင့် Infinix HiOS သို့မဟုတ် Tecno XOS Firmware Build ကို ဖတ်သည်။",
            brand = ServiceBrand.TRANSSION,
            group = ActionGroup.INFO,
            isAdb = true
        ),

        // ==================== ASUS / SONY SPECIFIC ====================
        ServiceActionItem(
            id = "asus_device_info",
            title = "Read Asus Bootloader & Root Status",
            command = "fastboot oem device-info",
            description = "Asus ROG / Zenfone ၏ Bootloader ပွင့်/မပွင့် အခြေအနေကို စစ်ဆေးသည်။",
            brand = ServiceBrand.ASUS_SONY,
            group = ActionGroup.INFO
        ),
        ServiceActionItem(
            id = "asus_go",
            title = "Execute Asus OEM Unlock Trigger",
            command = "fastboot oem asus-go",
            description = "Asus ဖုန်းများအား OEM Unlock Trigger Command ပေးပို့သည်။",
            brand = ServiceBrand.ASUS_SONY,
            group = ActionGroup.UNLOCK
        ),
        ServiceActionItem(
            id = "sony_baseband",
            title = "Read Sony Xperia Baseband Info",
            command = "fastboot getvar version-baseband",
            description = "Sony Xperia ဖုန်း၏ Baseband Modem ဗားရှင်းကို Fastboot မှ ဖတ်ယူသည်။",
            brand = ServiceBrand.ASUS_SONY,
            group = ActionGroup.INFO
        ),
        ServiceActionItem(
            id = "asus_sony_adb_ver",
            title = "Read Firmware & Model Fingerprint",
            command = "getprop ro.build.fingerprint",
            description = "ADB မှတစ်ဆင့် Asus/Sony ဖုန်း၏ Official Build Fingerprint အပြည့်အစုံကို ဖတ်သည်။",
            brand = ServiceBrand.ASUS_SONY,
            group = ActionGroup.INFO,
            isAdb = true
        ),

        // ==================== UNIVERSAL ADB INFO ====================
        ServiceActionItem(
            id = "adb_devices",
            title = "Check Connected ADB Devices",
            command = "adb devices -l",
            description = "ချိတ်ဆက်ထားသော ADB ဖုန်းများနှင့် မော်ဒယ်အချက်အလက်ကို စစ်ဆေးသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.INFO,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_model_ver",
            title = "Read Model & Android Release Version",
            command = "getprop ro.product.model",
            description = "ဖုန်း၏ Model Name အတိအကျကို ADB မှတစ်ဆင့် ဖတ်ယူသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.INFO,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_android_ver",
            title = "Read Android OS Version",
            command = "getprop ro.build.version.release",
            description = "လက်ရှိ ဖုန်းတွင် တင်ထားသော Android Version ကို စစ်ဆေးသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.INFO,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_getprop_all",
            title = "Read All Android System Properties",
            command = "getprop",
            description = "ဖုန်းတစ်ခုလုံး၏ Android 10–16 System Properties အားလုံးကို ဖတ်ယူသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.INFO,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_avb_state",
            title = "Check Verified Boot AVB 2.0 State",
            command = "getprop ro.boot.verifiedbootstate",
            description = "Verified Boot အခြေအနေ (green = Locked, orange = Unlocked) ကို စစ်ဆေးသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.INFO,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_slot_suffix",
            title = "Check Active A/B Boot Slot",
            command = "getprop ro.boot.slot_suffix",
            description = "ADB Mode မှနေ၍ လက်ရှိ Boot တက်ထားသော Slot (_a သို့မဟုတ် _b) ကို စစ်ဆေးသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.INFO,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_dyn_parts",
            title = "Check Dynamic Partitions Support",
            command = "getprop ro.boot.dynamic_partitions",
            description = "ဖုန်းတွင် Super / Dynamic Partition စနစ် ပါဝင်ခြင်း ရှိ/မရှိ စစ်ဆေးသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.INFO,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_sec_patch",
            title = "Read Android Security Patch Level",
            command = "getprop ro.build.version.security_patch",
            description = "ဖုန်း၏ လက်ရှိ Android Security Patch ခုနှစ်နှင့် လကို ဖတ်ယူသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.INFO,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_battery",
            title = "Read Battery Health & Temperature",
            command = "dumpsys battery",
            description = "ဘက်ထရီ ရာခိုင်နှုန်း၊ ဗို့အား၊ အပူချိန်နှင့် ကျန်းမာရေးအခြေအနေကို စစ်ဆေးသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.INFO,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_disk_free",
            title = "Inspect Partition Mounts & Free Space",
            command = "df -h",
            description = "Partition တစ်ခုချင်းစီ၏ စုစုပေါင်းပမာဏနှင့် နေရာလွတ်ကို စစ်ဆေးသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.INFO,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_kernel_uname",
            title = "Read Linux Kernel Version & Arch",
            command = "uname -a",
            description = "ဖုန်း၏ Linux Kernel Version နှင့် CPU Architecture (aarch64) ကို ဖတ်ယူသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.INFO,
            isAdb = true
        ),

        // ==================== UNIVERSAL ADB UNLOCK & BYPASS ====================
        ServiceActionItem(
            id = "adb_unlock_open_settings",
            title = "FRP Helper: Open Android Settings",
            command = "am start -n com.android.settings/com.android.settings.Settings",
            description = "FRP Screen မှ Android System Settings မျက်နှာပြင်သို့ တိုက်ရိုက်ဖွင့်ပေးသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.UNLOCK,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_unlock_open_browser",
            title = "FRP Helper: Launch Web Browser",
            command = "am start -a android.intent.action.VIEW -d \"https://google.com\"",
            description = "ADB မှတစ်ဆင့် ဖုန်း၏ Browser ကို တိုက်ရိုက်ဖွင့်လှစ်ပေးသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.UNLOCK,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_unlock_complete_setup",
            title = "Bypass Setup Wizard (Provisioned Flag)",
            command = "settings put global device_provisioned 1",
            description = "Setup Wizard ကျော်လွှားနိုင်ရန် device_provisioned flag ကို 1 သို့ သတ်မှတ်ပေးသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.UNLOCK,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_unlock_user_setup",
            title = "Mark User Setup Complete Flag",
            command = "settings put secure user_setup_complete 1",
            description = "Home Screen သို့ ဝင်ရောက်နိုင်ရန် user_setup_complete flag ကို 1 သို့ ပြောင်းသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.UNLOCK,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_unlock_dev_settings",
            title = "Open Developer Options (OEM Unlock)",
            command = "am start -a android.settings.APPLICATION_DEVELOPMENT_SETTINGS",
            description = "OEM Unlocking နှင့် USB Debugging ဖွင့်ရန် Developer Options ကို တိုက်ရိုက်ဖွင့်သည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.UNLOCK,
            isAdb = true
        ),

        // ==================== UNIVERSAL ADB SERVICE & REBOOTS ====================
        ServiceActionItem(
            id = "adb_dexopt",
            title = "Run ART Speed App Optimization",
            command = "cmd package bg-dexopt-job",
            description = "ဖုန်းပေါ့ပါးမြန်ဆန်စေရန် Android ART Runtime Background Dexopt ကို Run ပေးသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_pkg_all",
            title = "List All Installed System & User Apps",
            command = "pm list packages",
            description = "ဖုန်းအတွင်း သွင်းထားသမျှ System နှင့် User Package အားလုံးကို စာရင်းထုတ်ပြသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_pkg_3rd",
            title = "List Third-Party User Apps Only",
            command = "pm list packages -3",
            description = "User ကိုယ်တိုင် သွင်းထားသော ပြင်ပ App Package များကိုသာ သီးသန့် စစ်ဆေးသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_pkg_disabled",
            title = "List Disabled System Packages",
            command = "pm list packages -d",
            description = "Disable (ပိတ်ပင်) ထားသော Package များစာရင်းကို စစ်ဆေးသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_wm_reset",
            title = "Reset Screen DPI to Factory Default",
            command = "wm density reset",
            description = "ပြောင်းလဲထားသော ဖုန်းမျက်နှာပြင် DPI ကို မူရင်းစက်ရုံထုတ်အတိုင်း ပြန်ထားသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_reboot_sys",
            title = "Reboot Phone Normally",
            command = "adb reboot",
            description = "ADB Mode မှ ဖုန်းကို ပုံမှန်အတိုင်း ပြန်လည် Reboot ချသည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_reboot_bl",
            title = "Reboot to Fastboot Bootloader",
            command = "adb reboot bootloader",
            description = "ADB Mode မှ Fastboot Bootloader သို့ တိုက်ရိုက် ဝင်ရောက်သည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_reboot_fastbootd",
            title = "Reboot to Userspace Fastbootd",
            command = "adb reboot fastboot",
            description = "ADB Mode မှ Android 10–16 Userspace Fastbootd သို့ တိုက်ရိုက် ဝင်ရောက်သည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_reboot_rec",
            title = "Reboot to Recovery Mode",
            command = "adb reboot recovery",
            description = "ADB Mode မှ System Recovery သို့ တိုက်ရိုက် ဝင်ရောက်သည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_reboot_sideload",
            title = "Reboot to OTA Sideload Mode",
            command = "adb reboot sideload-auto-reboot",
            description = "ADB Sideload ဖြင့် Firmware တင်ရန် Sideload Mode သို့ တိုက်ရိုက်ဝင်သည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE,
            isAdb = true
        ),
        ServiceActionItem(
            id = "adb_reboot_edl",
            title = "Reboot to Qualcomm EDL 9008 Mode",
            command = "adb reboot edl",
            description = "ADB Mode မှ Qualcomm Emergency Download (9008) သို့ တိုက်ရိုက်ဝင်သည်။",
            brand = ServiceBrand.UNIVERSAL,
            group = ActionGroup.SERVICE,
            isAdb = true
        )
    )
}
