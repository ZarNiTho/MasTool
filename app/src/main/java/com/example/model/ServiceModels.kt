package com.example.model

enum class ThemeMode {
    DARK,
    LIGHT
}

enum class ToolScreenMode(
    val badgeTitle: String,
    val headerTitle: String
) {
    FASTBOOT_ONECLICK(
        badgeTitle = "[FB] Fastboot",
        headerTitle = "Fastboot Mode (Bootloader One-Click)"
    ),
    FASTBOOT_FLASH(
        badgeTitle = "[FB] Flash Tool",
        headerTitle = "Fastboot Flash (.img / .bat / UPDATE.APP)"
    ),
    ADB_ONECLICK(
        badgeTitle = "[ADB] Service",
        headerTitle = "ADB Mode (System & Package One-Click)"
    ),
    CMD_TERMINAL(
        badgeTitle = "[CMD] Terminal",
        headerTitle = "Dedicated Command Shell Terminal"
    )
}

enum class ServiceBrand(val displayName: String, val shortTag: String) {
    UNIVERSAL("Universal (Android 10–16)", "Universal"),
    XIAOMI("Xiaomi / Redmi / Poco", "Xiaomi"),
    HUAWEI("Huawei / Honor (HiSilicon)", "Huawei"),
    VIVO("Vivo / iQOO (BBK)", "Vivo"),
    OPPO_ONEPLUS("Oppo / Realme / OnePlus", "Oplus"),
    SAMSUNG("Samsung Galaxy", "Samsung"),
    MOTO_PIXEL("Google Pixel / Motorola", "Moto/Pixel"),
    TRANSSION("Infinix / Tecno / Itel", "Transsion"),
    ASUS_SONY("Asus ROG / Sony Xperia", "Asus/Sony")
}

enum class ActionGroup(val displayName: String) {
    ALL("All Actions"),
    BRAND_OEM("Brand OEM Specific"),
    INFO_GETVAR("Read Info"),
    UNLOCK_LOCK("Bootloader Unlock / Lock"),
    DYNAMIC_SLOT("Dynamic Partition & Slot A/B"),
    WIPE_ERASE("Wipe Data & Erase"),
    REBOOT_MODES("Reboot Modes"),
    ADB_SYSTEM("ADB System & Debloat")
}

data class ServiceActionItem(
    val id: String,
    val title: String,
    val command: String,
    val description: String = "",
    val brand: ServiceBrand,
    val group: ActionGroup,
    val isAdb: Boolean = false
)

enum class UsbProtocolType(val label: String) {
    ADB("ADB (FF:42:01)"),
    FASTBOOT("FASTBOOT (FF:42:03)"),
    VENDOR_PORT("USB PORT")
}

data class UsbPortInfo(
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
    val vendorHex: String,
    val productHex: String,
    val productName: String,
    val serialNumber: String,
    val interfaceId: Int,
    val endpointInAddress: Int,
    val endpointOutAddress: Int,
    val maxPacketSize: Int,
    val hasPermission: Boolean,
    val protocolType: UsbProtocolType
)

enum class FlashFileType(val label: String) {
    UPDATE_APP("UPDATE.APP"),
    SCRIPT("SCRIPT (.bat/.sh)"),
    IMAGE("IMAGE (.img/.bin)"),
    ARCHIVE("ZIP ARCHIVE"),
    RAW("RAW FILE")
}

data class ParsedFlashStep(
    val id: String,
    val order: Int,
    val command: String,
    val targetPartition: String,
    val sourceName: String,
    val dataOffset: Long,
    val dataSize: Long,
    val enabled: Boolean = true
)

data class SelectedFlashFile(
    val uriString: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val formattedSize: String,
    val fileType: FlashFileType,
    val magicHeaderHex: String,
    val isSparseImage: Boolean,
    val sparseInfo: String,
    val steps: List<ParsedFlashStep>
)

enum class LogLevel {
    INIT,
    CMD,
    USB,
    OKAY,
    INFO,
    WARN,
    FAIL
}

data class ConsoleLogEntry(
    val id: Long,
    val timestamp: String,
    val tag: String,
    val message: String,
    val level: LogLevel
)
