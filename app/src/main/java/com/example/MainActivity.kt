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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhoneAndroid
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
import com.example.ui.screens.AboutCommandReferenceDialog
import com.example.ui.screens.FullscreenCliTerminalScreen
import com.example.ui.screens.WorkbenchScreen
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

            MyApplicationTheme(darkTheme = isDark) {
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isDark = uiState.themeMode == ThemeMode.DARK

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onFilePicked(uri)
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED,
                    UsbManager.ACTION_USB_DEVICE_DETACHED,
                    UsbPortController.ACTION_USB_PERMISSION -> {
                        viewModel.scanUsbPorts()
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

    BackHandler(enabled = uiState.screenMode != ToolScreenMode.FASTBOOT_ONECLICK && !drawerState.isOpen) {
        viewModel.navigateToMode(ToolScreenMode.FASTBOOT_ONECLICK)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = if (isDark) Color(0xFF0B101B) else Color(0xFFF8FAFC),
                drawerContentColor = if (isDark) Color.White else Color(0xFF0F172A),
                windowInsets = WindowInsets.safeDrawing,
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .widthIn(max = 310.dp)
            ) {
                AdbFastbootDrawerContent(
                    uiState = uiState,
                    isDark = isDark,
                    onSelectMode = { mode ->
                        viewModel.navigateToMode(mode)
                        scope.launch { drawerState.close() }
                    },
                    onOpenAbout = {
                        scope.launch { drawerState.close() }
                        viewModel.setShowAboutDialog(true)
                    },
                    onToggleTheme = { viewModel.toggleTheme() }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = if (isDark) Color(0xFF060911) else Color(0xFFF1F5F9),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                ToolHeaderTopBar(
                    uiState = uiState,
                    onOpenDrawer = {
                        scope.launch { drawerState.open() }
                    },
                    onCycleMode = {
                        val modes = ToolScreenMode.entries
                        val next = modes[(modes.indexOf(uiState.screenMode) + 1) % modes.size]
                        viewModel.navigateToMode(next)
                    },
                    onUsbBadgeClick = {
                        viewModel.requestActivePortPermission()
                    },
                    onOpenAbout = {
                        viewModel.setShowAboutDialog(true)
                    },
                    onToggleTheme = {
                        viewModel.toggleTheme()
                    }
                )
            }
        ) { innerPadding ->
            if (uiState.showAboutDialog) {
                AboutCommandReferenceDialog(
                    isDark = isDark,
                    onDismiss = { viewModel.setShowAboutDialog(false) }
                )
            }
            if (uiState.screenMode == ToolScreenMode.CMD_TERMINAL) {
                FullscreenCliTerminalScreen(
                    uiState = uiState,
                    onTerminalInputChange = { viewModel.updateTerminalInput(it) },
                    onSubmitCommand = { viewModel.submitTerminalCommand(it) },
                    modifier = Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                )
            } else {
                WorkbenchScreen(
                    uiState = uiState,
                    availableActions = viewModel.getAvailableActions(),
                    onSelectBrand = { viewModel.setBrand(it) },
                    onSelectActionGroup = { viewModel.setActionGroup(it) },
                    onSelectAction = { viewModel.setSelectedAction(it) },
                    onSelectPartition = { viewModel.setPartition(it) },
                    onToggleVerity = { viewModel.toggleDisableVerity() },
                    onToggleAutoReboot = { viewModel.toggleAutoReboot() },
                    onToggleChecksum = { viewModel.toggleChecksum() },
                    onToggleFastbootd = { viewModel.toggleUseFastbootd() },
                    onToggleStep = { viewModel.toggleFlashStep(it) },
                    onPickFile = { filePickerLauncher.launch(arrayOf("*/*")) },
                    onScanPorts = { viewModel.scanUsbPorts() },
                    onStartPressed = { viewModel.onStartPressed() },
                    onExecuteCommand = { viewModel.executeCommand(it) },
                    onRawCommandChange = { viewModel.updateRawCommandInput(it) },
                    onClearLogs = { viewModel.clearLogs() },
                    onDismissAbout = { viewModel.setShowAboutDialog(false) },
                    modifier = Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
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
    val isTerminal = uiState.screenMode == ToolScreenMode.CMD_TERMINAL
    val isDark = uiState.themeMode == ThemeMode.DARK
    val barBg = when {
        isTerminal && isDark -> Color(0xFF000000)
        isTerminal && !isDark -> Color(0xFFFFFFFF)
        isDark -> Color(0xFF070B14)
        else -> Color(0xFFFFFFFF)
    }
    val badgeBorderColor = when (uiState.screenMode) {
        ToolScreenMode.FASTBOOT_ONECLICK -> if (isDark) Color(0xFF10B981) else Color(0xFF059669)
        ToolScreenMode.FASTBOOT_FLASH -> if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)
        ToolScreenMode.ADB_ONECLICK -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
        ToolScreenMode.CMD_TERMINAL -> if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
    }

    val usbBg = if (isDark) Color(0xFF1E1B4B) else Color(0xFFEEF2FF)
    val usbBorder = if (isDark) Color(0xFF4F46E5) else Color(0xFF6366F1)
    val usbIconTint = if (isDark) Color(0xFF818CF8) else Color(0xFF4338CA)
    val usbTextColor = if (isDark) Color.White else Color(0xFF312E81)
    val infoTint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

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
                            .size(36.dp)
                            .testTag("drawer_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Menu",
                            tint = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeBorderColor.copy(alpha = if (isDark) 0.14f else 0.10f),
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
                                color = if (isDark) Color.White else Color(0xFF0F172A),
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
                        color = usbBg,
                        border = BorderStroke(1.dp, usbBorder),
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
                                tint = usbIconTint,
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
                                color = usbTextColor,
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
                                .size(34.dp)
                                .testTag("topbar_about_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "About Commands",
                                tint = infoTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("topbar_theme_toggle")
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Light or Dark Theme",
                                tint = if (isDark) Color(0xFFFACC15) else Color(0xFF0F172A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(
                color = when {
                    isTerminal && isDark -> Color(0xFF1A1A1A)
                    isDark -> Color(0xFF1E293B)
                    else -> Color(0xFFCBD5E1)
                }
            )
        }
    }
}

@Composable
private fun AdbFastbootDrawerContent(
    uiState: ServiceUiState,
    isDark: Boolean,
    onSelectMode: (ToolScreenMode) -> Unit,
    onOpenAbout: () -> Unit,
    onToggleTheme: () -> Unit
) {
    var fastbootExpanded by remember { mutableStateOf(true) }
    val firstPort = uiState.usbPorts.firstOrNull()

    val primaryText = if (isDark) Color.White else Color(0xFF0F172A)
    val secondaryText = if (isDark) Color(0xFF64748B) else Color(0xFF475569)
    val cardBg = if (isDark) Color(0xFF111827) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)
    val statusBoxBg = if (isDark) Color(0xFF131B2E) else Color(0xFFF1F5F9)
    val accentBlue = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .systemBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NATIVE UNLOCK TOOL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Multi-Brand USB OTG Platform",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = Color(0xFF2563EB),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "PRO v3.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusBoxBg,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                    if (firstPort != null) Color(0xFF10B981) else Color(0xFF64748B)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = firstPort?.productName?.uppercase() ?: "WAITING USB OTG",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF334155),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = firstPort?.vendorHex ?: "0x18D1",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = accentBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { fastbootExpanded = !fastbootExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF3B1821) else Color(0xFFFEE2E2),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Fastboot Mode (A10–A16)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Bootloader / Fastbootd / Flash",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)
                        )
                    }

                    if (fastbootExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = cardBorder)
                        Spacer(modifier = Modifier.height(8.dp))

                        DrawerSubItem(
                            icon = Icons.Default.LockOpen,
                            iconTint = if (isDark) Color(0xFFFACC15) else Color(0xFFD97706),
                            title = "Fastboot One-Click",
                            subtitle = "Bootloader Unlock, FRP & OEM Service",
                            selected = uiState.screenMode == ToolScreenMode.FASTBOOT_ONECLICK,
                            isDark = isDark,
                            onClick = { onSelectMode(ToolScreenMode.FASTBOOT_ONECLICK) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        DrawerSubItem(
                            icon = Icons.Default.Bolt,
                            iconTint = accentBlue,
                            title = "Flash Tool",
                            subtitle = ".img / .bat / UPDATE.APP USB Flasher",
                            selected = uiState.screenMode == ToolScreenMode.FASTBOOT_FLASH,
                            isDark = isDark,
                            onClick = { onSelectMode(ToolScreenMode.FASTBOOT_FLASH) }
                        )
                    }
                }
            }

            DrawerPrimaryCard(
                icon = Icons.Default.PhoneAndroid,
                iconBg = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                iconTint = if (isDark) Color(0xFF10B981) else Color(0xFF059669),
                cardBg = cardBg,
                primaryText = primaryText,
                secondaryText = secondaryText,
                borderColor = if (uiState.screenMode == ToolScreenMode.ADB_ONECLICK) {
                    Color(0xFF10B981)
                } else {
                    cardBorder
                },
                title = "ADB One-Click Service",
                subtitle = "ADB System Info, Debloat & Reboots",
                onClick = { onSelectMode(ToolScreenMode.ADB_ONECLICK) }
            )

            DrawerPrimaryCard(
                icon = Icons.Default.DeveloperBoard,
                iconBg = if (isDark) Color(0xFF1E3A8A) else Color(0xFFE0F2FE),
                iconTint = accentBlue,
                cardBg = cardBg,
                primaryText = primaryText,
                secondaryText = secondaryText,
                borderColor = if (uiState.screenMode == ToolScreenMode.CMD_TERMINAL) {
                    accentBlue
                } else {
                    cardBorder
                },
                title = "Dedicated Terminal",
                subtitle = "Custom Command Execution Shell",
                onClick = { onSelectMode(ToolScreenMode.CMD_TERMINAL) }
            )
        }

        Column {
            HorizontalDivider(color = cardBorder)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Android Native USB Engine",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = secondaryText
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onToggleTheme() }
                            .testTag("drawer_theme_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = if (isDark) Color(0xFFFACC15) else Color(0xFF0F172A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isDark) "Light" else "Dark",
                            style = MaterialTheme.typography.labelMedium,
                            color = primaryText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onOpenAbout() }
                            .testTag("drawer_about_button")
                    )
                }
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
    isDark: Boolean,
    onClick: () -> Unit
) {
    val selectedBg = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val titleColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subColor = if (isDark) Color(0xFF64748B) else Color(0xFF475569)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) selectedBg else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = subColor,
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
    iconBg: Color,
    iconTint: Color,
    cardBg: Color,
    primaryText: Color,
    secondaryText: Color,
    borderColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = iconBg,
                    modifier = Modifier.size(36.dp)
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
                        color = primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = secondaryText
            )
        }
    }
}
