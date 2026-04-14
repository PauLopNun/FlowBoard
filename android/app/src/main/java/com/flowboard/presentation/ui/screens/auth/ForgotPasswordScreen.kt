package com.flowboard.presentation.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowboard.presentation.viewmodel.ForgotPasswordState
import com.flowboard.presentation.viewmodel.ForgotPasswordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    onPasswordReset: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Step 1 = enter email, Step 2 = enter code + new password
    val showCodeStep = state is ForgotPasswordState.CodeSent || state is ForgotPasswordState.Error && (state as? ForgotPasswordState.Error)?.afterCodeSent == true

    LaunchedEffect(state) {
        if (state is ForgotPasswordState.Success) {
            onPasswordReset()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (showCodeStep) "Enter your reset code" else "Forgot your password?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (showCodeStep)
                    "Check your email for the 6-digit code and set a new password."
                else
                    "Enter your email address and we'll send you a 6-digit code to reset your password.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Error message
            (state as? ForgotPasswordState.Error)?.message?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        msg,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (!showCodeStep) {
                // Step 1: email input
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = state !is ForgotPasswordState.Loading
                )

                Button(
                    onClick = { viewModel.requestReset(email.trim()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = email.isNotBlank() && state !is ForgotPasswordState.Loading
                ) {
                    if (state is ForgotPasswordState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Send Reset Code")
                    }
                }
            } else {
                // Step 2: code + new password
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("6-digit code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = state !is ForgotPasswordState.Loading
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = state !is ForgotPasswordState.Loading,
                    isError = newPassword.isNotBlank() && newPassword.length < 6,
                    supportingText = { if (newPassword.isNotBlank() && newPassword.length < 6) Text("At least 6 characters") }
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm new password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = state !is ForgotPasswordState.Loading,
                    isError = confirmPassword.isNotBlank() && confirmPassword != newPassword,
                    supportingText = { if (confirmPassword.isNotBlank() && confirmPassword != newPassword) Text("Passwords do not match") }
                )

                Button(
                    onClick = { viewModel.confirmReset(email.trim(), code.trim(), newPassword) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = code.length == 6 && newPassword.length >= 6 && newPassword == confirmPassword && state !is ForgotPasswordState.Loading
                ) {
                    if (state is ForgotPasswordState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Reset Password")
                    }
                }

                TextButton(onClick = { viewModel.requestReset(email.trim()) }) {
                    Text("Resend code")
                }
            }
        }
    }
}
