package com.flowboard.presentation.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowboard.data.remote.dto.UserPresenceInfo
import com.flowboard.domain.model.User
import com.flowboard.presentation.ui.components.ActiveUsersList
import com.flowboard.presentation.ui.theme.*
import com.flowboard.presentation.viewmodel.DocumentViewModel
import com.flowboard.presentation.viewmodel.LoginViewModel
import java.time.format.DateTimeFormatter

data class DashboardDocument(
    val id: String,
    val title: String,
    val preview: String,
    val lastModified: String,
    val owner: String,
    val isShared: Boolean,
    val activeEditors: Int,
    val icon: String = "📄"
)

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
    onLogout: () -> Unit = {},
    documentViewModel: DocumentViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel = hiltViewModel()
) {
    val activeUsers by documentViewModel.activeUsers.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }
    var selectedView by remember { mutableStateOf(ViewType.GRID) }

    // Sample documents - En producción estos vendrían del ViewModel
    val recentDocuments = remember {
        listOf(
            DashboardDocument(
                id = "doc1",
                title = "Project Proposal 2025",
                preview = "Comprehensive project proposal outlining objectives, timeline, and deliverables for Q1 2025...",
                lastModified = "2 hours ago",
                owner = "John Doe",
                isShared = true,
                activeEditors = 3,
                icon = "📋"
            ),
            DashboardDocument(
                id = "doc2",
                title = "Meeting Notes - Sprint Planning",
                preview = "Team meeting on November 25, 2025. Discussed sprint planning, feature prioritization...",
                lastModified = "5 hours ago",
                owner = "Jane Smith",
                isShared = true,
                activeEditors = 1,
                icon = "📝"
            ),
            DashboardDocument(
                id = "doc3",
                title = "Technical Architecture",
                preview = "Detailed technical specifications for the FlowBoard collaborative editor. CRDT implementation...",
                lastModified = "Yesterday",
                owner = "You",
                isShared = false,
                activeEditors = 0,
                icon = "⚙️"
            ),
            DashboardDocument(
                id = "doc4",
                title = "Design System Guidelines",
                preview = "UI/UX design system with color palette, typography, and component specifications...",
                lastModified = "2 days ago",
                owner = "Sarah Wilson",
                isShared = true,
                activeEditors = 2,
                icon = "🎨"
            ),
            DashboardDocument(
                id = "doc5",
                title = "Marketing Strategy Q1",
                preview = "Strategic marketing plan for Q1 2025 including campaigns, budget allocation...",
                lastModified = "3 days ago",
                owner = "Mike Johnson",
                isShared = true,
                activeEditors = 0,
                icon = "📊"
            )
        )
    }

    Scaffold(
        topBar = {
            ModernTopBar(
                activeUsers = activeUsers,
                onNotificationsClick = onNotificationsClick,
                onChatClick = onChatClick,
                onMenuClick = { showMenu = true },
                showMenu = showMenu,
                onDismissMenu = { showMenu = false },
                onProfileClick = {
                    showMenu = false
                    onProfileClick()
                },
                onSettingsClick = {
                    showMenu = false
                    onSettingsClick()
                },
                onTasksClick = {
                    showMenu = false
                    onTasksClick()
                },
                onLogout = {
                    showMenu = false
                    loginViewModel.logout()
                    onLogout()
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero Section
            item {
                HeroSection(onCreateDocument = onCreateDocument)
            }

            // Quick Actions
            item {
                QuickActionsSection(
                    onCreateDocument = onCreateDocument,
                    onTasksClick = onTasksClick,
                    onChatClick = onChatClick
                )
            }

            // View Toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Documents",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { selectedView = ViewType.GRID }
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Grid View",
                                tint = if (selectedView == ViewType.GRID)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { selectedView = ViewType.LIST }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewList,
                                contentDescription = "List View",
                                tint = if (selectedView == ViewType.LIST)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Documents Grid/List
            if (selectedView == ViewType.GRID) {
                items(recentDocuments.chunked(2)) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowItems.forEach { document ->
                            Box(modifier = Modifier.weight(1f)) {
                                DocumentCard(
                                    document = document,
                                    onClick = { onDocumentClick(document.id) }
                                )
                            }
                        }
                        // Fill empty space if odd number
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                items(recentDocuments) { document ->
                    DocumentListItem(
                        document = document,
                        onClick = { onDocumentClick(document.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTopBar(
    activeUsers: List<UserPresenceInfo>,
    onNotificationsClick: () -> Unit,
    onChatClick: () -> Unit,
    onMenuClick: () -> Unit,
    showMenu: Boolean,
    onDismissMenu: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTasksClick: () -> Unit,
    onLogout: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo/Brand
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(GradientStart, GradientEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "F",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = "FlowBoard",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active users
                if (activeUsers.isNotEmpty()) {
                    ActiveUsersList(users = activeUsers)
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Notifications
                IconButton(onClick = onNotificationsClick) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error
                            ) {
                                Text("3")
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications"
                        )
                    }
                }

                // Chat
                IconButton(onClick = onChatClick) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Chat"
                    )
                }

                // Menu
                Box {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = onDismissMenu
                    ) {
                        DropdownMenuItem(
                            text = { Text("Profile") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Person, contentDescription = null)
                            },
                            onClick = onProfileClick
                        )
                        DropdownMenuItem(
                            text = { Text("Tasks") },
                            leadingIcon = {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                            },
                            onClick = onTasksClick
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Settings, contentDescription = null)
                            },
                            onClick = onSettingsClick
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Logout") },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                            },
                            onClick = onLogout
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeroSection(
    onCreateDocument: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            GradientStart.copy(alpha = 0.1f),
                            GradientEnd.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(32.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                Column {
                    Text(
                        text = "Welcome back!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create and collaborate on documents in real-time",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Button(
                    onClick = onCreateDocument,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Document", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    onCreateDocument: () -> Unit,
    onTasksClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Column {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                icon = Icons.Outlined.Description,
                title = "New Document",
                color = LightPrimary,
                onClick = onCreateDocument,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = Icons.Outlined.CheckCircle,
                title = "Tasks",
                color = LightSecondary,
                onClick = onTasksClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = Icons.Outlined.ChatBubbleOutline,
                title = "Chat",
                color = LightTertiary,
                onClick = onChatClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
fun DocumentCard(
    document: DashboardDocument,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            hoveredElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = document.icon,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    if (document.isShared) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Shared",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = document.preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column {
                if (document.activeEditors > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Success)
                        )
                        Text(
                            text = "${document.activeEditors} editing now",
                            style = MaterialTheme.typography.labelSmall,
                            color = Success
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = "Modified ${document.lastModified}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DocumentListItem(
    document: DashboardDocument,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = document.icon,
                    style = MaterialTheme.typography.headlineSmall
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = document.preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                if (document.activeEditors > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Success)
                        )
                        Text(
                            text = "${document.activeEditors}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Success,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = document.lastModified,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

enum class ViewType {
    GRID,
    LIST
}
