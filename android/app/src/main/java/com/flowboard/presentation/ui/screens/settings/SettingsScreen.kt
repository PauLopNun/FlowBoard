package com.flowboard.presentation.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowboard.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Appearance Section
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    // Dark Mode Toggle
                    ListItem(
                        headlineContent = { Text("Dark Mode") },
                        supportingContent = { Text("Enable dark theme") },
                        leadingContent = {
                            Icon(
                                if (darkModeEnabled) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = darkModeEnabled,
                                onCheckedChange = { viewModel.setDarkMode(it) }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Notifications Section
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    // Push Notifications
                    ListItem(
                        headlineContent = { Text("Push Notifications") },
                        supportingContent = { Text("Receive notifications for updates") },
                        leadingContent = {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        },
                        trailingContent = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                            )
                        }
                    )

                    Divider()

                    // Document Shared
                    ListItem(
                        headlineContent = { Text("Document Shared") },
                        supportingContent = { Text("When someone shares a document with you") },
                        trailingContent = {
                            Switch(
                                checked = notificationsEnabled, // TODO: Individual settings
                                onCheckedChange = { /* TODO */ }
                            )
                        }
                    )

                    Divider()

                    // Chat Messages
                    ListItem(
                        headlineContent = { Text("Chat Messages") },
                        supportingContent = { Text("New messages in your chats") },
                        trailingContent = {
                            Switch(
                                checked = notificationsEnabled, // TODO: Individual settings
                                onCheckedChange = { /* TODO */ }
                            )
                        }
                    )

                    Divider()

                    // Task Updates
                    ListItem(
                        headlineContent = { Text("Task Updates") },
                        supportingContent = { Text("Changes to tasks you're watching") },
                        trailingContent = {
                            Switch(
                                checked = notificationsEnabled, // TODO: Individual settings
                                onCheckedChange = { /* TODO */ }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Data & Privacy Section
            Text(
                text = "Data & Privacy",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    // Sync Data
                    ListItem(
                        headlineContent = { Text("Sync Data") },
                        supportingContent = { Text("Automatically sync with server") },
                        leadingContent = {
                            Icon(Icons.Default.CloudSync, contentDescription = null)
                        },
                        trailingContent = {
                            Switch(
                                checked = true, // TODO: Implement
                                onCheckedChange = { /* TODO */ }
                            )
                        }
                    )

                    Divider()

                    // Clear Cache
                    ListItem(
                        headlineContent = { Text("Clear Cache") },
                        supportingContent = { Text("Free up storage space") },
                        leadingContent = {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            // TODO: Implement clear cache
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // About Section
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    // Version
                    ListItem(
                        headlineContent = { Text("Version") },
                        supportingContent = { Text("1.0.0") },
                        leadingContent = {
                            Icon(Icons.Default.Info, contentDescription = null)
                        }
                    )

                    Divider()

                    // Privacy Policy
                    ListItem(
                        headlineContent = { Text("Privacy Policy") },
                        leadingContent = {
                            Icon(Icons.Default.PrivacyTip, contentDescription = null)
                        },
                        trailingContent = {
                            Icon(Icons.Default.OpenInNew, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            // TODO: Open privacy policy
                        }
                    )

                    Divider()

                    // Terms of Service
                    ListItem(
                        headlineContent = { Text("Terms of Service") },
                        leadingContent = {
                            Icon(Icons.Default.Description, contentDescription = null)
                        },
                        trailingContent = {
                            Icon(Icons.Default.OpenInNew, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            // TODO: Open terms
                        }
                    )

                    Divider()

                    // Help & Support
                    ListItem(
                        headlineContent = { Text("Help & Support") },
                        leadingContent = {
                            Icon(Icons.Default.Help, contentDescription = null)
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            // TODO: Open support
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
