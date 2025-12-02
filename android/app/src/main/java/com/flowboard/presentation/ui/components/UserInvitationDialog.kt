package com.flowboard.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Diálogo para invitar usuarios a colaborar en el documento
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInvitationDialog(
    onDismiss: () -> Unit,
    onInviteUser: (String, PermissionLevel) -> Unit,
    suggestedUsers: List<SuggestedUser> = emptyList(),
    currentCollaborators: List<CollaboratorInfo> = emptyList(),
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPermission by remember { mutableStateOf(PermissionLevel.EDIT) }
    var showPermissionMenu by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Encabezado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Invite to collaborate",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                // Campo de búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter email or username") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Selector de permisos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Permission level:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Box {
                        AssistChip(
                            onClick = { showPermissionMenu = true },
                            label = { Text(selectedPermission.displayName) },
                            leadingIcon = {
                                Icon(
                                    selectedPermission.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )

                        DropdownMenu(
                            expanded = showPermissionMenu,
                            onDismissRequest = { showPermissionMenu = false }
                        ) {
                            PermissionLevel.values().forEach { permission ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                permission.displayName,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                permission.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedPermission = permission
                                        showPermissionMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            permission.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Usuarios sugeridos
                if (suggestedUsers.isNotEmpty()) {
                    Text(
                        text = "Suggested",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            suggestedUsers.filter {
                                it.username.contains(searchQuery, ignoreCase = true) ||
                                        it.email.contains(searchQuery, ignoreCase = true)
                            }
                        ) { user ->
                            UserListItem(
                                user = user,
                                isCollaborator = currentCollaborators.any { it.userId == user.id },
                                onInvite = { onInviteUser(user.id, selectedPermission) }
                            )
                        }
                    }
                }

                // Colaboradores actuales
                if (currentCollaborators.isNotEmpty()) {
                    HorizontalDivider()

                    Text(
                        text = "Current collaborators",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentCollaborators) { collaborator ->
                            CollaboratorListItem(
                                collaborator = collaborator
                            )
                        }
                    }
                }

                // Botón de invitar por email/username
                AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = { onInviteUser(searchQuery, selectedPermission) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Send invitation")
                    }
                }
            }
        }
    }
}

/**
 * Item de usuario en la lista de sugeridos
 */
@Composable
private fun UserListItem(
    user: SuggestedUser,
    isCollaborator: Boolean,
    onInvite: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isCollaborator) { onInvite() },
        color = if (isCollaborator)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (isCollaborator) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = stringToColor(user.username)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = user.username.take(1).uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Estado
            if (isCollaborator) {
                AssistChip(
                    onClick = {},
                    label = { Text("Collaborator") },
                    enabled = false
                )
            } else {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = "Invite",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Item de colaborador actual
 */
@Composable
private fun CollaboratorListItem(
    collaborator: CollaboratorInfo
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar con indicador de online
            Box {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = stringToColor(collaborator.username)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = collaborator.username.take(1).uppercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (collaborator.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color(0xFF4CAF50), CircleShape)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            }

            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = collaborator.username,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (collaborator.isOnline) "Online" else "Offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (collaborator.isOnline)
                        Color(0xFF4CAF50)
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Permiso
            AssistChip(
                onClick = {},
                label = { Text(collaborator.permission.displayName) },
                leadingIcon = {
                    Icon(
                        collaborator.permission.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

/**
 * Datos de un usuario sugerido
 */
data class SuggestedUser(
    val id: String,
    val username: String,
    val email: String
)

/**
 * Información de un colaborador actual
 */
data class CollaboratorInfo(
    val userId: String,
    val username: String,
    val isOnline: Boolean,
    val permission: PermissionLevel
)

/**
 * Niveles de permiso para colaboradores
 */
enum class PermissionLevel(
    val displayName: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    VIEW(
        "View only",
        "Can only read the document",
        Icons.Default.Visibility
    ),
    COMMENT(
        "Can comment",
        "Can read and leave comments",
        Icons.Default.Comment
    ),
    EDIT(
        "Can edit",
        "Can read and make changes",
        Icons.Default.Edit
    ),
    ADMIN(
        "Admin",
        "Full control including sharing",
        Icons.Default.AdminPanelSettings
    )
}

/**
 * Generar color basado en string
 */
private fun stringToColor(str: String): Color {
    val hash = str.hashCode()
    val hue = kotlin.math.abs(hash % 360)

    return when (hue % 10) {
        0 -> Color(0xFF1976D2)
        1 -> Color(0xFF388E3C)
        2 -> Color(0xFFD32F2F)
        3 -> Color(0xFFF57C00)
        4 -> Color(0xFF7B1FA2)
        5 -> Color(0xFF00796B)
        6 -> Color(0xFFC2185B)
        7 -> Color(0xFF5D4037)
        8 -> Color(0xFF303F9F)
        else -> Color(0xFF0097A7)
    }
}
