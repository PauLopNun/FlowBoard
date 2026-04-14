package com.flowboard

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.flowboard.presentation.ui.screens.auth.ForgotPasswordScreen
import com.flowboard.presentation.ui.screens.auth.LoginScreen
import com.flowboard.presentation.ui.screens.auth.RegisterScreen
import com.flowboard.presentation.ui.screens.chat.ChatListScreen
import com.flowboard.presentation.ui.screens.chat.ChatScreen
import com.flowboard.presentation.ui.screens.dashboard.DashboardScreen
import com.flowboard.presentation.ui.screens.documents.CollaborativeDocumentScreenV2
import com.flowboard.presentation.ui.screens.documents.MyDocumentsScreen
import com.flowboard.presentation.ui.screens.notifications.NotificationCenterScreen
import com.flowboard.presentation.ui.screens.profile.ProfileScreen
import com.flowboard.presentation.ui.screens.settings.SettingsScreen
import com.flowboard.presentation.ui.screens.tasks.CalendarScreen
import com.flowboard.presentation.ui.screens.tasks.CreateTaskScreen
import com.flowboard.presentation.ui.screens.tasks.TaskDetailScreen
import com.flowboard.presentation.ui.screens.tasks.TaskListScreen
import com.flowboard.presentation.ui.screens.workspace.WorkspaceScreen
import com.flowboard.presentation.ui.theme.FlowBoardTheme
import com.flowboard.presentation.viewmodel.ChatViewModel
import com.flowboard.presentation.viewmodel.DocumentViewModel
import com.flowboard.presentation.viewmodel.LoginState
import com.flowboard.presentation.viewmodel.LoginViewModel
import com.flowboard.presentation.viewmodel.NotificationViewModel
import com.flowboard.presentation.viewmodel.RegisterState
import com.flowboard.presentation.viewmodel.RegisterViewModel
import com.flowboard.presentation.viewmodel.SettingsViewModel
import com.flowboard.presentation.viewmodel.TaskViewModel

@Composable
fun FlowBoardApp(
    modifier: Modifier = Modifier
) {
    // Hoist SettingsViewModel here so FlowBoardTheme and SettingsScreen share the SAME instance
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    FlowBoardTheme(settingsViewModel = settingsViewModel) {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = hiltViewModel()
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsStateWithLifecycle()
    // Shared DocumentViewModel — hoisted so all screens see the same state instantly
    val documentViewModel: DocumentViewModel = hiltViewModel()
    val currentRoute by navController.currentBackStackEntryAsState()

    // Navigate to dashboard if already logged in
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            navController.navigate("dashboard") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = modifier
    ) {
        composable("login") {
            val loginState by loginViewModel.loginState.collectAsStateWithLifecycle()
            val googleError by loginViewModel.googleSignInError.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val activity = context as? Activity
            val snackbarHostState = remember { SnackbarHostState() }

            // Navigate to dashboard when login successful
            LaunchedEffect(loginState) {
                if (loginState is LoginState.Success) {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }

            // Show Google Sign-In errors as Snackbar so they don't block the form
            LaunchedEffect(googleError) {
                googleError?.let {
                    snackbarHostState.showSnackbar(it)
                    loginViewModel.clearGoogleSignInError()
                }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(data) } }
            ) { _ ->
                LoginScreen(
                    onLoginClick = { email, password ->
                        loginViewModel.login(email, password)
                    },
                    onRegisterClick = {
                        navController.navigate("register")
                    },
                    onForgotPasswordClick = {
                        navController.navigate("forgot_password")
                    },
                    onGoogleSignInClick = {
                        activity?.let { loginViewModel.signInWithGoogle(it) }
                    },
                    isLoading = loginState is LoginState.Loading,
                    error = (loginState as? LoginState.Error)?.message
                )
            }
        }

        composable("register") {
            val registerViewModel: RegisterViewModel = hiltViewModel()
            val registerState by registerViewModel.registerState.collectAsStateWithLifecycle()
            val loginState by loginViewModel.loginState.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val activity = context as? Activity

            // Navigate to dashboard when registration or Google sign-in successful
            LaunchedEffect(registerState) {
                if (registerState is RegisterState.Success) {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }
            LaunchedEffect(loginState) {
                if (loginState is LoginState.Success) {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }

            RegisterScreen(
                onRegisterClick = { email, password, username, fullName ->
                    registerViewModel.register(email, password, username, fullName)
                },
                onLoginClick = {
                    navController.popBackStack()
                },
                onGoogleSignInClick = {
                    activity?.let { loginViewModel.signInWithGoogle(it) }
                },
                isLoading = registerState is RegisterState.Loading || loginState is LoginState.Loading,
                error = (registerState as? RegisterState.Error)?.message
                    ?: (loginState as? LoginState.Error)?.message
            )
        }

        composable("forgot_password") {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onPasswordReset = {
                    navController.navigate("login") {
                        popUpTo("forgot_password") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            // Re-fetch documents whenever the dashboard becomes the active destination
            LaunchedEffect(currentRoute?.destination?.route) {
                if (currentRoute?.destination?.route == "dashboard") {
                    documentViewModel.fetchAllDocuments()
                }
            }

            DashboardScreen(
                onDocumentClick = { documentId ->
                    navController.navigate("document_edit/$documentId")
                },
                onCreateDocument = {
                    navController.navigate("document_new")
                },
                onViewAllDocuments = {
                    navController.navigate("my_documents")
                },
                onNotificationsClick = {
                    navController.navigate("notifications")
                },
                onChatClick = {
                    navController.navigate("chat_list")
                },
                onProfileClick = {
                    navController.navigate("profile")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                },
                onTasksClick = {
                    navController.navigate("tasks")
                },
                onCalendarClick = {
                    navController.navigate("calendar")
                },
                onWorkspaceClick = {
                    navController.navigate("workspaces")
                },
                onEditorDemoClick = {
                    navController.navigate("my_documents")
                },
                onLogout = {
                    loginViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                },
                documentViewModel = documentViewModel,
                loginViewModel = loginViewModel
            )
        }

        composable("tasks") {
            TaskListScreen(
                onTaskClick = { taskId ->
                    navController.navigate("task_detail/$taskId")
                },
                onCreateTaskClick = {
                    navController.navigate("create_task")
                },
                onNotificationsClick = {
                    navController.navigate("notifications")
                },
                onChatClick = {
                    navController.navigate("chat_list")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // My Documents - Lista de documentos guardados
        composable("my_documents") {
            MyDocumentsScreen(
                onDocumentClick = { documentId ->
                    navController.navigate("document_edit/$documentId")
                },
                onCreateDocument = {
                    navController.navigate("document_new")
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = documentViewModel
            )
        }

        // Crear nuevo documento: show title dialog, create via API, then open collaborative editor
        composable("document_new") {
            // Use the shared documentViewModel so the new doc is immediately visible everywhere
            val docListState by documentViewModel.documentListState.collectAsStateWithLifecycle()
            var title by remember { mutableStateOf("") }
            var isCreating by remember { mutableStateOf(false) }

            // Reset spinner if creation failed so user can retry
            LaunchedEffect(docListState.error) {
                if (docListState.error != null && isCreating) {
                    isCreating = false
                }
            }

            AlertDialog(
                onDismissRequest = {
                    if (!isCreating) navController.popBackStack()
                },
                title = { androidx.compose.material3.Text("New Document") },
                text = {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { androidx.compose.material3.Text("Document Title") },
                        singleLine = true,
                        enabled = !isCreating
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val docTitle = title.trim().ifBlank { "Untitled Document" }
                            isCreating = true
                            documentViewModel.createDocumentViaApi(docTitle) { documentId ->
                                navController.navigate("document_edit/$documentId") {
                                    popUpTo("document_new") { inclusive = true }
                                }
                            }
                        },
                        enabled = !isCreating
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            androidx.compose.material3.Text("Create")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { navController.popBackStack() },
                        enabled = !isCreating
                    ) {
                        androidx.compose.material3.Text("Cancel")
                    }
                }
            )
        }

        // Abrir documento existente en el editor colaborativo en tiempo real
        composable("document_edit/{documentId}") { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: return@composable

            CollaborativeDocumentScreenV2(
                documentId = documentId,
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate("dashboard") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onNavigateToDocument = { newDocId ->
                    navController.navigate("document_edit/$newDocId")
                }
            )
        }

        composable("create_task") {
            val taskViewModel: TaskViewModel = hiltViewModel()
            val uiState by taskViewModel.uiState.collectAsStateWithLifecycle()

            // Navigate back only after task is successfully created
            LaunchedEffect(uiState.message) {
                if (uiState.message == "Task created successfully") {
                    taskViewModel.clearMessage()
                    navController.popBackStack()
                }
            }

            CreateTaskScreen(
                onCreateTask = { title, description, priority, dueDate, isEvent, eventStartTime, eventEndTime, location ->
                    taskViewModel.createTask(
                        title = title,
                        description = description,
                        priority = priority,
                        dueDate = dueDate,
                        isEvent = isEvent,
                        eventStartTime = eventStartTime,
                        eventEndTime = eventEndTime,
                        location = location
                    )
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                isLoading = uiState.isLoading
            )
        }

        composable("task_detail/{taskId}") { backStackEntry ->
            val taskViewModel: TaskViewModel = hiltViewModel()
            val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
            val uiState by taskViewModel.uiState.collectAsStateWithLifecycle()
            val activeUsers by taskViewModel.activeUsers.collectAsStateWithLifecycle()

            LaunchedEffect(taskId) {
                taskViewModel.loadTaskById(taskId)
            }

            TaskDetailScreen(
                task = uiState.selectedTask,
                activeUsers = activeUsers,
                onUpdateTask = { task ->
                    taskViewModel.updateTask(task)
                },
                onDeleteTask = { id ->
                    taskViewModel.deleteTask(id)
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                isLoading = uiState.isLoading
            )
        }

        // Notifications screen
        composable("notifications") {
            val notificationViewModel: NotificationViewModel = hiltViewModel()
            val notifications by notificationViewModel.allNotifications.collectAsStateWithLifecycle()
            val unreadCount by notificationViewModel.unreadCount.collectAsStateWithLifecycle()

            NotificationCenterScreen(
                notifications = notifications,
                unreadCount = unreadCount,
                onNotificationClick = { notification ->
                    notificationViewModel.markAsRead(notification.id)
                    // Navigate to resource if deepLink is available
                    notification.deepLink?.let { navController.navigate(it) }
                },
                onMarkAsRead = { notificationId ->
                    notificationViewModel.markAsRead(notificationId)
                },
                onMarkAllAsRead = {
                    notificationViewModel.markAllAsRead()
                },
                onDeleteNotification = { notificationId ->
                    notificationViewModel.deleteNotification(notificationId)
                },
                onDeleteAll = {
                    notificationViewModel.deleteAllNotifications()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Chat list screen
        composable("chat_list") {
            val chatViewModel: ChatViewModel = hiltViewModel()

            ChatListScreen(
                viewModel = chatViewModel,
                onChatClick = { chatId ->
                    navController.navigate("chat/$chatId")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Chat screen
        composable("chat/{chatId}") { backStackEntry ->
            val chatViewModel: ChatViewModel = hiltViewModel()
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable

            LaunchedEffect(chatId) {
                chatViewModel.selectChat(chatId)
            }

            ChatScreen(
                chatRoomId = chatId,
                viewModel = chatViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Calendar screen
        composable("calendar") {
            CalendarScreen(
                onTaskClick = { taskId ->
                    navController.navigate("task_detail/$taskId")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Workspaces screen
        composable("workspaces") {
            WorkspaceScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onWorkspaceClick = { workspaceId ->
                    navController.navigate("workspace_docs/$workspaceId")
                }
            )
        }

        // Workspace documents — shows docs for a specific workspace
        composable("workspace_docs/{workspaceId}") { backStackEntry ->
            val workspaceId = backStackEntry.arguments?.getString("workspaceId") ?: return@composable
            MyDocumentsScreen(
                onDocumentClick = { documentId ->
                    navController.navigate("document_edit/$documentId")
                },
                onCreateDocument = {
                    navController.navigate("document_new")
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = documentViewModel
            )
        }

        // Profile screen
        composable("profile") {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Settings screen — reuses the same SettingsViewModel instance as FlowBoardTheme
        composable("settings") {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = settingsViewModel
            )
        }
    }
    } // end FlowBoardTheme
}
