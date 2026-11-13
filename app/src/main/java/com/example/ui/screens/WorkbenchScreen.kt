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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.material3.OutlinedTextField
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
    modifier: Modifier = Modifier
) {
    val isDark = uiState.themeMode == ThemeMode.DARK
    val cardBg = if (isDark) Color(0xFF111827) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF1F2937) else Color(0xFFCBD5E1)
    val consoleBg = if (isDark) Color(0xFF090D16) else Color(0xFF0F172A)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF060911) else Color(0xFFF1F5F9))
    ) {
        // COMPACT TOP SELECTION & START DECK (~26% height -> weight 0.72f)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.72f)
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
                    availableActions = availableActions,
                    onSelectBrand = onSelectBrand,
                    onSelectActionGroup = onSelectActionGroup,
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
                    onScanPorts = onScanPorts,
                    onStartPressed = onStartPressed
                )
                ToolScreenMode.CMD_TERMINAL -> TerminalDropdownStartPanel(
                    uiState = uiState,
                    availableActions = availableActions,
                    onSelectBrand = onSelectBrand,
                    onSelectAction = onSelectAction,
                    onRawCommandChange = onRawCommandChange,
                    onStartPressed = onStartPressed
                )
            }
        }

        // MIDDLE STATUS & PROGRESS STRIP
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isDark) Color(0xFF0B111E) else Color(0xFFE2E8F0)
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
                        color = if (isDark) Color(0xFF64748B) else Color(0xFF475569),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f%%", uiState.progressPercent),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFF64748B) else Color(0xFF475569)
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

        // EXPANDED BOTTOM LIVE LOG CONSOLE (~74% height -> weight 2.28f)
        LiveLogConsolePanel(
            uiState = uiState,
            consoleBg = consoleBg,
            onRawCommandChange = onRawCommandChange,
            onExecuteCommand = onExecuteCommand,
            onClearLogs = onClearLogs,
            modifier = Modifier
                .fillMaxWidth()
                .weight(2.28f)
        )
    }
}

/**
 * Dropdown-based One-Click & Brand-Specific Selector + Single START Button
 */
@Composable
private fun DropdownActionStartPanel(
    uiState: ServiceUiState,
    availableActions: List<ServiceActionItem>,
    onSelectBrand: (ServiceBrand) -> Unit,
    onSelectActionGroup: (ActionGroup) -> Unit,
    onSelectAction: (ServiceActionItem) -> Unit,
    onScanPorts: () -> Unit,
    onStartPressed: () -> Unit
) {
    var brandExpanded by remember { mutableStateOf(false) }
    var groupExpanded by remember { mutableStateOf(false) }
    var actionExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Row 1: Brand Dropdown + Category Group Dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SelectorDropdownBox(
                    text = "Brand: ${uiState.selectedBrand.shortTag}",
                    textColor = Color.White,
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

            Box(modifier = Modifier.weight(1f)) {
                SelectorDropdownBox(
                    text = uiState.selectedActionGroup.displayName,
                    textColor = Color(0xFF38BDF8),
                    onClick = { groupExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_dropdown")
                )
                DropdownMenu(
                    expanded = groupExpanded,
                    onDismissRequest = { groupExpanded = false }
                ) {
                    ActionGroup.entries.forEach { grp ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = grp.displayName,
                                    fontWeight = if (grp == uiState.selectedActionGroup) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                groupExpanded = false
                                onSelectActionGroup(grp)
                            }
                        )
                    }
                }
            }
        }

        // Row 2: Action / Command Selection Dropdown Box (70+ Brand & Universal Operations)
        Box(modifier = Modifier.fillMaxWidth()) {
            SelectorDropdownBox(
                text = uiState.selectedAction.title,
                subText = uiState.selectedAction.command,
                textColor = Color(0xFFFACC15),
                onClick = { actionExpanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("action_dropdown")
            )
            DropdownMenu(
                expanded = actionExpanded,
                onDismissRequest = { actionExpanded = false },
                modifier = Modifier.height(340.dp)
            ) {
                availableActions.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = item.command,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF38BDF8)
                                )
                            }
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF162032),
                border = BorderStroke(1.dp, Color(0xFF283548)),
                modifier = Modifier
                    .weight(0.85f)
                    .height(40.dp)
                    .clickable { onScanPorts() }
                    .testTag("scan_ports_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan USB",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Scan OTG",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
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
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier
                    .weight(1.45f)
                    .height(40.dp)
                    .testTag("start_action_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "START (${uiState.selectedBrand.shortTag.uppercase(Locale.US)})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold
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
    onSelectBrand: (ServiceBrand) -> Unit,
    onSelectPartition: (String) -> Unit,
    onToggleVerity: () -> Unit,
    onToggleAutoReboot: () -> Unit,
    onToggleChecksum: () -> Unit,
    onToggleFastbootd: () -> Unit,
    onToggleStep: (String) -> Unit,
    onPickFile: () -> Unit,
    onScanPorts: () -> Unit,
    onStartPressed: () -> Unit
) {
    var brandExpanded by remember { mutableStateOf(false) }
    var partitionExpanded by remember { mutableStateOf(false) }
    var showPartsDialog by remember { mutableStateOf(false) }

    val selectedFile = uiState.selectedFlashFile
    val totalParts = selectedFile?.steps?.size ?: 0
    val enabledParts = selectedFile?.steps?.count { it.enabled } ?: 0

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
                    modifier = Modifier.height(260.dp),
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
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step.targetPartition,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = step.command,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
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
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Row 1: Brand Dropdown + Partition Dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SelectorDropdownBox(
                    text = "Brand: ${uiState.selectedBrand.shortTag}",
                    textColor = Color.White,
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
                    textColor = Color(0xFF38BDF8),
                    onClick = { partitionExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = partitionExpanded,
                    onDismissRequest = { partitionExpanded = false },
                    modifier = Modifier.height(300.dp)
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

        // Row 2: Load Flash File (.img / .bat / UPDATE.APP) + Parts Counter + Option Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF162032),
                border = BorderStroke(1.dp, Color(0xFF283548)),
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
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedFile?.fileName ?: "Load .img / .bat / UPDATE.APP",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF162032),
                border = BorderStroke(1.dp, Color(0xFF283548)),
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
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold
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
                OptionTogglePill("Verity", uiState.disableVerity, onToggleVerity)
                OptionTogglePill("Reboot", uiState.autoReboot, onToggleAutoReboot)
                OptionTogglePill("Fastbootd", uiState.useFastbootd, onToggleFastbootd)
                OptionTogglePill("CRC", uiState.checksumVerify, onToggleChecksum)
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
                    .weight(0.95f)
                    .height(38.dp)
                    .testTag("execute_flash_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "START FLASH",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/**
 * Dedicated Terminal Control Panel with Brand Dropdown, Preset Command Dropdown, and Single START Button
 */
@Composable
private fun TerminalDropdownStartPanel(
    uiState: ServiceUiState,
    availableActions: List<ServiceActionItem>,
    onSelectBrand: (ServiceBrand) -> Unit,
    onSelectAction: (ServiceActionItem) -> Unit,
    onRawCommandChange: (String) -> Unit,
    onStartPressed: () -> Unit
) {
    var brandExpanded by remember { mutableStateOf(false) }
    var presetExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Row 1: Brand Dropdown + Preset Command Dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(0.85f)) {
                SelectorDropdownBox(
                    text = "Brand: ${uiState.selectedBrand.shortTag}",
                    textColor = Color.White,
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
                                brandExpanded = false
                                onSelectBrand(brand)
                            }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1.15f)) {
                SelectorDropdownBox(
                    text = uiState.selectedAction.title,
                    textColor = Color(0xFF38BDF8),
                    onClick = { presetExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = presetExpanded,
                    onDismissRequest = { presetExpanded = false },
                    modifier = Modifier.height(320.dp)
                ) {
                    availableActions.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = item.command,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            },
                            onClick = {
                                presetExpanded = false
                                onSelectAction(item)
                            }
                        )
                    }
                }
            }
        }

        // Row 2: Custom Command Input + Single START Button
        val isDark = uiState.themeMode == ThemeMode.DARK
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.rawCommandInput,
                onValueChange = onRawCommandChange,
                placeholder = {
                    Text(
                        text = "fastboot / adb / shell...",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                ),
                modifier = Modifier
                    .weight(1.35f)
                    .height(44.dp)
                    .testTag("terminal_cmd_input")
            )

            Button(
                onClick = onStartPressed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF16A34A),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier
                    .weight(0.65f)
                    .height(44.dp)
                    .testTag("terminal_send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("START", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun SelectorDropdownBox(
    text: String,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subText: String? = null
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF162032),
        border = BorderStroke(1.dp, Color(0xFF283548)),
        modifier = modifier
            .height(if (subText != null) 46.dp else 38.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subText != null) {
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
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
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (checked) Color(0xFF1D4ED8) else Color(0xFF162032),
        border = BorderStroke(
            1.dp,
            if (checked) Color(0xFF3B82F6) else Color(0xFF283548)
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
                tint = if (checked) Color.White else Color(0xFF64748B),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (checked) Color.White else Color(0xFF64748B),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LiveLogConsolePanel(
    uiState: ServiceUiState,
    consoleBg: Color,
    onRawCommandChange: (String) -> Unit,
    onExecuteCommand: (String) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()

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
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${uiState.consoleLogs.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B)
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
                            .size(26.dp)
                            .testTag("copy_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Console Logs",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(
                        onClick = onClearLogs,
                        modifier = Modifier
                            .size(26.dp)
                            .testTag("clear_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Console Logs",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF1E293B))

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
                    ConsoleTimestampRow(entry = entry)
                }
            }

            // Bottom Direct Command Input Bar
            if (uiState.screenMode != ToolScreenMode.CMD_TERMINAL) {
                HorizontalDivider(color = Color(0xFF1E293B))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0D1322))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.rawCommandInput,
                        onValueChange = onRawCommandChange,
                        placeholder = {
                            Text(
                                text = "adb / fastboot command...",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    )
                    Button(
                        onClick = {
                            val cmd = uiState.rawCommandInput.trim()
                            if (cmd.isNotEmpty()) {
                                onExecuteCommand(cmd)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Execute",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleTimestampRow(entry: ConsoleLogEntry) {
    val msgColor = when (entry.level) {
        LogLevel.INIT -> Color(0xFF10B981)
        LogLevel.CMD -> Color(0xFF38BDF8)
        LogLevel.USB -> Color(0xFF60A5FA)
        LogLevel.OKAY -> Color(0xFF34D399)
        LogLevel.INFO -> Color(0xFFE2E8F0)
        LogLevel.WARN -> Color(0xFFFACC15)
        LogLevel.FAIL -> Color(0xFFEF4444)
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
