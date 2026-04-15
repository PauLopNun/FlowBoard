package com.flowboard.presentation.ui.screens.documents

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowboard.data.models.DocumentUserPresence
import com.flowboard.data.models.crdt.ContentBlock
import com.flowboard.data.remote.websocket.ConnectionState
import com.flowboard.presentation.ui.components.CollaboratorRole
import com.flowboard.presentation.ui.components.ShareDocumentDialog
import com.flowboard.presentation.ui.components.UserAvatar
import com.flowboard.presentation.viewmodel.CollaborativeDocumentViewModel
import java.util.UUID

/**
 * Collaborative document editor — Notion-style block-based editor with real-time sync.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollaborativeDocumentScreenV2(
    documentId: String,
    onNavigateBack: () -> Unit,
    onNavigateToDocument: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CollaborativeDocumentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val document by viewModel.document.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val activeUsers by viewModel.activeUsers.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaving = uiState.isSaving

    var showShareDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    var showSlashMenu by remember { mutableStateOf(false) }
    var slashMenuBlockId by remember { mutableStateOf<String?>(null) }
    var showSubPageDialog by remember { mutableStateOf(false) }
    var subPageTitle by remember { mutableStateOf("") }
    var showCoverPicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current

    // Local snapshot list for smooth drag-to-reorder (avoids round-tripping through ViewModel on every step)
    val localBlocks: SnapshotStateList<com.flowboard.data.models.crdt.ContentBlock> =
        remember { mutableStateListOf() }
    var isDraggingActive by remember { mutableStateOf(false) }
    var draggedBlockId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(blocks) {
        if (!isDraggingActive) {
            localBlocks.clear()
            localBlocks.addAll(blocks)
        }
    }

    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        localBlocks.apply { add(to.index, removeAt(from.index)) }
    }

    // Page emoji — tappable icon above the title
    var pageEmoji by remember { mutableStateOf("📄") }
    var showEmojiPicker by remember { mutableStateOf(false) }

    // Cover color from ViewModel state
    val coverColor = uiState.coverColor

    LaunchedEffect(uiState.shareSuccessMessage) {
        uiState.shareSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearShareSuccess()
        }
    }
    LaunchedEffect(documentId) { viewModel.connectToDocument(documentId) }
    DisposableEffect(Unit) { onDispose { viewModel.disconnect() } }

    val blocks = document?.blocks ?: emptyList()
    val focusedBlock = blocks.find { it.id == focusedBlockId }

    // Word count derived from all block text
    val wordCount = remember(blocks) {
        blocks.filter { it.type != "divider" }
            .joinToString(" ") { it.content }
            .trim()
            .split("\\s+".toRegex())
            .count { it.isNotBlank() }
    }

    // Other users currently in the document (exclude self)
    val otherActiveUsers = remember(activeUsers, uiState.currentUserId) {
        activeUsers.filter { it.userId != uiState.currentUserId }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val docTitle = blocks.firstOrNull { it.type == "h1" }?.content
                ?: blocks.firstOrNull()?.content
                ?: "Untitled"
            DocumentTopBar(
                title = docTitle,
                connectionState = connectionState,
                activeUsers = activeUsers,
                breadcrumbs = uiState.breadcrumbs,
                onBack = onNavigateBack,
                onSave = { viewModel.saveDocument() },
                isSaving = isSaving,
                onShare = { showShareDialog = true },
                showExportMenu = showExportMenu,
                onToggleExportMenu = { showExportMenu = !showExportMenu },
                onDismissExportMenu = { showExportMenu = false },
                onExportMarkdown = {
                    showExportMenu = false
                    exportToMarkdown(blocks, docTitle, context)
                },
                onExportPdf = {
                    showExportMenu = false
                    exportToPdf(blocks, docTitle, context)
                }
            )
        },
        bottomBar = {
            Column {
                // Typing / presence indicator — shown when others are in the doc
                AnimatedVisibility(
                    visible = otherActiveUsers.isNotEmpty(),
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    TypingIndicatorBar(users = otherActiveUsers)
                }

                // Formatting toolbar — shown when a block is focused
                AnimatedVisibility(
                    visible = focusedBlockId != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    Column {
                        // Word count status row
                        Surface(tonalElevation = 4.dp) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.TextFields,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "$wordCount ${if (wordCount == 1) "word" else "words"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                        FormattingToolbar(
                            currentBlock = focusedBlock,
                            onBold = {
                                focusedBlockId?.let { id ->
                                    viewModel.updateFormatting(id,
                                        fontWeight = if (focusedBlock?.fontWeight == "bold") "normal" else "bold")
                                }
                            },
                            onItalic = {
                                focusedBlockId?.let { id ->
                                    viewModel.updateFormatting(id,
                                        fontStyle = if (focusedBlock?.fontStyle == "italic") "normal" else "italic")
                                }
                            },
                            onUnderline = {
                                focusedBlockId?.let { id ->
                                    viewModel.updateFormatting(id,
                                        textDecoration = if (focusedBlock?.textDecoration == "underline") "none" else "underline")
                                }
                            },
                            onBlockType = { type ->
                                focusedBlockId?.let { id -> viewModel.updateBlockType(id, type) }
                            },
                            onColorChange = { color ->
                                focusedBlockId?.let { id -> viewModel.updateFormatting(id, color = color) }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        // Auto-seed a title block for new/empty documents after the server responds with nothing
        LaunchedEffect(connectionState) {
            if (connectionState is ConnectionState.Connected) {
                kotlinx.coroutines.delay(2000L)
                if (document?.blocks.isNullOrEmpty()) {
                    viewModel.addBlock(
                        ContentBlock(id = UUID.randomUUID().toString(), type = "h1", content = ""),
                        null
                    )
                }
            }
        }

        if (blocks.isEmpty()) {
            // Empty state while connecting / loading
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                when (connectionState) {
                    is ConnectionState.Error -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.CloudOff, null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error)
                            Text("Couldn't connect",
                                style = MaterialTheme.typography.titleMedium)
                            FilledTonalButton(onClick = { viewModel.connectToDocument(documentId) }) {
                                Text("Retry")
                            }
                        }
                    }
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                if (connectionState is ConnectionState.Connected) "Loading document…"
                                else "Connecting…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // Cover image banner (shown only when a cover color is set)
                if (coverColor.isNotEmpty()) {
                    item(key = "cover") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(
                                    try { Color(android.graphics.Color.parseColor(
                                        if (coverColor.startsWith("#")) coverColor else "#$coverColor"
                                    )) } catch (_: Exception) { Color(0xFF3B82F6) }
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { showCoverPicker = true },
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                ) {
                                    Text("Change cover", style = MaterialTheme.typography.labelSmall)
                                }
                                FilledTonalButton(
                                    onClick = { viewModel.removeCover() },
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                ) {
                                    Text("Remove", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                itemsIndexed(localBlocks, key = { _, b -> b.id }) { index, block ->
                    // Compute sequential index within the current numbered-list run
                    val numberedIdx = if (block.type == "numbered") {
                        localBlocks.take(index + 1).count { it.type == "numbered" }
                    } else 1
                    val isTitle = index == 0 && block.type == "h1"
                    if (isTitle) {
                        // Emoji + title on the same row, with "Add cover" button if no cover
                        Column(modifier = Modifier.fillMaxWidth().animateItem()) {
                            if (coverColor.isEmpty()) {
                                Row(
                                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextButton(
                                        onClick = { showCoverPicker = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Image, null, modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add cover", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, top = if (coverColor.isEmpty()) 4.dp else 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pageEmoji,
                                    fontSize = 36.sp,
                                    modifier = Modifier
                                        .padding(start = 12.dp, end = 8.dp)
                                        .clickable { showEmojiPicker = true }
                                )
                            DocumentBlock(
                                block = block,
                                isFocused = focusedBlockId == block.id,
                                numberedIndex = numberedIdx,
                                onFocusChange = { focused -> if (focused) focusedBlockId = block.id },
                                onTextChange = { newText -> viewModel.insertText(block.id, newText, 0) },
                                onToggleTodo = { isChecked -> viewModel.toggleTodo(block.id, isChecked) },
                                onCursorChange = { pos -> viewModel.updateCursorPosition(block.id, pos) },
                                onEnterPressed = {
                                    viewModel.addBlock(ContentBlock(id = UUID.randomUUID().toString(), type = "p", content = ""), block.id)
                                },
                                onDeleteBlock = { if (blocks.size > 1) viewModel.deleteBlock(block.id) },
                                onSlashCommand = { showSlashMenu = true; slashMenuBlockId = block.id },
                                onMarkdownShortcut = { type, cleanedText ->
                                    viewModel.updateBlockType(block.id, type)
                                    viewModel.insertText(block.id, cleanedText, 0)
                                },
                                onNavigateToSubPage = { docId -> onNavigateToDocument(docId) },
                                onToggleDetail = { detail -> viewModel.updateBlockDetail(block.id, detail) },
                                isTitle = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        } // end Column wrapper
                    } else {
                        ReorderableItem(reorderState, key = block.id) { isDragging ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = if (isDragging) 6.dp else 0.dp,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                        clip = false
                                    )
                                    .background(
                                        if (isDragging) MaterialTheme.colorScheme.surface
                                        else androidx.compose.ui.graphics.Color.Transparent
                                    ),
                                verticalAlignment = Alignment.Top
                            ) {
                                // ── Left handle: + add and ⠿ drag ──────────────
                                Column(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .padding(top = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    AnimatedVisibility(
                                        visible = focusedBlockId == block.id && block.type != "divider",
                                        enter = fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            // + button — inserts a new block before this one
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                                    .clickable {
                                                        val afterId = localBlocks.getOrNull(index - 1)?.id
                                                        viewModel.addBlock(
                                                            ContentBlock(
                                                                id = UUID.randomUUID().toString(),
                                                                type = "p",
                                                                content = ""
                                                            ),
                                                            afterId
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Add, null,
                                                    modifier = Modifier.size(13.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                                )
                                            }
                                            // ⠿ drag handle
                                            Icon(
                                                Icons.Default.DragIndicator, null,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .draggableHandle(
                                                        onDragStarted = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            isDraggingActive = true
                                                            draggedBlockId = block.id
                                                        },
                                                        onDragStopped = { _ ->
                                                            isDraggingActive = false
                                                            val movedId = draggedBlockId ?: return@draggableHandle
                                                            draggedBlockId = null
                                                            val movedIdx = localBlocks.indexOfFirst { it.id == movedId }
                                                            val afterId = if (movedIdx <= 0) null
                                                                          else localBlocks.getOrNull(movedIdx - 1)?.id
                                                            viewModel.moveBlock(movedId, afterId)
                                                        }
                                                    ),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                            )
                                        }
                                    }
                                }
                                // ── Block content ──────────────────────────────
                                DocumentBlock(
                                    block = block,
                                    isFocused = focusedBlockId == block.id,
                                    numberedIndex = numberedIdx,
                                    onFocusChange = { focused -> if (focused) focusedBlockId = block.id },
                                    onTextChange = { newText -> viewModel.insertText(block.id, newText, 0) },
                                    onToggleTodo = { isChecked -> viewModel.toggleTodo(block.id, isChecked) },
                                    onCursorChange = { pos -> viewModel.updateCursorPosition(block.id, pos) },
                                    onEnterPressed = {
                                        viewModel.addBlock(ContentBlock(id = UUID.randomUUID().toString(), type = "p", content = ""), block.id)
                                    },
                                    onDeleteBlock = { if (localBlocks.size > 1) viewModel.deleteBlock(block.id) },
                                    onSlashCommand = { showSlashMenu = true; slashMenuBlockId = block.id },
                                    onMarkdownShortcut = { type, cleanedText ->
                                        viewModel.updateBlockType(block.id, type)
                                        viewModel.insertText(block.id, cleanedText, 0)
                                    },
                                    onNavigateToSubPage = { docId -> onNavigateToDocument(docId) },
                                    onToggleDetail = { detail -> viewModel.updateBlockDetail(block.id, detail) },
                                    isTitle = false,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Bottom action row: new block + sub-page
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.addBlock(
                                    ContentBlock(id = UUID.randomUUID().toString(), type = "p", content = ""),
                                    localBlocks.lastOrNull()?.id
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(Modifier.width(4.dp))
                            Text("New block", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                        TextButton(
                            onClick = { showSubPageDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.NoteAdd, null, modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(Modifier.width(4.dp))
                            Text("Sub-page", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }

    // Slash command menu
    if (showSlashMenu) {
        SlashCommandMenu(
            onDismiss = { showSlashMenu = false },
            onSelect = { type ->
                if (type == "subpage") {
                    showSubPageDialog = true
                } else {
                    slashMenuBlockId?.let { blockId ->
                        viewModel.updateBlockType(blockId, type)
                        // Clear the "/" from the block
                        val block = blocks.find { it.id == blockId }
                        if (block?.content == "/") {
                            viewModel.insertText(blockId, "", 0)
                        }
                    }
                }
                showSlashMenu = false
            }
        )
    }

    // Sub-page creation dialog
    if (showSubPageDialog) {
        AlertDialog(
            onDismissRequest = { showSubPageDialog = false; subPageTitle = "" },
            title = { Text("New Sub-page") },
            text = {
                OutlinedTextField(
                    value = subPageTitle,
                    onValueChange = { subPageTitle = it },
                    label = { Text("Page title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sourceBlockId = slashMenuBlockId
                        val finalTitle = subPageTitle.trim().ifBlank { "Untitled" }
                        viewModel.createSubPage(documentId, finalTitle, afterBlockId = sourceBlockId)
                        // Clear the "/" character left by the slash command trigger
                        sourceBlockId?.let { id ->
                            val src = blocks.find { it.id == id }
                            if (src?.content == "/") viewModel.insertText(id, "", 0)
                        }
                        showSubPageDialog = false
                        subPageTitle = ""
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showSubPageDialog = false; subPageTitle = "" }) { Text("Cancel") }
            }
        )
    }

    // Share dialog
    if (showShareDialog) {
        ShareDocumentDialog(
            documentTitle = blocks.firstOrNull()?.content ?: "Untitled",
            currentCollaborators = emptyList(),
            onInviteUser = { email, role ->
                val roleStr = when (role) {
                    CollaboratorRole.EDITOR -> "editor"
                    CollaboratorRole.VIEWER -> "viewer"
                    CollaboratorRole.OWNER -> "owner"
                }
                viewModel.shareDocument(email, roleStr)
                showShareDialog = false
            },
            onUpdatePermission = { _, _ -> },
            onRemovePermission = { _ -> },
            onDismiss = { showShareDialog = false }
        )
    }

    // Emoji picker dialog
    if (showEmojiPicker) {
        EmojiPickerDialog(
            current = pageEmoji,
            onSelect = { emoji ->
                pageEmoji = emoji
                showEmojiPicker = false
            },
            onDismiss = { showEmojiPicker = false }
        )
    }

    // Cover picker dialog
    if (showCoverPicker) {
        CoverPickerDialog(
            current = uiState.coverColor,
            onSelect = { color ->
                viewModel.updateCoverColor(color)
                showCoverPicker = false
            },
            onDismiss = { showCoverPicker = false }
        )
    }

    // Error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Emoji Picker Dialog
// ────────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmojiPickerDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val emojis = listOf(
        "📄", "📝", "📋", "📌", "📎", "🗒️", "📓", "📔", "📕", "📗",
        "💡", "🎯", "🚀", "⭐", "🔥", "✅", "🎨", "🔖", "💼", "🏆",
        "🌟", "💬", "🗂️", "📊", "📈", "🔑", "🛠️", "🎉", "💎", "🌈"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose an icon") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                emojis.chunked(6).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (emoji == current)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable { onSelect(emoji) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ────────────────────────────────────────────────────────────────────────────────
// Cover Color Picker Dialog
// ────────────────────────────────────────────────────────────────────────────────

@Composable
private fun CoverPickerDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val covers = listOf(
        "#3B82F6" to Color(0xFF3B82F6),  // Blue
        "#6366F1" to Color(0xFF6366F1),  // Indigo
        "#8B5CF6" to Color(0xFF8B5CF6),  // Violet
        "#EC4899" to Color(0xFFEC4899),  // Pink
        "#EF4444" to Color(0xFFEF4444),  // Red
        "#F97316" to Color(0xFFF97316),  // Orange
        "#F59E0B" to Color(0xFFF59E0B),  // Amber
        "#10B981" to Color(0xFF10B981),  // Emerald
        "#14B8A6" to Color(0xFF14B8A6),  // Teal
        "#06B6D4" to Color(0xFF06B6D4),  // Cyan
        "#64748B" to Color(0xFF64748B),  // Slate
        "#1F2937" to Color(0xFF1F2937),  // Dark
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a cover color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                covers.chunked(4).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { (hex, color) ->
                            val isSelected = current.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .height(40.dp)
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelect(hex) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ────────────────────────────────────────────────────────────────────────────────
// Typing / Presence Indicator
// ────────────────────────────────────────────────────────────────────────────────

@Composable
private fun TypingIndicatorBar(users: List<com.flowboard.data.models.DocumentUserPresence>) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            val names = when (users.size) {
                1 -> users[0].userName
                2 -> "${users[0].userName} and ${users[1].userName}"
                else -> "${users[0].userName} and ${users.size - 1} others"
            }
            Text(
                "$names ${if (users.size == 1) "is" else "are"} editing…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Document Top Bar
// ────────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentTopBar(
    title: String,
    connectionState: ConnectionState,
    activeUsers: List<DocumentUserPresence>,
    breadcrumbs: List<Pair<String, String>>,
    showExportMenu: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean,
    onShare: () -> Unit,
    onToggleExportMenu: () -> Unit,
    onDismissExportMenu: () -> Unit,
    onExportMarkdown: () -> Unit,
    onExportPdf: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                // Breadcrumb trail (only when there's a parent)
                if (breadcrumbs.size > 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        breadcrumbs.dropLast(1).forEachIndexed { index, (_, crumbTitle) ->
                            Text(
                                crumbTitle.ifBlank { "Untitled" }.let {
                                    if (it.length > 14) it.take(12) + "…" else it
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                " / ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
                Text(
                    text = title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val (color, label) = when (connectionState) {
                        is ConnectionState.Connected ->
                            MaterialTheme.colorScheme.primary to "Live"
                        is ConnectionState.Connecting ->
                            MaterialTheme.colorScheme.tertiary to "Connecting…"
                        else -> MaterialTheme.colorScheme.error to "Offline"
                    }
                    Box(Modifier.size(6.dp).background(color, CircleShape))
                    Text(label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (activeUsers.size > 1) {
                        Text("· ${activeUsers.size} editing",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        },
        actions = {
            if (activeUsers.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-10).dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    activeUsers.take(4).forEach { user ->
                        UserAvatar(username = user.userName, isOnline = true, size = 28.dp)
                    }
                    if (activeUsers.size > 4) {
                        Box(
                            Modifier.size(28.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+${activeUsers.size - 4}",
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            IconButton(onClick = onSave, enabled = !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.Save, "Save")
                }
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.PersonAdd, "Share")
            }
            Box {
                IconButton(onClick = onToggleExportMenu) {
                    Icon(Icons.Default.MoreVert, "More options")
                }
                DropdownMenu(
                    expanded = showExportMenu,
                    onDismissRequest = onDismissExportMenu
                ) {
                    DropdownMenuItem(
                        text = { Text("Export as Markdown") },
                        leadingIcon = { Icon(Icons.Default.Code, null) },
                        onClick = onExportMarkdown
                    )
                    DropdownMenuItem(
                        text = { Text("Export as PDF") },
                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) },
                        onClick = onExportPdf
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

// ────────────────────────────────────────────────────────────────────────────────
// Document Block
// ────────────────────────────────────────────────────────────────────────────────

@Composable
private fun DocumentBlock(
    block: ContentBlock,
    isFocused: Boolean,
    isTitle: Boolean,
    numberedIndex: Int = 1,
    onFocusChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onToggleTodo: (Boolean) -> Unit,
    onToggleDetail: (String) -> Unit = {},
    onCursorChange: (Int) -> Unit,
    onEnterPressed: () -> Unit,
    onDeleteBlock: () -> Unit,
    onSlashCommand: () -> Unit,
    onMarkdownShortcut: (type: String, cleanedText: String) -> Unit,
    onNavigateToSubPage: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(block.id) {
        mutableStateOf(TextFieldValue(block.content))
    }

    // Sync content if remote update changes it
    LaunchedEffect(block.content) {
        if (textFieldValue.text != block.content) {
            textFieldValue = TextFieldValue(block.content)
        }
    }

    val textStyle = buildTextStyle(block, isTitle)
    val placeholder = when {
        isTitle -> "Untitled"
        block.type == "h1" -> "Heading 1"
        block.type == "h2" -> "Heading 2"
        block.type == "h3" -> "Heading 3"
        block.type == "code" -> "// Code…"
        block.type == "quote" -> "Quote…"
        block.type == "callout" -> "Callout…"
        block.type == "todo" -> "To-do"
        block.type == "toggle" -> "Toggle heading"
        isFocused -> "Type '/' for commands"
        else -> ""
    }

    // Subpage block — clickable inline page-link card (Notion-style)
    if (block.type == "subpage") {
        val parts = block.content.split("||", limit = 2)
        val subDocId = parts[0]
        val pageTitle = parts.getOrElse(1) { "Untitled" }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .then(
                    if (onNavigateToSubPage != null && subDocId.isNotEmpty())
                        Modifier.clickable { onNavigateToSubPage(subDocId) }
                    else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.Article,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                pageTitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Divider block — no text field, just a line
    if (block.type == "divider") {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .clickable { onFocusChange(true) }
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        return
    }

    val horizontalPadding = if (isTitle) 20.dp else 20.dp
    val verticalPadding = when {
        isTitle -> 16.dp
        block.type.startsWith("h") -> 10.dp
        else -> 6.dp
    }

    val blockBackground = when (block.type) {
        "code" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        "callout" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        else -> Color.Transparent
    }

    fun handleValueChange(new: TextFieldValue, allowDelete: Boolean) {
        if (block.type == "p") {
            val shortcut = when {
                new.text.startsWith("### ") -> "h3" to new.text.removePrefix("### ")
                new.text.startsWith("## ") -> "h2" to new.text.removePrefix("## ")
                new.text.startsWith("# ") -> "h1" to new.text.removePrefix("# ")
                new.text.startsWith("- ") || new.text.startsWith("* ") -> "bullet" to new.text.drop(2)
                new.text.startsWith("1. ") -> "numbered" to new.text.drop(3)
                new.text == "```" -> "code" to ""
                new.text == "> " -> "quote" to ""
                new.text == "[] " || new.text == "[ ] " -> "todo" to ""
                else -> null
            }
            if (shortcut != null) {
                textFieldValue = TextFieldValue(shortcut.second)
                onMarkdownShortcut(shortcut.first, shortcut.second)
                return
            }
        }
        if (new.text == "/" && textFieldValue.text.isEmpty()) { onSlashCommand(); return }
        if (allowDelete && new.text.isEmpty() && textFieldValue.text.isEmpty()) { onDeleteBlock(); return }
        textFieldValue = new
        onTextChange(new.text)
        onCursorChange(new.selection.start)
    }

    // Toggle block — expandable section with header + collapsible body
    if (block.type == "toggle") {
        var toggleExpanded by remember(block.id) { mutableStateOf(false) }
        Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { toggleExpanded = !toggleExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (toggleExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(4.dp))
                BlockTextField(
                    textFieldValue = textFieldValue,
                    textStyle = textStyle.copy(fontWeight = FontWeight.SemiBold),
                    placeholder = "Toggle heading",
                    modifier = Modifier.weight(1f),
                    onFocusChange = onFocusChange,
                    onValueChange = { new -> handleValueChange(new, allowDelete = true) },
                    onEnterPressed = onEnterPressed
                )
            }
            AnimatedVisibility(visible = toggleExpanded) {
                var detailValue by remember(block.id) { mutableStateOf(TextFieldValue(block.detail)) }
                LaunchedEffect(block.detail) {
                    if (detailValue.text != block.detail) detailValue = TextFieldValue(block.detail)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, top = 4.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = detailValue,
                        onValueChange = { new ->
                            detailValue = new
                            onToggleDetail(new.text)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = textStyle.copy(fontWeight = FontWeight.Normal),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            Box {
                                if (detailValue.text.isEmpty()) {
                                    Text(
                                        "Toggle content…",
                                        style = textStyle,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }
            }
        }
        return
    }

    // Quote block — left border accent
    if (block.type == "quote") {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .heightIn(min = 24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.width(12.dp))
            BlockTextField(
                textFieldValue = textFieldValue,
                textStyle = textStyle.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic
                ),
                placeholder = placeholder,
                modifier = Modifier.weight(1f),
                onFocusChange = onFocusChange,
                onValueChange = { new -> handleValueChange(new, allowDelete = true) },
                onEnterPressed = onEnterPressed
            )
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(blockBackground)
            .clip(if (block.type in listOf("code", "callout")) RoundedCornerShape(8.dp) else RoundedCornerShape(0.dp))
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
        when (block.type) {
            "todo" -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = block.isChecked,
                        onCheckedChange = { checked -> onToggleTodo(checked) },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    BlockTextField(
                        textFieldValue = textFieldValue,
                        textStyle = if (block.isChecked)
                            textStyle.copy(textDecoration = TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else textStyle,
                        placeholder = placeholder,
                        modifier = Modifier.weight(1f),
                        onFocusChange = onFocusChange,
                        onValueChange = { new -> handleValueChange(new, allowDelete = true) },
                        onEnterPressed = onEnterPressed
                    )
                }
            }
            "callout" -> {
                Row(verticalAlignment = Alignment.Top) {
                    Text("💡", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(8.dp))
                    BlockTextField(
                        textFieldValue = textFieldValue,
                        textStyle = textStyle,
                        placeholder = placeholder,
                        modifier = Modifier.weight(1f),
                        onFocusChange = onFocusChange,
                        onValueChange = { new -> handleValueChange(new, allowDelete = true) },
                        onEnterPressed = onEnterPressed
                    )
                }
            }
            "bullet", "numbered" -> {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = if (block.type == "bullet") "•  " else "$numberedIndex.  ",
                        style = textStyle,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    BlockTextField(
                        textFieldValue = textFieldValue,
                        textStyle = textStyle,
                        placeholder = placeholder,
                        modifier = Modifier.weight(1f),
                        onFocusChange = onFocusChange,
                        onValueChange = { new -> handleValueChange(new, allowDelete = true) },
                        onEnterPressed = onEnterPressed
                    )
                }
            }
            else -> {
                BlockTextField(
                    textFieldValue = textFieldValue,
                    textStyle = textStyle,
                    placeholder = placeholder,
                    onFocusChange = onFocusChange,
                    onValueChange = { new -> handleValueChange(new, allowDelete = !isTitle) },
                    onEnterPressed = onEnterPressed
                )
            }
        }
    }
}

@Composable
private fun BlockTextField(
    textFieldValue: TextFieldValue,
    textStyle: TextStyle,
    placeholder: String,
    modifier: Modifier = Modifier,
    onFocusChange: (Boolean) -> Unit,
    onValueChange: (TextFieldValue) -> Unit,
    onEnterPressed: () -> Unit
) {
    BasicTextField(
        value = textFieldValue,
        onValueChange = { new ->
            // Detect Enter press by checking for newline in new text
            if (new.text.contains('\n')) {
                val trimmed = new.text.replace("\n", "")
                onValueChange(TextFieldValue(trimmed))
                onEnterPressed()
            } else {
                onValueChange(new)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { onFocusChange(it.isFocused) },
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Default
        ),
        decorationBox = { inner ->
            Box {
                if (textFieldValue.text.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        placeholder,
                        style = textStyle,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                }
                inner()
            }
        }
    )
}

@Composable
private fun buildTextStyle(block: ContentBlock, isTitle: Boolean): TextStyle {
    val base = when {
        isTitle -> MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp
        )
        block.type == "h1" -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        block.type == "h2" -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
        block.type == "h3" -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
        block.type == "code" -> MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
        else -> MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 26.sp)
    }
    // Empty / "#000000" / "default" → follow the current theme (white in dark, dark in light)
    val defaultTextColor = MaterialTheme.colorScheme.onBackground
    val resolvedColor = when {
        block.color.isEmpty() || block.color == "default" || block.color == "#000000" -> defaultTextColor
        else -> try {
            Color(android.graphics.Color.parseColor(
                if (block.color.startsWith("#")) block.color else "#${block.color}"
            ))
        } catch (_: Exception) { defaultTextColor }
    }
    return base.copy(
        fontWeight = if (block.fontWeight == "bold") FontWeight.Bold else base.fontWeight,
        fontStyle = if (block.fontStyle == "italic") FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (block.textDecoration == "underline") TextDecoration.Underline else null,
        color = resolvedColor
    )
}

// ────────────────────────────────────────────────────────────────────────────────
// Formatting Toolbar
// ────────────────────────────────────────────────────────────────────────────────

@Composable
private fun FormattingToolbar(
    currentBlock: ContentBlock?,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onBlockType: (String) -> Unit,
    onColorChange: (String) -> Unit
) {
    val textColors = listOf(
        // Row 1 — Neutrals
        "" to Color.Transparent,            // Auto (follows theme)
        "#1F2937" to Color(0xFF1F2937),     // Near-black
        "#6B7280" to Color(0xFF6B7280),     // Gray
        "#9CA3AF" to Color(0xFF9CA3AF),     // Light gray
        // Row 2 — Warm
        "#EF4444" to Color(0xFFEF4444),     // Red
        "#F97316" to Color(0xFFF97316),     // Orange
        "#F59E0B" to Color(0xFFF59E0B),     // Amber
        "#FBBF24" to Color(0xFFFBBF24),     // Yellow
        // Row 3 — Cool
        "#10B981" to Color(0xFF10B981),     // Emerald
        "#14B8A6" to Color(0xFF14B8A6),     // Teal
        "#06B6D4" to Color(0xFF06B6D4),     // Cyan
        "#3B82F6" to Color(0xFF3B82F6),     // Blue
        // Row 4 — Purple/Pink
        "#6366F1" to Color(0xFF6366F1),     // Indigo
        "#8B5CF6" to Color(0xFF8B5CF6),     // Violet
        "#A855F7" to Color(0xFFA855F7),     // Purple
        "#EC4899" to Color(0xFFEC4899),     // Pink
        // Row 5 — Dark/Earth
        "#92400E" to Color(0xFF92400E),     // Brown
        "#065F46" to Color(0xFF065F46),     // Dark green
        "#1E40AF" to Color(0xFF1E40AF),     // Dark blue
        "#7F1D1D" to Color(0xFF7F1D1D),     // Dark red
    )
    // Empty / "#000000" / "default" all mean "auto" in the new system
    val currentColor = currentBlock?.color
        ?.takeIf { it.isNotBlank() && it != "#000000" && it != "default" } ?: ""
    var showColorDropdown by remember { mutableStateOf(false) }

    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Block type chips
            BlockTypeChip("T", currentBlock?.type == "p",
                onClick = { onBlockType("p") })
            BlockTypeChip("H1", currentBlock?.type == "h1",
                onClick = { onBlockType("h1") })
            BlockTypeChip("H2", currentBlock?.type == "h2",
                onClick = { onBlockType("h2") })
            BlockTypeChip("H3", currentBlock?.type == "h3",
                onClick = { onBlockType("h3") })

            Box(
                Modifier.height(24.dp).width(1.dp).padding(horizontal = 4.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Format buttons
            FormatButton(Icons.Default.FormatBold, "Bold",
                active = currentBlock?.fontWeight == "bold", onClick = onBold)
            FormatButton(Icons.Default.FormatItalic, "Italic",
                active = currentBlock?.fontStyle == "italic", onClick = onItalic)
            FormatButton(Icons.Default.FormatUnderlined, "Underline",
                active = currentBlock?.textDecoration == "underline", onClick = onUnderline)

            Box(
                Modifier.height(24.dp).width(1.dp).padding(horizontal = 4.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            FormatButton(Icons.Default.CheckBox, "To-do",
                active = currentBlock?.type == "todo",
                onClick = { onBlockType(if (currentBlock?.type == "todo") "p" else "todo") })
            FormatButton(Icons.Default.FormatQuote, "Quote",
                active = currentBlock?.type == "quote",
                onClick = { onBlockType(if (currentBlock?.type == "quote") "p" else "quote") })
            FormatButton(Icons.Default.Code, "Code",
                active = currentBlock?.type == "code",
                onClick = { onBlockType(if (currentBlock?.type == "code") "p" else "code") })
            FormatButton(Icons.Default.FormatListBulleted, "Bullet list",
                active = currentBlock?.type == "bullet",
                onClick = { onBlockType(if (currentBlock?.type == "bullet") "p" else "bullet") })

            Box(
                Modifier.height(24.dp).width(1.dp).padding(horizontal = 4.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Color picker dropdown — same level as other format buttons
            Box {
                val indicatorColor = if (currentColor.isEmpty()) MaterialTheme.colorScheme.onBackground
                    else try { Color(android.graphics.Color.parseColor(currentColor)) }
                    catch (_: Exception) { MaterialTheme.colorScheme.onBackground }
                IconButton(
                    onClick = { showColorDropdown = !showColorDropdown },
                    modifier = Modifier.size(36.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.FormatColorText,
                            contentDescription = "Text color",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 14.dp, height = 3.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(indicatorColor)
                        )
                    }
                }
                DropdownMenu(
                    expanded = showColorDropdown,
                    onDismissRequest = { showColorDropdown = false }
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        textColors.chunked(4).forEach { rowColors ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                rowColors.forEach { (hex, color) ->
                                    val isAuto = hex.isEmpty()
                                    val isSelected = if (isAuto) currentColor.isEmpty()
                                        else currentColor.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(if (isSelected) 28.dp else 24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isAuto) MaterialTheme.colorScheme.surface
                                                else color
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                                    else if (isAuto) MaterialTheme.colorScheme.outlineVariant
                                                    else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                onColorChange(hex)
                                                showColorDropdown = false
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isAuto) {
                                            Text(
                                                "A",
                                                style = MaterialTheme.typography.labelSmall
                                                    .copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockTypeChip(label: String, active: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = active,
        onClick = onClick,
        label = {
            Text(label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
        },
        modifier = Modifier.height(32.dp)
    )
}

@Composable
private fun FormatButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = if (active)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Slash Command Menu (Notion-style)
// ────────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlashCommandMenu(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val commands = listOf(
        Triple("h1", Icons.Default.Title, "Heading 1"),
        Triple("h2", Icons.Default.Title, "Heading 2"),
        Triple("h3", Icons.Default.Title, "Heading 3"),
        Triple("p", Icons.Default.Subject, "Paragraph"),
        Triple("toggle", Icons.Default.ExpandMore, "Toggle"),
        Triple("todo", Icons.Default.CheckBox, "To-do"),
        Triple("bullet", Icons.Default.FormatListBulleted, "Bullet List"),
        Triple("numbered", Icons.Default.FormatListNumbered, "Numbered List"),
        Triple("quote", Icons.Default.FormatQuote, "Quote"),
        Triple("callout", Icons.Default.Lightbulb, "Callout"),
        Triple("code", Icons.Default.Code, "Code Block"),
        Triple("divider", Icons.Default.HorizontalRule, "Divider"),
        Triple("subpage", Icons.Default.NoteAdd, "Sub-page"),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp))
            Text("Turn into…", style = MaterialTheme.typography.titleMedium)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            commands.forEach { (type, icon, label) ->
                ListItem(
                    headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
                    leadingContent = {
                        Icon(icon, null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(type) }
                )
            }
        }
    }
}

data class BlockFormatting(
    val fontWeight: String? = null,
    val fontStyle: String? = null,
    val textDecoration: String? = null
)
