package com.flowboard

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import com.flowboard.presentation.ui.screens.auth.ForgotPasswordScreen
import com.flowboard.presentation.ui.screens.auth.LoginScreen
import com.flowboard.presentation.ui.screens.auth.RegisterScreen
import com.flowboard.presentation.ui.screens.chat.ChatListScreen
import com.flowboard.presentation.ui.screens.chat.ChatScreen
import com.flowboard.presentation.ui.screens.dashboard.DashboardScreen
import com.flowboard.presentation.ui.screens.documents.CollaborativeDocumentScreenV2
import com.flowboard.presentation.ui.screens.documents.DocumentTemplate
import com.flowboard.presentation.ui.screens.documents.MyDocumentsScreen
import com.flowboard.presentation.ui.screens.documents.SearchScreen
import com.flowboard.presentation.ui.screens.documents.TemplatesBottomSheet
import com.flowboard.presentation.ui.screens.notifications.NotificationCenterScreen
import com.flowboard.presentation.ui.screens.profile.ProfileScreen
import com.flowboard.presentation.ui.screens.settings.SettingsScreen
import com.flowboard.presentation.ui.screens.tasks.CalendarScreen
import com.flowboard.presentation.ui.screens.tasks.CreateTaskScreen
import com.flowboard.presentation.ui.screens.tasks.TaskDetailScreen
import com.flowboard.presentation.ui.screens.tasks.TaskListScreen
import com.flowboard.presentation.ui.screens.workspace.WorkspaceDocumentsScreen
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
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    FlowBoardTheme(settingsViewModel = settingsViewModel) {
        val navController = rememberNavController()
        val loginViewModel: LoginViewModel = hiltViewModel()
        val isLoggedIn by loginViewModel.isLoggedIn.collectAsStateWithLifecycle()
        val documentViewModel: DocumentViewModel = hiltViewModel()
        val currentRoute by navController.currentBackStackEntryAsState()

        var splashVisible by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            delay(1_400)
            splashVisible = false
        }

        LaunchedEffect(isLoggedIn) {
            if (isLoggedIn) {
                navController.navigate("dashboard") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = modifier,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(280)) + fadeIn(animationSpec = tween(280))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(280)) + fadeOut(animationSpec = tween(280))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(280)) + fadeIn(animationSpec = tween(280))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(280)) + fadeOut(animationSpec = tween(280))
            }
        ) {
            composable("login") {
                val loginState by loginViewModel.loginState.collectAsStateWithLifecycle()
                val googleError by loginViewModel.googleSignInError.collectAsStateWithLifecycle()
                val context = LocalContext.current
                val activity = context as? Activity
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(loginState) {
                    if (loginState is LoginState.Success) {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }

                LaunchedEffect(googleError) {
                    googleError?.let {
                        snackbarHostState.showSnackbar(it)
                        loginViewModel.clearGoogleSignInError()
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(data) } }
                ) { contentPadding ->
                    LoginScreen(
                        modifier = Modifier.padding(contentPadding),
                        onLoginClick = { email, password -> loginViewModel.login(email, password) },
                        onRegisterClick = { navController.navigate("register") },
                        onForgotPasswordClick = { navController.navigate("forgot_password") },
                        onGoogleSignInClick = { activity?.let { loginViewModel.signInWithGoogle(it) } },
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
                    onLoginClick = { navController.popBackStack() },
                    onGoogleSignInClick = { activity?.let { loginViewModel.signInWithGoogle(it) } },
                    isLoading = registerState is RegisterState.Loading || loginState is LoginState.Loading,
                    error = (registerState as? RegisterState.Error)?.message ?: (loginState as? LoginState.Error)?.message
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
                LaunchedEffect(currentRoute?.destination?.route) {
                    if (currentRoute?.destination?.route == "dashboard") {
                        documentViewModel.fetchAllDocuments()
                    }
                }

                DashboardScreen(
                    onDocumentClick = { documentId -> navController.navigate("document_edit/$documentId") },
                    onCreateDocument = { navController.navigate("document_new") },
                    onViewAllDocuments = { navController.navigate("my_documents") },
                    onNotificationsClick = { navController.navigate("notifications") },
                    onChatClick = { navController.navigate("chat_list") },
                    onProfileClick = { navController.navigate("profile") },
                    onSettingsClick = { navController.navigate("settings") },
                    onTasksClick = { navController.navigate("tasks") },
                    onCalendarClick = { navController.navigate("calendar") },
                    onWorkspaceClick = { navController.navigate("workspaces") },
                    onProjectsClick = { navController.navigate("projects") },
                    onEditorDemoClick = { navController.navigate("my_documents") },
                    onSearchClick = { navController.navigate("search") },
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
                    onTaskClick = { taskId -> navController.navigate("task_detail/$taskId") },
                    onCreateTaskClick = { navController.navigate("create_task") },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("search") {
                SearchScreen(
                    onDocumentClick = { documentId ->
                        navController.navigate("document_edit/$documentId")
                    },
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = documentViewModel
                )
            }

            composable("my_documents") {
                MyDocumentsScreen(
                    onDocumentClick = { documentId -> navController.navigate("document_edit/$documentId") },
                    onCreateDocument = { navController.navigate("document_new") },
                    onNavigateBack = { navController.popBackStack() },
                    onToggleStar = { documentViewModel.toggleStar(it) },
                    viewModel = documentViewModel
                )
            }

            composable(
                route = "document_new?workspaceId={workspaceId}",
                arguments = listOf(
                    navArgument("workspaceId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val initialWorkspaceId = backStackEntry.arguments?.getString("workspaceId")
                val docListState by documentViewModel.documentListState.collectAsStateWithLifecycle()
                var title by remember { mutableStateOf("") }
                var isCreating by remember { mutableStateOf(false) }
                var showTemplates by remember { mutableStateOf(false) }
                var selectedTemplate by remember { mutableStateOf<DocumentTemplate?>(null) }

                LaunchedEffect(docListState.error) {
                    if (docListState.error != null && isCreating) {
                        isCreating = false
                    }
                }

                AlertDialog(
                    onDismissRequest = { if (!isCreating) navController.popBackStack() },
                    title = { Text("New Document") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Document Title") },
                                singleLine = true,
                                enabled = !isCreating
                            )
                            if (selectedTemplate != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(selectedTemplate!!.emoji)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            selectedTemplate!!.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(
                                            onClick = { selectedTemplate = null },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                        ) {
                                            Text("Remove", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            } else {
                                TextButton(
                                    onClick = { showTemplates = true },
                                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                                    enabled = !isCreating
                                ) {
                                    Text("Use a template", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val docTitle = title.trim().ifBlank {
                                    selectedTemplate?.name ?: "Untitled Document"
                                }
                                isCreating = true
                                val templateId = selectedTemplate?.id
                                val wsVisibility = if (initialWorkspaceId != null) "workspace" else "private"
                                documentViewModel.createDocumentViaApi(
                                    title = docTitle,
                                    visibility = wsVisibility,
                                    workspaceId = initialWorkspaceId
                                ) { documentId ->
                                    val route = if (templateId != null)
                                        "document_edit/$documentId?template=$templateId"
                                    else
                                        "document_edit/$documentId"
                                    navController.navigate(route) {
                                        popUpTo("document_new?workspaceId={workspaceId}") { inclusive = true }
                                    }
                                }
                            },
                            enabled = !isCreating
                        ) {
                            if (isCreating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                Text("Create")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { navController.popBackStack() },
                            enabled = !isCreating
                        ) {
                            Text("Cancel")
                        }
                    }
                )

                if (showTemplates) {
                    TemplatesBottomSheet(
                        onDismiss = { showTemplates = false },
                        onSelectTemplate = { template ->
                            selectedTemplate = template
                            if (title.isBlank()) title = template.name
                            showTemplates = false
                        }
                    )
                }
            }

            composable(
                route = "document_edit/{documentId}?template={templateId}",
                arguments = listOf(
                    navArgument("documentId") { type = NavType.StringType },
                    navArgument("templateId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val documentId = backStackEntry.arguments?.getString("documentId") ?: return@composable
                val templateId = backStackEntry.arguments?.getString("templateId")

                CollaborativeDocumentScreenV2(
                    documentId = documentId,
                    templateId = templateId,
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

                LaunchedEffect(uiState.message) {
                    if (uiState.message == "Task created successfully") {
                        taskViewModel.clearMessage()
                        navController.popBackStack()
                    }
                }

                CreateTaskScreen(
                    onCreateTask = { t, d, p, du, e, s, en, l ->
                        taskViewModel.createTask(t, d, p, du, e, s, en, l)
                    },
                    onNavigateBack = { navController.popBackStack() },
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
                    onUpdateTask = { taskViewModel.updateTask(it) },
                    onDeleteTask = { taskViewModel.deleteTask(it) },
                    onNavigateBack = { navController.popBackStack() },
                    isLoading = uiState.isLoading
                )
            }

            composable("notifications") {
                val notificationViewModel: NotificationViewModel = hiltViewModel()
                val notifications by notificationViewModel.allNotifications.collectAsStateWithLifecycle()
                val unreadCount by notificationViewModel.unreadCount.collectAsStateWithLifecycle()

                NotificationCenterScreen(
                    notifications = notifications,
                    unreadCount = unreadCount,
                    onNotificationClick = { notification ->
                        notificationViewModel.markAsRead(notification.id)
                        notification.deepLink?.let { navController.navigate(it) }
                    },
                    onMarkAsRead = { notificationViewModel.markAsRead(it) },
                    onMarkAllAsRead = { notificationViewModel.markAllAsRead() },
                    onDeleteNotification = { notificationViewModel.deleteNotification(it) },
                    onDeleteAll = { notificationViewModel.deleteAllNotifications() },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("chat_list") {
                val chatViewModel: ChatViewModel = hiltViewModel()
                ChatListScreen(
                    viewModel = chatViewModel,
                    onChatClick = { navController.navigate("chat/$it") },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("chat/{chatId}") { backStackEntry ->
                val chatViewModel: ChatViewModel = hiltViewModel()
                val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable

                LaunchedEffect(chatId) {
                    chatViewModel.selectChat(chatId)
                }

                ChatScreen(
                    chatRoomId = chatId,
                    viewModel = chatViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("calendar") {
                CalendarScreen(
                    onTaskClick = { navController.navigate("task_detail/$it") },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("workspaces") {
                WorkspaceScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onWorkspaceClick = { navController.navigate("workspace_docs/$it") }
                )
            }

            composable("workspace_docs/{workspaceId}") { backStackEntry ->
                val workspaceId = backStackEntry.arguments?.getString("workspaceId") ?: ""
                WorkspaceDocumentsScreen(
                    workspaceId = workspaceId,
                    onNavigateBack = { navController.popBackStack() },
                    onDocumentClick = { navController.navigate("document_edit/$it") },
                    onCreateDocument = { wsId -> navController.navigate("document_new?workspaceId=$wsId") },
                    onChatClick = { chatId -> navController.navigate("chat/$chatId") }
                )
            }

            composable("projects") {
                com.flowboard.presentation.ui.screens.projects.ProjectListScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("profile") {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onLogout = {
                        loginViewModel.logout()
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = settingsViewModel
                )
            }
        }

        AnimatedVisibility(
            visible = splashVisible,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(400))
        ) {
            SplashScreen()
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = com.flowboard.R.drawable.app_logo),
                contentDescription = "FlowBoard Logo",
                modifier = Modifier.size(120.dp)
            )
        }
    }
}
