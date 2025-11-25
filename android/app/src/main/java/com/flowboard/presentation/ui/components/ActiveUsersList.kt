package com.flowboard.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowboard.data.remote.dto.UserPresenceInfo
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Lista horizontal de avatares de usuarios activos
 *
 * Características:
 * - Muestra hasta 5 avatares superpuestos
 * - Si hay más de 5, muestra "+N" adicional
 * - Indicador verde para usuarios online
 */
@Composable
fun ActiveUsersList(
    users: List<UserPresenceInfo>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 5
) {
    if (users.isEmpty()) return

    Row(
        modifier = modifier
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mostrar hasta maxVisible usuarios
        users.take(maxVisible).forEach { user ->
            UserAvatar(
                user = user,
                size = 32.dp
            )
        }

        // Si hay más usuarios, mostrar contador
        if (users.size > maxVisible) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${users.size - maxVisible}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Avatar de usuario con indicador de presencia
 */
@Composable
fun UserAvatar(
    user: UserPresenceInfo,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Mostrar iniciales del usuario
        val initials = user.fullName
            .split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
            .ifEmpty { user.username.take(2).uppercase() }

        Text(
            text = initials,
            style = MaterialTheme.typography.labelMedium,
            fontSize = (size.value / 2.5).sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )

        // Indicador de online (círculo verde en la esquina)
        if (user.isOnline) {
            Box(
                modifier = Modifier
                    .size(size / 3.5f)
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50))
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

/**
 * Preview de usuarios activos
 */
@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ActiveUsersListPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 2 usuarios
            ActiveUsersList(
                users = listOf(
                    UserPresenceInfo(
                        userId = "1",
                        username = "john_doe",
                        fullName = "John Doe",
                        profileImageUrl = null,
                        isOnline = true,
                        lastActivity = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    ),
                    UserPresenceInfo(
                        userId = "2",
                        username = "jane_smith",
                        fullName = "Jane Smith",
                        profileImageUrl = null,
                        isOnline = true,
                        lastActivity = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    )
                )
            )

            // 6 usuarios (muestra +1)
            ActiveUsersList(
                users = List(6) { index ->
                    UserPresenceInfo(
                        userId = index.toString(),
                        username = "user$index",
                        fullName = "User $index",
                        profileImageUrl = null,
                        isOnline = index % 2 == 0,
                        lastActivity = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    )
                }
            )
        }
    }
}
