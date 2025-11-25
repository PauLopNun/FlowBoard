package com.flowboard.presentation.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowboard.domain.model.ChatRoom
import com.flowboard.domain.model.ChatType
import com.flowboard.presentation.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    onChatClick: (String) -> Unit,
    onCreateChat: () -> Unit
) {
    val chatRooms by viewModel.chatRooms.collectAsState()
    val archivedChatRooms by viewModel.archivedChatRooms.collectAsState()
    val totalUnreadCount by viewModel.totalUnreadCount.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showArchived by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Messages",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                actions = {
                    // Unread badge
                    if (totalUnreadCount > 0) {
                        Badge(
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = if (totalUnreadCount > 99) "99+" else totalUnreadCount.toString()
                            )
                        }
                    }

                    // Archive button
                    IconButton(onClick = { showArchived = !showArchived }) {
                        Icon(
                            imageVector = if (showArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = if (showArchived) "Show Active" else "Show Archived"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateChat
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Chat"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs for filtering
            if (!showArchived) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("All") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Direct") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Groups") }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Projects") }
                    )
                }
            }

            // Chat list
            val displayedChats = if (showArchived) {
                archivedChatRooms
            } else {
                when (selectedTab) {
                    1 -> chatRooms.filter { it.type == ChatType.DIRECT }
                    2 -> chatRooms.filter { it.type == ChatType.GROUP }
                    3 -> chatRooms.filter { it.type == ChatType.PROJECT }
                    else -> chatRooms
                }
            }

            if (displayedChats.isEmpty()) {
                EmptyChatState(
                    isArchived = showArchived,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = displayedChats,
                        key = { it.id }
                    ) { chatRoom ->
                        ChatRoomItem(
                            chatRoom = chatRoom,
                            onClick = { onChatClick(chatRoom.id) },
                            onArchive = {
                                viewModel.archiveChat(chatRoom.id, !chatRoom.isArchived)
                            },
                            onMute = {
                                viewModel.muteChat(chatRoom.id, !chatRoom.isMuted)
                            },
                            onDelete = {
                                viewModel.deleteChat(chatRoom.id)
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun ChatRoomItem(
    chatRoom: ChatRoom,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onMute: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = chatRoom.name ?: getChatRoomDisplayName(chatRoom),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (chatRoom.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (chatRoom.isMuted) {
                    Icon(
                        imageVector = Icons.Default.VolumeOff,
                        contentDescription = "Muted",
                        modifier = Modifier
                            .size(16.dp)
                            .padding(start = 4.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        supportingContent = {
            Text(
                text = chatRoom.lastMessage?.content ?: "No messages yet",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = if (chatRoom.unreadCount > 0) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        leadingContent = {
            Box {
                ChatAvatar(
                    chatRoom = chatRoom,
                    size = 56.dp
                )

                // Online indicator for direct chats
                if (chatRoom.type == ChatType.DIRECT && chatRoom.participants.any { it.lastSeen != null }) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(2.dp)
                    )
                }
            }
        },
        trailingContent = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Timestamp
                chatRoom.lastMessage?.createdAt?.let { timestamp ->
                    Text(
                        text = formatTimestamp(timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Unread badge
                if (chatRoom.unreadCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = if (chatRoom.unreadCount > 99) "99+" else chatRoom.unreadCount.toString()
                        )
                    }
                }

                // Menu button
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (chatRoom.isMuted) "Unmute" else "Mute") },
                            onClick = {
                                onMute()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (chatRoom.isMuted) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = null
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(if (chatRoom.isArchived) "Unarchive" else "Archive") },
                            onClick = {
                                onArchive()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (chatRoom.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                    contentDescription = null
                                )
                            }
                        )

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                onDelete()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun ChatAvatar(
    chatRoom: ChatRoom,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when (chatRoom.type) {
                ChatType.DIRECT -> Icons.Default.Person
                ChatType.GROUP -> Icons.Default.Group
                ChatType.PROJECT -> Icons.Default.Work
                ChatType.TASK_THREAD -> Icons.Default.Task
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

@Composable
fun EmptyChatState(
    isArchived: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isArchived) Icons.Default.Archive else Icons.Default.Chat,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isArchived) "No archived chats" else "No messages yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isArchived) {
                "Archived conversations will appear here"
            } else {
                "Start a conversation to get started"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

// ==================== Helper Functions ====================

private fun getChatRoomDisplayName(chatRoom: ChatRoom): String {
    return when (chatRoom.type) {
        ChatType.DIRECT -> {
            chatRoom.participants.firstOrNull()?.userName ?: "Unknown User"
        }
        ChatType.GROUP -> {
            chatRoom.participants.joinToString(", ") { it.userName }
        }
        ChatType.PROJECT -> {
            "Project Chat"
        }
        ChatType.TASK_THREAD -> {
            "Task Discussion"
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        diff < 604_800_000 -> {
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            dayFormat.format(Date(timestamp))
        }
        else -> {
            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}
