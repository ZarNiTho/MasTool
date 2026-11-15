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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.sp
import com.example.data.CommandRepository
import com.example.model.ActionGroup
import com.example.model.ConsoleLogEntry
import com.example.model.LogLevel
import com.example.model.ServiceActionItem
import com.example.model.ServiceBrand
import com.example.model.ThemeMode
import com.example.model.ToolScreenMode
import com.example.ui.ServiceUiState
import java.util.Locale

@Composable
fun WorkbenchScreen(
    uiState: ServiceUiState,
    availableActions: List<ServiceActionItem>,
    onSelectBrand: (ServiceBrand) -> Unit,
    onSelectActionGroup: (ActionGroup) -> Unit,
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
    onExecuteCommand: (String) -> Unit,
    onRawCommandChange: (String) -> Unit,
    onClearLogs: () -> Unit,
    onDismissAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = uiState.themeMode == ThemeMode.DARK
    val screenBg = if (isDark) Color(0xFF060911) else Color(0xFFF1F5F9)
    val cardBg = if (isDark) Color(0xFF111827) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF1F2937) else Color(0xFFCBD5E1)
    val statusStripBg = if (isDark) Color(0xFF0B111E) else Color(0xFFE2E8F0)
    val statusStripText = if (isDark) Color(0xFF94A3B8) else Color(0xFF334155)
    val consoleBg = if (isDark) Color(0xFF090D16) else Color(0xFFFFFFFF)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        // COMPACT TOP SELECTION & START DECK (Clean, uncluttered, auto-fitting)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                .testTag("top_options_panel"),
            shape = RoundedCornerShape(12.dp),
            color = cardBg
        ) {
            when (uiState.screenMode) {
                ToolScreenMode.FASTBOOT_ONECLICK,
                ToolScreenMode.ADB_ONECLICK -> DropdownActionStartPanel(
                    uiState = uiState,
                    isDark = isDark,
                    availableActions = availableActions,
                    onSelectBrand = onSelectBrand,
                    onSelectAction = onSelectAction,
                    onScanPorts = onScanPorts,
                    onStartPressed = onStartPressed
                )
                ToolScreenMode.FASTBOOT_FLASH -> FlashDropdownStartPanel(
                    uiState = uiState,
                    isDark = isDark,
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
                ToolScreenMode.CMD_TERMINAL -> Unit
            }
        }

        // MIDDLE STATUS & PROGRESS STRIP
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = statusStripBg
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = cardBorder)
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
                        color = statusStripText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f%%", uiState.progressPercent),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = statusStripText
                    )
                }
                if (uiState.isBusy) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                    )
                } else {
                    HorizontalDivider(color = cardBorder)
                }
            }
        }

        // EXPANDED BOTTOM LIVE LOG CONSOLE
        LiveLogConsolePanel(
            uiState = uiState,
            isDark = isDark,
            consoleBg = consoleBg,
            onClearLogs = onClearLogs,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

/**
 * Clean One-Click Selector (Fastboot Tab has ONLY Fastboot actions; ADB Tab has ONLY ADB actions).
 */
@Composable
private fun DropdownActionStartPanel(
    uiState: ServiceUiState,
    isDark: Boolean,
    availableActions: List<ServiceActionItem>,
    onSelectBrand: (ServiceBrand) -> Unit,
    onSelectAction: (ServiceActionItem) -> Unit,
    onScanPorts: () -> Unit,
    onStartPressed: () -> Unit
) {
    var brandExpanded by remember { mutableStateOf(false) }
    var actionExpanded by remember { mutableStateOf(false) }

    val primaryText = if (isDark) Color.White else Color(0xFF0F172A)
    val actionHighlightText = if (isDark) Color(0xFFFACC15) else Color(0xFF0369A1)
    val scanBg = if (isDark) Color(0xFF162032) else Color(0xFFF8FAFC)
    val scanBorder = if (isDark) Color(0xFF283548) else Color(0xFFCBD5E1)
    val scanIconTint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: Brand Dropdown
        Box(modifier = Modifier.fillMaxWidth()) {
            SelectorDropdownBox(
                text = "Brand: ${uiState.selectedBrand.displayName}",
                textColor = primaryText,
                isDark = isDark,
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

        // Row 2: Clean Single-Line Action Dropdown
        Box(modifier = Modifier.fillMaxWidth()) {
            SelectorDropdownBox(
                text = uiState.selectedAction.title,
                textColor = actionHighlightText,
                isDark = isDark,
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
                                fontSize = 13.sp,
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

        // Row 3: Scan OTG + Single Primary START Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = scanBg,
                border = BorderStroke(1.dp, scanBorder),
                modifier = Modifier
                    .weight(0.85f)
                    .height(38.dp)
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
                        tint = scanIconTint,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Scan OTG",
                        style = MaterialTheme.typography.labelMedium,
                        color = primaryText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onStartPressed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF16A34A),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .weight(1.45f)
                    .height(38.dp)
                    .testTag("start_action_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start",
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "START (${uiState.selectedBrand.shortTag.uppercase(Locale.US)})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Flash Tool Panel with Brand Dropdown, Partition Dropdown, Storage File Loader, and Single START Button
 */
@Composable
private fun FlashDropdownStartPanel(
    uiState: ServiceUiState,
    isDark: Boolean,
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
    var brandExpanded by remember { mutableStateOf(false) }
    var partitionExpanded by remember { mutableStateOf(false) }
    var showPartsDialog by remember { mutableStateOf(false) }

    val selectedFile = uiState.selectedFlashFile
    val totalParts = selectedFile?.steps?.size ?: 0
    val enabledParts = selectedFile?.steps?.count { it.enabled } ?: 0

    val primaryText = if (isDark) Color.White else Color(0xFF0F172A)
    val accentText = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val boxBg = if (isDark) Color(0xFF162032) else Color(0xFFF8FAFC)
    val boxBorder = if (isDark) Color(0xFF283548) else Color(0xFFCBD5E1)

    if (showPartsDialog && selectedFile != null) {
        AlertDialog(
            onDismissRequest = { showPartsDialog = false },
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
                                tint = accentText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = step.targetPartition,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPartsDialog = false }) {
                    Text("Done")
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
        // Row 1: Brand Dropdown + Partition Dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SelectorDropdownBox(
                    text = "Brand: ${uiState.selectedBrand.shortTag}",
                    textColor = primaryText,
                    isDark = isDark,
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
                    textColor = accentText,
                    isDark = isDark,
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

        // Row 2: Load Flash File (.img / .bat / UPDATE.APP) + Parts Counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = boxBg,
                border = BorderStroke(1.dp, boxBorder),
                modifier = Modifier
                    .weight(1.4f)
                    .height(36.dp)
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
                        tint = accentText,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedFile?.fileName ?: "Load .img / .bat / UPDATE.APP",
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryText,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = boxBg,
                border = BorderStroke(1.dp, boxBorder),
                modifier = Modifier
                    .weight(0.75f)
                    .height(36.dp)
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
                        color = accentText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        // Row 3: Option Toggle Pills + Single START FLASH Button
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
                OptionTogglePill("Verity", uiState.disableVerity, isDark, onToggleVerity)
                OptionTogglePill("Reboot", uiState.autoReboot, isDark, onToggleAutoReboot)
                OptionTogglePill("Fastbootd", uiState.useFastbootd, isDark, onToggleFastbootd)
                OptionTogglePill("CRC", uiState.checksumVerify, isDark, onToggleChecksum)
            }

            Button(
                onClick = onStartPressed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF16A34A),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier
                    .weight(0.95f)
                    .height(36.dp)
                    .testTag("execute_flash_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "START FLASH",
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
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val boxBg = if (isDark) Color(0xFF162032) else Color(0xFFF8FAFC)
    val boxBorder = if (isDark) Color(0xFF283548) else Color(0xFFCBD5E1)
    val arrowTint = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = boxBg,
        border = BorderStroke(1.dp, boxBorder),
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
                tint = arrowTint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun OptionTogglePill(
    label: String,
    checked: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val uncheckedBg = if (isDark) Color(0xFF162032) else Color(0xFFF1F5F9)
    val uncheckedBorder = if (isDark) Color(0xFF283548) else Color(0xFFCBD5E1)
    val uncheckedFg = if (isDark) Color(0xFF64748B) else Color(0xFF475569)

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (checked) Color(0xFF2563EB) else uncheckedBg,
        border = BorderStroke(
            1.dp,
            if (checked) Color(0xFF3B82F6) else uncheckedBorder
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
                tint = if (checked) Color.White else uncheckedFg,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (checked) Color.White else uncheckedFg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Pure Live Log Console Panel (Bottom ~75% of screen).
 * Adapts cleanly to both Light and Dark themes.
 */
@Composable
private fun LiveLogConsolePanel(
    uiState: ServiceUiState,
    isDark: Boolean,
    consoleBg: Color,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()

    val headerBg = if (isDark) Color(0xFF090D16) else Color(0xFFF8FAFC)
    val dividerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val titleColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val countColor = if (isDark) Color(0xFF64748B) else Color(0xFF475569)
    val iconTint = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    LaunchedEffect(uiState.consoleLogs.size) {
        if (uiState.consoleLogs.isNotEmpty()) {
            listState.animateScrollToItem(uiState.consoleLogs.lastIndex)
        }
    }

    Surface(
        modifier = modifier.testTag("bottom_terminal_panel"),
        color = consoleBg
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // LIVE LOG CONSOLE Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE LOG CONSOLE",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = titleColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${uiState.consoleLogs.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = countColor
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
                            tint = iconTint,
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
                            tint = iconTint,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = dividerColor)

            // Expanded Log Stream List
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
                    ConsoleTimestampRow(entry = entry, isDark = isDark)
                }
            }
        }
    }
}

@Composable
private fun ConsoleTimestampRow(
    entry: ConsoleLogEntry,
    isDark: Boolean
) {
    val msgColor = if (isDark) {
        when (entry.level) {
            LogLevel.INIT -> Color(0xFF10B981)
            LogLevel.CMD -> Color(0xFF38BDF8)
            LogLevel.USB -> Color(0xFF60A5FA)
            LogLevel.OKAY -> Color(0xFF34D399)
            LogLevel.INFO -> Color(0xFFE2E8F0)
            LogLevel.WARN -> Color(0xFFFACC15)
            LogLevel.FAIL -> Color(0xFFEF4444)
        }
    } else {
        when (entry.level) {
            LogLevel.INIT -> Color(0xFF047857)
            LogLevel.CMD -> Color(0xFF0369A1)
            LogLevel.USB -> Color(0xFF1D4ED8)
            LogLevel.OKAY -> Color(0xFF059669)
            LogLevel.INFO -> Color(0xFF1E293B)
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
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = entry.message,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            color = msgColor,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * About & Command Reference Dialog:
 * Shows all Fastboot & ADB commands, their Brand, and their detailed explanations.
 */
@Composable
fun AboutCommandReferenceDialog(
    isDark: Boolean = true,
    onDismiss: () -> Unit
) {
    var filterTab by remember { mutableIntStateOf(0) } // 0 = Fastboot, 1 = ADB
    val filteredActions = remember(filterTab) {
        if (filterTab == 0) {
            CommandRepository.allActions.filter { !it.isAdb }
        } else {
            CommandRepository.allActions.filter { it.isAdb }
        }
    }

    val dialogBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val titleColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val inactiveTabBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val inactiveTabText = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val itemBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val itemBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val itemTitleColor = if (isDark) Color(0xFFFACC15) else Color(0xFF0F172A)
    val badgeBg = if (isDark) Color(0xFF0F172A) else Color(0xFFE0F2FE)
    val badgeText = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)
    val cmdTextColor = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
    val descColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
    val closeColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        titleContentColor = titleColor,
        textContentColor = descColor,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "About & Command Reference",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = titleColor
                        )
                        Text(
                            text = "FastDeck OTG • Android 10–16 Service Guide",
                            style = MaterialTheme.typography.labelSmall,
                            color = subtitleColor
                        )
                    }
                }

                // Filter Tabs: Fastboot Commands vs ADB Commands
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (filterTab == 0) Color(0xFF10B981) else inactiveTabBg,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clickable { filterTab = 0 }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Fastboot Commands",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (filterTab == 0) Color(0xFF052E16) else inactiveTabText
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (filterTab == 1) Color(0xFF38BDF8) else inactiveTabBg,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clickable { filterTab = 1 }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "ADB Commands",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (filterTab == 1) Color(0xFF082F49) else inactiveTabText
                            )
                        }
                    }
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .testTag("about_command_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredActions, key = { it.id }) { item ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = itemBg,
                        border = BorderStroke(1.dp, itemBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = itemTitleColor,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = badgeBg
                                ) {
                                    Text(
                                        text = item.brand.shortTag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = badgeText,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = item.command,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = cmdTextColor
                            )
                            if (item.description.isNotEmpty()) {
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = descColor
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = closeColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}
