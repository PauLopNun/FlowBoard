package com.flowboard.presentation.ui.screens.permissions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.flowboard.domain.model.*

/**
 * Dialog for sharing a resource and managing permissions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareDialog(
    resourceId: String,
    resourceType: ResourceType,
    permissionsList: PermissionListResponse?,
    isLoading: Boolean,
    onGrantPermission: (String, PermissionLevel) -> Unit,
    onUpdatePermission: (String, PermissionLevel) -> Unit,
    onRevokePermission: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf(PermissionLevel.VIEWER) }
    var showLevelPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Share ${resourceType.name.lowercase()}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Add new collaborator
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email address") },
                    placeholder = { Text("user@example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                if (emailInput.isNotBlank()) {
                                    onGrantPermission(emailInput, selectedLevel)
                                    emailInput = ""
                                }
                            },
                            enabled = emailInput.isNotBlank() && !isLoading
                        ) {
                            Text("Invite")
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Permission level selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Default access level:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { showLevelPicker = true }
                    ) {
                        Text(selectedLevel.name)
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select permission level"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Collaborators list
                Text(
                    text = "People with access",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Owner
                        permissionsList?.owner?.let { owner ->
                            item {
                                CollaboratorItem(
                                    user = owner,
                                    isOwner = true,
                                    onUpdatePermission = { },
                                    onRevokePermission = { }
                                )
                            }
                        }

                        // Collaborators
                        permissionsList?.collaborators?.let { collaborators ->
                            items(collaborators) { collaborator ->
                                CollaboratorItem(
                                    user = collaborator,
                                    isOwner = false,
                                    onUpdatePermission = { newLevel ->
                                        // Find permission ID (in real app, store this)
                                        onUpdatePermission("permission-id", newLevel)
                                    },
                                    onRevokePermission = {
                                        // Find permission ID (in real app, store this)
                                        onRevokePermission("permission-id")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Permission level picker dialog
    if (showLevelPicker) {
        AlertDialog(
            onDismissRequest = { showLevelPicker = false },
            title = { Text("Select permission level") },
            text = {
                Column {
                    PermissionLevel.values().filter {
                        it != PermissionLevel.NONE && it != PermissionLevel.OWNER
                    }.forEach { level ->
                        TextButton(
                            onClick = {
                                selectedLevel = level
                                showLevelPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = level.name,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = getPermissionDescription(level),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLevelPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CollaboratorItem(
    user: UserPermissionInfo,
    isOwner: Boolean,
    onUpdatePermission: (PermissionLevel) -> Unit,
    onRevokePermission: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Avatar
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = user.userName.firstOrNull()?.uppercase() ?: "?",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = user.userName,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Online indicator
                if (user.isOnline) {
                    Surface(
                        modifier = Modifier.size(8.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.primary
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Permission level
                if (isOwner) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Owner") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                } else {
                    AssistChip(
                        onClick = { showMenu = true },
                        label = { Text(user.permissionLevel.name) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        PermissionLevel.values().filter {
                            it != PermissionLevel.NONE && it != PermissionLevel.OWNER
                        }.forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level.name) },
                                onClick = {
                                    onUpdatePermission(level)
                                    showMenu = false
                                }
                            )
                        }
                        Divider()
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Remove access",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                onRevokePermission()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun getPermissionDescription(level: PermissionLevel): String {
    return when (level) {
        PermissionLevel.VIEWER -> "Can view only"
        PermissionLevel.COMMENTER -> "Can view and comment"
        PermissionLevel.EDITOR -> "Can view and edit"
        PermissionLevel.ADMIN -> "Full access except transfer"
        PermissionLevel.OWNER -> "Full access"
        PermissionLevel.NONE -> "No access"
    }
}
