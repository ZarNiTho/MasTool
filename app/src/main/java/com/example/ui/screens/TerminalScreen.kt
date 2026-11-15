package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TerminalLine
import com.example.ui.ServiceUiState

/**
 * Pure Termux-style Fullscreen CLI Terminal Box.
 * Contains no One-Click logs, no dropdowns, no cards, and no extra toolbars.
 */
@Composable
fun FullscreenCliTerminalScreen(
    uiState: ServiceUiState,
    onTerminalInputChange: (String) -> Unit,
    onSubmitCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(uiState.terminalLines.size, uiState.isBusy) {
        val targetIdx = uiState.terminalLines.size // prompt item index
        listState.animateScrollToItem(targetIdx)
        if (!uiState.isBusy) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .imePadding()
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
            .testTag("fullscreen_termux_terminal")
    ) {
        SelectionContainer {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                items(uiState.terminalLines, key = { it.id }) { line ->
                    TermuxOutputLine(line = line)
                }

                // Inline Termux Prompt Line directly below CLI output
                item(key = "termux_inline_prompt") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "~ $ ",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4ADE80)
                        )
                        if (uiState.isBusy) {
                            Text(
                                text = "running...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        } else {
                            BasicTextField(
                                value = uiState.terminalInput,
                                onValueChange = { next ->
                                    if (next.contains('\n')) {
                                        val clean = next.replace("\n", "")
                                        onSubmitCommand(clean)
                                    } else {
                                        onTerminalInputChange(next)
                                    }
                                },
                                singleLine = true,
                                cursorBrush = SolidColor(Color(0xFF4ADE80)),
                                textStyle = TextStyle(
                                    color = Color(0xFFFFFFFF),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.5.sp,
                                    lineHeight = 19.sp
                                ),
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Send,
                                    autoCorrectEnabled = false
                                ),
                                keyboardActions = KeyboardActions(
                                    onSend = { onSubmitCommand(uiState.terminalInput) },
                                    onDone = { onSubmitCommand(uiState.terminalInput) }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .onPreviewKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown &&
                                            (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                        ) {
                                            onSubmitCommand(uiState.terminalInput)
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                    .testTag("termux_cli_input")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TermuxOutputLine(line: TerminalLine) {
    if (line.isCommand) {
        val promptPrefix = "~ $ "
        val cmdBody = line.text.removePrefix(promptPrefix)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)) {
                    append(promptPrefix)
                }
                withStyle(SpanStyle(color = Color(0xFFFFFFFF))) {
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
            color = if (line.isError) Color(0xFFF87171) else Color(0xFFE5E7EB),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
        )
    }
}
