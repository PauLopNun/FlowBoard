package com.flowboard.presentation.ui.screens.documents

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowboard.data.local.entities.DocumentEntity
import com.flowboard.presentation.viewmodel.DocumentViewModel

/**
 * Pantalla de lista de documentos.
 * Carga documentos propios Y compartidos desde el servidor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDocumentsScreen(
    onDocumentClick: (String) -> Unit,
    onCreateDocument: () -> Unit,
    onNavigateBack: () -> Unit,
    onToggleStar: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DocumentViewModel = hiltViewModel()
) {
    val listState by viewModel.documentListState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    // Cargar documentos del servidor al entrar
    LaunchedEffect(Unit) {
        viewModel.fetchAllDocuments()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Documents") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Botón de recargar
                    IconButton(onClick = { viewModel.fetchAllDocuments() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateDocument,
                icon = { Icon(Icons.Default.Add, "Create") },
                text = { Text("New Document") }
            )
        }
    ) { padding ->

        if (listState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        listState.error?.let { error ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text("Could not load documents", style = MaterialTheme.typography.titleMedium)
                    Text(error, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilledTonalButton(onClick = { viewModel.fetchAllDocuments() }) {
                        Text("Retry")
                    }
                }
            }
            return@Scaffold
        }

        val ownedDocs = listState.ownedDocuments
        val sharedDocs = listState.sharedWithMe

        if (ownedDocs.isEmpty() && sharedDocs.isEmpty()) {
            Box(
                modifier = modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Text(
                        "No documents yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        "Create your first document to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(onClick = onCreateDocument) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Create Document")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Documentos propios
                if (ownedDocs.isNotEmpty()) {
                    item {
                        Text(
                            "My Documents (${ownedDocs.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(items = ownedDocs, key = { it.id }) { doc ->
                        DocumentCard(
                            title = doc.title,
                            subtitle = "Owner • ${formatDate(doc.updatedAt)}",
                            onClick = { onDocumentClick(doc.id) },
                            onDelete = { showDeleteDialog = doc.id },
                            showDelete = true,
                            isStarred = doc.isStarred,
                            onToggleStar = { onToggleStar(doc.id) },
                            onDuplicate = { viewModel.duplicateDocument(doc.id) }
                        )
                    }
                }

                // Documentos compartidos conmigo
                if (sharedDocs.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Shared with me (${sharedDocs.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(items = sharedDocs, key = { "shared_${it.id}" }) { doc ->
                        DocumentCard(
                            title = doc.title,
                            subtitle = "Shared by ${doc.ownerName ?: "someone"} • ${formatDate(doc.updatedAt)}",
                            onClick = { onDocumentClick(doc.id) },
                            onDelete = null,
                            showDelete = false,
                            isStarred = doc.isStarred,
                            onToggleStar = { onToggleStar(doc.id) },
                            onDuplicate = null
                        )
                    }
                }
            }
        }

        // Diálogo de confirmación de eliminación
        showDeleteDialog?.let { documentId ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete Document?") },
                text = { Text("This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteDocumentViaApi(documentId)
                            showDeleteDialog = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun DocumentCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    showDelete: Boolean,
    isStarred: Boolean = false,
    onToggleStar: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Star icon button
            if (onToggleStar != null) {
                IconButton(onClick = onToggleStar) {
                    Icon(
                        imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (isStarred) "Unstar" else "Star",
                        tint = if (isStarred) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            if (showDelete && onDelete != null) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (onDuplicate != null) {
                            DropdownMenuItem(
                                text = { Text("Duplicate") },
                                onClick = {
                                    showMenu = false
                                    onDuplicate()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ContentCopy, null)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun formatDate(dateStr: String): String {
    return try {
        // dateStr format: "2024-01-15T10:30:00"
        val parts = dateStr.substringBefore("T").split("-")
        if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else dateStr
    } catch (e: Exception) {
        "Recently modified"
    }
}
