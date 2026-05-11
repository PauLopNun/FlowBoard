package com.flowboard.presentation.ui.screens.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.flowboard.data.local.entities.DocumentEntity
import com.flowboard.data.local.entities.WorkspaceEntity
import com.flowboard.presentation.viewmodel.ChatViewModel
import com.flowboard.presentation.viewmodel.WorkspaceDocumentsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceDocumentsScreen(
    workspaceId: String,
    onNavigateBack: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onCreateDocument: (workspaceId: String) -> Unit,
    onChatClick: (chatId: String) -> Unit = {},
    viewModel: WorkspaceDocumentsViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chatRooms by chatViewModel.chatRooms.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var moveToWorkspaceDoc by remember { mutableStateOf<DocumentEntity?>(null) }

    LaunchedEffect(workspaceId) { viewModel.load(workspaceId) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    fun openOrCreateWorkspaceChat() {
        val existing = chatRooms.firstOrNull { it.resourceId == workspaceId }
        if (existing != null) {
            onChatClick(existing.id)
        } else {
            chatViewModel.createGroupChat(
                name = "${uiState.workspaceName.ifBlank { "Workspace" }} Chat",
                participantIds = emptyList(),
                resourceId = workspaceId,
                resourceType = com.flowboard.domain.model.ResourceType.WORKSPACE
            ) { chatId -> onChatClick(chatId) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (!uiState.workspaceImageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = uiState.workspaceImageUrl,
                                        contentDescription = uiState.workspaceName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        uiState.workspaceName.take(1).uppercase().ifBlank { "W" },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(uiState.workspaceName.ifBlank { "Workspace" })
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { openOrCreateWorkspaceChat() }) {
                        Icon(Icons.Default.Chat, "Group chat")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onCreateDocument(workspaceId) }) {
                Icon(Icons.Default.Add, "New workspace document")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Visibility info banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "Documents shared with all workspace members",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.documents.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Description, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No shared documents yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Create a document and set visibility to \"Workspace\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.documents, key = { it.id }) { doc ->
                        WorkspaceDocumentCard(
                            document = doc,
                            onClick = { onDocumentClick(doc.id) },
                            hasOtherWorkspaces = uiState.workspaces.any { it.id != workspaceId },
                            onMovePrivate = { viewModel.moveToPrivate(doc.id) },
                            onMoveWorkspace = { moveToWorkspaceDoc = doc },
                            onDelete = { viewModel.deleteDocument(doc.id) }
                        )
                    }
                }
            }
        }
    }

    moveToWorkspaceDoc?.let { doc ->
        MoveWorkspaceDocumentDialog(
            document = doc,
            workspaces = uiState.workspaces.filter { it.id != workspaceId },
            onDismiss = { moveToWorkspaceDoc = null },
            onMove = { targetWorkspaceId ->
                viewModel.moveToWorkspace(doc.id, targetWorkspaceId)
                moveToWorkspaceDoc = null
            }
        )
    }
}

@Composable
private fun WorkspaceDocumentCard(
    document: DocumentEntity,
    onClick: () -> Unit,
    hasOtherWorkspaces: Boolean,
    onMovePrivate: () -> Unit,
    onMoveWorkspace: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    document.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                document.ownerName?.let {
                    Text(
                        "by $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.Group,
                contentDescription = "Workspace",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "More options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Move to private") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        onClick = {
                            showMenu = false
                            onMovePrivate()
                        }
                    )
                    if (hasOtherWorkspaces) {
                        DropdownMenuItem(
                            text = { Text("Move to another workspace") },
                            leadingIcon = { Icon(Icons.Default.DriveFileMove, null) },
                            onClick = {
                                showMenu = false
                                onMoveWorkspace()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Move to trash", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MoveWorkspaceDocumentDialog(
    document: DocumentEntity,
    workspaces: List<WorkspaceEntity>,
    onDismiss: () -> Unit,
    onMove: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to workspace") },
        text = {
            if (workspaces.isEmpty()) {
                Text("No other workspaces available.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(document.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    workspaces.forEach { workspace ->
                        ListItem(
                            headlineContent = { Text(workspace.name) },
                            supportingContent = { Text("${workspace.memberCount} member${if (workspace.memberCount == 1) "" else "s"}") },
                            leadingContent = { Icon(Icons.Default.Group, null) },
                            modifier = Modifier.fillMaxWidth().clickable { onMove(workspace.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
