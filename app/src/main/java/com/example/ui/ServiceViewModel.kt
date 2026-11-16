package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEffectManager
import com.example.data.CommandRepository
import com.example.data.FlashFileProcessor
import com.example.data.UsbPortController
import com.example.model.ActionGroup
import com.example.model.AppFontSize
import com.example.model.ConsoleLogEntry
import com.example.model.LogLevel
import com.example.model.SelectedFlashFile
import com.example.model.ServiceActionItem
import com.example.model.ServiceBrand
import com.example.model.TerminalLine
import com.example.model.ThemeDesign
import com.example.model.ThemeMode
import com.example.model.ToolScreenMode
import com.example.model.UsbPortInfo
import com.example.model.UsbProtocolType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    val themeDesign: ThemeDesign = ThemeDesign.CYBER_NEON,
    val fontSize: AppFontSize = AppFontSize.MEDIUM,
    val soundEnabled: Boolean = true,
    val screenMode: ToolScreenMode = ToolScreenMode.FASTBOOT_INFO,
    val usbPorts: List<UsbPortInfo> = emptyList(),
    val selectedPortName: String? = null,
    val selectedBrand: ServiceBrand = ServiceBrand.UNIVERSAL,
    val selectedAction: ServiceActionItem = CommandRepository.allActions.first {
        !it.isAdb && it.group == ActionGroup.INFO
    },
    val selectedPartition: String = CommandRepository.supportedPartitions.first(),
    val disableVerity: Boolean = true,
    val autoReboot: Boolean = true,
    val checksumVerify: Boolean = true,
    val useFastbootd: Boolean = false,
    val selectedFlashFile: SelectedFlashFile? = null,
    val isBusy: Boolean = false,
    val statusBarText: String = "USB OTG Ready",
    val progressPercent: Float = 0f,
    val consoleLogs: List<ConsoleLogEntry> = emptyList(),
    val terminalInput: String = "",
    val terminalLines: List<TerminalLine> = emptyList(),
    val terminalHistory: List<String> = emptyList()
)

class ServiceViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.applicationContext.getSharedPreferences(
        "fastdeck_otg_prefs",
        Context.MODE_PRIVATE
    )
    private val usbController = UsbPortController(application.applicationContext)
    private val soundManager = SoundEffectManager()
    private val logSequence = AtomicLong(1L)
    private val terminalSequence = AtomicLong(1L)

    private var activeOperationJob: Job? = null
    private var hasCompletedInitialUsbScan = false
    private var knownPortsByName: Map<String, UsbPortInfo> = emptyMap()

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<ServiceUiState> = _uiState.asStateFlow()

    init {
        soundManager.isEnabled = _uiState.value.soundEnabled
        scanUsbPorts(userInitiated = false)
    }

    private fun loadInitialState(): ServiceUiState {
        val savedMode = prefs.getString("theme_mode", ThemeMode.DARK.name)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.DARK
        val savedDesign = prefs.getString("theme_design", ThemeDesign.CYBER_NEON.name)
            ?.let { runCatching { ThemeDesign.valueOf(it) }.getOrNull() }
            ?: ThemeDesign.CYBER_NEON
        val savedFont = prefs.getString("font_size", AppFontSize.MEDIUM.name)
            ?.let { runCatching { AppFontSize.valueOf(it) }.getOrNull() }
            ?: AppFontSize.MEDIUM
        val savedSound = prefs.getBoolean("sound_enabled", true)

        return ServiceUiState(
            themeMode = savedMode,
            themeDesign = savedDesign,
            fontSize = savedFont,
            soundEnabled = savedSound
        )
    }

    /**
     * Strictly filters actions by active Drawer sub-mode (ADB/Fastboot + Info/Unlock/Service)
     * and selected Brand so every section stays focused and uncluttered.
     */
    fun getAvailableActions(
        mode: ToolScreenMode = _uiState.value.screenMode,
        brand: ServiceBrand = _uiState.value.selectedBrand
    ): List<ServiceActionItem> {
        val modeFiltered = when (mode) {
            ToolScreenMode.ADB_INFO -> CommandRepository.allActions.filter {
                it.isAdb && it.group == ActionGroup.INFO
            }
            ToolScreenMode.ADB_UNLOCK -> CommandRepository.allActions.filter {
                it.isAdb && it.group == ActionGroup.UNLOCK
            }
            ToolScreenMode.ADB_SERVICE -> CommandRepository.allActions.filter {
                it.isAdb && it.group == ActionGroup.SERVICE
            }
            ToolScreenMode.FASTBOOT_INFO -> CommandRepository.allActions.filter {
                !it.isAdb && it.group == ActionGroup.INFO
            }
            ToolScreenMode.FASTBOOT_UNLOCK -> CommandRepository.allActions.filter {
                !it.isAdb && it.group == ActionGroup.UNLOCK
            }
            ToolScreenMode.FASTBOOT_SERVICE,
            ToolScreenMode.FASTBOOT_FLASH -> CommandRepository.allActions.filter {
                !it.isAdb && it.group == ActionGroup.SERVICE
            }
            ToolScreenMode.CMD_TERMINAL,
            ToolScreenMode.SETTINGS,
            ToolScreenMode.ABOUT -> emptyList()
        }
        val brandFiltered = modeFiltered.filter { it.brand == brand }
        return brandFiltered.ifEmpty {
            modeFiltered.filter { it.brand == ServiceBrand.UNIVERSAL }.ifEmpty { modeFiltered }
        }
    }

    fun toggleTheme() {
        val next = if (_uiState.value.themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
        setThemeMode(next)
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setThemeDesign(design: ThemeDesign) {
        prefs.edit().putString("theme_design", design.name).apply()
        _uiState.update { it.copy(themeDesign = design) }
    }

    fun setFontSize(fontSize: AppFontSize) {
        prefs.edit().putString("font_size", fontSize.name).apply()
        _uiState.update { it.copy(fontSize = fontSize) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
        soundManager.isEnabled = enabled
        _uiState.update { it.copy(soundEnabled = enabled) }
    }

    fun navigateToMode(mode: ToolScreenMode) {
        val actions = getAvailableActions(mode = mode, brand = _uiState.value.selectedBrand)
        _uiState.update { state ->
            val nextAction = if (actions.isNotEmpty() && state.selectedAction !in actions) {
                actions.first()
            } else {
                state.selectedAction
            }
            state.copy(
                screenMode = mode,
                selectedAction = nextAction
            )
        }
    }

    fun setBrand(brand: ServiceBrand) {
        val available = getAvailableActions(mode = _uiState.value.screenMode, brand = brand)
        _uiState.update { state ->
            state.copy(
                selectedBrand = brand,
                selectedAction = available.firstOrNull() ?: state.selectedAction
            )
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

    fun scanUsbPorts(userInitiated: Boolean = true) {
        viewModelScope.launch {
            val ports = withContext(Dispatchers.IO) {
                usbController.scanPorts()
            }
            val currentMap = ports.associateBy { it.deviceName }
            val newlyAttached = ports.filter { !knownPortsByName.containsKey(it.deviceName) }
            val detached = knownPortsByName.values.filter { !currentMap.containsKey(it.deviceName) }

            if (hasCompletedInitialUsbScan) {
                if (newlyAttached.isNotEmpty()) {
                    soundManager.playPortAttached()
                } else if (detached.isNotEmpty()) {
                    soundManager.playPortDetached()
                }
            }

            detached.forEach { removed ->
                appendLog(
                    tag = "USB",
                    message = "Port Disconnected: ${removed.productName}",
                    level = LogLevel.WARN
                )
            }

            newlyAttached.forEach { p ->
                appendLog(
                    tag = "USB",
                    message = "Port Connected: ${p.productName} [${p.protocolType.label}]",
                    level = LogLevel.USB
                )
            }

            ports.filter { !it.hasPermission }.forEach { p ->
                usbController.requestPermission(p.deviceName)
            }

            knownPortsByName = currentMap
            hasCompletedInitialUsbScan = true

            val first = ports.firstOrNull()
            _uiState.update {
                it.copy(
                    usbPorts = ports,
                    selectedPortName = first?.deviceName,
                    statusBarText = if (first != null) {
                        "Connected: ${first.productName} (${first.protocolType.label})"
                    } else {
                        "USB OTG Ready"
                    }
                )
            }

            if (userInitiated && ports.isEmpty() && detached.isEmpty()) {
                appendLog(
                    tag = "USB",
                    message = "No USB OTG device detected.",
                    level = LogLevel.INFO
                )
            }
        }
    }

    fun requestActivePortPermission() {
        val port = _uiState.value.usbPorts.firstOrNull()
        if (port != null) {
            usbController.requestPermission(port.deviceName)
        } else {
            scanUsbPorts(userInitiated = true)
        }
    }

    fun onFilePicked(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    statusBarText = "Loading firmware..."
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
                message = "Loaded: ${parsed.fileName} (${parsed.formattedSize}, ${parsed.steps.size} part)",
                level = LogLevel.OKAY
            )
        }
    }

    fun onStartPressed() {
        if (_uiState.value.isBusy) {
            stopActiveOperation()
            return
        }
        val state = _uiState.value
        when (state.screenMode) {
            ToolScreenMode.FASTBOOT_FLASH -> executeDirectUsbFlash()
            ToolScreenMode.CMD_TERMINAL -> submitTerminalCommand(state.terminalInput)
            ToolScreenMode.ADB_INFO,
            ToolScreenMode.ADB_UNLOCK,
            ToolScreenMode.ADB_SERVICE,
            ToolScreenMode.FASTBOOT_INFO,
            ToolScreenMode.FASTBOOT_SERVICE,
            ToolScreenMode.FASTBOOT_UNLOCK -> {
                executeServiceAction(state.selectedAction, state.selectedBrand)
            }
            ToolScreenMode.SETTINGS,
            ToolScreenMode.ABOUT -> Unit
        }
    }

    fun stopActiveOperation() {
        val job = activeOperationJob
        activeOperationJob = null
        job?.cancel()
        if (_uiState.value.isBusy) {
            soundManager.playActionStop()
            _uiState.update {
                it.copy(
                    isBusy = false,
                    statusBarText = "Operation Stopped"
                )
            }
            appendLog(
                tag = "STOP",
                message = "Operation stopped.",
                level = LogLevel.WARN
            )
        }
    }

    fun updateTerminalInput(input: String) {
        _uiState.update { it.copy(terminalInput = input) }
    }

    /**
     * Executes pure Termux-style CLI commands strictly inside the dedicated Terminal screen.
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

        activeOperationJob = viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }

            try {
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
                                ports.forEach { p ->
                                    appendTerminalLine("${p.serialNumber}\t${p.protocolType.label}")
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
                                        verboseWireLogs = false,
                                        onProgress = {},
                                        onLog = { msg, lvl ->
                                            appendTerminalLine(msg, isError = lvl == LogLevel.FAIL)
                                        }
                                    )
                                } else {
                                    val wireList = toFastbootWireProtocolList(cmd)
                                    usbController.executeFastbootWireCommands(
                                        deviceName = portName,
                                        wireCommands = wireList,
                                        verboseWireLogs = false,
                                        onLog = { msg, lvl ->
                                            appendTerminalLine(msg, isError = lvl == LogLevel.FAIL)
                                        }
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
                                    verboseWireLogs = false,
                                    onLog = { msg, lvl ->
                                        appendTerminalLine(msg, isError = lvl == LogLevel.FAIL)
                                    }
                                )
                            }
                        }

                        else -> {
                            val portName = activePort?.deviceName
                            if (portName != null && activePort.protocolType == UsbProtocolType.ADB) {
                                usbController.executeAdbOverUsbOtg(
                                    deviceName = portName,
                                    command = cmd,
                                    verboseWireLogs = false,
                                    onLog = { msg, lvl ->
                                        appendTerminalLine(msg, isError = lvl == LogLevel.FAIL)
                                    }
                                )
                            } else {
                                usbController.executeLocalShell(
                                    command = cmd,
                                    onLog = { msg, lvl ->
                                        appendTerminalLine(msg, isError = lvl == LogLevel.FAIL)
                                    }
                                )
                            }
                        }
                    }
                }
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
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
            appendLog("FLASH", "Load a firmware file first.", LogLevel.WARN)
            return
        }
        val enabledSteps = file.steps.filter { it.enabled }
        if (enabledSteps.isEmpty()) {
            appendLog("FLASH", "No partitions selected.", LogLevel.WARN)
            return
        }
        val portName = state.selectedPortName ?: run {
            appendLog("FLASH", "No USB OTG device connected.", LogLevel.FAIL)
            return
        }

        soundManager.playActionStart()
        activeOperationJob = viewModelScope.launch {
            var completedNormally = false
            try {
                _uiState.update {
                    it.copy(
                        isBusy = true,
                        progressPercent = 0f,
                        statusBarText = "Flashing ${file.fileName}..."
                    )
                }
                appendLog(
                    tag = "FLASH",
                    message = "Start Flash: ${file.fileName} (${enabledSteps.size} partition)",
                    level = LogLevel.CMD
                )
                val uri = Uri.parse(file.uriString)

                withContext(Dispatchers.IO) {
                    if (state.useFastbootd) {
                        appendLog("FLASH", "Entering Fastbootd...", LogLevel.USB)
                        usbController.executeFastbootWireCommands(
                            deviceName = portName,
                            wireCommands = listOf("reboot-fastboot"),
                            verboseWireLogs = false
                        ) { msg, lvl ->
                            appendLog("FLASH", msg, lvl)
                        }
                    }

                    enabledSteps.forEachIndexed { idx, step ->
                        appendLog(
                            tag = "FLASH",
                            message = "[${idx + 1}/${enabledSteps.size}] ${step.targetPartition}",
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
                                verboseWireLogs = false,
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
                                verboseWireLogs = false,
                                onLog = { msg, lvl -> appendLog("USB", msg, lvl) }
                            )
                        }
                    }

                    if (state.autoReboot) {
                        appendLog("FLASH", "Rebooting device...", LogLevel.INFO)
                        usbController.executeFastbootWireCommands(
                            deviceName = portName,
                            wireCommands = listOf("reboot"),
                            verboseWireLogs = false
                        ) { msg, lvl ->
                            appendLog("USB", msg, lvl)
                        }
                    }
                }
                completedNormally = true
                appendLog("DONE", "Flash finished.", LogLevel.OKAY)
            } catch (c: CancellationException) {
                throw c
            } finally {
                if (completedNormally) {
                    soundManager.playActionStop()
                }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        progressPercent = if (completedNormally) 100f else 0f,
                        statusBarText = "USB OTG Ready"
                    )
                }
            }
        }
    }

    private fun executeServiceAction(action: ServiceActionItem, brand: ServiceBrand) {
        if (_uiState.value.isBusy) return
        val cmd = action.command.trim()
        if (cmd.isEmpty()) return

        soundManager.playActionStart()
        activeOperationJob = viewModelScope.launch {
            var completedNormally = false
            try {
                _uiState.update {
                    it.copy(
                        isBusy = true,
                        statusBarText = "Running: ${action.title}",
                        progressPercent = 0f
                    )
                }
                appendLog(
                    tag = "START",
                    message = "[${brand.shortTag}] ${action.title}",
                    level = LogLevel.CMD
                )

                val state = _uiState.value
                val portName = state.selectedPortName

                withContext(Dispatchers.IO) {
                    when {
                        cmd.startsWith("fastboot", ignoreCase = true) -> {
                            if (portName == null) {
                                appendLog(
                                    tag = "FAIL",
                                    message = "No Fastboot USB OTG device connected.",
                                    level = LogLevel.FAIL
                                )
                            } else {
                                val wireList = toFastbootWireProtocolList(cmd)
                                usbController.executeFastbootWireCommands(
                                    deviceName = portName,
                                    wireCommands = wireList,
                                    verboseWireLogs = false,
                                    onLog = { msg, lvl -> appendLog("FB", msg, lvl) }
                                )
                            }
                        }
                        else -> {
                            if (portName != null) {
                                usbController.executeAdbOverUsbOtg(
                                    deviceName = portName,
                                    command = cmd,
                                    verboseWireLogs = false,
                                    onLog = { msg, lvl -> appendLog("ADB", msg, lvl) }
                                )
                            } else {
                                appendLog(
                                    tag = "FAIL",
                                    message = "No ADB USB OTG device connected.",
                                    level = LogLevel.FAIL
                                )
                            }
                        }
                    }
                }
                completedNormally = true
                appendLog(
                    tag = "DONE",
                    message = "${action.title} finished.",
                    level = LogLevel.OKAY
                )
            } catch (c: CancellationException) {
                throw c
            } finally {
                if (completedNormally) {
                    soundManager.playActionStop()
                }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusBarText = "USB OTG Ready"
                    )
                }
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
