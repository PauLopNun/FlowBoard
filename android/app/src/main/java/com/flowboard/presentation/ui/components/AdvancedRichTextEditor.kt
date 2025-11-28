package com.flowboard.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Editor de texto rico mejorado
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedRichTextEditor(
    initialContent: RichTextContent = RichTextContent(),
    onContentChange: (RichTextContent) -> Unit,
    onSave: (RichTextContent) -> Unit = {},
    modifier: Modifier = Modifier,
    placeholder: String = "Start typing...",
    autoSave: Boolean = true,
    autoSaveDelayMs: Long = 2000L
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(
            text = initialContent.plainText,
            selection = TextRange(initialContent.plainText.length)
        ))
    }

    var formatRanges by remember { mutableStateOf(initialContent.formatRanges) }
    var showToolbar by remember { mutableStateOf(true) }
    var showColorPicker by remember { mutableStateOf(false) }
    var lastSavedContent by remember { mutableStateOf(initialContent) }

    // Auto-guardado
    LaunchedEffect(textFieldValue.text, formatRanges) {
        if (textFieldValue.text.isNotEmpty()) {
            if (autoSave) {
                kotlinx.coroutines.delay(autoSaveDelayMs)
                val content = RichTextContent(
                    plainText = textFieldValue.text,
                    formatRanges = formatRanges
                )
                if (content != lastSavedContent) {
                    onSave(content)
                    lastSavedContent = content
                }
            }
        }
    }

    // Notificar cambios
    LaunchedEffect(textFieldValue.text, formatRanges) {
        onContentChange(
            RichTextContent(
                plainText = textFieldValue.text,
                formatRanges = formatRanges
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Barra superior
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rich Text Editor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { showToolbar = !showToolbar }) {
                        Icon(
                            imageVector = if (showToolbar) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle toolbar"
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            val content = RichTextContent(
                                plainText = textFieldValue.text,
                                formatRanges = formatRanges
                            )
                            onSave(content)
                            lastSavedContent = content
                        }
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            }
        }

        // Barra de herramientas
        AnimatedVisibility(visible = showToolbar) {
            Surface(
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Negrita
                        IconButton(
                            onClick = {
                                applyFormatToSelection(
                                    textFieldValue = textFieldValue,
                                    currentRanges = formatRanges,
                                    onRangesUpdate = { formatRanges = it },
                                    formatUpdate = { it.copy(isBold = !it.isBold) }
                                )
                            }
                        ) {
                            Icon(Icons.Default.FormatBold, "Bold")
                        }

                        // Cursiva
                        IconButton(
                            onClick = {
                                applyFormatToSelection(
                                    textFieldValue = textFieldValue,
                                    currentRanges = formatRanges,
                                    onRangesUpdate = { formatRanges = it },
                                    formatUpdate = { it.copy(isItalic = !it.isItalic) }
                                )
                            }
                        ) {
                            Icon(Icons.Default.FormatItalic, "Italic")
                        }

                        // Subrayado
                        IconButton(
                            onClick = {
                                applyFormatToSelection(
                                    textFieldValue = textFieldValue,
                                    currentRanges = formatRanges,
                                    onRangesUpdate = { formatRanges = it },
                                    formatUpdate = { it.copy(isUnderline = !it.isUnderline) }
                                )
                            }
                        ) {
                            Icon(Icons.Default.FormatUnderlined, "Underline")
                        }

                        VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 4.dp))

                        // Tamaños
                        FilledTonalButton(
                            onClick = {
                                applyFormatToSelection(
                                    textFieldValue = textFieldValue,
                                    currentRanges = formatRanges,
                                    onRangesUpdate = { formatRanges = it },
                                    formatUpdate = { FormatRange(it.start, it.end, fontSize = 32, isBold = true) }
                                )
                            },
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("H1", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                applyFormatToSelection(
                                    textFieldValue = textFieldValue,
                                    currentRanges = formatRanges,
                                    onRangesUpdate = { formatRanges = it },
                                    formatUpdate = { FormatRange(it.start, it.end, fontSize = 24, isBold = true) }
                                )
                            },
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("H2", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                applyFormatToSelection(
                                    textFieldValue = textFieldValue,
                                    currentRanges = formatRanges,
                                    onRangesUpdate = { formatRanges = it },
                                    formatUpdate = { FormatRange(it.start, it.end, fontSize = 18, isBold = true) }
                                )
                            },
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("H3", fontWeight = FontWeight.Bold)
                        }

                        VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 4.dp))

                        // Color
                        IconButton(onClick = { showColorPicker = !showColorPicker }) {
                            Icon(Icons.Default.FormatColorText, "Color")
                        }

                        // Limpiar formato
                        IconButton(
                            onClick = {
                                val start = textFieldValue.selection.start
                                val end = textFieldValue.selection.end
                                if (start != end) {
                                    formatRanges = formatRanges.filter {
                                        it.end <= start || it.start >= end
                                    }.toMutableList()
                                }
                            }
                        ) {
                            Icon(Icons.Default.FormatClear, "Clear Format")
                        }
                    }

                    if (showColorPicker) {
                        RichTextColorPicker(
                            onColorSelected = { color ->
                                applyFormatToSelection(
                                    textFieldValue = textFieldValue,
                                    currentRanges = formatRanges,
                                    onRangesUpdate = { formatRanges = it },
                                    formatUpdate = { FormatRange(it.start, it.end, color = color.value.toLong()) }
                                )
                                showColorPicker = false
                            }
                        )
                    }

                    HorizontalDivider()
                }
            }
        }

        // Editor - Solo texto plano, sin duplicación
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(16.dp)
        ) {
            if (textFieldValue.text.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    // Ajustar rangos cuando se elimina texto
                    if (newValue.text.length < textFieldValue.text.length) {
                        formatRanges = formatRanges.mapNotNull { range ->
                            when {
                                range.end <= newValue.text.length -> range
                                range.start >= newValue.text.length -> null
                                else -> range.copy(end = minOf(range.end, newValue.text.length))
                            }
                        }.toMutableList()
                    }
                    textFieldValue = newValue
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 28.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            )
        }

        // Barra inferior
        Surface(
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = "${textFieldValue.text.length} characters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Aplicar formato a la selección actual
 */
private fun applyFormatToSelection(
    textFieldValue: TextFieldValue,
    currentRanges: List<FormatRange>,
    onRangesUpdate: (List<FormatRange>) -> Unit,
    formatUpdate: (FormatRange) -> FormatRange
) {
    val start = textFieldValue.selection.start
    val end = textFieldValue.selection.end

    if (start == end) return // No hay selección

    // Eliminar rangos que se solapan con la selección
    val filteredRanges = currentRanges.filter {
        it.end <= start || it.start >= end
    }.toMutableList()

    // Agregar nuevo rango con formato
    val baseRange = FormatRange(start, end)
    filteredRanges.add(formatUpdate(baseRange))

    onRangesUpdate(filteredRanges)
}

/**
 * Modelo de contenido de texto rico con formato
 */
@Serializable
data class RichTextContent(
    val plainText: String = "",
    val formatRanges: List<FormatRange> = emptyList()
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): RichTextContent {
            return try {
                Json.decodeFromString<RichTextContent>(json)
            } catch (e: Exception) {
                RichTextContent()
            }
        }
    }
}

/**
 * Rango de formato en el texto
 */
@Serializable
data class FormatRange(
    val start: Int,
    val end: Int,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val fontSize: Int? = null,
    val color: Long? = null
)

/**
 * Color picker para el editor
 */
@Composable
private fun RichTextColorPicker(
    onColorSelected: (Color) -> Unit
) {
    val colors = listOf(
        Color.Black,
        Color(0xFF1976D2), // Blue
        Color(0xFF388E3C), // Green
        Color(0xFFD32F2F), // Red
        Color(0xFFF57C00), // Orange
        Color(0xFF7B1FA2), // Purple
        Color(0xFF00796B), // Teal
        Color(0xFFC2185B), // Pink
        Color(0xFF5D4037), // Brown
        Color(0xFF616161), // Gray
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Text Color:",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(end = 8.dp)
        )

        colors.forEach { color ->
            Surface(
                onClick = { onColorSelected(color) },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.size(32.dp),
                color = color,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                )
            ) {}
        }
    }
}
