package com.flowboard.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.flowboard.data.remote.websocket.WebSocketState

/**
 * Banner que muestra el estado de conexión WebSocket
 *
 * Se muestra animado cuando:
 * - Está conectando
 * - Está reconectando
 * - Hay un error
 * - Está desconectado
 *
 * Se oculta cuando la conexión es exitosa
 */
@Composable
fun ConnectionStatusBanner(
    connectionState: WebSocketState,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = connectionState !is WebSocketState.Connected,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        when (connectionState) {
            is WebSocketState.Connecting -> {
                StatusBanner(
                    message = "Conectando al servidor...",
                    icon = Icons.Default.CloudSync,
                    color = MaterialTheme.colorScheme.primary,
                    showProgress = true
                )
            }

            is WebSocketState.Reconnecting -> {
                StatusBanner(
                    message = "Reconectando (intento ${connectionState.attempt}/${connectionState.maxAttempts})...",
                    icon = Icons.Default.CloudSync,
                    color = MaterialTheme.colorScheme.secondary,
                    showProgress = true
                )
            }

            is WebSocketState.Disconnected -> {
                StatusBanner(
                    message = "Sin conexión al servidor",
                    icon = Icons.Default.CloudOff,
                    color = MaterialTheme.colorScheme.error,
                    action = {
                        TextButton(onClick = onReconnect) {
                            Text("Reconectar", color = MaterialTheme.colorScheme.onError)
                        }
                    }
                )
            }

            is WebSocketState.Error -> {
                StatusBanner(
                    message = "Error: ${connectionState.message}",
                    icon = Icons.Default.Error,
                    color = MaterialTheme.colorScheme.error,
                    action = if (connectionState.isRecoverable) {
                        {
                            TextButton(onClick = onReconnect) {
                                Text("Reintentar", color = MaterialTheme.colorScheme.onError)
                            }
                        }
                    } else null
                )
            }

            else -> { /* Connected - no mostrar banner */ }
        }
    }
}

@Composable
private fun StatusBanner(
    message: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    action: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.95f),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (showProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (showProgress) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onError
                )
            }

            action?.invoke()
        }
    }
}
