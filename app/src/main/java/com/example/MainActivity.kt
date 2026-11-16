package com.example

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PermDeviceInformation
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.UsbPortController
import com.example.model.ThemeMode
import com.example.model.ToolScreenMode
import com.example.ui.ServiceUiState
import com.example.ui.ServiceViewModel
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.FullscreenCliTerminalScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WorkbenchScreen
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: ServiceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            viewModel.scanUsbPorts()
        }
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isDark = uiState.themeMode == ThemeMode.DARK
            val view = LocalView.current

            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as? Activity)?.window ?: return@SideEffect
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !isDark
                    controller.isAppearanceLightNavigationBars = !isDark
                }
            }

            MyApplicationTheme(
                themeMode = uiState.themeMode,
                themeDesign = uiState.themeDesign,
                fontSize = uiState.fontSize
            ) {
                FastDeckRootApp(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            viewModel.scanUsbPorts()
        }
    }
}

@Composable
fun FastDeckRootApp(
    uiState: ServiceUiState,
    viewModel: ServiceViewModel
) {
    val context = LocalContext.current
    val palette = LocalAppPalette.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onFilePicked(uri)
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.onFolderPicked(treeUri)
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED,
                    UsbManager.ACTION_USB_DEVICE_DETACHED,
                    UsbPortController.ACTION_USB_PERMISSION -> {
                        viewModel.scanUsbPorts(userInitiated = false)
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(UsbPortController.ACTION_USB_PERMISSION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    BackHandler(enabled = !drawerState.isOpen && uiState.screenMode != ToolScreenMode.FASTBOOT_INFO) {
        viewModel.navigateToMode(ToolScreenMode.FASTBOOT_INFO)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = palette.drawerBg,
                drawerContentColor = palette.textPrimary,
                windowInsets = WindowInsets.safeDrawing,
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .widthIn(max = 315.dp)
            ) {
                FastDeckDrawerContent(
                    uiState = uiState,
                    onSelectMode = { mode ->
                        viewModel.navigateToMode(mode)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = palette.screenBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                ToolHeaderTopBar(
                    uiState = uiState,
                    onOpenDrawer = {
                        scope.launch { drawerState.open() }
                    },
                    onCycleMode = {
                        val cycleModes = listOf(
                            ToolScreenMode.ADB_INFO,
                            ToolScreenMode.ADB_UNLOCK,
                            ToolScreenMode.ADB_SERVICE,
                            ToolScreenMode.FASTBOOT_FLASH,
                            ToolScreenMode.FASTBOOT_INFO,
                            ToolScreenMode.FASTBOOT_SERVICE,
                            ToolScreenMode.FASTBOOT_UNLOCK,
                            ToolScreenMode.CMD_TERMINAL
                        )
                        val idx = cycleModes.indexOf(uiState.screenMode)
                        val next = if (idx >= 0) cycleModes[(idx + 1) % cycleModes.size] else ToolScreenMode.FASTBOOT_INFO
                        viewModel.navigateToMode(next)
                    },
                    onUsbBadgeClick = {
                        viewModel.requestActivePortPermission()
                    },
                    onOpenAbout = {
                        viewModel.navigateToMode(ToolScreenMode.ABOUT)
                    },
                    onToggleTheme = {
                        viewModel.toggleTheme()
                    }
                )
            }
        ) { innerPadding ->
            val contentModifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)

            when (uiState.screenMode) {
                ToolScreenMode.CMD_TERMINAL -> FullscreenCliTerminalScreen(
                    uiState = uiState,
                    onTerminalInputChange = { viewModel.updateTerminalInput(it) },
                    onSubmitCommand = { viewModel.submitTerminalCommand(it) },
                    modifier = contentModifier
                )
                ToolScreenMode.SETTINGS -> SettingsScreen(
                    uiState = uiState,
                    onSelectThemeMode = { viewModel.setThemeMode(it) },
                    onSelectThemeDesign = { viewModel.setThemeDesign(it) },
                    onSelectFontSize = { viewModel.setFontSize(it) },
                    onToggleSound = { viewModel.setSoundEnabled(it) },
                    modifier = contentModifier
                )
                ToolScreenMode.ABOUT -> AboutScreen(
                    modifier = contentModifier
                )
                else -> WorkbenchScreen(
                    uiState = uiState,
                    availableActions = viewModel.getAvailableActions(),
                    onSelectBrand = { viewModel.setBrand(it) },
                    onSelectAction = { viewModel.setSelectedAction(it) },
                    onSelectPartition = { viewModel.setPartition(it) },
                    onToggleVerity = { viewModel.toggleDisableVerity() },
                    onToggleClearUserdata = { viewModel.toggleClearUserdata() },
                    onToggleAutoReboot = { viewModel.toggleAutoReboot() },
                    onToggleChecksum = { viewModel.toggleChecksum() },
                    onToggleFastbootd = { viewModel.toggleUseFastbootd() },
                    onToggleStep = { viewModel.toggleFlashStep(it) },
                    onToggleAllSteps = { viewModel.toggleAllFlashSteps(it) },
                    onPickFile = { filePickerLauncher.launch(arrayOf("*/*")) },
                    onPickFolder = { folderPickerLauncher.launch(null) },
                    onScanPorts = { viewModel.scanUsbPorts(userInitiated = true) },
                    onStartPressed = { viewModel.onStartPressed() },
                    onClearLogs = { viewModel.clearLogs() },
                    modifier = contentModifier
                )
            }
        }
    }
}

@Composable
private fun ToolHeaderTopBar(
    uiState: ServiceUiState,
    onOpenDrawer: () -> Unit,
    onCycleMode: () -> Unit,
    onUsbBadgeClick: () -> Unit,
    onOpenAbout: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val palette = LocalAppPalette.current
    val isTerminal = uiState.screenMode == ToolScreenMode.CMD_TERMINAL
    val barBg = if (isTerminal) palette.terminalBg else palette.headerBg

    val badgeBorderColor = when {
        uiState.screenMode.isAdbSection -> palette.badgeAdb
        uiState.screenMode == ToolScreenMode.FASTBOOT_FLASH -> palette.badgeFlash
        uiState.screenMode.isFastbootSection -> palette.badgeFastboot
        uiState.screenMode == ToolScreenMode.CMD_TERMINAL -> palette.badgeTerminal
        uiState.screenMode == ToolScreenMode.SETTINGS -> palette.badgeSetting
        else -> palette.badgeAbout
    }

    Surface(
        color = barBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("drawer_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Menu",
                            tint = palette.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeBorderColor.copy(alpha = if (palette.isDark) 0.14f else 0.10f),
                        border = BorderStroke(1.dp, badgeBorderColor),
                        modifier = Modifier
                            .heightIn(min = 32.dp)
                            .clickable { onCycleMode() }
                            .testTag("active_mode_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (uiState.screenMode) {
                                    ToolScreenMode.FASTBOOT_FLASH -> Icons.Default.Bolt
                                    ToolScreenMode.SETTINGS -> Icons.Default.Settings
                                    ToolScreenMode.ABOUT -> Icons.Default.Info
                                    else -> Icons.Default.Terminal
                                },
                                contentDescription = null,
                                tint = badgeBorderColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = uiState.screenMode.badgeTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val firstPort = uiState.usbPorts.firstOrNull()
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = palette.usbBadgeBg,
                        border = BorderStroke(1.dp, palette.usbBadgeBorder),
                        modifier = Modifier
                            .heightIn(min = 32.dp)
                            .clickable { onUsbBadgeClick() }
                            .testTag("usb_otg_host_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Usb,
                                contentDescription = "USB OTG Status",
                                tint = palette.accentSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (firstPort != null) {
                                    "OTG (${firstPort.vendorHex})"
                                } else {
                                    "USB OTG HOST READY"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = palette.usbBadgeText,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!isTerminal) {
                        Spacer(modifier = Modifier.width(2.dp))

                        IconButton(
                            onClick = onOpenAbout,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("topbar_about_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Open About Screen",
                                tint = palette.accentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("topbar_theme_toggle")
                        ) {
                            Icon(
                                imageVector = if (palette.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Light or Dark Theme",
                                tint = if (palette.isDark) Color(0xFFFACC15) else palette.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = palette.divider)
        }
    }
}

@Composable
private fun FastDeckDrawerContent(
    uiState: ServiceUiState,
    onSelectMode: (ToolScreenMode) -> Unit
) {
    val palette = LocalAppPalette.current
    var adbExpanded by remember { mutableStateOf(true) }
    var fastbootExpanded by remember { mutableStateOf(true) }
    val firstPort = uiState.usbPorts.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .systemBarsPadding()
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Drawer Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FASTDECK OTG",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "All-Android (5–16) Service Suite",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = palette.accentPrimary.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, palette.accentPrimary),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "PRO v3.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accentPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Active OTG Port Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = palette.cardAltBg,
                border = BorderStroke(1.dp, palette.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (firstPort != null) Color(0xFF10B981) else palette.textMuted
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = firstPort?.productName?.uppercase() ?: "WAITING USB OTG",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = firstPort?.vendorHex ?: "0x18D1",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = palette.accentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 1. ADB Expandable Section (Info, Unlock, Service)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (uiState.screenMode.isAdbSection) palette.badgeAdb else palette.cardBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { adbExpanded = !adbExpanded }
                            .padding(vertical = 2.dp)
                            .testTag("drawer_adb_header"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.badgeAdb.copy(alpha = 0.16f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = palette.badgeAdb,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ADB",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "Info • Unlock • Service",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        Icon(
                            imageVector = if (adbExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle ADB Section",
                            tint = palette.badgeAdb
                        )
                    }

                    AnimatedVisibility(visible = adbExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            HorizontalDivider(color = palette.divider)
                            Spacer(modifier = Modifier.height(2.dp))
                            DrawerSubItem(
                                icon = Icons.Default.PermDeviceInformation,
                                iconTint = palette.badgeAdb,
                                title = "Info",
                                subtitle = "Device Props, AVB, Battery & Patch",
                                selected = uiState.screenMode == ToolScreenMode.ADB_INFO,
                                onClick = { onSelectMode(ToolScreenMode.ADB_INFO) },
                                tag = "drawer_adb_info"
                            )
                            DrawerSubItem(
                                icon = Icons.Default.LockOpen,
                                iconTint = palette.actionHighlight,
                                title = "Unlock",
                                subtitle = "FRP, Setup Bypass & Account Helpers",
                                selected = uiState.screenMode == ToolScreenMode.ADB_UNLOCK,
                                onClick = { onSelectMode(ToolScreenMode.ADB_UNLOCK) },
                                tag = "drawer_adb_unlock"
                            )
                            DrawerSubItem(
                                icon = Icons.Default.Build,
                                iconTint = palette.accentSecondary,
                                title = "Service",
                                subtitle = "Debloat, Reboots, Apps & Optimize",
                                selected = uiState.screenMode == ToolScreenMode.ADB_SERVICE,
                                onClick = { onSelectMode(ToolScreenMode.ADB_SERVICE) },
                                tag = "drawer_adb_service"
                            )
                        }
                    }
                }
            }

            // 2. Fastboot Expandable Section (Flash, Info, Service, Unlock)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (uiState.screenMode.isFastbootSection) palette.badgeFastboot else palette.cardBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { fastbootExpanded = !fastbootExpanded }
                            .padding(vertical = 2.dp)
                            .testTag("drawer_fastboot_header"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.badgeFastboot.copy(alpha = 0.16f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = palette.badgeFastboot,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Fastboot",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "Flash • Info • Service • Unlock",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        Icon(
                            imageVector = if (fastbootExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Fastboot Section",
                            tint = palette.badgeFastboot
                        )
                    }

                    AnimatedVisibility(visible = fastbootExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            HorizontalDivider(color = palette.divider)
                            Spacer(modifier = Modifier.height(2.dp))
                            DrawerSubItem(
                                icon = Icons.Default.Bolt,
                                iconTint = palette.badgeFlash,
                                title = "Flash",
                                subtitle = ".img / .bat / UPDATE.APP USB Flasher",
                                selected = uiState.screenMode == ToolScreenMode.FASTBOOT_FLASH,
                                onClick = { onSelectMode(ToolScreenMode.FASTBOOT_FLASH) },
                                tag = "drawer_fastboot_flash"
                            )
                            DrawerSubItem(
                                icon = Icons.Default.PermDeviceInformation,
                                iconTint = palette.badgeFastboot,
                                title = "Info",
                                subtitle = "Getvar All, Slots, ARB & OEM State",
                                selected = uiState.screenMode == ToolScreenMode.FASTBOOT_INFO,
                                onClick = { onSelectMode(ToolScreenMode.FASTBOOT_INFO) },
                                tag = "drawer_fastboot_info"
                            )
                            DrawerSubItem(
                                icon = Icons.Default.Build,
                                iconTint = palette.accentPrimary,
                                title = "Service",
                                subtitle = "Wipe, Slot A/B, Logical Parts & Reboot",
                                selected = uiState.screenMode == ToolScreenMode.FASTBOOT_SERVICE,
                                onClick = { onSelectMode(ToolScreenMode.FASTBOOT_SERVICE) },
                                tag = "drawer_fastboot_service"
                            )
                            DrawerSubItem(
                                icon = Icons.Default.LockOpen,
                                iconTint = palette.actionHighlight,
                                title = "Unlock",
                                subtitle = "Bootloader Unlock, Relock & FRP Erase",
                                selected = uiState.screenMode == ToolScreenMode.FASTBOOT_UNLOCK,
                                onClick = { onSelectMode(ToolScreenMode.FASTBOOT_UNLOCK) },
                                tag = "drawer_fastboot_unlock"
                            )
                        }
                    }
                }
            }

            // 3. Terminal CLI
            DrawerPrimaryCard(
                icon = Icons.Default.DeveloperBoard,
                iconTint = palette.badgeTerminal,
                selected = uiState.screenMode == ToolScreenMode.CMD_TERMINAL,
                title = "Terminal CLI",
                subtitle = "Termux-Style Command Shell",
                onClick = { onSelectMode(ToolScreenMode.CMD_TERMINAL) },
                tag = "drawer_terminal_cli"
            )

            // 4. Setting
            DrawerPrimaryCard(
                icon = Icons.Default.Settings,
                iconTint = palette.badgeSetting,
                selected = uiState.screenMode == ToolScreenMode.SETTINGS,
                title = "Setting",
                subtitle = "Font Size & 3 Theme Designs",
                onClick = { onSelectMode(ToolScreenMode.SETTINGS) },
                tag = "drawer_settings"
            )

            // 5. About
            DrawerPrimaryCard(
                icon = Icons.Default.Info,
                iconTint = palette.badgeAbout,
                selected = uiState.screenMode == ToolScreenMode.ABOUT,
                title = "About",
                subtitle = "Command Reference & App Info",
                onClick = { onSelectMode(ToolScreenMode.ABOUT) },
                tag = "drawer_about"
            )
        }

        Column {
            HorizontalDivider(color = palette.divider)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Android Native USB Engine",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = palette.textMuted
                )
                Text(
                    text = uiState.themeDesign.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accentPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DrawerSubItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val palette = LocalAppPalette.current

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) palette.cardAltBg else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, iconTint.copy(alpha = 0.6f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) palette.textPrimary else palette.textPrimary.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DrawerPrimaryCard(
    icon: ImageVector,
    iconTint: Color,
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String
) {
    val palette = LocalAppPalette.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) palette.cardAltBg else palette.cardBg,
        border = BorderStroke(1.dp, if (selected) iconTint else palette.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = iconTint.copy(alpha = 0.16f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (selected) iconTint else palette.textMuted
            )
        }
    }
}
