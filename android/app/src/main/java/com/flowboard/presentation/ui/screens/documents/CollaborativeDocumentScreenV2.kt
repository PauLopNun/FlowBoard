package com.flowboard.presentation.ui.screens.documents

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
    modifier: Modifier = Modifier,
    viewModel: CollaborativeDocumentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val document by viewModel.document.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val activeUsers by viewModel.activeUsers.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showShareDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    var showSlashMenu by remember { mutableStateOf(false) }
    var slashMenuBlockId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

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
                onBack = onNavigateBack,
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
            AnimatedVisibility(
                visible = focusedBlockId != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
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
                    }
                )
            }
        }
    ) { padding ->
        if (blocks.isEmpty()) {
            // Empty state while loading
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                when (connectionState) {
                    is ConnectionState.Connecting -> CircularProgressIndicator()
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
                    else -> CircularProgressIndicator()
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                itemsIndexed(blocks, key = { _, b -> b.id }) { index, block ->
                    DocumentBlock(
                        block = block,
                        isFocused = focusedBlockId == block.id,
                        onFocusChange = { focused ->
                            if (focused) focusedBlockId = block.id
                        },
                        onTextChange = { newText ->
                            viewModel.insertText(block.id, newText, 0)
                        },
                        onCursorChange = { pos ->
                            viewModel.updateCursorPosition(block.id, pos)
                        },
                        onEnterPressed = {
                            val newBlock = ContentBlock(
                                id = UUID.randomUUID().toString(),
                                type = "p",
                                content = ""
                            )
                            viewModel.addBlock(newBlock, block.id)
                        },
                        onDeleteBlock = {
                            if (blocks.size > 1) viewModel.deleteBlock(block.id)
                        },
                        onSlashCommand = {
                            showSlashMenu = true
                            slashMenuBlockId = block.id
                        },
                        onMarkdownShortcut = { type, cleanedText ->
                            viewModel.updateBlockType(block.id, type)
                            viewModel.insertText(block.id, cleanedText, 0)
                        },
                        isTitle = index == 0 && block.type == "h1"
                    )
                }

                // Add new block button at the bottom
                item {
                    TextButton(
                        onClick = {
                            val newBlock = ContentBlock(
                                id = UUID.randomUUID().toString(),
                                type = "p",
                                content = ""
                            )
                            viewModel.addBlock(newBlock, blocks.lastOrNull()?.id)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                "Add a new block",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
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
                slashMenuBlockId?.let { blockId ->
                    viewModel.updateBlockType(blockId, type)
                    // Clear the "/" from the block
                    val block = blocks.find { it.id == blockId }
                    if (block?.content == "/") {
                        viewModel.insertText(blockId, "", 0)
                    }
                }
                showSlashMenu = false
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

    // Error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
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
    showExportMenu: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onToggleExportMenu: () -> Unit,
    onDismissExportMenu: () -> Unit,
    onExportMarkdown: () -> Unit,
    onExportPdf: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
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
                Icon(Icons.Default.ArrowBack, "Back")
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
    onFocusChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onCursorChange: (Int) -> Unit,
    onEnterPressed: () -> Unit,
    onDeleteBlock: () -> Unit,
    onSlashCommand: () -> Unit,
    onMarkdownShortcut: (type: String, cleanedText: String) -> Unit,
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
        isFocused -> "Type '/' for commands"
        else -> ""
    }

    val horizontalPadding = if (isTitle) 20.dp else 20.dp
    val verticalPadding = when {
        isTitle -> 16.dp
        block.type.startsWith("h") -> 10.dp
        else -> 6.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (block.type == "code")
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else Color.Transparent
            )
            .clip(if (block.type == "code") RoundedCornerShape(8.dp) else RoundedCornerShape(0.dp))
            .padding(
                horizontal = horizontalPadding,
                vertical = verticalPadding
            )
    ) {
        fun handleValueChange(new: TextFieldValue, allowDelete: Boolean) {
            // Markdown shortcut detection (only on paragraph blocks)
            if (block.type == "p") {
                val shortcut = when {
                    new.text.startsWith("### ") -> "h3" to new.text.removePrefix("### ")
                    new.text.startsWith("## ") -> "h2" to new.text.removePrefix("## ")
                    new.text.startsWith("# ") -> "h1" to new.text.removePrefix("# ")
                    new.text.startsWith("- ") || new.text.startsWith("* ") ->
                        "bullet" to new.text.drop(2)
                    new.text.startsWith("1. ") -> "numbered" to new.text.drop(3)
                    new.text == "```" -> "code" to ""
                    else -> null
                }
                if (shortcut != null) {
                    textFieldValue = TextFieldValue(shortcut.second)
                    onMarkdownShortcut(shortcut.first, shortcut.second)
                    return
                }
            }
            if (new.text == "/" && textFieldValue.text.isEmpty()) {
                onSlashCommand()
                return
            }
            if (allowDelete && new.text.isEmpty() && textFieldValue.text.isEmpty()) {
                onDeleteBlock()
                return
            }
            textFieldValue = new
            onTextChange(new.text)
            onCursorChange(new.selection.start)
        }

        if (block.type == "bullet" || block.type == "numbered") {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = if (block.type == "bullet") "•  " else "1.  ",
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
        } else {
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
    return base.copy(
        fontWeight = if (block.fontWeight == "bold") FontWeight.Bold else base.fontWeight,
        fontStyle = if (block.fontStyle == "italic") FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (block.textDecoration == "underline") TextDecoration.Underline else null,
        color = if (block.color.isNotEmpty() && block.color != "#000000" && block.color != "default")
            try { Color(android.graphics.Color.parseColor(
                if (block.color.startsWith("#")) block.color else "#${block.color}"
            )) }
            catch (_: Exception) { Color.Unspecified }
        else Color.Unspecified
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
    onBlockType: (String) -> Unit
) {
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

            FormatButton(Icons.Default.Code, "Code",
                active = currentBlock?.type == "code",
                onClick = { onBlockType(if (currentBlock?.type == "code") "p" else "code") })
            FormatButton(Icons.Default.FormatListBulleted, "Bullet list",
                active = currentBlock?.type == "bullet",
                onClick = { onBlockType(if (currentBlock?.type == "bullet") "p" else "bullet") })
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
        Triple("bullet", Icons.Default.FormatListBulleted, "Bullet List"),
        Triple("numbered", Icons.Default.FormatListNumbered, "Numbered List"),
        Triple("code", Icons.Default.Code, "Code Block"),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp))
                Text("Turn into…", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                commands.forEach { (type, icon, label) ->
                    ListItem(
                        headlineContent = { Text(label) },
                        leadingContent = {
                            Icon(icon, null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(type) }
                    )
                }
            }
        },
        confirmButton = {}
    )
}

data class BlockFormatting(
    val fontWeight: String? = null,
    val fontStyle: String? = null,
    val textDecoration: String? = null
)
