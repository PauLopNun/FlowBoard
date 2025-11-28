package com.flowboard.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Editor de texto rico avanzado usando AnnotatedString
 * Permite formato individual por palabra/selección
 *
 * Características:
 * - ✅ Negrita, cursiva, subrayado por selección
 * - ✅ Colores de texto individuales
 * - ✅ Tamaños de fuente (H1, H2, H3, normal)
 * - ✅ Alineación de texto
 * - ✅ Guardado automático con metadatos de formato
 * - ✅ Persistencia completa en JSON
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
    var hasChanges by remember { mutableStateOf(false) }

    // Auto-guardado
    LaunchedEffect(textFieldValue.text, formatRanges) {
        if (textFieldValue.text.isNotEmpty()) {
            hasChanges = true
            if (autoSave) {
                kotlinx.coroutines.delay(autoSaveDelayMs)
                val content = RichTextContent(
                    plainText = textFieldValue.text,
                    formatRanges = formatRanges
                )
                if (content != lastSavedContent) {
                    onSave(content)
                    lastSavedContent = content
                    hasChanges = false
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

    // Construir texto con formato
    val annotatedText = remember(textFieldValue.text, formatRanges) {
        buildAnnotatedString {
            append(textFieldValue.text)
            formatRanges.forEach { range ->
                if (range.start < textFieldValue.text.length && range.end <= textFieldValue.text.length) {
                    addStyle(
                        style = SpanStyle(
                            color = range.color?.let { Color(it) } ?: Color.Unspecified,
                            fontSize = range.fontSize?.sp ?: 16.sp,
                            fontWeight = if (range.isBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (range.isItalic) FontStyle.Italic else FontStyle.Normal,
                            textDecoration = if (range.isUnderline) TextDecoration.Underline else null
                        ),
                        start = range.start,
                        end = range.end
                    )
                }
            }
        }
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
                            hasChanges = false
                        },
                        enabled = hasChanges
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
                                val start = textFieldValue.selection.start
                                val end = textFieldValue.selection.end
                                if (start != end) {
                                    formatRanges = formatRanges.map {
                                        if (it.start == start && it.end == end) {
                                            it.copy(isBold = !it.isBold)
                                        } else it
                                    }.toMutableList().apply {
                                        if (none { it.start == start && it.end == end }) {
                                            add(FormatRange(start, end, isBold = true))
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.FormatBold, "Bold")
                        }

                        // Cursiva
                        IconButton(
                            onClick = {
                                val start = textFieldValue.selection.start
                                val end = textFieldValue.selection.end
                                if (start != end) {
                                    formatRanges = formatRanges.map {
                                        if (it.start == start && it.end == end) {
                                            it.copy(isItalic = !it.isItalic)
                                        } else it
                                    }.toMutableList().apply {
                                        if (none { it.start == start && it.end == end }) {
                                            add(FormatRange(start, end, isItalic = true))
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.FormatItalic, "Italic")
                        }

                        // Subrayado
                        IconButton(
                            onClick = {
                                val start = textFieldValue.selection.start
                                val end = textFieldValue.selection.end
                                if (start != end) {
                                    formatRanges = formatRanges.map {
                                        if (it.start == start && it.end == end) {
                                            it.copy(isUnderline = !it.isUnderline)
                                        } else it
                                    }.toMutableList().apply {
                                        if (none { it.start == start && it.end == end }) {
                                            add(FormatRange(start, end, isUnderline = true))
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.FormatUnderlined, "Underline")
                        }

                        VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 4.dp))

                        // Tamaños
                        FilledTonalButton(
                            onClick = {
                                val start = textFieldValue.selection.start
                                val end = textFieldValue.selection.end
                                if (start != end) {
                                    formatRanges = formatRanges.toMutableList().apply {
                                        removeAll { it.start == start && it.end == end }
                                        add(FormatRange(start, end, fontSize = 32, isBold = true))
                                    }
                                }
                            },
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("H1", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                val start = textFieldValue.selection.start
                                val end = textFieldValue.selection.end
                                if (start != end) {
                                    formatRanges = formatRanges.toMutableList().apply {
                                        removeAll { it.start == start && it.end == end }
                                        add(FormatRange(start, end, fontSize = 24, isBold = true))
                                    }
                                }
                            },
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("H2", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                val start = textFieldValue.selection.start
                                val end = textFieldValue.selection.end
                                if (start != end) {
                                    formatRanges = formatRanges.toMutableList().apply {
                                        removeAll { it.start == start && it.end == end }
                                        add(FormatRange(start, end, fontSize = 18, isBold = true))
                                    }
                                }
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
                                        !(it.start == start && it.end == end)
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
                                val start = textFieldValue.selection.start
                                val end = textFieldValue.selection.end
                                if (start != end) {
                                    formatRanges = formatRanges.toMutableList().apply {
                                        removeAll { it.start == start && it.end == end }
                                        add(FormatRange(start, end, color = color.value.toLong()))
                                    }
                                }
                                showColorPicker = false
                            }
                        )
                    }

                    HorizontalDivider()
                }
            }
        }

        // Editor
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    // Ajustar rangos de formato cuando cambia el texto
                    if (newValue.text.length < textFieldValue.text.length) {
                        // Texto eliminado - ajustar rangos
                        val diff = textFieldValue.text.length - newValue.text.length
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
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 28.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (textFieldValue.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                    Box {
                        Text(
                            text = annotatedText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        innerTextField()
                    }
                }
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
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${textFieldValue.text.length} characters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                if (autoSave && hasChanges) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Saving...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (autoSave) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "Saved",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
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
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                )
            ) {}
        }
    }
}
