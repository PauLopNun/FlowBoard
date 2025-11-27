package com.flowboard.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Editor de texto fluido estilo Notion/Google Docs con formato rico
 * - Un solo TextField multilinea
 * - Barra de herramientas de formato
 * - Enter crea automáticamente nuevos párrafos
 * - Sincronización en tiempo real
 */
@Composable
fun FluidDocumentEditor(
    content: String,
    onContentChange: (String) -> Unit,
    onCursorPositionChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Start typing..."
) {
    var textFieldValue by remember(content) {
        mutableStateOf(TextFieldValue(
            text = content,
            selection = TextRange(content.length)
        ))
    }

    var currentFormat by remember { mutableStateOf(TextFormat()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Update cuando el contenido cambia externamente (de otros usuarios)
    LaunchedEffect(content) {
        if (textFieldValue.text != content) {
            // Preservar la posición del cursor si es posible
            val newSelection = if (textFieldValue.selection.start <= content.length) {
                textFieldValue.selection
            } else {
                TextRange(content.length)
            }
            textFieldValue = TextFieldValue(
                text = content,
                selection = newSelection
            )
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Barra de herramientas
        EditorToolbar(
            currentFormat = currentFormat,
            onFormatChange = { newFormat ->
                currentFormat = newFormat
            }
        )

        Divider()

        // Área de texto
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    onContentChange(newValue.text)
                    onCursorPositionChange(newValue.selection.start)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = currentFormat.textColor,
                    fontSize = currentFormat.fontSize.sp,
                    fontWeight = if (currentFormat.isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (currentFormat.isItalic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = if (currentFormat.isUnderline) TextDecoration.Underline else null,
                    textAlign = currentFormat.alignment
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (textFieldValue.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

/**
 * Versión alternativa con bloques pero fluidos
 * Maneja automáticamente la creación de bloques cuando se presiona Enter
 */
@Composable
fun FluidBlockEditor(
    blocks: List<String>,
    onBlocksChange: (List<String>) -> Unit,
    onCursorPositionChange: (blockIndex: Int, position: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentBlockIndex by remember { mutableStateOf(0) }
    val focusRequesters = remember(blocks.size) {
        List(blocks.size) { FocusRequester() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        blocks.forEachIndexed { index, blockContent ->
            var textFieldValue by remember(blockContent) {
                mutableStateOf(TextFieldValue(
                    text = blockContent,
                    selection = TextRange(blockContent.length)
                ))
            }

            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    // Detectar si se presionó Enter
                    if (newValue.text.contains("\n")) {
                        // Dividir el texto en el Enter
                        val parts = newValue.text.split("\n", limit = 2)
                        val beforeEnter = parts[0]
                        val afterEnter = if (parts.size > 1) parts[1] else ""

                        // Actualizar bloques
                        val newBlocks = blocks.toMutableList()
                        newBlocks[index] = beforeEnter
                        newBlocks.add(index + 1, afterEnter)
                        onBlocksChange(newBlocks)

                        // Mover foco al siguiente bloque
                        currentBlockIndex = index + 1
                        if (index + 1 < focusRequesters.size) {
                            focusRequesters[index + 1].requestFocus()
                        }
                    } else {
                        textFieldValue = newValue
                        val newBlocks = blocks.toMutableList()
                        newBlocks[index] = newValue.text
                        onBlocksChange(newBlocks)
                        onCursorPositionChange(index, newValue.selection.start)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (index < focusRequesters.size) {
                            Modifier.focusRequester(focusRequesters[index])
                        } else {
                            Modifier
                        }
                    ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Default
                ),
                decorationBox = { innerTextField ->
                    if (textFieldValue.text.isEmpty() && index == 0) {
                        Text(
                            text = "Start typing...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                    innerTextField()
                }
            )
        }
    }

    // Auto-focus en el primer bloque
    LaunchedEffect(Unit) {
        if (focusRequesters.isNotEmpty()) {
            focusRequesters[0].requestFocus()
        }
    }
}
