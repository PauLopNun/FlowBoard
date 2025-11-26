package com.flowboard.presentation.ui.screens.documents

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flowboard.domain.model.CollaborativeDocument
import com.flowboard.domain.model.ContentBlock
import com.flowboard.presentation.ui.components.CollaborativeRichTextEditor
import com.flowboard.presentation.ui.components.UserAvatar
import com.flowboard.presentation.viewmodel.DocumentViewModel
import com.flowboard.presentation.viewmodel.UserCursor

/**
 * Collaborative document editor screen
 * This is the foundation for Google Docs-like functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollaborativeDocumentScreen(
    viewModel: com.flowboard.presentation.viewmodel.DocumentViewModel,
    onNavigateBack: () -> Unit,
    onShareDocument: () -> Unit,
    modifier: Modifier = Modifier
) {
    val documentState by viewModel.documentState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val activeUsers by viewModel.activeUsers.collectAsState()

    var showVersionHistory by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(documentState.document?.blocks?.firstOrNull { it.type == "h1" }?.content ?: "Untitled Document")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Connection status
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (documentState.isConnected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.error,
                                        androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            Text(
                                text = if (documentState.isConnected) "Connected" else "Offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (activeUsers.isNotEmpty()) {
                                Text(
                                    text = "• ${activeUsers.size} user(s) online",
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
                                    username = user.username,
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
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
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
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main editor area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Collaboration info card
                if (activeUsers.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Real-time collaboration active",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Rich text editor
                documentState.document?.let { doc ->
                    CollaborativeRichTextEditor(
                        blocks = doc.blocks,
                        onOperation = { op ->
                            viewModel.updateContent(op)
                        },
                        onCursorChange = { blockId, position ->
                            viewModel.updateCursor(blockId, position)
                        },
                        onFormattingChange = { op ->
                            viewModel.updateFormatting(op)
                        },
                        activeUsers = activeUsers,
                        userCursors = documentState.userCursors,
                        placeholder = "Start writing your document...",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Document metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Auto-saved",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    documentState.document?.let {
                        Text(
                            text = "${it.blocks.sumOf { it.content.length }} words",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Version history sidebar
            AnimatedVisibility(
                visible = showVersionHistory,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                VersionHistorySidebar(
                    onClose = { showVersionHistory = false },
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                )
            }
        }
    }

    // Share dialog - Using the new ShareDocumentDialog component
    if (showShareDialog) {
        com.flowboard.presentation.ui.components.ShareDocumentDialog(
            documentTitle = documentState.document?.blocks?.firstOrNull { it.type == "h1" }?.content ?: "Untitled Document",
            currentCollaborators = emptyList(), // TODO: Load from ViewModel
            onInviteUser = { email, role ->
                // TODO: Call ViewModel method to invite user
                showShareDialog = false
            },
            onUpdatePermission = { userId, role ->
                // TODO: Call ViewModel method to update permission
            },
            onRemovePermission = { userId ->
                // TODO: Call ViewModel method to remove permission
            },
            onDismiss = { showShareDialog = false }
        )
    }
}

@Composable
private fun VersionHistorySidebar(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Version History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Placeholder for version history
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(5) { index ->
                    VersionHistoryItem(
                        timestamp = "2 hours ago",
                        author = "User ${index + 1}",
                        changes = "${(index + 1) * 12} characters changed"
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionHistoryItem(
    timestamp: String,
    author: String,
    changes: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = timestamp,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "by $author",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = changes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


