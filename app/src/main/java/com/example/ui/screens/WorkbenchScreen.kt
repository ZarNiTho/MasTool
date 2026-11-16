package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CommandRepository
import com.example.model.ConsoleLogEntry
import com.example.model.LogLevel
import com.example.model.ServiceActionItem
import com.example.model.ServiceBrand
import com.example.model.ToolScreenMode
import com.example.ui.ServiceUiState
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.LocalFontConfig
import java.util.Locale

@Composable
fun WorkbenchScreen(
    uiState: ServiceUiState,
    availableActions: List<ServiceActionItem>,
    onSelectBrand: (ServiceBrand) -> Unit,
    onSelectAction: (ServiceActionItem) -> Unit,
    onSelectPartition: (String) -> Unit,
    onToggleVerity: () -> Unit,
    onToggleAutoReboot: () -> Unit,
    onToggleChecksum: () -> Unit,
    onToggleFastbootd: () -> Unit,
    onToggleStep: (String) -> Unit,
    onPickFile: () -> Unit,
    onScanPorts: () -> Unit,
    onStartPressed: () -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    val fontConfig = LocalFontConfig.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.screenBg)
    ) {
        // COMPACT TOP SELECTION & START DECK (Styled with active ThemeDesign accent & glow border)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .border(1.dp, palette.panelBorderBrush, RoundedCornerShape(12.dp))
                .testTag("top_options_panel"),
            shape = RoundedCornerShape(12.dp),
            color = palette.cardBg
        ) {
            Column {
                // Signature ThemeDesign top glow accent stripe
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(palette.cardAccentBrush)
                )
                when (uiState.screenMode) {
                    ToolScreenMode.ADB_INFO,
                    ToolScreenMode.ADB_UNLOCK,
                    ToolScreenMode.ADB_SERVICE,
                    ToolScreenMode.FASTBOOT_INFO,
                    ToolScreenMode.FASTBOOT_SERVICE,
                    ToolScreenMode.FASTBOOT_UNLOCK -> DropdownActionStartPanel(
                        uiState = uiState,
                        availableActions = availableActions,
                        onSelectBrand = onSelectBrand,
                        onSelectAction = onSelectAction,
                        onScanPorts = onScanPorts,
                        onStartPressed = onStartPressed
                    )
                    ToolScreenMode.FASTBOOT_FLASH -> FlashDropdownStartPanel(
                        uiState = uiState,
                        onSelectBrand = onSelectBrand,
                        onSelectPartition = onSelectPartition,
                        onToggleVerity = onToggleVerity,
                        onToggleAutoReboot = onToggleAutoReboot,
                        onToggleChecksum = onToggleChecksum,
                        onToggleFastbootd = onToggleFastbootd,
                        onToggleStep = onToggleStep,
                        onPickFile = onPickFile,
                        onStartPressed = onStartPressed
                    )
                    ToolScreenMode.CMD_TERMINAL,
                    ToolScreenMode.SETTINGS,
                    ToolScreenMode.ABOUT -> Unit
                }
            }
        }

        // MIDDLE STATUS & PROGRESS STRIP
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = palette.statusStripBg
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = palette.divider)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.statusBarText,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f%%", uiState.progressPercent),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = palette.accentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (uiState.isBusy) {
                    LinearProgressIndicator(
                        color = palette.accentPrimary,
                        trackColor = palette.cardBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                    )
                } else {
                    HorizontalDivider(color = palette.divider)
                }
            }
        }

        // EXPANDED BOTTOM LIVE LOG CONSOLE
        LiveLogConsolePanel(
            uiState = uiState,
            consoleFontSp = fontConfig.consoleSp,
            onClearLogs = onClearLogs,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

@Composable
private fun DropdownActionStartPanel(
    uiState: ServiceUiState,
    availableActions: List<ServiceActionItem>,
    onSelectBrand: (ServiceBrand) -> Unit,
    onSelectAction: (ServiceActionItem) -> Unit,
    onScanPorts: () -> Unit,
    onStartPressed: () -> Unit
) {
    val palette = LocalAppPalette.current
    var brandExpanded by remember { mutableStateOf(false) }
    var actionExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            SelectorDropdownBox(
                text = "Brand: ${uiState.selectedBrand.displayName}",
                textColor = palette.textPrimary,
                onClick = { brandExpanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("brand_dropdown")
            )
            DropdownMenu(
                expanded = brandExpanded,
                onDismissRequest = { brandExpanded = false }
            ) {
                ServiceBrand.entries.forEach { brand ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = brand.displayName,
                                fontWeight = if (brand == uiState.selectedBrand) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            brandExpanded = false
                            onSelectBrand(brand)
                        }
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            SelectorDropdownBox(
                text = uiState.selectedAction.title,
                textColor = palette.actionHighlight,
                onClick = { actionExpanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("action_dropdown")
            )
            DropdownMenu(
                expanded = actionExpanded,
                onDismissRequest = { actionExpanded = false },
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                availableActions.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = item.title,
                                fontWeight = if (item.id == uiState.selectedAction.id) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        onClick = {
                            actionExpanded = false
                            onSelectAction(item)
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = palette.cardAltBg,
                border = BorderStroke(1.dp, palette.cardBorder),
                modifier = Modifier
                    .weight(0.85f)
                    .height(40.dp)
                    .clickable { onScanPorts() }
                    .testTag("scan_ports_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan USB",
                        tint = palette.accentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Scan OTG",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onStartPressed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isBusy) Color(0xFFDC2626) else palette.startButtonBg,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .weight(1.45f)
                    .height(40.dp)
                    .testTag("start_action_button")
            ) {
                Icon(
                    imageVector = if (uiState.isBusy) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isBusy) "Stop" else "Start",
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (uiState.isBusy) {
                        "STOP"
                    } else {
                        "START (${uiState.selectedBrand.shortTag.uppercase(Locale.US)})"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FlashDropdownStartPanel(
    uiState: ServiceUiState,
    onSelectBrand: (ServiceBrand) -> Unit,
    onSelectPartition: (String) -> Unit,
    onToggleVerity: () -> Unit,
    onToggleAutoReboot: () -> Unit,
    onToggleChecksum: () -> Unit,
    onToggleFastbootd: () -> Unit,
    onToggleStep: (String) -> Unit,
    onPickFile: () -> Unit,
    onStartPressed: () -> Unit
) {
    val palette = LocalAppPalette.current
    var brandExpanded by remember { mutableStateOf(false) }
    var partitionExpanded by remember { mutableStateOf(false) }
    var showPartsDialog by remember { mutableStateOf(false) }

    val selectedFile = uiState.selectedFlashFile
    val totalParts = selectedFile?.steps?.size ?: 0
    val enabledParts = selectedFile?.steps?.count { it.enabled } ?: 0

    if (showPartsDialog && selectedFile != null) {
        AlertDialog(
            onDismissRequest = { showPartsDialog = false },
            containerColor = palette.cardBg,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(
                    text = "${selectedFile.fileName} ($enabledParts/$totalParts Parts)",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(selectedFile.steps, key = { it.id }) { step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleStep(step.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (step.enabled) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = palette.accentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = step.targetPartition,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPartsDialog = false }) {
                    Text("Done", color = palette.accentPrimary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SelectorDropdownBox(
                    text = "Brand: ${uiState.selectedBrand.shortTag}",
                    textColor = palette.textPrimary,
                    onClick = { brandExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = brandExpanded,
                    onDismissRequest = { brandExpanded = false }
                ) {
                    ServiceBrand.entries.forEach { brand ->
                        DropdownMenuItem(
                            text = { Text(brand.displayName, fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                onSelectBrand(brand)
                                brandExpanded = false
                            }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                SelectorDropdownBox(
                    text = uiState.selectedPartition,
                    textColor = palette.accentPrimary,
                    onClick = { partitionExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = partitionExpanded,
                    onDismissRequest = { partitionExpanded = false },
                    modifier = Modifier.heightIn(max = 280.dp)
                ) {
                    CommandRepository.supportedPartitions.forEach { part ->
                        DropdownMenuItem(
                            text = { Text(part, fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                onSelectPartition(part)
                                partitionExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = palette.cardAltBg,
                border = BorderStroke(1.dp, palette.cardBorder),
                modifier = Modifier
                    .weight(1.4f)
                    .height(38.dp)
                    .clickable { onPickFile() }
                    .testTag("pick_flash_file_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = palette.accentPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedFile?.fileName ?: "Load .img / .bat / UPDATE.APP",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = palette.cardAltBg,
                border = BorderStroke(1.dp, palette.cardBorder),
                modifier = Modifier
                    .weight(0.75f)
                    .height(38.dp)
                    .clickable(enabled = totalParts > 0) { showPartsDialog = true }
                    .testTag("parts_count_button")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$enabledParts/$totalParts Parts",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accentPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1.15f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OptionTogglePill("Verity", uiState.disableVerity, onToggleVerity)
                OptionTogglePill("Reboot", uiState.autoReboot, onToggleAutoReboot)
                OptionTogglePill("Fastbootd", uiState.useFastbootd, onToggleFastbootd)
                OptionTogglePill("CRC", uiState.checksumVerify, onToggleChecksum)
            }

            Button(
                onClick = onStartPressed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isBusy) Color(0xFFDC2626) else palette.startButtonBg,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier
                    .weight(0.95f)
                    .height(38.dp)
                    .testTag("execute_flash_button")
            ) {
                Icon(
                    imageVector = if (uiState.isBusy) Icons.Default.Stop else Icons.Default.Bolt,
                    contentDescription = if (uiState.isBusy) "Stop Flash" else "Start Flash",
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (uiState.isBusy) "STOP" else "START FLASH",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SelectorDropdownBox(
    text: String,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = palette.cardAltBg,
        border = BorderStroke(1.dp, palette.cardBorder),
        modifier = modifier
            .heightIn(min = 38.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun OptionTogglePill(
    label: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalAppPalette.current

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (checked) palette.accentPrimary else palette.cardAltBg,
        border = BorderStroke(
            1.dp,
            if (checked) palette.accentPrimary else palette.cardBorder
        ),
        modifier = Modifier
            .height(34.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (checked) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (checked) Color.White else palette.textMuted,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (checked) Color.White else palette.textSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LiveLogConsolePanel(
    uiState: ServiceUiState,
    consoleFontSp: androidx.compose.ui.unit.TextUnit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.consoleLogs.size) {
        if (uiState.consoleLogs.isNotEmpty()) {
            listState.animateScrollToItem(uiState.consoleLogs.lastIndex)
        }
    }

    Surface(
        modifier = modifier.testTag("bottom_terminal_panel"),
        color = palette.consoleBg
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.headerBg)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(palette.accentPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE LOG CONSOLE",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = palette.accentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${uiState.consoleLogs.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = palette.textMuted
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val allLogs = uiState.consoleLogs.joinToString("\n") {
                                "[${it.timestamp}] ${it.message}"
                            }
                            clipboard.setText(AnnotatedString(allLogs))
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("copy_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Console Logs",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(
                        onClick = onClearLogs,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("clear_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Console Logs",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = palette.divider)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("terminal_lazy_list"),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(uiState.consoleLogs, key = { it.id }) { entry ->
                    ConsoleTimestampRow(
                        entry = entry,
                        consoleFontSp = consoleFontSp
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsoleTimestampRow(
    entry: ConsoleLogEntry,
    consoleFontSp: androidx.compose.ui.unit.TextUnit
) {
    val palette = LocalAppPalette.current
    val msgColor = if (palette.isDark) {
        when (entry.level) {
            LogLevel.INIT -> palette.accentPrimary
            LogLevel.CMD -> palette.accentSecondary
            LogLevel.USB -> Color(0xFF60A5FA)
            LogLevel.OKAY -> Color(0xFF34D399)
            LogLevel.INFO -> palette.textPrimary
            LogLevel.WARN -> Color(0xFFFACC15)
            LogLevel.FAIL -> Color(0xFFEF4444)
        }
    } else {
        when (entry.level) {
            LogLevel.INIT -> palette.accentPrimary
            LogLevel.CMD -> palette.accentSecondary
            LogLevel.USB -> Color(0xFF1D4ED8)
            LogLevel.OKAY -> Color(0xFF059669)
            LogLevel.INFO -> palette.textPrimary
            LogLevel.WARN -> Color(0xFFB45309)
            LogLevel.FAIL -> Color(0xFFDC2626)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "[${entry.timestamp}]",
            fontSize = consoleFontSp,
            fontFamily = FontFamily.Monospace,
            color = palette.textMuted
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = entry.message,
            fontSize = consoleFontSp,
            fontFamily = FontFamily.Monospace,
            color = msgColor,
            modifier = Modifier.weight(1f)
        )
    }
}
