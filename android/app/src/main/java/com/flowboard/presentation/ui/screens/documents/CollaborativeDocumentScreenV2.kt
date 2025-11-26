package com.flowboard.presentation.ui.screens.documents

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowboard.data.models.crdt.ContentBlock
import com.flowboard.data.remote.websocket.ConnectionState
import com.flowboard.presentation.ui.components.CollaborativeCursorsLayer
import com.flowboard.presentation.ui.components.ShareDocumentDialog
import com.flowboard.presentation.ui.components.CollaboratorRole
import com.flowboard.presentation.ui.components.DocumentCollaborator
import com.flowboard.presentation.ui.components.UserAvatar
import com.flowboard.presentation.viewmodel.CollaborativeDocumentViewModel
import java.util.UUID

/**
 * Modern collaborative document editor with real-time sync
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollaborativeDocumentScreenV2(
    documentId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollaborativeDocumentViewModel = hiltViewModel()
) {
    val document by viewModel.document.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val activeUsers by viewModel.activeUsers.collectAsStateWithLifecycle()
    val remoteCursors by viewModel.remoteCursors.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showShareDialog by remember { mutableStateOf(false) }
    var showVersionHistory by remember { mutableStateOf(false) }

    // Connect to document on mount
    LaunchedEffect(documentId) {
        viewModel.connectToDocument(documentId)
    }

    // Disconnect on unmount
    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnect()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = document?.blocks?.firstOrNull()?.content ?: "Untitled Document",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Connection indicator
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        when (connectionState) {
                                            is ConnectionState.Connected -> MaterialTheme.colorScheme.primary
                                            is ConnectionState.Connecting -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.error
                                        },
                                        androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            Text(
                                text = when (connectionState) {
                                    is ConnectionState.Connected -> "Connected"
                                    is ConnectionState.Connecting -> "Connecting..."
                                    is ConnectionState.Error -> "Offline"
                                    else -> "Disconnected"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (activeUsers.isNotEmpty()) {
                                Text(
                                    text = "• ${activeUsers.size} online",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Active users avatars
                    if (activeUsers.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((-8).dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            activeUsers.take(5).forEach { user ->
                                UserAvatar(
                                    username = user.userName,
                                    isOnline = user.isOnline,
                                    size = 32.dp
                                )
                            }
                            if (activeUsers.size > 5) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "+${activeUsers.size - 5}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    IconButton(onClick = { showVersionHistory = !showVersionHistory }) {
                        Icon(Icons.Default.History, contentDescription = "Version History")
                    }

                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Document editor
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                document?.blocks?.let { blocks ->
                    items(blocks, key = { it.id }) { block ->
                        CollaborativeBlock(
                            block = block,
                            onTextChange = { newText ->
                                viewModel.insertText(block.id, newText, 0)
                            },
                            onCursorPositionChange = { position ->
                                viewModel.updateCursorPosition(block.id, position)
                            },
                            onFormatChange = { formatting ->
                                viewModel.updateFormatting(
                                    blockId = block.id,
                                    fontWeight = formatting.fontWeight,
                                    fontStyle = formatting.fontStyle,
                                    textDecoration = formatting.textDecoration
                                )
                            },
                            onDelete = {
                                viewModel.deleteBlock(block.id)
                            }
                        )
                    }
                }

                // Add new block button
                item {
                    OutlinedButton(
                        onClick = {
                            val lastBlockId = document?.blocks?.lastOrNull()?.id
                            viewModel.addBlock(
                                block = ContentBlock(
                                    id = UUID.randomUUID().toString(),
                                    type = "p",
                                    content = ""
                                ),
                                afterBlockId = lastBlockId
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Block")
                    }
                }
            }

            // Collaborative cursors overlay
            CollaborativeCursorsLayer(
                cursors = remoteCursors,
                getCursorPosition = { cursor ->
                    // Calculate cursor position on screen
                    // This is a simplified version - in production you'd need to calculate actual positions
                    Pair(100f, 100f)
                }
            )

            // Show error if any
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }

    // Share dialog
    if (showShareDialog) {
        ShareDocumentDialog(
            documentTitle = document?.blocks?.firstOrNull()?.content ?: "Untitled",
            currentCollaborators = emptyList(), // TODO: Load from permissions
            onInviteUser = { email, role ->
                // TODO: Implement invitation
                showShareDialog = false
            },
            onUpdatePermission = { userId, role ->
                // TODO: Implement permission update
            },
            onRemovePermission = { userId ->
                // TODO: Implement permission removal
            },
            onDismiss = { showShareDialog = false }
        )
    }
}

/**
 * Collaborative block editor
 */
@Composable
fun CollaborativeBlock(
    block: ContentBlock,
    onTextChange: (String) -> Unit,
    onCursorPositionChange: (Int) -> Unit,
    onFormatChange: (BlockFormatting) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember(block.content) {
        mutableStateOf(TextFieldValue(block.content))
    }
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Formatting toolbar (shown when focused)
        AnimatedVisibility(
            visible = isFocused,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bold
                IconButton(
                    onClick = {
                        onFormatChange(
                            BlockFormatting(
                                fontWeight = if (block.fontWeight == "bold") "normal" else "bold"
                            )
                        )
                    }
                ) {
                    Icon(
                        Icons.Default.FormatBold,
                        contentDescription = "Bold",
                        tint = if (block.fontWeight == "bold")
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Italic
                IconButton(
                    onClick = {
                        onFormatChange(
                            BlockFormatting(
                                fontStyle = if (block.fontStyle == "italic") "normal" else "italic"
                            )
                        )
                    }
                ) {
                    Icon(
                        Icons.Default.FormatItalic,
                        contentDescription = "Italic",
                        tint = if (block.fontStyle == "italic")
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Underline
                IconButton(
                    onClick = {
                        onFormatChange(
                            BlockFormatting(
                                textDecoration = if (block.textDecoration == "underline") "none" else "underline"
                            )
                        )
                    }
                ) {
                    Icon(
                        Icons.Default.FormatUnderlined,
                        contentDescription = "Underline",
                        tint = if (block.textDecoration == "underline")
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Delete
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Block content
        BasicTextField(
            value = textValue,
            onValueChange = { newValue ->
                textValue = newValue
                onTextChange(newValue.text)
                onCursorPositionChange(newValue.selection.start)
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
            textStyle = getBlockTextStyle(block),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    if (textValue.text.isEmpty()) {
                        Text(
                            text = when (block.type) {
                                "h1" -> "Heading 1"
                                "h2" -> "Heading 2"
                                "h3" -> "Heading 3"
                                "p" -> "Start typing..."
                                "code" -> "Code block"
                                else -> "Text"
                            },
                            style = getBlockTextStyle(block),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )

        Divider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun getBlockTextStyle(block: ContentBlock): TextStyle {
    return when (block.type) {
        "h1" -> MaterialTheme.typography.headlineLarge
        "h2" -> MaterialTheme.typography.headlineMedium
        "h3" -> MaterialTheme.typography.headlineSmall
        "code" -> MaterialTheme.typography.bodyMedium.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        else -> MaterialTheme.typography.bodyLarge
    }.copy(
        fontWeight = if (block.fontWeight == "bold") FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (block.fontStyle == "italic") FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (block.textDecoration == "underline") TextDecoration.Underline else null,
        fontSize = block.fontSize.sp
    )
}

data class BlockFormatting(
    val fontWeight: String? = null,
    val fontStyle: String? = null,
    val textDecoration: String? = null
)
