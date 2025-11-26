package com.flowboard.presentation.ui.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowboard.presentation.ui.components.TaskCard
import com.flowboard.presentation.ui.components.ActiveUsersList
import com.flowboard.presentation.ui.components.ConnectionStatusBanner
import com.flowboard.presentation.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onTaskClick: (String) -> Unit,
    onCreateTaskClick: () -> Unit,
    onDocumentsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: TaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val pendingTasks by viewModel.pendingTasks.collectAsStateWithLifecycle()
    val completedTasks by viewModel.completedTasks.collectAsStateWithLifecycle()
    val overdueTasks by viewModel.overdueTasks.collectAsStateWithLifecycle()

    // WebSocket state
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val activeUsers by viewModel.activeUsers.collectAsStateWithLifecycle()

    // Auth data from ViewModel
    val token by viewModel.authToken.collectAsStateWithLifecycle()
    val userId by viewModel.userId.collectAsStateWithLifecycle()
    val boardId by viewModel.boardId.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf(TaskFilter.ALL) }
    var showMenu by remember { mutableStateOf(false) }

    // Connect to WebSocket when screen mounts (only if auth data is available)
    LaunchedEffect(boardId, token, userId) {
        val currentBoardId = boardId
        val currentToken = token
        val currentUserId = userId
        if (currentBoardId != null && currentToken != null && currentUserId != null) {
            viewModel.connectToBoard(currentBoardId, currentToken, currentUserId)
        }
    }

    // Disconnect when screen unmounts
    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnectFromBoard()
        }
    }

    val filteredTasks = when (selectedFilter) {
        TaskFilter.ALL -> allTasks
        TaskFilter.PENDING -> pendingTasks
        TaskFilter.COMPLETED -> completedTasks
        TaskFilter.OVERDUE -> overdueTasks
    }

    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            // Handle error display (could use SnackbarHost)
        }
    }

    // Show success messages
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            // Handle message display
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Tasks") },
                    actions = {
                        // Show active users
                        ActiveUsersList(users = activeUsers)

                        Spacer(modifier = Modifier.width(8.dp))

                        // Notifications button
                        IconButton(onClick = onNotificationsClick) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }

                        // Chat button
                        IconButton(onClick = onChatClick) {
                            Icon(Icons.Default.Chat, contentDescription = "Chat")
                        }

                        IconButton(onClick = { viewModel.syncTasks() }) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync")
                        }

                        // More options menu
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Profile") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onProfileClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Settings, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onSettingsClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Collaborative Documents") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Description, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDocumentsClick()
                                    }
                                )
                                Divider()
                                DropdownMenuItem(
                                    text = { Text("Logout") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Logout, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        viewModel.logout()
                                        onLogout()
                                    }
                                )
                            }
                        }
                    }
                )
                // Show connection status banner
                ConnectionStatusBanner(
                    connectionState = connectionState,
                    onReconnect = {
                        val currentBoardId = boardId
                        val currentToken = token
                        val currentUserId = userId
                        if (currentBoardId != null && currentToken != null && currentUserId != null) {
                            viewModel.reconnect(currentBoardId, currentToken, currentUserId)
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateTaskClick
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedFilter.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                TaskFilter.entries.forEach { filter ->
                    Tab(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        text = { 
                            Text(
                                text = filter.displayName,
                                fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Loading indicator
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Task list
            if (filteredTasks.isEmpty() && !uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No ${selectedFilter.displayName.lowercase()} tasks",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (selectedFilter == TaskFilter.ALL) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create your first task!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = filteredTasks,
                        key = { it.id }
                    ) { task ->
                        TaskCard(
                            task = task,
                            onClick = { onTaskClick(task.id) },
                            onToggleComplete = { viewModel.toggleTaskStatus(task.id) }
                        )
                    }
                }
            }
        }
    }
}

enum class TaskFilter(val displayName: String) {
    ALL("All"),
    PENDING("Pending"),
    COMPLETED("Completed"),
    OVERDUE("Overdue")
}