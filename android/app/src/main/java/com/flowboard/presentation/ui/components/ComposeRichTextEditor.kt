package com.flowboard.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.flowboard.data.remote.dto.UserPresenceInfo
import kotlin.math.abs

/**
 * Editor de texto enriquecido moderno usando compose-rich-editor
 * con soporte para colaboración en tiempo real
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeRichTextEditor(
    initialHtml: String = "",
    onContentChange: (String) -> Unit,
    activeUsers: List<UserPresenceInfo> = emptyList(),
    onInviteUser: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Start typing something amazing..."
) {
    val richTextState = rememberRichTextState()
    var showToolbar by remember { mutableStateOf(true) }
    var showColorPicker by remember { mutableStateOf(false) }

    // Cargar contenido inicial
    LaunchedEffect(initialHtml) {
        if (initialHtml.isNotEmpty() && richTextState.annotatedString.text.isEmpty()) {
            richTextState.setHtml(initialHtml)
        }
    }

    // Notificar cambios
    LaunchedEffect(richTextState.annotatedString) {
        if (richTextState.annotatedString.text.isNotEmpty()) {
            onContentChange(richTextState.toHtml())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Barra superior con usuarios activos
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 4.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Usuarios activos
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((-8).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        activeUsers.take(5).forEach { user ->
                            UserAvatar(
                                username = user.username,
                                isOnline = user.isOnline,
                                size = 32.dp
                            )
                        }
                        if (activeUsers.size > 5) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "+${activeUsers.size - 5}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // Botones de acción
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Botón de invitar
                        FilledTonalButton(
                            onClick = onInviteUser,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Invite")
                        }

                        // Toggle toolbar
                        IconButton(onClick = { showToolbar = !showToolbar }) {
                            Icon(
                                imageVector = if (showToolbar) Icons.Default.KeyboardArrowUp
                                else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle toolbar"
                            )
                        }
                    }
                }

                // Mostrar nombres de usuarios activos si hay alguno
                AnimatedVisibility(
                    visible = activeUsers.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Editing with: ${activeUsers.joinToString(", ") { it.username }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Toolbar de formato
        AnimatedVisibility(
            visible = showToolbar,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // Formato de texto
                        FormatIconButton(
                            icon = Icons.Default.FormatBold,
                            selected = richTextState.currentSpanStyle.fontWeight == FontWeight.Bold,
                            onClick = {
                                richTextState.toggleSpanStyle(
                                    SpanStyle(fontWeight = FontWeight.Bold)
                                )
                            }
                        )

                        FormatIconButton(
                            icon = Icons.Default.FormatItalic,
                            selected = richTextState.currentSpanStyle.fontStyle == FontStyle.Italic,
                            onClick = {
                                richTextState.toggleSpanStyle(
                                    SpanStyle(fontStyle = FontStyle.Italic)
                                )
                            }
                        )

                        FormatIconButton(
                            icon = Icons.Default.FormatUnderlined,
                            selected = richTextState.currentSpanStyle.textDecoration == TextDecoration.Underline,
                            onClick = {
                                richTextState.toggleSpanStyle(
                                    SpanStyle(textDecoration = TextDecoration.Underline)
                                )
                            }
                        )

                        FormatIconButton(
                            icon = Icons.Default.StrikethroughS,
                            selected = richTextState.currentSpanStyle.textDecoration == TextDecoration.LineThrough,
                            onClick = {
                                richTextState.toggleSpanStyle(
                                    SpanStyle(textDecoration = TextDecoration.LineThrough)
                                )
                            }
                        )

                        VerticalDivider(modifier = Modifier.height(32.dp).padding(horizontal = 4.dp))

                        // Títulos
                        FilledTonalButton(
                            onClick = {
                                richTextState.toggleSpanStyle(
                                    SpanStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                )
                            },
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("H1", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                richTextState.toggleSpanStyle(
                                    SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                )
                            },
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("H2", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                richTextState.toggleSpanStyle(
                                    SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                )
                            },
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("H3", fontWeight = FontWeight.Bold)
                        }

                        VerticalDivider(modifier = Modifier.height(32.dp).padding(horizontal = 4.dp))

                        // Alineación
                        FormatIconButton(
                            icon = Icons.Default.FormatAlignLeft,
                            selected = richTextState.currentParagraphStyle.textAlign == TextAlign.Start,
                            onClick = {
                                richTextState.toggleParagraphStyle(
                                    ParagraphStyle(textAlign = TextAlign.Start)
                                )
                            }
                        )

                        FormatIconButton(
                            icon = Icons.Default.FormatAlignCenter,
                            selected = richTextState.currentParagraphStyle.textAlign == TextAlign.Center,
                            onClick = {
                                richTextState.toggleParagraphStyle(
                                    ParagraphStyle(textAlign = TextAlign.Center)
                                )
                            }
                        )

                        FormatIconButton(
                            icon = Icons.Default.FormatAlignRight,
                            selected = richTextState.currentParagraphStyle.textAlign == TextAlign.End,
                            onClick = {
                                richTextState.toggleParagraphStyle(
                                    ParagraphStyle(textAlign = TextAlign.End)
                                )
                            }
                        )

                        VerticalDivider(modifier = Modifier.height(32.dp).padding(horizontal = 4.dp))

                        // Listas
                        FormatIconButton(
                            icon = Icons.Default.FormatListBulleted,
                            selected = richTextState.isUnorderedList,
                            onClick = { richTextState.toggleUnorderedList() }
                        )

                        FormatIconButton(
                            icon = Icons.Default.FormatListNumbered,
                            selected = richTextState.isOrderedList,
                            onClick = { richTextState.toggleOrderedList() }
                        )

                        VerticalDivider(modifier = Modifier.height(32.dp).padding(horizontal = 4.dp))

                        // Bloque de código
                        FormatIconButton(
                            icon = Icons.Default.Code,
                            selected = richTextState.isCodeSpan,
                            onClick = { richTextState.toggleCodeSpan() }
                        )

                        // Color
                        IconButton(onClick = { showColorPicker = !showColorPicker }) {
                            Icon(Icons.Default.FormatColorText, "Text Color")
                        }
                    }

                    // Selector de color
                    if (showColorPicker) {
                        ColorPickerRow(
                            onColorSelected = { color ->
                                richTextState.toggleSpanStyle(SpanStyle(color = color))
                                showColorPicker = false
                            }
                        )
                    }

                    HorizontalDivider()
                }
            }
        }

        // Editor de texto
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            RichTextEditor(
                state = richTextState,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 28.sp
                ),
                colors = RichTextEditorDefaults.richTextEditorColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            )
        }

        // Barra inferior con estadísticas
        Surface(
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${richTextState.annotatedString.text.length} characters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Text(
                    text = "${richTextState.annotatedString.text.split("\\s+".toRegex()).size} words",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Botón de formato reutilizable
 */
@Composable
private fun FormatIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(36.dp)
            .then(
                if (selected) {
                    Modifier.background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (selected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Selector de color para el texto
 */
@Composable
private fun ColorPickerRow(
    onColorSelected: (Color) -> Unit
) {
    val colors = listOf(
        Color.Black to "Black",
        Color(0xFF1976D2) to "Blue",
        Color(0xFF388E3C) to "Green",
        Color(0xFFD32F2F) to "Red",
        Color(0xFFF57C00) to "Orange",
        Color(0xFF7B1FA2) to "Purple",
        Color(0xFF00796B) to "Teal",
        Color(0xFFC2185B) to "Pink",
        Color(0xFF5D4037) to "Brown",
        Color(0xFF616161) to "Gray",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Color:",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(end = 8.dp)
        )

        colors.forEach { (color, name) ->
            Surface(
                onClick = { onColorSelected(color) },
                shape = RoundedCornerShape(6.dp),
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

/**
 * Avatar de usuario con indicador de presencia
 */
@Composable
internal fun UserAvatar(
    username: String,
    isOnline: Boolean,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.size(size),
            shape = CircleShape,
            color = stringToColor(username),
            border = if (isOnline) {
                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else null
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = username.take(1).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        // Indicador de online
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.3f)
                    .align(Alignment.BottomEnd)
                    .background(Color(0xFF4CAF50), CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

/**
 * Generar un color único basado en el nombre de usuario
 */
private fun stringToColor(str: String): Color {
    val hash = str.hashCode()
    val hue = abs(hash % 360)

    // Colores vibrantes para avatares
    return when (hue % 10) {
        0 -> Color(0xFF1976D2)
        1 -> Color(0xFF388E3C)
        2 -> Color(0xFFD32F2F)
        3 -> Color(0xFFF57C00)
        4 -> Color(0xFF7B1FA2)
        5 -> Color(0xFF00796B)
        6 -> Color(0xFFC2185B)
        7 -> Color(0xFF5D4037)
        8 -> Color(0xFF303F9F)
        else -> Color(0xFF0097A7)
    }
}
