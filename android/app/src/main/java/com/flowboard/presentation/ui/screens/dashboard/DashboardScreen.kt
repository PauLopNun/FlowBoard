package com.flowboard.presentation.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowboard.R
import com.flowboard.data.local.entities.DocumentEntity
import com.flowboard.data.local.entities.WorkspaceEntity
import com.flowboard.presentation.ui.theme.*
import com.flowboard.presentation.viewmodel.DocumentViewModel
import com.flowboard.presentation.viewmodel.LoginViewModel
import com.flowboard.presentation.viewmodel.ChatViewModel
import com.flowboard.presentation.viewmodel.NotificationViewModel
import com.flowboard.presentation.ui.util.formatRelativeDate
import com.flowboard.presentation.viewmodel.ProfileViewModel
import com.flowboard.presentation.viewmodel.WorkspaceViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DashboardView {
    HOME, SEARCH, TASKS, MY_DOCUMENTS, SHARED_WITH_ME, TRASH
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
    onWorkspaceSelected: (String) -> Unit = {},
    onCreateWorkspaceDocument: (String) -> Unit = {},
    onViewAllDocuments: () -> Unit = {},
    onEditorDemoClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    documentViewModel: DocumentViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    workspaceViewModel: WorkspaceViewModel = hiltViewModel()
) {
    val documentListState by documentViewModel.documentListState.collectAsStateWithLifecycle()
    val trashedDocuments by documentViewModel.trashedDocuments.collectAsStateWithLifecycle()
    val currentUser by profileViewModel.user.collectAsStateWithLifecycle()
    val workspaceState by workspaceViewModel.uiState.collectAsStateWithLifecycle()
    val unreadNotifications by notificationViewModel.unreadCount.collectAsStateWithLifecycle()
    val unreadChats by chatViewModel.totalUnreadCount.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var currentView by remember { mutableStateOf(DashboardView.HOME) }
    var searchQuery by remember { mutableStateOf("") }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var moveToWorkspaceDocument by remember { mutableStateOf<DocumentEntity?>(null) }

    LaunchedEffect(Unit) {
        documentViewModel.fetchAllDocuments()
        workspaceViewModel.fetchWorkspaces()
        while (true) {
            notificationViewModel.refresh()
            chatViewModel.refreshRooms()
            delay(15_000)
        }
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
                    onNotificationsNavigate = { scope.launch { drawerState.close() }; onNotificationsClick() },
                    onChatNavigate = { scope.launch { drawerState.close() }; onChatClick() },
                    onCalendarNavigate = { scope.launch { drawerState.close() }; onCalendarClick() },
                    onWorkspaceNavigate = { scope.launch { drawerState.close() }; onWorkspaceClick() },
                    onWorkspaceSelected = { workspaceId ->
                        scope.launch { drawerState.close() }
                        onWorkspaceSelected(workspaceId)
                    },
                    onCreateWorkspaceDocument = { workspaceId ->
                        scope.launch { drawerState.close() }
                        onCreateWorkspaceDocument(workspaceId)
                    },
                    onCreateDocument = { scope.launch { drawerState.close() }; onCreateDocument() },
                    currentUser = currentUser,
                    onProfileClick = onProfileClick,
                    onSettingsClick = onSettingsClick,
                    onLogout = { scope.launch { drawerState.close() }; onLogout() },
                    documents = documentListState.ownedDocuments,
                    sharedDocuments = documentListState.sharedWithMe,
                    workspaces = workspaceState.workspaces,
                    unreadNotifications = unreadNotifications,
                    unreadChats = unreadChats,
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
                            BadgedBox(
                                badge = {
                                    if (unreadNotifications > 0) {
                                        Badge { Text(formatBadgeCount(unreadNotifications)) }
                                    }
                                }
                            ) {
                                Icon(Icons.Outlined.Notifications, "Notifications")
                            }
                        }
                        IconButton(onClick = onChatClick) {
                            BadgedBox(
                                badge = {
                                    if (unreadChats > 0) {
                                        Badge { Text(formatBadgeCount(unreadChats)) }
                                    }
                                }
                            ) {
                                Icon(Icons.Outlined.ChatBubbleOutline, "Chat")
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                if (currentView == DashboardView.TRASH) {
                    if (trashedDocuments.isNotEmpty()) {
                        ExtendedFloatingActionButton(
                            onClick = { showEmptyTrashDialog = true },
                            icon = { Icon(Icons.Outlined.DeleteForever, "Empty trash") },
                            text = { Text("Empty trash") },
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                } else {
                    FloatingActionButton(
                        onClick = onCreateDocument,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, "New Page")
                    }
                }
            }
        ) { paddingValues ->
             DashboardContent(
                paddingValues = paddingValues,
                currentView = currentView,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                documentListState = documentListState,
                trashedDocuments = trashedDocuments,
                currentUserName = currentUser?.fullName?.takeIf { it.isNotBlank() } ?: currentUser?.username ?: "there",
                onDocumentClick = onDocumentClick,
                onDeleteDocument = { documentViewModel.deleteDocumentViaApi(it) },
                onRestoreDocument = { documentViewModel.restoreDocument(it) },
                onPermanentDeleteDocument = { documentViewModel.permanentlyDeleteDocument(it) },
                onEmptyTrash = { showEmptyTrashDialog = true },
                onCreateDocument = onCreateDocument,
                workspaces = workspaceState.workspaces,
                onMoveDocumentToWorkspace = { moveToWorkspaceDocument = it },
                onMoveDocumentToPrivate = { documentViewModel.moveDocumentToPrivate(it) }
            )
        }
    }

    moveToWorkspaceDocument?.let { doc ->
        MoveToWorkspaceDialog(
            document = doc,
            workspaces = workspaceState.workspaces,
            onDismiss = { moveToWorkspaceDocument = null },
            onMove = { workspaceId ->
                documentViewModel.moveDocumentToWorkspace(doc.id, workspaceId)
                moveToWorkspaceDocument = null
            }
        )
    }

    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Empty trash?") },
            text = { Text("This will permanently delete every page currently in trash.") },
            confirmButton = {
                Button(
                    onClick = {
                        documentViewModel.permanentlyDeleteDocuments(trashedDocuments.map { it.id })
                        showEmptyTrashDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete all")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MoveToWorkspaceDialog(
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
                Text("Create or join a workspace first.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(document.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    workspaces.forEach { workspace ->
                        ListItem(
                            headlineContent = { Text(workspace.name) },
                            supportingContent = { Text("${workspace.memberCount} member${if (workspace.memberCount == 1) "" else "s"}") },
                            leadingContent = { Icon(Icons.Default.Group, null) },
                            modifier = Modifier.clickable { onMove(workspace.id) }
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

@Composable
fun DashboardSidebar(
    currentView: DashboardView,
    onNavigate: (DashboardView) -> Unit,
    onTasksNavigate: () -> Unit,
    onNotificationsNavigate: () -> Unit,
    onChatNavigate: () -> Unit,
    onCalendarNavigate: () -> Unit,
    onWorkspaceNavigate: () -> Unit,
    onWorkspaceSelected: (String) -> Unit,
    onCreateWorkspaceDocument: (String) -> Unit,
    onCreateDocument: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit,
    documents: List<DocumentEntity>,
    sharedDocuments: List<DocumentEntity>,
    workspaces: List<WorkspaceEntity>,
    unreadNotifications: Int,
    unreadChats: Int,
    onDocumentClick: (String) -> Unit,
    onCreateSharedDocument: () -> Unit,
    onCreateSubPage: ((String, String) -> Unit)?,
    currentUser: com.flowboard.domain.model.User? = null
) {
    var createSubPageParentId by remember { mutableStateOf<String?>(null) }
    var createSubPageTitle by remember { mutableStateOf("") }
    var workspacesExpanded by remember { mutableStateOf(true) }
    var privateExpanded by remember { mutableStateOf(false) }
    var sharedExpanded by remember { mutableStateOf(false) }

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
    Column(modifier = Modifier.fillMaxSize()) {
        // User header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onProfileClick)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            ) {
                if (currentUser?.profileImageUrl != null) {
                    AsyncImage(
                        model = currentUser.profileImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = (currentUser?.fullName?.takeIf { it.isNotBlank() }
                                ?: currentUser?.username ?: "?")
                                .first().uppercaseChar().toString(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentUser?.fullName?.takeIf { it.isNotBlank() }
                        ?: currentUser?.username ?: "Mi cuenta",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (currentUser?.email != null) {
                    Text(
                        text = currentUser.email,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider()
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            NavigationItem(Icons.Outlined.Home, "Home", currentView == DashboardView.HOME, onClick = { onNavigate(DashboardView.HOME) })
            NavigationItem(Icons.Outlined.Notifications, "Notifications", false, onNotificationsNavigate, unreadNotifications)
            NavigationItem(Icons.Outlined.CheckCircle, "Tasks", false, onTasksNavigate)
            NavigationItem(Icons.Outlined.Chat, "Chat", false, onChatNavigate, unreadChats)
            NavigationItem(Icons.Outlined.CalendarMonth, "Calendar", false, onCalendarNavigate)
            NavigationItem(Icons.Outlined.Group, "Workspaces", false, onWorkspaceNavigate)
            NavigationItem(Icons.Outlined.Search, "Search", currentView == DashboardView.SEARCH, onClick = { onNavigate(DashboardView.SEARCH) })
            Spacer(modifier = Modifier.height(16.dp))

            val workspaceDocs = (sharedDocuments + documents)
                .filter { it.visibility == "workspace" }
                .distinctBy { it.id }
            val workspaceDocsById = workspaceDocs.groupBy { it.workspaceId }

            // WORKSPACES section — collapsible
            SidebarSectionHeader(
                label = "WORKSPACES",
                expanded = workspacesExpanded,
                onToggle = { workspacesExpanded = !workspacesExpanded },
                onLabelClick = onWorkspaceNavigate,
                isSelected = false,
                onAdd = onWorkspaceNavigate
            )
            if (workspacesExpanded) {
                if (workspaces.isEmpty()) {
                    Text("No workspaces yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(start = 40.dp, top = 4.dp, bottom = 8.dp))
                } else {
                    workspaces.take(8).forEach { workspace ->
                        WorkspaceSidebarItem(
                            workspace = workspace,
                            onClick = { onWorkspaceSelected(workspace.id) },
                            onCreateDocument = { onCreateWorkspaceDocument(workspace.id) }
                        )
                        val docsForWorkspace = workspaceDocsById[workspace.id].orEmpty()
                        docsForWorkspace
                            .filter { doc -> doc.parentId == null || docsForWorkspace.none { it.id == doc.parentId } }
                            .take(4)
                            .forEach { doc ->
                                PageTreeItem(
                                    doc = doc,
                                    allDocuments = docsForWorkspace,
                                    onDocumentClick = onDocumentClick,
                                    depth = 1
                                )
                            }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // PRIVATE section — collapsible
            val personalDocs = documents
                .filter { it.visibility != "workspace" }
                .distinctBy { it.id }
            SidebarSectionHeader(
                label = "PRIVATE",
                expanded = privateExpanded,
                onToggle = { privateExpanded = !privateExpanded },
                onLabelClick = { onNavigate(DashboardView.MY_DOCUMENTS) },
                isSelected = currentView == DashboardView.MY_DOCUMENTS,
                onAdd = onCreateDocument
            )
            if (privateExpanded) {
                val rootDocs = personalDocs.filter { it.parentId == null }
                if (rootDocs.isEmpty()) {
                    Text("No pages yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.padding(start = 40.dp, top = 4.dp))
                } else {
                    rootDocs.forEach { doc ->
                        PageTreeItem(
                            doc = doc,
                            allDocuments = personalDocs,
                            onDocumentClick = onDocumentClick,
                            onCreateSubPage = { createSubPageParentId = it },
                            badgeTextForDocument = { if (it.visibility == "shared") "Shared" else null }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // SHARED WITH ME section — collapsible
            val sharedPages = sharedDocuments
                .filter { it.visibility != "workspace" }
                .distinctBy { it.id }
            SidebarSectionHeader(
                label = "SHARED WITH ME",
                expanded = sharedExpanded,
                onToggle = { sharedExpanded = !sharedExpanded },
                onLabelClick = { onNavigate(DashboardView.SHARED_WITH_ME) },
                isSelected = currentView == DashboardView.SHARED_WITH_ME,
                onAdd = null
            )
            if (sharedExpanded) {
                val rootSharedPages = sharedPages.filter { it.parentId == null }
                if (rootSharedPages.isEmpty()) {
                    Text("No shared pages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.padding(start = 40.dp, top = 4.dp))
                } else {
                    rootSharedPages.forEach { doc ->
                        PageTreeItem(
                            doc = doc,
                            allDocuments = sharedPages,
                            onDocumentClick = onDocumentClick,
                            badgeTextForDocument = { sharedDoc -> sharedDoc.ownerName?.takeIf { it.isNotBlank() } ?: "Shared" }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(modifier = Modifier.height(4.dp))
            NavigationItem(Icons.Outlined.Delete, "Trash", currentView == DashboardView.TRASH, onClick = { onNavigate(DashboardView.TRASH) })
            NavigationItem(Icons.AutoMirrored.Filled.Logout, "Logout", false, onLogout)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SidebarSectionHeader(
    label: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onLabelClick: () -> Unit,
    isSelected: Boolean,
    onAdd: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 44.dp)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onLabelClick)
                .padding(vertical = 14.dp)
        )
        if (onAdd != null) {
            IconButton(onClick = onAdd, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun NavigationItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    badgeCount: Int = 0
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
            if (badgeCount > 0) {
                Badge {
                    Text(formatBadgeCount(badgeCount))
                }
            }
        }
    }
}

@Composable
private fun WorkspaceSidebarItem(
    workspace: WorkspaceEntity,
    onClick: () -> Unit,
    onCreateDocument: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!workspace.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = workspace.imageUrl,
                            contentDescription = workspace.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            workspace.name.take(1).uppercase().ifBlank { "W" },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(workspace.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${workspace.memberCount} member${if (workspace.memberCount == 1) "" else "s"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onCreateDocument, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatBadgeCount(count: Int): String = if (count > 99) "99+" else count.toString()

@Composable
fun DashboardContent(
    paddingValues: PaddingValues,
    currentView: DashboardView,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    documentListState: com.flowboard.presentation.viewmodel.DocumentListState,
    trashedDocuments: List<DocumentEntity> = emptyList(),
    currentUserName: String,
    onDocumentClick: (String) -> Unit,
    onDeleteDocument: (String) -> Unit,
    onRestoreDocument: (String) -> Unit = {},
    onPermanentDeleteDocument: (String) -> Unit = {},
    onEmptyTrash: () -> Unit = {},
    onCreateDocument: () -> Unit,
    workspaces: List<WorkspaceEntity> = emptyList(),
    onMoveDocumentToWorkspace: (DocumentEntity) -> Unit = {},
    onMoveDocumentToPrivate: (String) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize().padding(paddingValues).background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            val viewTitle = when (currentView) {
                DashboardView.HOME -> "Home"
                DashboardView.SEARCH -> "Search"
                DashboardView.TASKS -> "Tasks"
                DashboardView.MY_DOCUMENTS -> "My Pages"
                DashboardView.SHARED_WITH_ME -> "Shared with me"
                DashboardView.TRASH -> "Trash"
            }
            Text(text = viewTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (currentView == DashboardView.SEARCH) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = searchQuery, onValueChange = onSearchQueryChange, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search documents...") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, shape = RoundedCornerShape(12.dp))
            } else if (currentView == DashboardView.HOME) {
                val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                    in 5..11 -> "Good morning"
                    in 12..17 -> "Good afternoon"
                    else -> "Good evening"
                }
                Text("$greeting, $currentUserName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (currentView == DashboardView.TRASH) {
                Text("Pages moved to trash can be restored or permanently deleted", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (currentView == DashboardView.SHARED_WITH_ME) {
                Text("Pages other people invited you to edit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (currentView == DashboardView.TRASH) {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (trashedDocuments.isEmpty()) {
                    item { EmptyState("Trash is empty") }
                } else {
                    items(trashedDocuments) { doc ->
                        TrashDocumentItem(
                            title = doc.title,
                            onRestore = { onRestoreDocument(doc.id) },
                            onPermanentDelete = { onPermanentDeleteDocument(doc.id) }
                        )
                    }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val personalDocs = documentListState.ownedDocuments
                    .filter { it.visibility != "workspace" }
                    .distinctBy { it.id }
                val sharedDocs = documentListState.sharedWithMe
                    .filter { it.visibility != "workspace" }
                    .distinctBy { it.id }
                val allDocs = (personalDocs + sharedDocs)
                    .distinctBy { it.id }
                    .sortedByDescending { it.updatedAt }
                val filteredDocs = when (currentView) {
                    DashboardView.HOME -> allDocs
                    DashboardView.MY_DOCUMENTS -> personalDocs
                    DashboardView.SHARED_WITH_ME -> sharedDocs
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
                    val emptyMessage = when (currentView) {
                        DashboardView.MY_DOCUMENTS -> "No personal pages yet"
                        DashboardView.SHARED_WITH_ME -> "No shared pages yet"
                        DashboardView.SEARCH -> if (searchQuery.isBlank()) "Search your pages" else "No documents found"
                        else -> "No documents found"
                    }
                    item { EmptyState(emptyMessage) }
                } else if (currentView == DashboardView.HOME || currentView == DashboardView.MY_DOCUMENTS || currentView == DashboardView.SHARED_WITH_ME) {
                    val documentTree = buildDocumentTree(filteredDocs)
                    val ownedDocumentIds = personalDocs.map { it.id }.toSet()
                    items(documentTree, key = { it.document.id }) { node ->
                        DocumentTreeListItem(
                            node = node,
                            onDocumentClick = onDocumentClick,
                            onDeleteDocument = onDeleteDocument,
                            canMoveDocument = { it.id in ownedDocumentIds },
                            canDeleteDocument = { it.id in ownedDocumentIds },
                            badgeTextForDocument = { document ->
                                when {
                                    document.id !in ownedDocumentIds -> document.ownerName?.takeIf { it.isNotBlank() }?.let { "Shared by $it" } ?: "Shared with me"
                                    document.visibility == "shared" -> "Shared"
                                    else -> null
                                }
                            },
                            hasWorkspaces = workspaces.isNotEmpty(),
                            onMoveDocumentToWorkspace = onMoveDocumentToWorkspace,
                            onMoveDocumentToPrivate = onMoveDocumentToPrivate
                        )
                    }
                } else {
                    items(filteredDocs) { doc ->
                        val ownedDocumentIds = personalDocs.map { it.id }.toSet()
                        val isSharedWithMe = doc.id !in ownedDocumentIds
                        SimpleDocumentItem(
                            title = doc.title,
                            updatedAt = doc.updatedAt,
                            isShared = isSharedWithMe,
                            onClick = { onDocumentClick(doc.id) },
                            onDelete = { onDeleteDocument(doc.id) },
                            badgeText = if (isSharedWithMe) doc.ownerName?.takeIf { it.isNotBlank() }?.let { "Shared by $it" } ?: "Shared with me" else if (doc.visibility == "shared") "Shared" else null,
                            showMenu = !isSharedWithMe
                        )
                    }
                }
            }
        }
    }
}

private data class DocumentTreeNode(
    val document: DocumentEntity,
    val children: List<DocumentTreeNode>,
    val depth: Int
)

private fun buildDocumentTree(documents: List<DocumentEntity>): List<DocumentTreeNode> {
    val uniqueDocuments = documents.distinctBy { it.id }
    val byParent = uniqueDocuments.groupBy { it.parentId }
    val visited = mutableSetOf<String>()

    fun buildNode(document: DocumentEntity, depth: Int): DocumentTreeNode {
        visited += document.id
        val children = byParent[document.id]
            .orEmpty()
            .asSequence()
            .filter { it.id !in visited }
            .sortedByDescending { it.updatedAt }
            .map { buildNode(it, depth + 1) }
            .toList()
        return DocumentTreeNode(document, children, depth)
    }

    val roots = byParent[null]
        .orEmpty()
        .sortedByDescending { it.updatedAt }
        .map { buildNode(it, 0) }

    val unattached = uniqueDocuments
        .filter { it.id !in visited }
        .sortedByDescending { it.updatedAt }
        .map { buildNode(it, 0) }

    return roots + unattached
}

@Composable
fun RecentPageCard(title: String, updatedAt: String, onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.width(150.dp).height(100.dp).clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("📄", fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title.ifBlank { "Untitled" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatRelativeDate(updatedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1)
        }
    }
}

@Composable
private fun DocumentTreeListItem(
    node: DocumentTreeNode,
    onDocumentClick: (String) -> Unit,
    onDeleteDocument: (String) -> Unit,
    canMoveDocument: (DocumentEntity) -> Boolean = { false },
    canDeleteDocument: (DocumentEntity) -> Boolean = { true },
    badgeTextForDocument: (DocumentEntity) -> String? = { null },
    hasWorkspaces: Boolean = false,
    onMoveDocumentToWorkspace: (DocumentEntity) -> Unit = {},
    onMoveDocumentToPrivate: (String) -> Unit = {}
) {
    var expanded by remember(node.document.id) { mutableStateOf(true) }
    val document = node.document
    val hasChildren = node.children.isNotEmpty()
    val badgeText = badgeTextForDocument(document)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth().clickable { onDocumentClick(document.id) }
        ) {
            Row(
                modifier = Modifier.padding(start = (12 + node.depth * 22).dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasChildren) {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(28.dp))
                }
                Icon(Icons.Outlined.Description, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(document.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val metadata = listOfNotNull(formatRelativeDate(document.updatedAt).ifBlank { null }?.let { "Edited $it" }, badgeText).joinToString(" | ")
                    Text(metadata, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (hasChildren) {
                    Text("${node.children.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (canDeleteDocument(document)) {
                    DocumentItemMenu(
                        document = document,
                        onDelete = { onDeleteDocument(document.id) },
                        canMove = canMoveDocument(document),
                        hasWorkspaces = hasWorkspaces,
                        onMoveToWorkspace = { onMoveDocumentToWorkspace(document) },
                        onMoveToPrivate = { onMoveDocumentToPrivate(document.id) }
                    )
                } else {
                    Icon(Icons.Outlined.Group, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (expanded) {
            node.children.forEach { child ->
                DocumentTreeListItem(
                    node = child,
                    onDocumentClick = onDocumentClick,
                    onDeleteDocument = onDeleteDocument,
                    canMoveDocument = canMoveDocument,
                    canDeleteDocument = canDeleteDocument,
                    badgeTextForDocument = badgeTextForDocument,
                    hasWorkspaces = hasWorkspaces,
                    onMoveDocumentToWorkspace = onMoveDocumentToWorkspace,
                    onMoveDocumentToPrivate = onMoveDocumentToPrivate
                )
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
    onDelete: () -> Unit,
    badgeText: String? = null,
    showMenu: Boolean = true
) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Description, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                val metadata = listOfNotNull(formatRelativeDate(updatedAt).ifBlank { null }?.let { "Edited $it" }, badgeText).joinToString(" | ")
                Text(metadata, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isShared) { Icon(Icons.Default.Group, null, modifier = Modifier.size(16.dp)) }
            if (showMenu) {
                DocumentItemMenu(onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun DocumentItemMenu(
    document: DocumentEntity? = null,
    onDelete: () -> Unit,
    canMove: Boolean = false,
    hasWorkspaces: Boolean = false,
    onMoveToWorkspace: () -> Unit = {},
    onMoveToPrivate: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
        if (canMove && document != null && document.visibility != "workspace" && hasWorkspaces) {
            DropdownMenuItem(
                text = { Text("Move to workspace") },
                leadingIcon = { Icon(Icons.Default.DriveFileMove, null) },
                onClick = { showMenu = false; onMoveToWorkspace() }
            )
        }
        if (canMove && document?.visibility == "workspace") {
            DropdownMenuItem(
                text = { Text("Move to private") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                onClick = { showMenu = false; onMoveToPrivate() }
            )
        }
        DropdownMenuItem(
            text = { Text("Move to Trash") },
            leadingIcon = { Icon(Icons.Outlined.Delete, null) },
            onClick = { showMenu = false; onDelete() }
        )
    }
}

@Composable
fun PageTreeItem(
    doc: DocumentEntity,
    allDocuments: List<DocumentEntity>,
    onDocumentClick: (String) -> Unit,
    onCreateSubPage: ((String) -> Unit)? = null,
    badgeTextForDocument: (DocumentEntity) -> String? = { null },
    depth: Int = 0,
    visited: Set<String> = emptySet()
) {
    var expanded by remember(doc.id) { mutableStateOf(false) }
    val children = remember(allDocuments, doc.id, visited) {
        allDocuments
            .filter { it.parentId == doc.id && it.id !in visited }
            .sortedByDescending { it.updatedAt }
    }
    val badgeText = badgeTextForDocument(doc)
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
            if (!badgeText.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 82.dp).padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (onCreateSubPage != null) {
                IconButton(onClick = { onCreateSubPage(doc.id) }, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp)) }
            }
        }
        if (expanded) {
            children.forEach {
                PageTreeItem(
                    doc = it,
                    allDocuments = allDocuments,
                    onDocumentClick = onDocumentClick,
                    onCreateSubPage = onCreateSubPage,
                    badgeTextForDocument = badgeTextForDocument,
                    depth = depth + 1,
                    visited = visited + doc.id
                )
            }
        }
    }
}

@Composable
fun TrashDocumentItem(title: String, onRestore: () -> Unit, onPermanentDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Restore") },
                    leadingIcon = { Icon(Icons.Outlined.Restore, null) },
                    onClick = { showMenu = false; onRestore() }
                )
                DropdownMenuItem(
                    text = { Text("Delete permanently", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; onPermanentDelete() }
                )
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.NoteAdd, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(0.5f))
        Text(message, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
