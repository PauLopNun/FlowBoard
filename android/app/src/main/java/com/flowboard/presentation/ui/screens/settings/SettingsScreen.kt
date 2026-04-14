package com.flowboard.presentation.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowboard.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val darkModePreference by viewModel.darkModeEnabled.collectAsStateWithLifecycle()
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    // null = follow system; show toggle state as the effective value
    val darkModeEnabled = darkModePreference ?: systemDark
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val notifDocsEnabled by viewModel.notifDocsEnabled.collectAsStateWithLifecycle()
    val notifChatEnabled by viewModel.notifChatEnabled.collectAsStateWithLifecycle()
    val notifTasksEnabled by viewModel.notifTasksEnabled.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val flowboardUrl = "https://github.com/PauLopNun/FlowBoard"

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        supportingContent = {
                            Text(
                                if (darkModePreference == null)
                                    "Siguiendo el sistema"
                                else if (darkModePreference == true)
                                    "Modo oscuro activado"
                                else
                                    "Modo claro activado"
                            )
                        },
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
                                checked = notifDocsEnabled,
                                onCheckedChange = { viewModel.setNotifDocsEnabled(it) }
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
                                checked = notifChatEnabled,
                                onCheckedChange = { viewModel.setNotifChatEnabled(it) }
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
                                checked = notifTasksEnabled,
                                onCheckedChange = { viewModel.setNotifTasksEnabled(it) }
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
                            context.cacheDir.deleteRecursively()
                            scope.launch {
                                snackbarHostState.showSnackbar("Cache cleared")
                            }
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
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(flowboardUrl))
                            )
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
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(flowboardUrl))
                            )
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
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(flowboardUrl))
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
