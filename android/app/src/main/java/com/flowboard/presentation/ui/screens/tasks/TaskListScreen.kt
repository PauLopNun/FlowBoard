package com.flowboard.presentation.ui.screens.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowboard.domain.model.Task
import com.flowboard.presentation.ui.components.TaskCard
import com.flowboard.presentation.ui.components.ActiveUsersList
import com.flowboard.presentation.ui.components.ConnectionStatusBanner
import com.flowboard.presentation.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onTaskClick: (String) -> Unit,
    onCreateTaskClick: () -> Unit,
    onNavigateBack: () -> Unit = {},
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

    // rememberSaveable preserves filter selection and view mode across configuration changes
    // (e.g. screen rotation) without requiring the ViewModel to hold pure UI state.
    var selectedFilter by rememberSaveable { mutableStateOf(TaskFilter.ALL) }
    var isKanbanView by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Connect to WebSocket when screen mounts (only if auth data is available)
    LaunchedEffect(boardId, token, userId) {
        val currentBoardId = boardId
        val currentToken = token
        val currentUserId = userId
        if (currentBoardId != null && currentToken != null && currentUserId != null) {
            viewModel.connectToBoard(currentBoardId, currentToken, currentUserId)
        }
    }

    // Do NOT disconnect on unmount: TaskWebSocketClient is @Singleton and navigating to
    // TaskDetail (a child screen) would disconnect and immediately reconnect on return,
    // causing a rapid connect/disconnect loop. The ViewModel.onCleared() handles cleanup
    // when the task back-stack entry is permanently removed.

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
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Show active users
                        ActiveUsersList(users = activeUsers)

                        Spacer(modifier = Modifier.width(8.dp))

                        // Toggle List / Kanban view
                        IconButton(onClick = { isKanbanView = !isKanbanView }) {
                            Icon(
                                if (isKanbanView) Icons.Default.ViewList else Icons.Default.ViewColumn,
                                contentDescription = if (isKanbanView) "List View" else "Board View"
                            )
                        }

                        IconButton(onClick = { viewModel.syncTasks() }) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync")
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
        if (isKanbanView) {
            KanbanBoard(
                allTasks = allTasks,
                pendingTasks = pendingTasks,
                completedTasks = completedTasks,
                overdueTasks = overdueTasks,
                isLoading = uiState.isLoading,
                onTaskClick = onTaskClick,
                onToggleComplete = { viewModel.toggleTaskStatus(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Filter Tabs with count badges
                ScrollableTabRow(
                    selectedTabIndex = selectedFilter.ordinal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TaskFilter.entries.forEach { filter ->
                        val count = when (filter) {
                            TaskFilter.ALL -> allTasks.size
                            TaskFilter.PENDING -> pendingTasks.size
                            TaskFilter.COMPLETED -> completedTasks.size
                            TaskFilter.OVERDUE -> overdueTasks.size
                        }
                        Tab(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = filter.displayName,
                                        fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (count > 0) {
                                        Surface(
                                            color = if (selectedFilter == filter)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = count.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                color = if (selectedFilter == filter)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                if (filteredTasks.isEmpty() && !uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (selectedFilter == TaskFilter.COMPLETED)
                                    Icons.Default.Check
                                else
                                    Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Text(
                                text = when (selectedFilter) {
                                    TaskFilter.ALL -> "No tasks yet"
                                    TaskFilter.PENDING -> "Nothing pending"
                                    TaskFilter.COMPLETED -> "Nothing completed yet"
                                    TaskFilter.OVERDUE -> "No overdue tasks"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (selectedFilter) {
                                    TaskFilter.ALL -> "Tap + to create your first task"
                                    TaskFilter.PENDING -> "All tasks are done!"
                                    TaskFilter.COMPLETED -> "Complete a task to see it here"
                                    TaskFilter.OVERDUE -> "Great job staying on track"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (selectedFilter == TaskFilter.ALL) {
                                Spacer(Modifier.height(4.dp))
                                FilledTonalButton(onClick = onCreateTaskClick) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("New task")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items = filteredTasks, key = { it.id }) { task ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.StartToEnd) {
                                        viewModel.toggleTaskStatus(task.id)
                                    }
                                    false // keep item in list, just toggle status
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromEndToStart = false,
                                backgroundContent = {
                                    val bgColor by animateColorAsState(
                                        targetValue = when (dismissState.targetValue) {
                                            SwipeToDismissBoxValue.StartToEnd ->
                                                if (task.isCompleted) Color(0xFFF59E0B) else Color(0xFF10B981)
                                            else -> Color.Transparent
                                        },
                                        label = "swipe_bg"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(bgColor, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Icon(
                                            imageVector = if (task.isCompleted)
                                                Icons.Default.Check
                                            else
                                                Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                },
                                modifier = Modifier.animateItem()
                            ) {
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
    }
}

// ─── Kanban Board ─────────────────────────────────────────────────────────────

@Composable
private fun KanbanBoard(
    allTasks: List<Task>,
    pendingTasks: List<Task>,
    completedTasks: List<Task>,
    overdueTasks: List<Task>,
    isLoading: Boolean,
    onTaskClick: (String) -> Unit,
    onToggleComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = listOf(
        Triple("To Do", MaterialTheme.colorScheme.primary, pendingTasks),
        Triple("Overdue", MaterialTheme.colorScheme.error, overdueTasks),
        Triple("Done", MaterialTheme.colorScheme.tertiary, completedTasks),
    )

    if (isLoading) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyRow(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(columns) { (title, headerColor, tasks) ->
            KanbanColumn(
                title = title,
                headerColor = headerColor,
                tasks = tasks,
                onTaskClick = onTaskClick,
                onToggleComplete = onToggleComplete
            )
        }
    }
}

@Composable
private fun KanbanColumn(
    title: String,
    headerColor: androidx.compose.ui.graphics.Color,
    tasks: List<Task>,
    onTaskClick: (String) -> Unit,
    onToggleComplete: (String) -> Unit
) {
    Surface(
        modifier = Modifier.width(280.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Column header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(10.dp),
                    color = headerColor,
                    shape = CircleShape
                ) {}
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = tasks.size.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 600.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onClick = { onTaskClick(task.id) },
                            onToggleComplete = { onToggleComplete(task.id) }
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