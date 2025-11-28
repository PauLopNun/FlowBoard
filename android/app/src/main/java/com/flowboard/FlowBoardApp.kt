package com.flowboard

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.util.Log
import com.flowboard.presentation.ui.screens.auth.LoginScreen
import com.flowboard.presentation.ui.screens.auth.RegisterScreen
import com.flowboard.presentation.ui.screens.dashboard.DashboardScreen
import com.flowboard.presentation.ui.screens.tasks.TaskListScreen
import com.flowboard.presentation.ui.screens.tasks.CreateTaskScreen
import com.flowboard.presentation.ui.screens.tasks.TaskDetailScreen
import com.flowboard.presentation.ui.screens.documents.DocumentEditorScreen
import com.flowboard.presentation.ui.screens.documents.MyDocumentsScreen
import com.flowboard.presentation.ui.screens.notifications.NotificationCenterScreen
import com.flowboard.presentation.ui.screens.chat.ChatListScreen
import com.flowboard.presentation.ui.screens.chat.ChatScreen
import com.flowboard.presentation.ui.screens.profile.ProfileScreen
import com.flowboard.presentation.ui.screens.settings.SettingsScreen
import com.flowboard.presentation.viewmodel.LoginState
import com.flowboard.presentation.viewmodel.LoginViewModel
import com.flowboard.presentation.viewmodel.RegisterState
import com.flowboard.presentation.viewmodel.RegisterViewModel
import com.flowboard.presentation.viewmodel.TaskViewModel
import com.flowboard.presentation.viewmodel.DocumentViewModel
import com.flowboard.presentation.viewmodel.NotificationViewModel
import com.flowboard.presentation.viewmodel.ChatViewModel

@Composable
fun FlowBoardApp(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = hiltViewModel()
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsStateWithLifecycle()

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
            val context = LocalContext.current
            val activity = context as? Activity

            // Navigate to dashboard when login successful
            LaunchedEffect(loginState) {
                if (loginState is LoginState.Success) {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }

            LoginScreen(
                onLoginClick = { email, password ->
                    loginViewModel.login(email, password)
                },
                onRegisterClick = {
                    navController.navigate("register")
                },
                onForgotPasswordClick = {
                    Log.d("FlowBoardApp", "Forgot Password clicked!")
                    // navController.navigate("forgot_password") // Uncomment when screen is implemented
                },
                onGoogleSignInClick = {
                    activity?.let { loginViewModel.signInWithGoogle(it) }
                },
                isLoading = loginState is LoginState.Loading,
                error = (loginState as? LoginState.Error)?.message
            )
        }

        composable("register") {
            val registerViewModel: RegisterViewModel = hiltViewModel()
            val registerState by registerViewModel.registerState.collectAsStateWithLifecycle()

            // Navigate to dashboard when registration successful
            LaunchedEffect(registerState) {
                if (registerState is RegisterState.Success) {
                    navController.navigate("dashboard") {
                        popUpTo("register") { inclusive = true }
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
                isLoading = registerState is RegisterState.Loading,
                error = (registerState as? RegisterState.Error)?.message
            )
        }

        composable("dashboard") {
            val documentViewModel: DocumentViewModel = hiltViewModel()

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
            val taskViewModel: TaskViewModel = hiltViewModel()

            TaskListScreen(
                onTaskClick = { taskId ->
                    navController.navigate("task_detail/$taskId")
                },
                onCreateTaskClick = {
                    navController.navigate("create_task")
                },
                onDocumentsClick = {
                    navController.navigate("documents")
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
                onLogout = {
                    loginViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("tasks") { inclusive = true }
                    }
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
                }
            )
        }

        // Crear nuevo documento
        composable("document_new") {
            DocumentEditorScreen(
                documentId = null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Editar documento existente
        composable("document_edit/{documentId}") { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: return@composable

            DocumentEditorScreen(
                documentId = documentId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("create_task") {
            val taskViewModel: TaskViewModel = hiltViewModel()
            val uiState by taskViewModel.uiState.collectAsStateWithLifecycle()

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
                    navController.popBackStack()
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
                onCreateChat = {
                    navController.navigate("chat_create")
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

        // Profile screen
        composable("profile") {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Settings screen
        composable("settings") {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
