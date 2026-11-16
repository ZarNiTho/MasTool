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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CommandRepository
import com.example.model.ActionGroup
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.LocalFontConfig

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    val fontConfig = LocalFontConfig.current

    var protocolTab by remember { mutableIntStateOf(0) } // 0 = Fastboot, 1 = ADB
    var selectedGroup by remember { mutableStateOf<ActionGroup?>(null) }

    val filteredActions = remember(protocolTab, selectedGroup) {
        CommandRepository.allActions.filter { item ->
            val matchesProtocol = if (protocolTab == 0) !item.isAdb else item.isAdb
            val matchesGroup = selectedGroup == null || item.group == selectedGroup
            matchesProtocol && matchesGroup
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.screenBg)
            .testTag("about_screen")
    ) {
        // Hero Info Banner with active ThemeDesign accent bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.cardBg,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .border(1.dp, palette.panelBorderBrush, RoundedCornerShape(12.dp))
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(palette.cardAccentBrush)
                )
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.accentPrimary.copy(alpha = 0.14f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = palette.accentPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "FastDeck OTG • Command Guide",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "Android 10–16 Native USB OTG Engine",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = palette.accentSecondary.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, palette.accentSecondary)
                        ) {
                            Text(
                                text = "v3.0",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Protocol Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProtocolFilterPill(
                            label = "Fastboot Commands",
                            selected = protocolTab == 0,
                            accentColor = palette.badgeFastboot,
                            onClick = { protocolTab = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        ProtocolFilterPill(
                            label = "ADB Commands",
                            selected = protocolTab == 1,
                            accentColor = palette.badgeAdb,
                            onClick = { protocolTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Group Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GroupChip(
                            label = "All (${filteredActions.size})",
                            selected = selectedGroup == null,
                            onClick = { selectedGroup = null }
                        )
                        ActionGroup.entries.forEach { grp ->
                            GroupChip(
                                label = grp.displayName,
                                selected = selectedGroup == grp,
                                onClick = { selectedGroup = grp }
                            )
                        }
                    }
                }
            }
        }

        // Scrollable Command List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("about_command_list"),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredActions, key = { it.id }) { item ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = palette.cardBg,
                    border = BorderStroke(1.dp, palette.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.actionHighlight,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = palette.accentPrimary.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = item.group.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.accentPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = palette.cardAltBg
                                ) {
                                    Text(
                                        text = item.brand.shortTag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = palette.accentSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = item.command,
                                fontFamily = FontFamily.Monospace,
                                fontSize = fontConfig.consoleSp,
                                color = palette.accentSecondary
                            )
                        }

                        if (item.description.isNotEmpty()) {
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = (11.5f * fontConfig.scale).sp,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProtocolFilterPill(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) accentColor else palette.cardAltBg,
        border = BorderStroke(1.dp, if (selected) accentColor else palette.cardBorder),
        modifier = modifier
            .height(36.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else palette.textSecondary
            )
        }
    }
}

@Composable
private fun GroupChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalAppPalette.current
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (selected) palette.accentPrimary.copy(alpha = 0.16f) else palette.cardAltBg,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) palette.accentPrimary else palette.cardBorder
        ),
        modifier = Modifier
            .height(30.dp)
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) palette.accentPrimary else palette.textSecondary
            )
        }
    }
}
