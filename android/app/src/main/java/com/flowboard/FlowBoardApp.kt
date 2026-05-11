package com.flowboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import com.flowboard.data.models.crdt.ContentBlock
import com.flowboard.domain.model.NotificationType
import com.itextpdf.kernel.pdf.PdfDocument as ITextPdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
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
                    onWorkspaceSelected = { workspaceId -> navController.navigate("workspace_docs/$workspaceId") },
                    onCreateWorkspaceDocument = { workspaceId -> navController.navigate("document_new?workspaceId=$workspaceId") },
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
                var importedContent by remember { mutableStateOf("") }
                var importedFileName by remember { mutableStateOf<String?>(null) }
                val context = LocalContext.current
                val importLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    uri ?: return@rememberLauncherForActivityResult
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    val displayName = displayNameFromUri(context, uri)
                    val name = displayName
                        ?.substringBeforeLast(".")
                        ?.takeIf { it.isNotBlank() }
                        ?: uri.lastPathSegment
                            ?.substringAfterLast("/")
                            ?.substringBeforeLast(".")
                            ?.takeIf { it.isNotBlank() }
                    val isPdf = isPdfUri(context, uri, displayName)
                    val rawText = if (isPdf) {
                        readPdfTextFromUri(context, uri).trim()
                    } else {
                        readTextFromUri(context, uri).trim()
                    }
                    if (rawText.isNotBlank()) {
                        if (title.isBlank() && name != null) title = name
                        val finalTitle = title.trim().ifBlank { name ?: "Imported Document" }
                        importedContent = markdownToFlowBoardContent(finalTitle, rawText)
                        importedFileName = displayName ?: name ?: "Imported file"
                    } else if (isPdf) {
                        val fallbackName = displayName ?: name ?: "Imported PDF"
                        if (title.isBlank() && name != null) title = name
                        val finalTitle = title.trim().ifBlank { name ?: "Imported PDF" }
                        importedContent = pdfFallbackContent(finalTitle, fallbackName)
                        importedFileName = fallbackName
                    }
                }

                LaunchedEffect(docListState.error) {
                    if (docListState.error != null && isCreating) {
                        isCreating = false
                    }
                }

                fun closeNewDocument() {
                    if (!navController.popBackStack()) {
                        navController.navigate("dashboard") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }

                // Intercept system back gesture — without this, the dialog dismisses
                // but the route stays alive showing a blank white screen.
                BackHandler(enabled = !isCreating) { closeNewDocument() }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AlertDialog(
                        onDismissRequest = { if (!isCreating) closeNewDocument() },
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
                                OutlinedButton(
                                    onClick = { importLauncher.launch(arrayOf("application/pdf", "text/*", "text/markdown", "application/octet-stream")) },
                                    enabled = !isCreating && selectedTemplate == null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (importedFileName == null) "Import PDF/Markdown/Text" else "Imported: $importedFileName")
                                }
                                if (importedFileName != null) {
                                    TextButton(
                                        onClick = {
                                            importedContent = ""
                                            importedFileName = null
                                        },
                                        enabled = !isCreating
                                    ) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Remove import")
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val docTitle = title.trim().ifBlank {
                                        selectedTemplate?.name ?: importedFileName ?: "Untitled Document"
                                    }
                                    isCreating = true
                                    val templateId = selectedTemplate?.id
                                    val wsVisibility = if (initialWorkspaceId != null) "workspace" else "private"
                                    val content = importedContent.ifBlank { "" }
                                    documentViewModel.createDocumentViaApi(
                                        title = docTitle,
                                        content = content,
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
                                onClick = { closeNewDocument() },
                                enabled = !isCreating
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }

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

                LaunchedEffect(Unit) {
                    notificationViewModel.refresh()
                }

                NotificationCenterScreen(
                    notifications = notifications,
                    unreadCount = unreadCount,
                    onNotificationClick = { notification ->
                        val isInvitation = notification.type == NotificationType.WORKSPACE_INVITATION ||
                            (notification.type == NotificationType.DOCUMENT_SHARED &&
                                notification.title.contains("invitation", ignoreCase = true))
                        if (!isInvitation) {
                            notificationViewModel.markAsRead(notification.id)
                            notification.deepLink?.let { navController.navigate(it.substringBefore("?")) }
                        }
                    },
                    onMarkAsRead = { notificationViewModel.markAsRead(it) },
                    onMarkAllAsRead = { notificationViewModel.markAllAsRead() },
                    onDeleteNotification = { notificationViewModel.deleteNotification(it) },
                    onDeleteAll = { notificationViewModel.deleteAllNotifications() },
                    onAcceptInvitation = { notification ->
                        notificationViewModel.acceptInvitation(notification.id) {
                            documentViewModel.fetchAllDocuments()
                            when (notification.type) {
                                NotificationType.WORKSPACE_INVITATION -> navController.navigate("workspaces")
                                NotificationType.DOCUMENT_SHARED -> notification.resourceId?.let {
                                    navController.navigate("document_edit/$it")
                                }
                                else -> Unit
                            }
                        }
                    },
                    onDeclineInvitation = { notificationViewModel.declineInvitation(it.id) },
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

private fun readTextFromUri(context: Context, uri: Uri): String {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
    }.getOrDefault("")
}

private fun readPdfTextFromUri(context: Context, uri: Uri): String {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val pdfDocument = ITextPdfDocument(PdfReader(inputStream))
            try {
                (1..pdfDocument.numberOfPages)
                    .joinToString("\n\n") { pageNumber ->
                        PdfTextExtractor.getTextFromPage(pdfDocument.getPage(pageNumber)).trim()
                    }
                    .trim()
            } finally {
                pdfDocument.close()
            }
        }.orEmpty()
    }.getOrDefault("")
}

private fun displayNameFromUri(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        }
    }.getOrNull()
}

private fun isPdfUri(context: Context, uri: Uri, displayName: String?): Boolean {
    val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
    return mimeType == "application/pdf" || displayName?.endsWith(".pdf", ignoreCase = true) == true
}

private fun pdfFallbackContent(title: String, fileName: String): String {
    return markdownToFlowBoardContent(
        title,
        "PDF imported: $fileName\n\nFlowBoard could not extract selectable text from this PDF. It may be scanned, protected, or image-only."
    )
}

private fun markdownToFlowBoardContent(title: String, rawText: String): String {
    val blocks = mutableListOf<ContentBlock>()
    blocks += ContentBlock(
        id = UUID.randomUUID().toString(),
        type = "h1",
        content = title.ifBlank { "Imported Document" }
    )

    val lines = rawText.replace("\r\n", "\n").split("\n")
    var inCode = false
    val codeBuffer = StringBuilder()

    fun addCodeBlock() {
        blocks += ContentBlock(
            id = UUID.randomUUID().toString(),
            type = "code",
            content = codeBuffer.toString().trimEnd()
        )
        codeBuffer.clear()
    }

    lines.forEach { rawLine ->
        val line = rawLine.trimEnd()
        if (line.trim().startsWith("```")) {
            if (inCode) addCodeBlock()
            inCode = !inCode
            return@forEach
        }
        if (inCode) {
            codeBuffer.appendLine(line)
            return@forEach
        }

        val trimmed = line.trim()
        if (trimmed.isBlank()) return@forEach

        val block = when {
            trimmed.startsWith("### ") -> ContentBlock(UUID.randomUUID().toString(), "h3", trimmed.removePrefix("### ").trim())
            trimmed.startsWith("## ") -> ContentBlock(UUID.randomUUID().toString(), "h2", trimmed.removePrefix("## ").trim())
            trimmed.startsWith("# ") -> ContentBlock(UUID.randomUUID().toString(), "h1", trimmed.removePrefix("# ").trim())
            trimmed.startsWith("- [ ] ", ignoreCase = true) -> ContentBlock(UUID.randomUUID().toString(), "todo", trimmed.drop(6).trim(), isChecked = false)
            trimmed.startsWith("- [x] ", ignoreCase = true) -> ContentBlock(UUID.randomUUID().toString(), "todo", trimmed.drop(6).trim(), isChecked = true)
            trimmed.startsWith("- ") -> ContentBlock(UUID.randomUUID().toString(), "bullet", trimmed.removePrefix("- ").trim())
            trimmed.startsWith("* ") -> ContentBlock(UUID.randomUUID().toString(), "bullet", trimmed.removePrefix("* ").trim())
            Regex("""^\d+\.\s+""").containsMatchIn(trimmed) -> ContentBlock(
                UUID.randomUUID().toString(),
                "numbered",
                trimmed.replaceFirst(Regex("""^\d+\.\s+"""), "").trim()
            )
            trimmed.startsWith("> ") -> ContentBlock(UUID.randomUUID().toString(), "quote", trimmed.removePrefix("> ").trim())
            trimmed == "---" || trimmed == "***" -> ContentBlock(UUID.randomUUID().toString(), "divider", "")
            else -> ContentBlock(UUID.randomUUID().toString(), "p", trimmed)
        }
        blocks += block
    }

    if (inCode && codeBuffer.isNotBlank()) {
        addCodeBlock()
    }

    return Json { encodeDefaults = true }.encodeToString(blocks)
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
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
