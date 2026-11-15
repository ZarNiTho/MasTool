package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TerminalLine
import com.example.model.ThemeMode
import com.example.ui.ServiceUiState

/**
 * Pure Termux-style Fullscreen CLI Terminal Box.
 * Relies cleanly on parent Scaffold WindowInsets.safeDrawing padding (no duplicate imePadding).
 * Supports both Dark (#000000) and Light (#F8FAFC) themes.
 */
@Composable
fun FullscreenCliTerminalScreen(
    uiState: ServiceUiState,
    onTerminalInputChange: (String) -> Unit,
    onSubmitCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = uiState.themeMode == ThemeMode.DARK
    val terminalBg = if (isDark) Color(0xFF000000) else Color(0xFFF8FAFC)
    val dockBg = if (isDark) Color(0xFF0A0D14) else Color(0xFFFFFFFF)
    val dividerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)
    val promptColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF047857)
    val inputColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A)
    val hintColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val keyBg = if (isDark) Color(0xFF111827) else Color(0xFFF1F5F9)
    val keyBorder = if (isDark) Color(0xFF283548) else Color(0xFFCBD5E1)
    val keyTextColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)

    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }

    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = uiState.terminalInput,
                selection = TextRange(uiState.terminalInput.length)
            )
        )
    }
    var historyCursor by remember { mutableIntStateOf(-1) }

    LaunchedEffect(uiState.terminalInput) {
        if (uiState.terminalInput != textFieldValue.text) {
            textFieldValue = TextFieldValue(
                text = uiState.terminalInput,
                selection = TextRange(uiState.terminalInput.length)
            )
        }
    }

    LaunchedEffect(uiState.terminalLines.size) {
        if (uiState.terminalLines.isNotEmpty()) {
            listState.animateScrollToItem(uiState.terminalLines.lastIndex)
        }
    }

    fun submitCurrent() {
        if (uiState.isBusy) return
        historyCursor = -1
        onSubmitCommand(textFieldValue.text)
    }

    fun insertToken(token: String) {
        val current = textFieldValue.text
        val nextText = when {
            current.isBlank() -> "$token "
            current.endsWith(" ") -> "$current$token "
            else -> "$current $token "
        }
        historyCursor = -1
        textFieldValue = TextFieldValue(
            text = nextText,
            selection = TextRange(nextText.length)
        )
        onTerminalInputChange(nextText)
        try {
            focusRequester.requestFocus()
            keyboardController?.show()
        } catch (_: Exception) {
        }
    }

    fun stepHistoryUp() {
        val history = uiState.terminalHistory
        if (history.isEmpty()) return
        val nextCursor = if (historyCursor == -1) {
            history.lastIndex
        } else {
            (historyCursor - 1).coerceAtLeast(0)
        }
        historyCursor = nextCursor
        val cmd = history[nextCursor]
        textFieldValue = TextFieldValue(text = cmd, selection = TextRange(cmd.length))
        onTerminalInputChange(cmd)
    }

    fun stepHistoryDown() {
        val history = uiState.terminalHistory
        if (history.isEmpty() || historyCursor == -1) return
        val nextCursor = historyCursor + 1
        if (nextCursor > history.lastIndex) {
            historyCursor = -1
            textFieldValue = TextFieldValue(text = "", selection = TextRange(0))
            onTerminalInputChange("")
        } else {
            historyCursor = nextCursor
            val cmd = history[nextCursor]
            textFieldValue = TextFieldValue(text = cmd, selection = TextRange(cmd.length))
            onTerminalInputChange(cmd)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(terminalBg)
            .testTag("fullscreen_termux_terminal")
    ) {
        // Scrollable, copy-selectable CLI output area (never wraps the BasicTextField!)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    try {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    } catch (_: Exception) {
                    }
                }
        ) {
            if (uiState.terminalLines.isEmpty()) {
                Text(
                    text = "~ $ ready (type 'help', 'adb devices', or 'fastboot devices')",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.5.sp,
                    color = hintColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                )
            } else {
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.terminalLines, key = { it.id }) { line ->
                            TermuxOutputLine(line = line, isDark = isDark)
                        }
                    }
                }
            }
        }

        // Termux Extra-Keys Bar (always stays pinned directly above the prompt & keyboard)
        HorizontalDivider(color = dividerColor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(dockBg)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TermuxExtraKey("adb", keyBg, keyBorder, keyTextColor) { insertToken("adb") }
            TermuxExtraKey("fastboot", keyBg, keyBorder, keyTextColor) { insertToken("fastboot") }
            TermuxExtraKey("shell", keyBg, keyBorder, keyTextColor) { insertToken("shell") }
            TermuxExtraKey("devices", keyBg, keyBorder, keyTextColor) { insertToken("devices") }
            TermuxExtraKey("reboot", keyBg, keyBorder, keyTextColor) { insertToken("reboot") }
            TermuxExtraKey("↑", keyBg, keyBorder, promptColor) { stepHistoryUp() }
            TermuxExtraKey("↓", keyBg, keyBorder, promptColor) { stepHistoryDown() }
            TermuxExtraKey("CLS", keyBg, keyBorder, if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)) {
                historyCursor = -1
                onSubmitCommand("clear")
            }
        }
        HorizontalDivider(color = dividerColor)

        // Pinned Termux CLI Input Prompt Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(dockBg)
                .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "~ $ ",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = promptColor
            )
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
            ) {
                if (uiState.isBusy) {
                    Text(
                        text = "running...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = hintColor
                    )
                } else {
                    if (textFieldValue.text.isEmpty()) {
                        Text(
                            text = "adb / fastboot command...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.5.sp,
                            color = hintColor
                        )
                    }
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { next ->
                            if (next.text.contains('\n')) {
                                val clean = next.text.replace("\n", "")
                                historyCursor = -1
                                textFieldValue = TextFieldValue("", TextRange(0))
                                onSubmitCommand(clean)
                            } else {
                                textFieldValue = next
                                onTerminalInputChange(next.text)
                            }
                        },
                        singleLine = true,
                        cursorBrush = SolidColor(promptColor),
                        textStyle = TextStyle(
                            color = inputColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { submitCurrent() },
                            onDone = { submitCurrent() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown) {
                                    when (keyEvent.key) {
                                        Key.Enter, Key.NumPadEnter -> {
                                            submitCurrent()
                                            true
                                        }
                                        Key.DirectionUp -> {
                                            stepHistoryUp()
                                            true
                                        }
                                        Key.DirectionDown -> {
                                            stepHistoryDown()
                                            true
                                        }
                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            }
                            .testTag("termux_cli_input")
                    )
                }
            }
            Spacer(modifier = Modifier.width(2.dp))
            IconButton(
                onClick = { submitCurrent() },
                enabled = !uiState.isBusy,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("termux_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Run Command",
                    tint = if (uiState.isBusy) hintColor else promptColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun TermuxExtraKey(
    label: String,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .defaultMinSize(minHeight = 32.dp)
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
private fun TermuxOutputLine(
    line: TerminalLine,
    isDark: Boolean
) {
    val promptColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF047857)
    val cmdColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A)
    val outputColor = if (isDark) Color(0xFFE5E7EB) else Color(0xFF1E293B)
    val errorColor = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)

    if (line.isCommand) {
        val promptPrefix = "~ $ "
        val cmdBody = line.text.removePrefix(promptPrefix)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = promptColor, fontWeight = FontWeight.Bold)) {
                    append(promptPrefix)
                }
                withStyle(SpanStyle(color = cmdColor)) {
                    append(cmdBody)
                }
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
        )
    } else {
        Text(
            text = line.text,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = if (line.isError) errorColor else outputColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
        )
    }
}
