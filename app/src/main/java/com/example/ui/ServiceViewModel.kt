package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CommandRepository
import com.example.data.FlashFileProcessor
import com.example.data.UsbPortController
import com.example.model.ActionGroup
import com.example.model.ConsoleLogEntry
import com.example.model.LogLevel
import com.example.model.SelectedFlashFile
import com.example.model.ServiceActionItem
import com.example.model.ServiceBrand
import com.example.model.TerminalLine
import com.example.model.ThemeMode
import com.example.model.ToolScreenMode
import com.example.model.UsbPortInfo
import com.example.model.UsbProtocolType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

data class ServiceUiState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val screenMode: ToolScreenMode = ToolScreenMode.FASTBOOT_ONECLICK,
    val showAboutDialog: Boolean = false,
    val usbPorts: List<UsbPortInfo> = emptyList(),
    val selectedPortName: String? = null,
    val selectedBrand: ServiceBrand = ServiceBrand.UNIVERSAL,
    val selectedActionGroup: ActionGroup = ActionGroup.ALL,
    val selectedAction: ServiceActionItem = CommandRepository.allActions.first { !it.isAdb },
    val selectedPartition: String = CommandRepository.supportedPartitions.first(),
    val disableVerity: Boolean = true,
    val autoReboot: Boolean = true,
    val checksumVerify: Boolean = true,
    val useFastbootd: Boolean = false,
    val selectedFlashFile: SelectedFlashFile? = null,
    val isBusy: Boolean = false,
    val statusBarText: String = "Direct USB OTG Host Ready",
    val progressPercent: Float = 0f,
    val rawCommandInput: String = "",
    val consoleLogs: List<ConsoleLogEntry> = emptyList(),
    val terminalInput: String = "",
    val terminalLines: List<TerminalLine> = emptyList(),
    val terminalHistory: List<String> = emptyList()
)

class ServiceViewModel(application: Application) : AndroidViewModel(application) {

    private val usbController = UsbPortController(application.applicationContext)
    private val logSequence = AtomicLong(1L)
    private val terminalSequence = AtomicLong(1L)

    private val _uiState = MutableStateFlow(ServiceUiState())
    val uiState: StateFlow<ServiceUiState> = _uiState.asStateFlow()

    init {
        appendLog(
            tag = "INIT",
            message = "ADB & Fastboot Standalone USB OTG Engine Initialized.",
            level = LogLevel.INIT
        )
        scanUsbPorts()
    }

    /**
     * Strictly separates Fastboot actions (!isAdb) and ADB actions (isAdb) by tab,
     * and filters cleanly by the selected Brand so the scroll box is never cluttered.
     */
    fun getAvailableActions(): List<ServiceActionItem> {
        val state = _uiState.value
        val modeFiltered = when (state.screenMode) {
            ToolScreenMode.ADB_ONECLICK -> CommandRepository.allActions.filter { it.isAdb }
            else -> CommandRepository.allActions.filter { !it.isAdb }
        }
        val brandFiltered = modeFiltered.filter { it.brand == state.selectedBrand }
        return brandFiltered.ifEmpty { modeFiltered }
    }

    fun setShowAboutDialog(show: Boolean) {
        _uiState.update { it.copy(showAboutDialog = show) }
    }

    fun toggleTheme() {
        _uiState.update {
            val next = if (it.themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
            it.copy(themeMode = next)
        }
    }

    fun navigateToMode(mode: ToolScreenMode) {
        _uiState.update { it.copy(screenMode = mode) }
        val actions = getAvailableActions()
        if (actions.isNotEmpty() && _uiState.value.selectedAction !in actions) {
            val first = actions.first()
            _uiState.update {
                it.copy(selectedAction = first)
            }
        }
        appendLog(
            tag = "NAV",
            message = "Mode: ${mode.headerTitle}",
            level = LogLevel.INFO
        )
    }

    fun setBrand(brand: ServiceBrand) {
        _uiState.update { it.copy(selectedBrand = brand) }
        val available = getAvailableActions()
        if (available.isNotEmpty()) {
            val firstMatch = available.first()
            _uiState.update {
                it.copy(selectedAction = firstMatch)
            }
        }
        appendLog("BRAND", "Selected Brand: ${brand.displayName}", LogLevel.INFO)
    }

    fun setActionGroup(group: ActionGroup) {
        _uiState.update { it.copy(selectedActionGroup = group) }
        val available = getAvailableActions()
        if (available.isNotEmpty()) {
            _uiState.update {
                it.copy(selectedAction = available.first())
            }
        }
    }

    fun setSelectedAction(action: ServiceActionItem) {
        _uiState.update {
            it.copy(selectedAction = action)
        }
    }

    fun setPartition(partitionLabel: String) {
        val cleanPart = partitionLabel.substringBefore(" ").lowercase(Locale.US)
        _uiState.update { state ->
            val updatedFile = state.selectedFlashFile?.let { file ->
                if (file.steps.size == 1 && cleanPart != "auto") {
                    file.copy(
                        steps = file.steps.map {
                            it.copy(
                                targetPartition = cleanPart,
                                command = "fastboot flash $cleanPart ${file.fileName}"
                            )
                        }
                    )
                } else {
                    file
                }
            }
            state.copy(
                selectedPartition = partitionLabel,
                selectedFlashFile = updatedFile
            )
        }
        appendLog("PART", "Target Partition: $partitionLabel", LogLevel.INFO)
    }

    fun toggleDisableVerity() {
        _uiState.update { it.copy(disableVerity = !it.disableVerity) }
    }

    fun toggleAutoReboot() {
        _uiState.update { it.copy(autoReboot = !it.autoReboot) }
    }

    fun toggleChecksum() {
        _uiState.update { it.copy(checksumVerify = !it.checksumVerify) }
    }

    fun toggleUseFastbootd() {
        _uiState.update { it.copy(useFastbootd = !it.useFastbootd) }
    }

    fun toggleFlashStep(stepId: String) {
        _uiState.update { state ->
            val file = state.selectedFlashFile ?: return@update state
            val updated = file.steps.map { s ->
                if (s.id == stepId) s.copy(enabled = !s.enabled) else s
            }
            state.copy(selectedFlashFile = file.copy(steps = updated))
        }
    }

    fun scanUsbPorts() {
        viewModelScope.launch {
            val ports = withContext(Dispatchers.IO) {
                usbController.scanPorts()
            }
            val first = ports.firstOrNull()
            _uiState.update {
                it.copy(
                    usbPorts = ports,
                    selectedPortName = first?.deviceName,
                    statusBarText = if (first != null) {
                        "USB OTG Connected: ${first.productName} (${first.vendorHex}:${first.productHex})"
                    } else {
                        "Direct USB OTG Host Ready"
                    }
                )
            }
            if (ports.isEmpty()) {
                appendLog(
                    tag = "USB",
                    message = "USB OTG Scan: 0 slave devices attached.",
                    level = LogLevel.INFO
                )
            } else {
                ports.forEach { p ->
                    appendLog(
                        tag = "USB",
                        message = "Attached: ${p.productName} [VID:${p.vendorHex} PID:${p.productHex}] ${p.protocolType.label} Perm=${p.hasPermission}",
                        level = LogLevel.USB
                    )
                    if (!p.hasPermission) {
                        usbController.requestPermission(p.deviceName)
                    }
                }
            }
        }
    }

    fun requestActivePortPermission() {
        val port = _uiState.value.usbPorts.firstOrNull()
        if (port != null) {
            usbController.requestPermission(port.deviceName)
            appendLog("USB", "Requested USB Host Permission for ${port.deviceName}", LogLevel.USB)
        } else {
            scanUsbPorts()
        }
    }

    fun onFilePicked(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    statusBarText = "Parsing firmware file..."
                )
            }
            val context = getApplication<Application>().applicationContext
            val rawPart = _uiState.value.selectedPartition.substringBefore(" ").lowercase(Locale.US)
            val fallbackPart = if (rawPart == "auto") "boot" else rawPart

            val parsed = withContext(Dispatchers.IO) {
                FlashFileProcessor.inspectAndParseUri(context, uri, fallbackPart)
            }
            _uiState.update {
                it.copy(
                    selectedFlashFile = parsed,
                    isBusy = false,
                    statusBarText = "Loaded: ${parsed.fileName} (${parsed.formattedSize})",
                    screenMode = ToolScreenMode.FASTBOOT_FLASH
                )
            }
            appendLog(
                tag = "FILE",
                message = "Loaded ${parsed.fileName} (${parsed.formattedSize}) [${parsed.fileType.label}] Header: ${parsed.magicHeaderHex}",
                level = LogLevel.OKAY
            )
            appendLog(
                tag = "PARTS",
                message = "Parsed ${parsed.steps.size} partition task(s) • ${parsed.sparseInfo}",
                level = LogLevel.INFO
            )
        }
    }

    fun onStartPressed() {
        if (_uiState.value.isBusy) {
            appendLog("BUSY", "Operation already in progress. Please wait.", LogLevel.WARN)
            return
        }
        val state = _uiState.value
        when (state.screenMode) {
            ToolScreenMode.FASTBOOT_FLASH -> executeDirectUsbFlash()
            ToolScreenMode.CMD_TERMINAL -> submitTerminalCommand(state.terminalInput)
            ToolScreenMode.FASTBOOT_ONECLICK, ToolScreenMode.ADB_ONECLICK -> {
                appendLog(
                    tag = "START",
                    message = "[${state.selectedBrand.shortTag}] ${state.selectedAction.title}",
                    level = LogLevel.CMD
                )
                executeCommand(state.selectedAction.command, logRawCommand = false)
            }
        }
    }

    fun updateTerminalInput(input: String) {
        _uiState.update { it.copy(terminalInput = input) }
    }

    /**
     * Executes pure Termux-style CLI commands strictly inside the dedicated Terminal screen
     * without mixing One-Click logs or timestamps.
     */
    fun submitTerminalCommand(rawCmd: String) {
        if (_uiState.value.isBusy) return
        val cmd = rawCmd.trim()
        _uiState.update { it.copy(terminalInput = "") }

        if (cmd.isEmpty()) {
            appendTerminalLine("~ $ ", isCommand = true)
            return
        }

        if (cmd.equals("clear", ignoreCase = true) || cmd.equals("cls", ignoreCase = true)) {
            _uiState.update { it.copy(terminalLines = emptyList()) }
            return
        }

        appendTerminalLine("~ $ $cmd", isCommand = true)
        _uiState.update { state ->
            val updatedHistory = if (state.terminalHistory.lastOrNull() == cmd) {
                state.terminalHistory
            } else {
                (state.terminalHistory + cmd).takeLast(50)
            }
            state.copy(terminalHistory = updatedHistory)
        }

        if (cmd.equals("help", ignoreCase = true)) {
            appendTerminalLine("fastboot devices | fastboot <cmd> | adb devices | adb shell <cmd> | adb reboot | clear")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }

            withContext(Dispatchers.IO) {
                val lower = cmd.lowercase(Locale.US)
                val ports = usbController.scanPorts()
                val activePort = ports.firstOrNull {
                    if (lower.startsWith("fastboot")) {
                        it.protocolType == UsbProtocolType.FASTBOOT
                    } else if (lower.startsWith("adb")) {
                        it.protocolType == UsbProtocolType.ADB
                    } else {
                        true
                    }
                } ?: ports.firstOrNull()

                if (activePort != null) {
                    _uiState.update { it.copy(usbPorts = ports, selectedPortName = activePort.deviceName) }
                }

                when {
                    lower == "fastboot devices" || lower == "fastboot devices -l" -> {
                        val fbPorts = ports.filter { it.protocolType == UsbProtocolType.FASTBOOT }
                        if (fbPorts.isEmpty()) {
                            // no output on real fastboot devices if empty, or show clean status
                            if (ports.isNotEmpty()) {
                                ports.forEach { p ->
                                    appendTerminalLine("${p.serialNumber}\t${p.protocolType.label}")
                                }
                            }
                        } else {
                            fbPorts.forEach { p ->
                                appendTerminalLine("${p.serialNumber}\tfastboot")
                            }
                        }
                    }

                    lower == "adb devices" || lower == "adb devices -l" -> {
                        appendTerminalLine("List of devices attached")
                        val adbPorts = ports.filter { it.protocolType == UsbProtocolType.ADB }
                        adbPorts.forEach { p ->
                            val status = if (p.hasPermission) "device" else "unauthorized"
                            appendTerminalLine("${p.serialNumber}\t$status")
                            if (!p.hasPermission) {
                                usbController.requestPermission(p.deviceName)
                            }
                        }
                    }

                    lower.startsWith("fastboot ") -> {
                        val portName = activePort?.deviceName
                        if (portName == null) {
                            appendTerminalLine("< waiting for any device >", isError = true)
                        } else {
                            val tokens = cmd.split(Regex("\\s+"))
                            val flashIdx = tokens.indexOfFirst { it.equals("flash", ignoreCase = true) }
                            val loadedFile = _uiState.value.selectedFlashFile
                            if (flashIdx >= 0 && flashIdx + 1 < tokens.size && loadedFile != null) {
                                val targetPart = tokens[flashIdx + 1]
                                val uri = Uri.parse(loadedFile.uriString)
                                usbController.flashBinaryStreamOverUsb(
                                    deviceName = portName,
                                    uri = uri,
                                    partition = targetPart,
                                    dataOffset = 0L,
                                    dataSize = loadedFile.fileSizeBytes,
                                    disableVerity = _uiState.value.disableVerity,
                                    onProgress = {},
                                    onLog = { msg, lvl -> handleCliTransportOutput(msg, lvl) }
                                )
                            } else {
                                val wireList = toFastbootWireProtocolList(cmd)
                                usbController.executeFastbootWireCommands(
                                    deviceName = portName,
                                    wireCommands = wireList,
                                    onLog = { msg, lvl -> handleCliTransportOutput(msg, lvl) }
                                )
                            }
                        }
                    }

                    lower == "adb" || lower.startsWith("adb ") -> {
                        val portName = activePort?.deviceName
                        if (portName == null) {
                            appendTerminalLine("error: no devices/emulators found", isError = true)
                        } else {
                            usbController.executeAdbOverUsbOtg(
                                deviceName = portName,
                                command = cmd,
                                onLog = { msg, lvl -> handleCliTransportOutput(msg, lvl) }
                            )
                        }
                    }

                    else -> {
                        val portName = activePort?.deviceName
                        if (portName != null && activePort.protocolType == UsbProtocolType.ADB) {
                            usbController.executeAdbOverUsbOtg(
                                deviceName = portName,
                                command = cmd,
                                onLog = { msg, lvl -> handleCliTransportOutput(msg, lvl) }
                            )
                        } else {
                            usbController.executeLocalShell(
                                command = cmd,
                                onLog = { msg, lvl ->
                                    if (!msg.startsWith("Local Shell Exit 0")) {
                                        appendTerminalLine(msg, isError = lvl == LogLevel.FAIL)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            _uiState.update { it.copy(isBusy = false) }
        }
    }

    private fun handleCliTransportOutput(msg: String, level: LogLevel) {
        // Suppress verbose internal USB packet trace in CLI mode, keeping real device/CLI output only
        if (msg.startsWith("TX (") ||
            msg.startsWith("TX ->") ||
            msg.startsWith("RX <-") ||
            msg.startsWith("ADB USB TX -> CNXN") ||
            msg.startsWith("ADB USB TX -> AUTH_SIGNATURE") ||
            msg.startsWith("ADB USB TX -> OPEN") ||
            msg.startsWith("ADB USB Connected:") ||
            msg == "ADB stream closed (OKAY)."
        ) {
            return
        }
        if (msg.startsWith("ADB USB TX -> AUTH_RSAPUBLICKEY")) {
            appendTerminalLine("* Tap 'Allow USB Debugging' on target phone *", isError = false)
            return
        }
        appendTerminalLine(msg, isError = level == LogLevel.FAIL)
    }

    private fun appendTerminalLine(text: String, isCommand: Boolean = false, isError: Boolean = false) {
        val entry = TerminalLine(
            id = terminalSequence.getAndIncrement(),
            text = text,
            isCommand = isCommand,
            isError = isError
        )
        _uiState.update { s ->
            s.copy(terminalLines = (s.terminalLines + entry).takeLast(500))
        }
    }

    fun executeDirectUsbFlash() {
        if (_uiState.value.isBusy) return
        val state = _uiState.value
        val file = state.selectedFlashFile ?: run {
            appendLog("FLASH", "No flash file loaded. Tap 'Load .img / .bat / UPDATE.APP' first.", LogLevel.WARN)
            return
        }
        val enabledSteps = file.steps.filter { it.enabled }
        if (enabledSteps.isEmpty()) {
            appendLog("FLASH", "0 partitions enabled.", LogLevel.WARN)
            return
        }
        val portName = state.selectedPortName ?: run {
            appendLog("FLASH", "No USB OTG Fastboot device attached.", LogLevel.FAIL)
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    progressPercent = 0f,
                    statusBarText = "Flashing ${file.fileName}..."
                )
            }
            val uri = Uri.parse(file.uriString)

            withContext(Dispatchers.IO) {
                if (state.useFastbootd) {
                    appendLog("FB", "Switching to Userspace Fastbootd...", LogLevel.USB)
                    usbController.executeFastbootWireCommands(portName, listOf("reboot-fastboot")) { msg, lvl ->
                        appendLog("FB", msg, lvl)
                    }
                }

                enabledSteps.forEachIndexed { idx, step ->
                    appendLog(
                        tag = "FLASH",
                        message = "[${idx + 1}/${enabledSteps.size}] Flashing ${step.targetPartition}...",
                        level = LogLevel.CMD
                    )
                    if (step.dataSize > 0L) {
                        usbController.flashBinaryStreamOverUsb(
                            deviceName = portName,
                            uri = uri,
                            partition = step.targetPartition,
                            dataOffset = step.dataOffset,
                            dataSize = step.dataSize,
                            disableVerity = state.disableVerity,
                            onProgress = { ratio ->
                                val totalPct = ((idx + ratio) / enabledSteps.size.toFloat()) * 100f
                                _uiState.update {
                                    it.copy(
                                        progressPercent = totalPct.coerceIn(0f, 100f),
                                        statusBarText = "Flashing ${step.targetPartition}..."
                                    )
                                }
                            },
                            onLog = { msg, lvl -> appendLog("USB", msg, lvl) }
                        )
                    } else {
                        val wireCmds = toFastbootWireProtocolList(step.command)
                        usbController.executeFastbootWireCommands(
                            deviceName = portName,
                            wireCommands = wireCmds,
                            onLog = { msg, lvl -> appendLog("USB", msg, lvl) }
                        )
                    }
                }

                if (state.autoReboot) {
                    appendLog("CMD", "Rebooting device to system...", LogLevel.CMD)
                    usbController.executeFastbootWireCommands(portName, listOf("reboot")) { msg, lvl ->
                        appendLog("USB", msg, lvl)
                    }
                }
            }

            _uiState.update {
                it.copy(
                    isBusy = false,
                    progressPercent = 100f,
                    statusBarText = "Direct USB OTG Host Ready"
                )
            }
        }
    }

    fun executeCommand(rawCmd: String, logRawCommand: Boolean = true) {
        if (_uiState.value.isBusy) {
            appendLog("BUSY", "Another command is currently running.", LogLevel.WARN)
            return
        }
        val cmd = rawCmd.trim()
        if (cmd.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    statusBarText = "Executing service operation...",
                    progressPercent = 0f
                )
            }
            if (logRawCommand) {
                appendLog("CMD", "> $cmd", LogLevel.CMD)
            }

            val state = _uiState.value
            val portName = state.selectedPortName

            withContext(Dispatchers.IO) {
                when {
                    cmd.startsWith("fastboot", ignoreCase = true) -> {
                        if (portName == null) {
                            appendLog(
                                tag = "FAIL",
                                message = "No USB OTG Fastboot device connected.",
                                level = LogLevel.FAIL
                            )
                        } else {
                            val tokens = cmd.split(Regex("\\s+"))
                            val flashIdx = tokens.indexOfFirst { it.equals("flash", ignoreCase = true) }
                            val loadedFile = state.selectedFlashFile
                            if (flashIdx >= 0 && flashIdx + 1 < tokens.size && loadedFile != null) {
                                val targetPart = tokens[flashIdx + 1]
                                val uri = Uri.parse(loadedFile.uriString)
                                usbController.flashBinaryStreamOverUsb(
                                    deviceName = portName,
                                    uri = uri,
                                    partition = targetPart,
                                    dataOffset = 0L,
                                    dataSize = loadedFile.fileSizeBytes,
                                    disableVerity = state.disableVerity,
                                    onProgress = { ratio ->
                                        _uiState.update { it.copy(progressPercent = ratio * 100f) }
                                    },
                                    onLog = { msg, lvl -> appendLog("FB", msg, lvl) }
                                )
                            } else {
                                val wireList = toFastbootWireProtocolList(cmd)
                                usbController.executeFastbootWireCommands(
                                    deviceName = portName,
                                    wireCommands = wireList,
                                    onLog = { msg, lvl -> appendLog("FB", msg, lvl) }
                                )
                            }
                        }
                    }
                    else -> {
                        if (portName != null) {
                            usbController.executeAdbOverUsbOtg(
                                deviceName = portName,
                                command = cmd,
                                onLog = { msg, lvl -> appendLog("OTG-ADB", msg, lvl) }
                            )
                        } else if (cmd.startsWith("adb reboot", ignoreCase = true) ||
                            cmd.startsWith("adb devices", ignoreCase = true) ||
                            cmd.startsWith("adb sideload", ignoreCase = true)
                        ) {
                            appendLog(
                                tag = "ADB",
                                message = "No USB OTG ADB slave device connected.",
                                level = LogLevel.FAIL
                            )
                        } else {
                            usbController.executeLocalShell(
                                command = cmd,
                                onLog = { msg, lvl -> appendLog("LOCAL-SH", msg, lvl) }
                            )
                        }
                    }
                }
            }

            _uiState.update {
                it.copy(
                    isBusy = false,
                    statusBarText = "Direct USB OTG Host Ready"
                )
            }
        }
    }

    private fun toFastbootWireProtocolList(cliCommand: String): List<String> {
        val body = cliCommand
            .replace(Regex("^fastboot(\\s+--[a-zA-Z0-9_-]+)*\\s+", RegexOption.IGNORE_CASE), "")
            .trim()
        return when {
            body.equals("-w", ignoreCase = true) -> listOf("erase:userdata", "erase:metadata")
            body.equals("erase modemst1", ignoreCase = true) -> listOf("erase:modemst1", "erase:modemst2")
            body.equals("reboot fastboot", ignoreCase = true) -> listOf("reboot-fastboot")
            body.equals("reboot recovery", ignoreCase = true) -> listOf("reboot-recovery")
            body.startsWith("getvar ", ignoreCase = true) -> listOf("getvar:${body.substringAfter(" ").trim()}")
            body.startsWith("erase ", ignoreCase = true) -> listOf("erase:${body.substringAfter(" ").trim()}")
            body.startsWith("set_active ", ignoreCase = true) -> listOf("set_active:${body.substringAfter(" ").trim()}")
            body.startsWith("--set-active=", ignoreCase = true) -> listOf("set_active:${body.substringAfter("=").trim()}")
            body.startsWith("snapshot-update ", ignoreCase = true) -> listOf("snapshot-update:${body.substringAfter(" ").trim()}")
            body.startsWith("create-logical-partition ", ignoreCase = true) -> {
                val args = body.removePrefix("create-logical-partition ").trim().replace(Regex("\\s+"), ":")
                listOf("create-logical-partition:$args")
            }
            body.startsWith("delete-logical-partition ", ignoreCase = true) ->
                listOf("delete-logical-partition:${body.substringAfter(" ").trim()}")
            body.startsWith("resize-logical-partition ", ignoreCase = true) -> {
                val args = body.removePrefix("resize-logical-partition ").trim().replace(Regex("\\s+"), ":")
                listOf("resize-logical-partition:$args")
            }
            else -> listOf(body)
        }
    }

    fun updateRawCommandInput(input: String) {
        _uiState.update { it.copy(rawCommandInput = input) }
    }

    fun clearLogs() {
        _uiState.update { it.copy(consoleLogs = emptyList()) }
    }

    private fun appendLog(tag: String, message: String, level: LogLevel) {
        val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = ConsoleLogEntry(
            id = logSequence.getAndIncrement(),
            timestamp = ts,
            tag = tag,
            message = message,
            level = level
        )
        _uiState.update { s ->
            s.copy(consoleLogs = (s.consoleLogs + entry).takeLast(300))
        }
    }
}
