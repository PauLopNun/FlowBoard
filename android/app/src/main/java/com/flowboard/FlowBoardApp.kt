package com.flowboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.flowboard.presentation.ui.screens.auth.LoginScreen
import com.flowboard.presentation.ui.screens.tasks.TaskListScreen
import com.flowboard.presentation.viewmodel.LoginState
import com.flowboard.presentation.viewmodel.LoginViewModel

@Composable
fun FlowBoardApp(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = hiltViewModel()
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsStateWithLifecycle()

    // Navigate to tasks if already logged in
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            navController.navigate("tasks") {
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

            // Navigate to tasks when login successful
            LaunchedEffect(loginState) {
                if (loginState is LoginState.Success) {
                    navController.navigate("tasks") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }

            LoginScreen(
                onLoginClick = { email, password ->
                    loginViewModel.login(email, password)
                },
                onRegisterClick = {
                    // TODO: Navigate to register screen when implemented
                },
                isLoading = loginState is LoginState.Loading,
                error = (loginState as? LoginState.Error)?.message
            )
        }

        composable("tasks") {
            TaskListScreen(
                onTaskClick = { taskId ->
                    // TODO: Navigate to task detail when implemented
                },
                onCreateTaskClick = {
                    // TODO: Navigate to create task when implemented
                },
                onLogout = {
                    // Navigate to login screen
                    navController.navigate("login") {
                        popUpTo("tasks") { inclusive = true }
                    }
                }
            )
        }
    }
}
