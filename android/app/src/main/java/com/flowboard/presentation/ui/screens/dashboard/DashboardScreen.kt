package com.flowboard.presentation.ui.screens.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowboard.R
import com.flowboard.presentation.ui.theme.*
import com.flowboard.presentation.viewmodel.DocumentViewModel
import com.flowboard.presentation.viewmodel.LoginViewModel
import com.flowboard.presentation.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

enum class DashboardView {
    HOME, INBOX, SEARCH, TASKS, MY_DOCUMENTS, TRASH
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
    onCalendarClick: () -> Unit = {},
    onWorkspaceClick: () -> Unit = {},
    onViewAllDocuments: () -> Unit = {},
    onEditorDemoClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    documentViewModel: DocumentViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val documentListState by documentViewModel.documentListState.collectAsStateWithLifecycle()
    val currentUser by profileViewModel.user.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var currentView by remember { mutableStateOf(DashboardView.HOME) }
    var searchQuery by remember { mutableStateOf("") }

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
                    onTasksNavigate = { scope.launch { drawerState.close() }; onTasksClick() },
                    onChatNavigate = { scope.launch { drawerState.close() }; onChatClick() },
                    onCalendarNavigate = { scope.launch { drawerState.close() }; onCalendarClick() },
                    onWorkspaceNavigate = { scope.launch { drawerState.close() }; onWorkspaceClick() },
                    onCreateDocument = { scope.launch { drawerState.close() }; onCreateDocument() },
                    onProfileClick = onProfileClick,
                    onSettingsClick = onSettingsClick,
                    onLogout = { scope.launch { drawerState.close() }; onLogout() },
                    documents = documentListState.ownedDocuments,
                    sharedDocuments = documentListState.sharedWithMe,
                    onDocumentClick = { docId -> scope.launch { drawerState.close() }; onDocumentClick(docId) },
                    onCreateSharedDocument = { scope.launch { drawerState.close() }; onCreateDocument() },
                    onCreateSubPage = { parentId, title ->
                        documentViewModel.createSubPageViaApi(parentId, title) { docId ->
                            scope.launch { drawerState.close() }
                            onDocumentClick(docId)
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        // LOGO UN POCO MÁS GRANDE (DE 40dp A 48dp) PARA QUE SE VEA MEJOR
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "FlowBoard",
                            modifier = Modifier.height(48.dp)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
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
                currentUserName = currentUser?.fullName?.takeIf { it.isNotBlank() } ?: currentUser?.username ?: "there",
                onDocumentClick = onDocumentClick,
                onDeleteDocument = { documentViewModel.deleteDocumentViaApi(it) },
                onCreateDocument = onCreateDocument
            )
        }
    }
}

@Composable
fun DashboardSidebar(
    currentView: DashboardView,
    onNavigate: (DashboardView) -> Unit,
    onTasksNavigate: () -> Unit,
    onChatNavigate: () -> Unit,
    onCalendarNavigate: () -> Unit,
    onWorkspaceNavigate: () -> Unit,
    onCreateDocument: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit,
    documents: List<com.flowboard.data.local.entities.DocumentEntity>,
    sharedDocuments: List<com.flowboard.data.local.entities.DocumentEntity>,
    onDocumentClick: (String) -> Unit,
    onCreateSharedDocument: () -> Unit,
    onCreateSubPage: ((String, String) -> Unit)?
) {
    var createSubPageParentId by remember { mutableStateOf<String?>(null) }
    var createSubPageTitle by remember { mutableStateOf("") }

    if (createSubPageParentId != null) {
        AlertDialog(
            onDismissRequest = { createSubPageParentId = null; createSubPageTitle = "" },
            title = { Text("New sub-page") },
            text = {
                OutlinedTextField(
                    value = createSubPageTitle,
                    onValueChange = { createSubPageTitle = it },
                    label = { Text("Page title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onCreateSubPage?.invoke(createSubPageParentId!!, createSubPageTitle.trim().ifBlank { "Untitled" })
                    createSubPageParentId = null
                    createSubPageTitle = ""
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { createSubPageParentId = null; createSubPageTitle = "" }) { Text("Cancel") }
            }
        )
    }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).clickable { onProfileClick() }
            ) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("W", fontWeight = FontWeight.Bold) }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("My Workspace", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            NavigationItem(Icons.Outlined.Home, "Home", currentView == DashboardView.HOME) { onNavigate(DashboardView.HOME) }
            NavigationItem(Icons.Outlined.Inbox, "Inbox", currentView == DashboardView.INBOX) { onNavigate(DashboardView.INBOX) }
            NavigationItem(Icons.Outlined.CheckCircle, "Tasks", false, onTasksNavigate)
            NavigationItem(Icons.Outlined.Chat, "Chat", false, onChatNavigate)
            NavigationItem(Icons.Outlined.CalendarMonth, "Calendar", false, onCalendarNavigate)
            NavigationItem(Icons.Outlined.Group, "Workspaces", false, onWorkspaceNavigate)
            NavigationItem(Icons.Outlined.Search, "Search", currentView == DashboardView.SEARCH) { onNavigate(DashboardView.SEARCH) }
            Spacer(modifier = Modifier.height(12.dp))
            NavigationItem(Icons.Default.Add, "New Page", false, onCreateDocument)
            Spacer(modifier = Modifier.height(16.dp))
            
            val workspaceDocs = (sharedDocuments + documents.filter { it.isPublic }).distinctBy { it.id }
            Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("WORKSPACE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                IconButton(onClick = onCreateSharedDocument, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val workspaceRoots = workspaceDocs.filter { it.parentId == null }
            if (workspaceRoots.isEmpty()) {
                Text("No shared pages yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp))
            } else {
                workspaceRoots.take(6).forEach { doc ->
                    PageTreeItem(doc = doc, children = workspaceDocs.filter { it.parentId == doc.id }, onDocumentClick = onDocumentClick)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("PRIVATE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            }
            NavigationItem(Icons.Outlined.Folder, "My Documents", currentView == DashboardView.MY_DOCUMENTS) { onNavigate(DashboardView.MY_DOCUMENTS) }
            val rootDocs = documents.filter { !it.isPublic && it.parentId == null }
            if (rootDocs.isEmpty()) {
                Text("No pages yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.padding(start = 12.dp, top = 4.dp))
            } else {
                rootDocs.take(8).forEach { doc ->
                    PageTreeItem(doc = doc, children = documents.filter { it.parentId == doc.id }, onDocumentClick = onDocumentClick, onCreateSubPage = { createSubPageParentId = it })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        HorizontalDivider()
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            NavigationItem(Icons.Outlined.Person, "Profile", false, onProfileClick)
            NavigationItem(Icons.Outlined.Settings, "Settings", false, onSettingsClick)
            NavigationItem(Icons.Outlined.Delete, "Trash", currentView == DashboardView.TRASH) { onNavigate(DashboardView.TRASH) }
            NavigationItem(Icons.AutoMirrored.Filled.Logout, "Logout", false, onLogout)
        }
    }
}

@Composable
fun NavigationItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

@Composable
fun DashboardContent(paddingValues: PaddingValues, currentView: DashboardView, searchQuery: String, onSearchQueryChange: (String) -> Unit, documentListState: com.flowboard.presentation.viewmodel.DocumentListState, currentUserName: String, onDocumentClick: (String) -> Unit, onDeleteDocument: (String) -> Unit, onCreateDocument: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(paddingValues).background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(text = currentView.name.replace("_", " ").lowercase().capitalize(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (currentView == DashboardView.SEARCH) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = searchQuery, onValueChange = onSearchQueryChange, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search documents...") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, shape = RoundedCornerShape(12.dp))
            } else if (currentView == DashboardView.HOME) {
                Text("Good morning, $currentUserName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val allDocs = (documentListState.ownedDocuments + documentListState.sharedWithMe).sortedByDescending { it.updatedAt }
            val filteredDocs = when (currentView) {
                DashboardView.HOME -> allDocs
                DashboardView.INBOX -> documentListState.sharedWithMe
                DashboardView.MY_DOCUMENTS -> documentListState.ownedDocuments
                DashboardView.SEARCH -> if (searchQuery.isBlank()) emptyList() else allDocs.filter { it.title.contains(searchQuery, true) }
                else -> emptyList()
            }
            if (currentView == DashboardView.HOME && filteredDocs.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text("JUMP BACK IN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            filteredDocs.take(5).forEach { RecentPageCard(it.title, it.updatedAt) { onDocumentClick(it.id) } }
                        }
                    }
                }
            }
            if (documentListState.isLoading) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (filteredDocs.isEmpty()) {
                item { EmptyState("No documents found") }
            } else {
                items(filteredDocs) { doc ->
                    SimpleDocumentItem(doc.title, doc.updatedAt, doc.ownerId != "me", { onDocumentClick(doc.id) }, { onDeleteDocument(doc.id) })
                }
            }
        }
    }
}

@Composable
fun RecentPageCard(title: String, updatedAt: String, onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.width(150.dp).clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("📄", fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title.ifBlank { "Untitled" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(updatedAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1)
        }
    }
}

@Composable
fun SimpleDocumentItem(title: String, updatedAt: String, isShared: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Description, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text("Last edited $updatedAt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isShared) { Icon(Icons.Default.Group, null, modifier = Modifier.size(16.dp)) }
            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() })
            }
        }
    }
}

@Composable
fun PageTreeItem(doc: com.flowboard.data.local.entities.DocumentEntity, children: List<com.flowboard.data.local.entities.DocumentEntity>, onDocumentClick: (String) -> Unit, onCreateSubPage: ((String) -> Unit)? = null, depth: Int = 0) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = (12 + depth * 16).dp).clickable { onDocumentClick(doc.id) }
        ) {
            if (children.isNotEmpty()) {
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(20.dp)) {
                    Icon(if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp))
                }
            } else { Spacer(Modifier.width(20.dp)) }
            Icon(Icons.Outlined.Description, null, modifier = Modifier.size(16.dp))
            Text(doc.title, modifier = Modifier.weight(1f).padding(8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (onCreateSubPage != null) {
                IconButton(onClick = { onCreateSubPage(doc.id) }, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp)) }
            }
        }
        if (expanded) { children.forEach { PageTreeItem(it, emptyList(), onDocumentClick, onCreateSubPage, depth + 1) } }
    }
}

@Composable
fun EmptyState(message: String) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.NoteAdd, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(0.5f))
        Text(message, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
