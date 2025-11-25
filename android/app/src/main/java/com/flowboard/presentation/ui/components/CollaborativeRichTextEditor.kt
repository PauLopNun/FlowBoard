package com.flowboard.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import com.flowboard.data.remote.dto.UserPresenceInfo
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.TextField
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextLayoutResult
import com.flowboard.domain.model.ContentBlock
import com.flowboard.data.models.crdt.DocumentOperation
import com.flowboard.presentation.viewmodel.UserCursor
import kotlin.math.abs

private fun stringToColor(str: String): Color {
    val hash = str.hashCode()
    val red = abs(hash % 255)
    val green = abs((hash / 255) % 255)
    val blue = abs((hash / 255 / 255) % 255)
    return Color(red, green, blue)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollaborativeRichTextEditor(
    blocks: List<ContentBlock>,
    onOperation: (com.flowboard.data.models.crdt.DocumentOperation) -> Unit,
    onCursorChange: (blockId: String, position: Int) -> Unit,
    onFormattingChange: (com.flowboard.data.models.crdt.UpdateBlockFormattingOperation) -> Unit,
    activeUsers: List<UserPresenceInfo>,
    userCursors: List<com.flowboard.presentation.viewmodel.UserCursor>,
    modifier: Modifier = Modifier,
    placeholder: String = "Start typing...",
    enabled: Boolean = true,
) {
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    var showFormattingBar by remember { mutableStateOf(true) }
    var fontSize by remember { mutableStateOf(MaterialTheme.typography.bodyLarge.fontSize) }
    var fontColor by remember { mutableStateOf(Color.Black) }
    var textAlign by remember { mutableStateOf(androidx.compose.ui.text.style.TextAlign.Start) }
    var lastFocusedBlockId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Active users indicator
        AnimatedVisibility(
            visible = activeUsers.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.shapes.small
                    )
                    .padding(8.dp),
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
                Spacer(modifier = Modifier.weight(1f))
                // Active user avatars
                Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                    activeUsers.take(3).forEach { user ->
                        UserAvatar(
                            username = user.username,
                            isOnline = user.isOnline,
                            size = 24.dp
                        )
                    }
                }
            }
        }

        // Formatting toolbar
        AnimatedVisibility(
            visible = showFormattingBar,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Bold
                    FormattingButton(
                        icon = Icons.Default.FormatBold,
                        selected = isBold,
                        onClick = {
                            isBold = !isBold
                            lastFocusedBlockId?.let {
                                onFormattingChange(
                                    com.flowboard.data.models.crdt.UpdateBlockFormattingOperation(
                                        operationId = java.util.UUID.randomUUID().toString(),
                                        boardId = "", // boardId is not available here
                                        blockId = it,
                                        fontWeight = if (isBold) "bold" else "normal"
                                    )
                                )
                            }
                        }
                    )
                    // Italic
                    FormattingButton(
                        icon = Icons.Default.FormatItalic,
                        selected = isItalic,
                        onClick = {
                            isItalic = !isItalic
                            lastFocusedBlockId?.let {
                                onFormattingChange(
                                    com.flowboard.data.models.crdt.UpdateBlockFormattingOperation(
                                        operationId = java.util.UUID.randomUUID().toString(),
                                        boardId = "", // boardId is not available here
                                        blockId = it,
                                        fontStyle = if (isItalic) "italic" else "normal"
                                    )
                                )
                            }
                        }
                    )
                    // Underline
                    FormattingButton(
                        icon = Icons.Default.FormatUnderlined,
                        selected = isUnderline,
                        onClick = {
                            isUnderline = !isUnderline
                            lastFocusedBlockId?.let {
                                onFormattingChange(
                                    com.flowboard.data.models.crdt.UpdateBlockFormattingOperation(
                                        operationId = java.util.UUID.randomUUID().toString(),
                                        boardId = "", // boardId is not available here
                                        blockId = it,
                                        textDecoration = if (isUnderline) "underline" else "none"
                                    )
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                    VerticalDivider(modifier = Modifier.height(32.dp))
                    Spacer(modifier = Modifier.width(8.dp))

                    // Font size
                    var showFontSizeMenu by remember { mutableStateOf(false) }
                    Box {
                        FormattingButton(
                            icon = Icons.Default.FormatSize,
                            selected = false,
                            onClick = { showFontSizeMenu = true }
                        )
                        DropdownMenu(
                            expanded = showFontSizeMenu,
                            onDismissRequest = { showFontSizeMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("12") }, onClick = {
                                fontSize = 12.sp
                                lastFocusedBlockId?.let {
                                    onFormattingChange(
                                        com.flowboard.data.models.crdt.UpdateBlockFormattingOperation(
                                            operationId = java.util.UUID.randomUUID().toString(),
                                            boardId = "", // boardId is not available here
                                            blockId = it,
                                            fontSize = 12
                                        )
                                    )
                                }
                                showFontSizeMenu = false
                            })
                            DropdownMenuItem(text = { Text("16") }, onClick = {
                                fontSize = 16.sp
                                lastFocusedBlockId?.let {
                                    onFormattingChange(
                                        com.flowboard.data.models.crdt.UpdateBlockFormattingOperation(
                                            operationId = java.util.UUID.randomUUID().toString(),
                                            boardId = "", // boardId is not available here
                                            blockId = it,
                                            fontSize = 16
                                        )
                                    )
                                }
                                showFontSizeMenu = false
                            })
                            DropdownMenuItem(text = { Text("20") }, onClick = {
                                fontSize = 20.sp
                                lastFocusedBlockId?.let {
                                    onFormattingChange(
                                        com.flowboard.data.models.crdt.UpdateBlockFormattingOperation(
                                            operationId = java.util.UUID.randomUUID().toString(),
                                            boardId = "", // boardId is not available here
                                            blockId = it,
                                            fontSize = 20
                                        )
                                    )
                                }
                                showFontSizeMenu = false
                            })
                            DropdownMenuItem(text = { Text("24") }, onClick = {
                                fontSize = 24.sp
                                lastFocusedBlockId?.let {
                                    onFormattingChange(
                                        com.flowboard.data.models.crdt.UpdateBlockFormattingOperation(
                                            operationId = java.util.UUID.randomUUID().toString(),
                                            boardId = "", // boardId is not available here
                                            blockId = it,
                                            fontSize = 24
                                        )
                                    )
                                }
                                showFontSizeMenu = false
                            })
                        }
                    }

                    // Font color
                    var showFontColorMenu by remember { mutableStateOf(false) }
                    Box {
                        FormattingButton(
                            icon = Icons.Default.FormatColorText,
                            selected = false,
                            onClick = { showFontColorMenu = true }
                        )
                        DropdownMenu(
                            expanded = showFontColorMenu,
                            onDismissRequest = { showFontColorMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("Black") }, onClick = {
                                fontColor = Color.Black
                                lastFocusedBlockId?.let {
                                    onFormattingChange(
                                        com.flowboard.data.models.crdt.UpdateBlockFormattingOperation(
                                            operationId = java.util.UUID.randomUUID().toString(),
                                            boardId = "", // boardId is not available here
                                            blockId = it,
                                            color = "#000000"
                                        )
                                    )
                                }
                                showFontColorMenu = false
                            })
                            DropdownMenuItem(text = { Text("Red") }, onClick = {
                                fontColor = Color.Red
                                lastFocusedBlockId?.let {
                                    onFormattingChange(
                                        com.flowboard.data.models.crdt.UpdateBlockFormattingOperation(
                                            operationId = java.util.UUID.randomUUID().toString(),
                                            boardId = "", // boardId is not available here
                                            blockId = it,
                                            color = "#FF0000"
                                        )
                                    )
                                }
                                showFontColorMenu = false
                            })
                            DropdownMenuItem(text = { Text("Blue") }, onClick = {
                                fontColor = Color.Blue
                                lastFocusedBlockId?.let {
                                    onFormattingChange(
                                        com.flowboard.data.models.crdt.UpdateBlockFormattingOperation(
                                            operationId = java.util.UUID.randomUUID().toString(),
                                            boardId = "", // boardId is not available here
                                            blockId = it,
                                            color = "#0000FF"
                                        )
                                    )
                                }
                                showFontColorMenu = false
                            })
                            DropdownMenuItem(text = { Text("Green") }, onClick = {
                                fontColor = Color.Green
                                lastFocusedBlockId?.let {
                                    onFormattingChange(
                                        com.flowboard.data.models.crdt.UpdateBlockFormattingOperation(
                                            operationId = java.util.UUID.randomUUID().toString(),
                                            boardId = "", // boardId is not available here
                                            blockId = it,
                                            color = "#00FF00"
                                        )
                                    )
                                }
                                showFontColorMenu = false
                            })
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    VerticalDivider(modifier = Modifier.height(32.dp))
                    Spacer(modifier = Modifier.width(8.dp))

                    // Text alignment
                    FormattingButton(
                        icon = Icons.Default.FormatAlignLeft,
                        selected = textAlign == androidx.compose.ui.text.style.TextAlign.Start,
                        onClick = {
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                            lastFocusedBlockId?.let {
                                onFormattingChange(
                                    com.flowboard.data.models.crdt.UpdateBlockFormattingOperation(
                                        operationId = java.util.UUID.randomUUID().toString(),
                                        boardId = "", // boardId is not available here
                                        blockId = it,
                                        textAlign = "start"
                                    )
                                )
                            }
                        }
                    )
                    FormattingButton(
                        icon = Icons.Default.FormatAlignCenter,
                        selected = textAlign == androidx.compose.ui.text.style.TextAlign.Center,
                        onClick = {
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            lastFocusedBlockId?.let {
                                onFormattingChange(
                                    com.flowboard.data.models.crdt.UpdateBlockFormattingOperation(
                                        operationId = java.util.UUID.randomUUID().toString(),
                                        boardId = "", // boardId is not available here
                                        blockId = it,
                                        textAlign = "center"
                                    )
                                )
                            }
                        }
                    )
                    FormattingButton(
                        icon = Icons.Default.FormatAlignRight,
                        selected = textAlign == androidx.compose.ui.text.style.TextAlign.End,
                        onClick = {
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                            lastFocusedBlockId?.let {
                                onFormattingChange(
                                    com.flowboard.data.models.crdt.UpdateBlockFormattingOperation(
                                        operationId = java.util.UUID.randomUUID().toString(),
                                        boardId = "", // boardId is not available here
                                        blockId = it,
                                        textAlign = "end"
                                    )
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                    VerticalDivider(modifier = Modifier.height(32.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // List
                    FormattingButton(
                        icon = Icons.Default.FormatListBulleted,
                        selected = false,
                        onClick = { /* TODO: Add list formatting */ }
                    )
                    // Numbered list
                    FormattingButton(
                        icon = Icons.Default.FormatListNumbered,
                        selected = false,
                        onClick = { /* TODO: Add numbered list */ }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Toggle formatting bar
                    IconButton(
                        onClick = { showFormattingBar = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Hide formatting",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Show formatting bar button when hidden
        if (!showFormattingBar) {
            TextButton(
                onClick = { showFormattingBar = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Show formatting options")
            }
        }

        // Text editor
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (enabled)
                    MaterialTheme.colorScheme.surface
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column {
                blocks.forEach { block ->
                    var textFieldValue by remember(block.content) { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(block.content)) }
                    var textLayoutResult by remember<androidx.compose.ui.text.TextLayoutResult?> { mutableStateOf(null) }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row {
                            var expanded by remember { mutableStateOf(false) }
                            val items = listOf("p", "h1")
                            Box {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Block type")
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    items.forEach { selectionOption ->
                                        DropdownMenuItem(
                                            text = { Text(selectionOption) },
                                            onClick = {
                                                val op = com.flowboard.data.models.crdt.UpdateBlockTypeOperation(
                                                    operationId = java.util.UUID.randomUUID().toString(),
                                                    boardId = "", // boardId is not available here
                                                    blockId = block.id,
                                                    newType = selectionOption
                                                )
                                                onOperation(op)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                                                        BasicTextField(
                                                            value = textFieldValue,
                                                            onValueChange = {
                                                                textFieldValue = it
                                                                val op = com.flowboard.data.models.crdt.UpdateBlockContentOperation(
                                                                    operationId = java.util.UUID.randomUUID().toString(),
                                                                    boardId = "", // boardId is not available here
                                                                    blockId = block.id,
                                                                    content = it.text,
                                                                    position = 0 // position is not available here
                                                                )
                                                                onOperation(op)
                                                                onCursorChange(block.id, it.selection.start)
                                                            },
                                                            onTextLayout = {
                                                                textLayoutResult = it
                                                            },
                                                            modifier = Modifier
                                                                .onFocusChanged {
                                                                    if (it.isFocused) {
                                                                        lastFocusedBlockId = block.id
                                                                    }
                                                                }
                                                                .weight(1f)
                                                                .padding(16.dp),
                                                            textStyle = TextStyle(
                                                                color = fontColor,
                                                                fontSize = fontSize,
                                                                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                                                fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                                                                textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None,
                                                                textAlign = textAlign
                                                            ),
                                                            enabled = enabled,
                                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                                            decorationBox = { innerTextField ->
                                                                Box {
                                                                    if (textFieldValue.text.isEmpty()) {
                                                                        Text(
                                                                            text = placeholder,
                                                                            style = MaterialTheme.typography.bodyLarge,
                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                                        )
                                                                    }
                                                                    innerTextField()
                                                                    userCursors.filter { it.blockId == block.id }.forEach { cursor ->
                                                                        textLayoutResult?.let { layoutResult ->
                                                                            val cursorRect = layoutResult.getCursorRect(cursor.position)
                                                                            val user = activeUsers.find { it.userId == cursor.userId }
                                                                            if (user != null) {
                                                                                Cursor(
                                                                                    user = user.username,
                                                                                    color = stringToColor(user.username),
                                                                                    modifier = Modifier.offset(
                                                                                        x = cursorRect.left.dp,
                                                                                        y = cursorRect.top.dp
                                                                                    )
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        )
                                                    }
                        IconButton(
                            onClick = {
                                val op = com.flowboard.data.models.crdt.DeleteBlockOperation(
                                    operationId = java.util.UUID.randomUUID().toString(),
                                    boardId = "", // boardId is not available here
                                    blockId = block.id
                                )
                                onOperation(op)
                            },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete block")
                        }
                    }
                }
                Button(onClick = {
                    val op = com.flowboard.data.models.crdt.AddBlockOperation(
                        operationId = java.util.UUID.randomUUID().toString(),
                        boardId = "", // boardId is not available here
                        block = com.flowboard.data.models.crdt.ContentBlock(
                            id = java.util.UUID.randomUUID().toString(),
                            type = "p",
                            content = ""
                        ),
                        afterBlockId = blocks.last().id
                    )
                    onOperation(op)
                }) {
                    Text("Add block")
                }
            }
        }

        // Character count
        Text(
            text = "${blocks.sumOf { it.content.length }} characters",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
private fun FormattingButton(
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
                        MaterialTheme.shapes.small
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
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun UserAvatar(
    username: String,
    isOnline: Boolean,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.size(size),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            border = if (isOnline) {
                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else null
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = username.take(1).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        // Online indicator
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.3f)
                    .align(Alignment.BottomEnd)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

