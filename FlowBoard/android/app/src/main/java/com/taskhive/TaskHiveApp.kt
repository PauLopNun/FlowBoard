package com.flowboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.flowboard.presentation.ui.screens.auth.LoginScreen
import com.flowboard.presentation.ui.screens.tasks.TaskListScreen

@Composable
fun FlowBoardApp(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = modifier
    ) {
        composable("login") {
            LoginScreen(
                onLoginClick = { email, password ->
                    // Handle login logic
                    navController.navigate("tasks") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onRegisterClick = {
                    // Navigate to register screen
                }
            )
        }

        composable("tasks") {
            TaskListScreen(
                onTaskClick = { taskId ->
                    // Navigate to task detail
                },
                onCreateTaskClick = {
                    // Navigate to create task
                }
            )
        }
    }
}