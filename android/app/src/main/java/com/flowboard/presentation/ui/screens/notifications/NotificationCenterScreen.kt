package com.flowboard.presentation.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flowboard.domain.model.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    notifications: List<Notification>,
    unreadCount: Int,
    onNotificationClick: (Notification) -> Unit,
    onMarkAsRead: (String) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onDeleteNotification: (String) -> Unit,
    onDeleteAll: () -> Unit,
    onAcceptInvitation: (Notification) -> Unit,
    onDeclineInvitation: (Notification) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(NotificationFilter.ALL) }
    val regularUnreadCount = notifications.count { !it.isRead && !it.isActionableInvitation() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Notifications")
                        if (unreadCount > 0) {
                            Text(
                                text = "$unreadCount unread",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (regularUnreadCount > 0) {
                        IconButton(
                            onClick = {
                                notifications
                                    .filter { !it.isRead && !it.isActionableInvitation() }
                                    .forEach { onMarkAsRead(it.id) }
                            }
                        ) {
                            Icon(Icons.Default.DoneAll, "Mark all as read")
                        }
                    }

                    // Menu
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "More options")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete all") },
                            onClick = {
                                onDeleteAll()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            FilterChips(
                selectedFilter = filter,
                notifications = notifications,
                onFilterChange = { filter = it }
            )

            HorizontalDivider()

            val filteredNotifications = notifications.filter { filter.matches(it) }

            if (filteredNotifications.isEmpty()) {
                EmptyState(filter = filter)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredNotifications,
                        key = { it.id }
                    ) { notification ->
                        NotificationItem(
                            notification = notification,
                            onClick = {
                                if (!notification.isRead && !notification.isActionableInvitation()) {
                                    onMarkAsRead(notification.id)
                                }
                                onNotificationClick(notification)
                            },
                            onDelete = { onDeleteNotification(notification.id) },
                            onAcceptInvitation = { onAcceptInvitation(notification) },
                            onDeclineInvitation = { onDeclineInvitation(notification) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChips(
    selectedFilter: NotificationFilter,
    notifications: List<Notification>,
    onFilterChange: (NotificationFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NotificationFilter.values().forEach { filter ->
            val count = notifications.count { filter.matches(it) }
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = {
                    Text(
                        if (count > 0 && filter != NotificationFilter.ALL) {
                            "${filter.label} $count"
                        } else {
                            filter.label
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onAcceptInvitation: () -> Unit,
    onDeclineInvitation: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isInvitation = notification.isActionableInvitation()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon
                NotificationIcon(notification.type, notification.priority)

                // Content
                Column(modifier = Modifier.weight(1f)) {
                    // Title
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Message
                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Metadata
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Time
                        Text(
                            text = formatTimestamp(notification.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Action user
                        notification.actionUserName?.let { userName ->
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (isInvitation && !notification.isRead) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onAcceptInvitation,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Accept")
                            }
                            OutlinedButton(
                                onClick = onDeclineInvitation,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Decline")
                            }
                        }
                    }
                }
            }

            // Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationIcon(type: NotificationType, priority: NotificationPriority) {
    val (icon, color) = when (type) {
        NotificationType.TASK_ASSIGNED -> Icons.Default.Assignment to MaterialTheme.colorScheme.primary
        NotificationType.TASK_COMPLETED -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.tertiary
        NotificationType.TASK_DUE_SOON -> Icons.Default.AccessTime to MaterialTheme.colorScheme.secondary
        NotificationType.TASK_OVERDUE -> Icons.Default.Warning to MaterialTheme.colorScheme.error
        NotificationType.COMMENT_MENTION -> Icons.Default.AlternateEmail to MaterialTheme.colorScheme.primary
        NotificationType.COMMENT_REPLY -> Icons.Default.Reply to MaterialTheme.colorScheme.primary
        NotificationType.CHAT_MESSAGE -> Icons.Default.Chat to MaterialTheme.colorScheme.primary
        NotificationType.PERMISSION_GRANTED -> Icons.Default.Lock to MaterialTheme.colorScheme.tertiary
        NotificationType.DOCUMENT_SHARED -> Icons.Default.Share to MaterialTheme.colorScheme.primary
        NotificationType.WORKSPACE_INVITATION -> Icons.Default.GroupAdd to MaterialTheme.colorScheme.secondary
        NotificationType.PROJECT_INVITATION -> Icons.Default.Group to MaterialTheme.colorScheme.secondary
        else -> Icons.Default.Notifications to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.size(40.dp),
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun EmptyState(filter: NotificationFilter) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = if (filter == NotificationFilter.ALL) "No notifications" else "No ${filter.label.lowercase()}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (filter == NotificationFilter.ALL) "You're all caught up!" else "Try changing the filter",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

private enum class NotificationFilter(val label: String) {
    ALL("All"),
    INVITATIONS("Invites"),
    MESSAGES("Messages"),
    DOCUMENTS("Documents"),
    TASKS("Tasks")
}

private fun NotificationFilter.matches(notification: Notification): Boolean {
    return when (this) {
        NotificationFilter.ALL -> true
        NotificationFilter.INVITATIONS -> notification.isActionableInvitation()
        NotificationFilter.MESSAGES -> notification.type == NotificationType.CHAT_MESSAGE ||
            notification.type == NotificationType.COMMENT_MENTION ||
            notification.type == NotificationType.COMMENT_REPLY
        NotificationFilter.DOCUMENTS -> notification.type == NotificationType.DOCUMENT_SHARED ||
            notification.type == NotificationType.DOCUMENT_UPDATED ||
            notification.type == NotificationType.PERMISSION_GRANTED ||
            notification.type == NotificationType.PERMISSION_REVOKED
        NotificationFilter.TASKS -> notification.type == NotificationType.TASK_ASSIGNED ||
            notification.type == NotificationType.TASK_COMPLETED ||
            notification.type == NotificationType.TASK_DUE_SOON ||
            notification.type == NotificationType.TASK_OVERDUE ||
            notification.type == NotificationType.DEADLINE_REMINDER
    }
}

private fun Notification.isActionableInvitation(): Boolean {
    return type == NotificationType.WORKSPACE_INVITATION ||
        (type == NotificationType.DOCUMENT_SHARED && title.contains("invitation", ignoreCase = true))
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}
