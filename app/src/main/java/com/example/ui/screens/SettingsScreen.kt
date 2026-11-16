package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppFontSize
import com.example.model.ThemeDesign
import com.example.model.ThemeMode
import com.example.ui.ServiceUiState
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.LocalFontConfig
import com.example.ui.theme.resolvePalette

@Composable
fun SettingsScreen(
    uiState: ServiceUiState,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectThemeDesign: (ThemeDesign) -> Unit,
    onSelectFontSize: (AppFontSize) -> Unit,
    onToggleSound: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    val fontConfig = LocalFontConfig.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.screenBg)
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section 1: Theme Mode (Dark / Light)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.cardBg,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.panelBorderBrush, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (palette.isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = palette.accentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Appearance Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeModeOptionCard(
                        title = "Dark Mode",
                        subtitle = "OLED / Low-Glare",
                        icon = Icons.Default.DarkMode,
                        selected = uiState.themeMode == ThemeMode.DARK,
                        onClick = { onSelectThemeMode(ThemeMode.DARK) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("setting_theme_dark")
                    )
                    ThemeModeOptionCard(
                        title = "Light Mode",
                        subtitle = "Daylight High-Clarity",
                        icon = Icons.Default.LightMode,
                        selected = uiState.themeMode == ThemeMode.LIGHT,
                        onClick = { onSelectThemeMode(ThemeMode.LIGHT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("setting_theme_light")
                    )
                }
            }
        }

        // Section 2: 3 Distinct Theme Designs & Color Effects
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.cardBg,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.panelBorderBrush, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = palette.accentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Theme Color Design (3 Styles)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "မတူညီသော အရောင်၊ Border Glow နှင့် Visual Effect များ",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }

                ThemeDesign.entries.forEach { design ->
                    val previewPalette = resolvePalette(uiState.themeMode, design)
                    val selected = uiState.themeDesign == design
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) previewPalette.cardAltBg else palette.screenBg,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            brush = if (selected) {
                                Brush.horizontalGradient(
                                    listOf(previewPalette.accentPrimary, previewPalette.accentSecondary)
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(palette.cardBorder, palette.cardBorder)
                                )
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectThemeDesign(design) }
                            .testTag("theme_design_${design.name.lowercase()}")
                    ) {
                        Column {
                            // Top signature gradient effect bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                previewPalette.accentPrimary,
                                                previewPalette.accentSecondary
                                            )
                                        )
                                    )
                            )
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
                                    // Dual color swatch preview
                                    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(previewPalette.accentPrimary)
                                                .border(1.5.dp, previewPalette.cardBg, CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(previewPalette.accentSecondary)
                                                .border(1.5.dp, previewPalette.cardBg, CircleShape)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = design.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.textPrimary
                                        )
                                        Text(
                                            text = design.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary
                                        )
                                    }
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected Design",
                                        tint = previewPalette.accentPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Font Size Selector + Live Preview
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.cardBg,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.panelBorderBrush, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FormatSize,
                        contentDescription = null,
                        tint = palette.accentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Font Size",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Console နှင့် UI စာလုံးအရွယ်အစား ချိန်ညှိရန်",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppFontSize.entries.forEach { sizeOption ->
                        val selected = uiState.fontSize == sizeOption
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) palette.accentPrimary.copy(alpha = 0.16f) else palette.screenBg,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) palette.accentPrimary else palette.cardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clickable { onSelectFontSize(sizeOption) }
                                .testTag("font_size_${sizeOption.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = sizeOption.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) palette.accentPrimary else palette.textPrimary
                                )
                                Text(
                                    text = "${sizeOption.consoleSp.toInt()}sp",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }

                // Live Preview Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.consoleBg,
                    border = BorderStroke(1.dp, palette.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "LIVE FONT PREVIEW (${uiState.fontSize.displayName})",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentPrimary
                        )
                        HorizontalDivider(color = palette.divider)
                        Text(
                            text = "[OKAY] fastboot getvar current-slot -> _a",
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontConfig.consoleSp,
                            color = palette.textPrimary
                        )
                    }
                }
            }
        }

        // Section 4: Audio Feedback (Action Start/Stop + USB Port Attach/Detach)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.cardBg,
            border = BorderStroke(1.dp, palette.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleSound(!uiState.soundEnabled) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sound Effects",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Action Start/Stop နှင့် USB Port အဖြုတ်/အတက် အသံများ",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
                Switch(
                    checked = uiState.soundEnabled,
                    onCheckedChange = onToggleSound,
                    modifier = Modifier.testTag("setting_sound_switch")
                )
            }
        }

        // Section 5: Engine & USB Host Summary
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.cardBg,
            border = BorderStroke(1.dp, palette.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Usb,
                    contentDescription = null,
                    tint = palette.accentSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FastDeck OTG Native Engine",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Detected OTG Devices: ${uiState.usbPorts.size} • Theme: ${uiState.themeDesign.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) palette.accentPrimary.copy(alpha = 0.14f) else palette.screenBg,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) palette.accentPrimary else palette.cardBorder
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) palette.accentPrimary else palette.textSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }
        }
    }
}
