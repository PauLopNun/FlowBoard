package com.flowboard.presentation.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowboard.domain.model.ChatType
import com.flowboard.presentation.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChatDialog(
    viewModel: ChatViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onChatCreated: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )
        },
        title = {
            Text("New Chat")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Direct") },
                        icon = { Icon(Icons.Default.Person, null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Group") },
                        icon = { Icon(Icons.Default.Group, null) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Project") },
                        icon = { Icon(Icons.Default.Work, null) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> DirectChatForm(
                        viewModel = viewModel,
                        onChatCreated = onChatCreated,
                        onDismiss = onDismiss
                    )
                    1 -> GroupChatForm(
                        viewModel = viewModel,
                        onChatCreated = onChatCreated,
                        onDismiss = onDismiss
                    )
                    2 -> ProjectChatForm(
                        viewModel = viewModel,
                        onChatCreated = onChatCreated,
                        onDismiss = onDismiss
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
fun DirectChatForm(
    viewModel: ChatViewModel,
    onChatCreated: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var userEmail by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = userEmail,
            onValueChange = { userEmail = it },
            label = { Text("User email or username") },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    isLoading = true
                    // TODO: Search user by email and get userId
                    val userId = "user_123"
                    viewModel.createDirectChat(userId, userEmail)
                    onDismiss()
                },
                enabled = userEmail.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Start Chat")
                }
            }
        }
    }
}

@Composable
fun GroupChatForm(
    viewModel: ChatViewModel,
    onChatCreated: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var selectedUsers by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("Group name") },
            leadingIcon = {
                Icon(Icons.Default.Group, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text(
            text = "Add members",
            style = MaterialTheme.typography.titleSmall
        )

        // TODO: Add user search and selection
        Text(
            text = "${selectedUsers.size} members selected",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    isLoading = true
                    viewModel.createGroupChat(groupName, selectedUsers)
                    onDismiss()
                },
                enabled = groupName.isNotBlank() && selectedUsers.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create Group")
                }
            }
        }
    }
}

@Composable
fun ProjectChatForm(
    viewModel: ChatViewModel,
    onChatCreated: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedProject by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // TODO: Fetch projects from ProjectRepository

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Select a project",
            style = MaterialTheme.typography.titleSmall
        )

        // TODO: Add project selection list

        Text(
            text = if (selectedProject != null) "Project selected" else "No project selected",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    isLoading = true
                    selectedProject?.let { projectId ->
                        viewModel.createProjectChat(
                            projectId = projectId,
                            projectName = "Project Name", // TODO: Get from project
                            participantIds = emptyList() // TODO: Get project members
                        )
                    }
                    onDismiss()
                },
                enabled = selectedProject != null && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create Chat")
                }
            }
        }
    }
}
