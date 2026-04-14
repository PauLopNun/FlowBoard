package com.flowboard.presentation.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    val uiState by viewModel.uiState.collectAsState()
    var searchError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = userEmail,
            onValueChange = {
                userEmail = it
                searchError = null
            },
            label = { Text("User email or username") },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = searchError != null,
            supportingText = searchError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
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
                    searchError = null
                    viewModel.searchAndCreateDirectChat(
                        email = userEmail,
                        onSuccess = { onDismiss() },
                        onError = { error -> searchError = error }
                    )
                },
                enabled = userEmail.isNotBlank() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
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
    val uiState by viewModel.uiState.collectAsState()

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
            text = "Members can be invited after the group is created.",
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
                    viewModel.createGroupChat(groupName, emptyList()) { chatId ->
                        onChatCreated(chatId)
                        onDismiss()
                    }
                },
                enabled = groupName.isNotBlank() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
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
