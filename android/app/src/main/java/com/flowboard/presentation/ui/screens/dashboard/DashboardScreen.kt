package com.flowboard.presentation.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowboard.presentation.ui.theme.*
import com.flowboard.presentation.viewmodel.DocumentViewModel
import com.flowboard.presentation.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

import com.flowboard.presentation.ui.screens.tasks.TaskListScreen

enum class DashboardView {
    HOME,
    INBOX,
    SEARCH,
    TASKS,
    MY_DOCUMENTS,
    TRASH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onDocumentClick: (String) -> Unit,
    onCreateDocument: () -> Unit,
    onNotificationsClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
    onViewAllDocuments: () -> Unit = {},
    onEditorDemoClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    documentViewModel: DocumentViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel = hiltViewModel()
) {
    val documentListState by documentViewModel.documentListState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var currentView by remember { mutableStateOf(DashboardView.HOME) }
    var searchQuery by remember { mutableStateOf("") }

    // Trigger load documents
    LaunchedEffect(Unit) {
        documentViewModel.fetchAllDocuments()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                DashboardSidebar(
                    currentView = currentView,
                    onNavigate = { view ->
                        currentView = view
                        scope.launch { drawerState.close() }
                    },
                    onCreateDocument = {
                        scope.launch { drawerState.close() }
                        onCreateDocument()
                    },
                    onProfileClick = onProfileClick,
                    onSettingsClick = onSettingsClick,
                    onLogout = {
                        scope.launch { drawerState.close() }
                        loginViewModel.logout()
                        onLogout()
                    },
                    documents = documentListState.ownedDocuments,
                    onDocumentClick = { docId ->
                        scope.launch { drawerState.close() }
                        onDocumentClick(docId)
                    }
                )
            }
        }
    ) {
        if (currentView == DashboardView.TASKS) {
            // Temporarily wrapping TaskListScreen to allow drawer access if we modify it later, 
            // or just letting it be a full screen experience within the dashboard context.
            // For better UX, we might want to pass a navigation icon to TaskListScreen in the future.
            TaskListScreen(
                onTaskClick = { /* Handle task click */ },
                onCreateTaskClick = { /* Handle create */ },
                onNotificationsClick = onNotificationsClick,
                onChatClick = onChatClick,
                onProfileClick = onProfileClick,
                onSettingsClick = onSettingsClick,
                onLogout = onLogout
            )
            // Add a floating button to open drawer if TaskListScreen doesn't support it?
            // For now, let's stick to the requested functional access.
             Box(modifier = Modifier.fillMaxSize()) {
                IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
             }
        } else {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                "FlowBoard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { 
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(Icons.Default.Menu, "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = onNotificationsClick) {
                                Icon(Icons.Outlined.Notifications, "Notifications")
                            }
                            IconButton(onClick = onChatClick) {
                                Icon(Icons.Outlined.ChatBubbleOutline, "Chat")
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = onCreateDocument,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, "New Page")
                    }
                }
            ) { paddingValues ->
                 DashboardContent(
                    paddingValues = paddingValues,
                    currentView = currentView,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    documentListState = documentListState,
                    onDocumentClick = onDocumentClick,
                    onDeleteDocument = { docId ->
                        documentViewModel.deleteDocumentViaApi(docId)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardSidebar(
    currentView: DashboardView,
    onNavigate: (DashboardView) -> Unit,
    onCreateDocument: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit,
    documents: List<com.flowboard.data.local.entities.DocumentEntity>,
    onDocumentClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Workspace Selector (Placeholder)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clickable { onProfileClick() }
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("W", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "My Workspace",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Main Navigation
        NavigationItem(
            icon = Icons.Outlined.Home, 
            label = "Home", 
            isSelected = currentView == DashboardView.HOME,
            onClick = { onNavigate(DashboardView.HOME) }
        )
        NavigationItem(
            icon = Icons.Outlined.Inbox, 
            label = "Inbox", 
            isSelected = currentView == DashboardView.INBOX,
            onClick = { onNavigate(DashboardView.INBOX) }
        )
        NavigationItem(
            icon = Icons.Outlined.CheckCircle, 
            label = "Tasks", 
            isSelected = currentView == DashboardView.TASKS,
            onClick = { onNavigate(DashboardView.TASKS) }
        )
        NavigationItem(
            icon = Icons.Outlined.Search, 
            label = "Search", 
            isSelected = currentView == DashboardView.SEARCH,
            onClick = { onNavigate(DashboardView.SEARCH) }
        )

        Spacer(modifier = Modifier.height(12.dp))
        
        // Actions
        NavigationItem(
            icon = Icons.Default.Add,
            label = "New Page",
            isSelected = false,
            onClick = onCreateDocument
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Favorites / Private
        Text(
            "PRIVATE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 12.dp)
        )
        
        NavigationItem(
            icon = Icons.Outlined.Folder, 
            label = "My Documents", 
            isSelected = currentView == DashboardView.MY_DOCUMENTS,
            onClick = { onNavigate(DashboardView.MY_DOCUMENTS) }
        )

        // Dynamic list of private documents
        if (documents.isEmpty()) {
             Text(
                "No recent pages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 12.dp, top = 8.dp)
            )
        } else {
            documents.take(5).forEach { doc ->
                NavigationItem(
                    icon = Icons.Outlined.Description,
                    label = doc.title,
                    isSelected = false,
                    onClick = { onDocumentClick(doc.id) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
         // Shared
        Text(
            "SHARED",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        NavigationItem(Icons.Outlined.Group, "Team Updates", false) {}

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Actions
        NavigationItem(Icons.Outlined.Settings, "Settings", false, onSettingsClick)
        NavigationItem(
            icon = Icons.Outlined.Delete, 
            label = "Trash", 
            isSelected = currentView == DashboardView.TRASH,
            onClick = { onNavigate(DashboardView.TRASH) }
        )
        NavigationItem(Icons.AutoMirrored.Filled.Logout, "Logout", false, onLogout)
    }
}


@Composable
fun NavigationItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    paddingValues: PaddingValues,
    currentView: DashboardView,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    documentListState: com.flowboard.presentation.viewmodel.DocumentListState,
    onDocumentClick: (String) -> Unit,
    onDeleteDocument: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header Section
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = when (currentView) {
                    DashboardView.HOME -> "Home"
                    DashboardView.INBOX -> "Inbox"
                    DashboardView.SEARCH -> "Search"
                    DashboardView.TRASH -> "Trash"
                    DashboardView.TASKS -> "Tasks"
                    DashboardView.MY_DOCUMENTS -> "My Documents"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            if (currentView == DashboardView.SEARCH) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search documents...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            } else if (currentView == DashboardView.HOME) {
                Text(
                    "Good morning, User",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Filter Logic
            val allDocs = (documentListState.ownedDocuments + documentListState.sharedWithMe)
                .sortedByDescending { it.updatedAt }
            
            val filteredDocs = when (currentView) {
                DashboardView.HOME -> allDocs
                DashboardView.INBOX -> documentListState.sharedWithMe
                DashboardView.MY_DOCUMENTS -> documentListState.ownedDocuments
                DashboardView.SEARCH -> if (searchQuery.isBlank()) emptyList() else allDocs.filter { 
                    it.title.contains(searchQuery, ignoreCase = true) 
                }
                DashboardView.TRASH, DashboardView.TASKS -> emptyList() // Tasks handled by parent
            }

            // Recently Visited Header (only for Home)
            if (currentView == DashboardView.HOME && filteredDocs.isNotEmpty()) {
                item {
                    Text(
                        "RECENTLY VISITED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )
                }
            }
            
            if (documentListState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filteredDocs.isEmpty()) {
                item {
                     EmptyState(
                         message = when(currentView) {
                             DashboardView.SEARCH -> "Type to search..."
                             DashboardView.TRASH -> "Trash is empty"
                             DashboardView.INBOX -> "No shared documents"
                             DashboardView.MY_DOCUMENTS -> "No documents created yet"
                             else -> "No documents yet"
                         }
                     )
                }
            } else {
                items(filteredDocs) { doc ->
                    SimpleDocumentItem(
                        title = doc.title,
                        updatedAt = doc.updatedAt, 
                        isShared = doc.ownerId != "me", // Simplified logic
                        onClick = { onDocumentClick(doc.id) },
                        onDelete = { onDeleteDocument(doc.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleDocumentItem(
    title: String,
    updatedAt: String,
    isShared: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Last edited $updatedAt", // Simplified
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isShared) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = "Shared",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // Action Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { 
                            Icon(
                                Icons.Outlined.Delete, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            ) 
                        },
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
fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.NoteAdd,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}