package com.flowboard.presentation.ui.components

import androidx.compose.animation.*
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
import androidx.compose.ui.window.DialogProperties
import com.flowboard.presentation.ui.theme.*

data class DocumentCollaborator(
    val userId: String,
    val email: String,
    val name: String,
    val role: CollaboratorRole,
    val isOwner: Boolean = false
)

enum class CollaboratorRole(val displayName: String, val description: String) {
    VIEWER("Viewer", "Can view and comment"),
    EDITOR("Editor", "Can edit content"),
    OWNER("Owner", "Full control")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareDocumentDialog(
    documentTitle: String,
    currentCollaborators: List<DocumentCollaborator>,
    onInviteUser: (email: String, role: CollaboratorRole) -> Unit,
    onUpdatePermission: (userId: String, role: CollaboratorRole) -> Unit,
    onRemovePermission: (userId: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var emailInput by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(CollaboratorRole.EDITOR) }
    var showRoleSelector by remember { mutableStateOf(false) }
    var isInviting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Share Document",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = documentTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Invite Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Invite Collaborators",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Email Input
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = {
                                emailInput = it
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter email address") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null)
                            },
                            trailingIcon = {
                                if (emailInput.isNotEmpty()) {
                                    IconButton(onClick = { emailInput = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            isError = errorMessage != null,
                            supportingText = {
                                errorMessage?.let {
                                    Text(
                                        text = it,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Role Selector + Invite Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Role Selector
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { showRoleSelector = !showRoleSelector },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (selectedRole == CollaboratorRole.EDITOR)
                                            Icons.Default.Edit
                                        else
                                            Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(selectedRole.displayName)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (showRoleSelector)
                                            Icons.Default.KeyboardArrowUp
                                        else
                                            Icons.Default.KeyboardArrowDown,
                                        contentDescription = null
                                    )
                                }

                                DropdownMenu(
                                    expanded = showRoleSelector,
                                    onDismissRequest = { showRoleSelector = false }
                                ) {
                                    CollaboratorRole.values()
                                        .filter { it != CollaboratorRole.OWNER }
                                        .forEach { role ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(
                                                            text = role.displayName,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Text(
                                                            text = role.description,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = if (role == CollaboratorRole.EDITOR)
                                                            Icons.Default.Edit
                                                        else
                                                            Icons.Default.Visibility,
                                                        contentDescription = null,
                                                        tint = if (role == selectedRole)
                                                            MaterialTheme.colorScheme.primary
                                                        else
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                onClick = {
                                                    selectedRole = role
                                                    showRoleSelector = false
                                                }
                                            )
                                        }
                                }
                            }

                            // Invite Button
                            Button(
                                onClick = {
                                    if (emailInput.isBlank()) {
                                        errorMessage = "Please enter an email address"
                                        return@Button
                                    }
                                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                                        errorMessage = "Please enter a valid email address"
                                        return@Button
                                    }
                                    isInviting = true
                                    onInviteUser(emailInput, selectedRole)
                                    emailInput = ""
                                    errorMessage = null
                                    isInviting = false
                                },
                                modifier = Modifier.height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = emailInput.isNotBlank() && !isInviting
                            ) {
                                if (isInviting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Invite",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Invite")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Collaborators List
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "People with access",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${currentCollaborators.size} ${if (currentCollaborators.size == 1) "person" else "people"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentCollaborators) { collaborator ->
                            CollaboratorItem(
                                collaborator = collaborator,
                                onUpdateRole = { newRole ->
                                    onUpdatePermission(collaborator.userId, newRole)
                                },
                                onRemove = {
                                    onRemovePermission(collaborator.userId)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
fun CollaboratorItem(
    collaborator: DocumentCollaborator,
    onUpdateRole: (CollaboratorRole) -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRemoveConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when (collaborator.role) {
                                CollaboratorRole.OWNER -> CollabPurple
                                CollaboratorRole.EDITOR -> CollabBlue
                                CollaboratorRole.VIEWER -> CollabGreen
                            }.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = collaborator.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (collaborator.role) {
                            CollaboratorRole.OWNER -> CollabPurple
                            CollaboratorRole.EDITOR -> CollabBlue
                            CollaboratorRole.VIEWER -> CollabGreen
                        }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = collaborator.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (collaborator.isOwner) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CollabPurple.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Owner",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CollabPurple,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = collaborator.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Role selector / Remove button
            if (collaborator.isOwner) {
                Text(
                    text = "Owner",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Box {
                    OutlinedButton(
                        onClick = { showMenu = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = when (collaborator.role) {
                                CollaboratorRole.EDITOR -> Icons.Default.Edit
                                CollaboratorRole.VIEWER -> Icons.Default.Visibility
                                else -> Icons.Default.Person
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = collaborator.role.displayName,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        CollaboratorRole.values()
                            .filter { it != CollaboratorRole.OWNER }
                            .forEach { role ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = role.displayName,
                                                fontWeight = if (role == collaborator.role)
                                                    FontWeight.Bold
                                                else
                                                    FontWeight.Normal
                                            )
                                            Text(
                                                text = role.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (role == CollaboratorRole.EDITOR)
                                                Icons.Default.Edit
                                            else
                                                Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = if (role == collaborator.role)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    trailingIcon = {
                                        if (role == collaborator.role) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    onClick = {
                                        onUpdateRole(role)
                                        showMenu = false
                                    }
                                )
                            }

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Remove access",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PersonRemove,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                showRemoveConfirmation = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Remove confirmation dialog
    if (showRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Remove access?") },
            text = {
                Text("${collaborator.name} will no longer have access to this document.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemove()
                        showRemoveConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
