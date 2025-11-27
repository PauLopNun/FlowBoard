package com.flowboard.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Toolbar para el editor de documentos con opciones de formato
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorToolbar(
    currentFormat: TextFormat,
    onFormatChange: (TextFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var showFontSizePicker by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column {
            // Fila principal de herramientas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón de tipo de texto (H1, H2, P, etc.)
                TextTypeButton(
                    currentType = currentFormat.textType,
                    onTypeChange = { type ->
                        onFormatChange(currentFormat.copy(textType = type))
                    }
                )

                VerticalDivider(
                    modifier = Modifier
                        .height(32.dp)
                        .padding(horizontal = 4.dp)
                )

                // Negrita
                IconToggleButton(
                    checked = currentFormat.isBold,
                    onCheckedChange = { onFormatChange(currentFormat.copy(isBold = it)) }
                ) {
                    Icon(
                        Icons.Default.FormatBold,
                        contentDescription = "Negrita",
                        tint = if (currentFormat.isBold) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Cursiva
                IconToggleButton(
                    checked = currentFormat.isItalic,
                    onCheckedChange = { onFormatChange(currentFormat.copy(isItalic = it)) }
                ) {
                    Icon(
                        Icons.Default.FormatItalic,
                        contentDescription = "Cursiva",
                        tint = if (currentFormat.isItalic) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Subrayado
                IconToggleButton(
                    checked = currentFormat.isUnderline,
                    onCheckedChange = { onFormatChange(currentFormat.copy(isUnderline = it)) }
                ) {
                    Icon(
                        Icons.Default.FormatUnderlined,
                        contentDescription = "Subrayado",
                        tint = if (currentFormat.isUnderline) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(32.dp)
                        .padding(horizontal = 4.dp)
                )

                // Tamaño de fuente
                IconButton(onClick = { showFontSizePicker = !showFontSizePicker }) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${currentFormat.fontSize}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Tamaño",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Color de texto
                IconButton(onClick = { showColorPicker = !showColorPicker }) {
                    Icon(
                        Icons.Default.ColorLens,
                        contentDescription = "Color",
                        tint = currentFormat.textColor
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(32.dp)
                        .padding(horizontal = 4.dp)
                )

                // Alineación izquierda
                IconToggleButton(
                    checked = currentFormat.alignment == TextAlign.Start,
                    onCheckedChange = { onFormatChange(currentFormat.copy(alignment = TextAlign.Start)) }
                ) {
                    Icon(
                        Icons.Default.FormatAlignLeft,
                        contentDescription = "Izquierda"
                    )
                }

                // Alineación centro
                IconToggleButton(
                    checked = currentFormat.alignment == TextAlign.Center,
                    onCheckedChange = { onFormatChange(currentFormat.copy(alignment = TextAlign.Center)) }
                ) {
                    Icon(
                        Icons.Default.FormatAlignCenter,
                        contentDescription = "Centro"
                    )
                }

                // Alineación derecha
                IconToggleButton(
                    checked = currentFormat.alignment == TextAlign.End,
                    onCheckedChange = { onFormatChange(currentFormat.copy(alignment = TextAlign.End)) }
                ) {
                    Icon(
                        Icons.Default.FormatAlignRight,
                        contentDescription = "Derecha"
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(32.dp)
                        .padding(horizontal = 4.dp)
                )

                // Lista con viñetas
                IconToggleButton(
                    checked = currentFormat.isBulletList,
                    onCheckedChange = { onFormatChange(currentFormat.copy(isBulletList = it)) }
                ) {
                    Icon(
                        Icons.Default.FormatListBulleted,
                        contentDescription = "Lista"
                    )
                }

                // Lista numerada
                IconToggleButton(
                    checked = currentFormat.isNumberedList,
                    onCheckedChange = { onFormatChange(currentFormat.copy(isNumberedList = it)) }
                ) {
                    Icon(
                        Icons.Default.FormatListNumbered,
                        contentDescription = "Lista numerada"
                    )
                }

                Spacer(Modifier.weight(1f))

                // Más opciones
                IconButton(onClick = { showMoreOptions = !showMoreOptions }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                }
            }

            // Selector de tamaño de fuente
            if (showFontSizePicker) {
                FontSizePicker(
                    currentSize = currentFormat.fontSize,
                    onSizeSelected = { size ->
                        onFormatChange(currentFormat.copy(fontSize = size))
                        showFontSizePicker = false
                    },
                    onDismiss = { showFontSizePicker = false }
                )
            }

            // Selector de color
            if (showColorPicker) {
                ColorPicker(
                    currentColor = currentFormat.textColor,
                    onColorSelected = { color ->
                        onFormatChange(currentFormat.copy(textColor = color))
                        showColorPicker = false
                    },
                    onDismiss = { showColorPicker = false }
                )
            }
        }
    }
}

@Composable
private fun TextTypeButton(
    currentType: TextType,
    onTypeChange: (TextType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.height(40.dp)
        ) {
            Text(
                currentType.displayName,
                style = MaterialTheme.typography.bodySmall
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TextType.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = {
                        onTypeChange(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FontSizePicker(
    currentSize: Int,
    onSizeSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sizes = listOf(8, 10, 12, 14, 16, 18, 20, 24, 28, 32, 36, 48, 64, 72)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                "Tamaño de fuente",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                sizes.forEach { size ->
                    FilterChip(
                        selected = size == currentSize,
                        onClick = { onSizeSelected(size) },
                        label = { Text("$size") }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorPicker(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = listOf(
        Color.Black to "Negro",
        Color(0xFF2196F3) to "Azul",
        Color(0xFF4CAF50) to "Verde",
        Color(0xFFF44336) to "Rojo",
        Color(0xFFFF9800) to "Naranja",
        Color(0xFF9C27B0) to "Morado",
        Color(0xFFFFEB3B) to "Amarillo",
        Color(0xFF795548) to "Marrón",
        Color.Gray to "Gris"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                "Color de texto",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colors.forEach { (color, name) ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(color, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { onColorSelected(color) }) {
                            if (color == currentColor) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = name,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Data class para el formato de texto
 */
data class TextFormat(
    val textType: TextType = TextType.PARAGRAPH,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val fontSize: Int = 16,
    val textColor: Color = Color.Black,
    val alignment: TextAlign = TextAlign.Start,
    val isBulletList: Boolean = false,
    val isNumberedList: Boolean = false
)

/**
 * Tipos de texto disponibles
 */
enum class TextType(val displayName: String, val defaultSize: Int) {
    HEADING_1("Título 1", 32),
    HEADING_2("Título 2", 24),
    HEADING_3("Título 3", 20),
    PARAGRAPH("Párrafo", 16),
    CODE("Código", 14),
    QUOTE("Cita", 16)
}
