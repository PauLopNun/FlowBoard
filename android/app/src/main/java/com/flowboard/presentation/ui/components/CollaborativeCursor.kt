package com.flowboard.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.flowboard.data.models.CursorPosition
import kotlinx.coroutines.delay

/**
 * Represents a remote user's cursor in the document
 */
data class RemoteCursor(
    val userId: String,
    val userName: String,
    val color: Color,
    val position: CursorPosition?,
    val lastUpdate: Long = System.currentTimeMillis()
)

/**
 * Collaborative cursor indicator component
 * Shows where other users are typing in real-time
 */
@Composable
fun CollaborativeCursor(
    cursor: RemoteCursor,
    offsetX: Float,
    offsetY: Float,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(true) }

    // Blink animation
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 530, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_blink"
    )

    // Pulse animation for label
    val labelScale by rememberInfiniteTransition(label = "label_pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "label_scale"
    )

    LaunchedEffect(cursor.lastUpdate) {
        visible = true
        // Hide cursor after 3 seconds of inactivity
        delay(3000)
        visible = false
    }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
            .zIndex(1000f)
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            // Cursor line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(20.dp)
                    .background(cursor.color.copy(alpha = alpha))
            )

            Spacer(modifier = Modifier.height(2.dp))

            // User label
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier.scale(labelScale),
                    shape = RoundedCornerShape(4.dp),
                    color = cursor.color,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = cursor.userName,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Selection highlight for remote user
 */
@Composable
fun RemoteSelection(
    color: Color,
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .offset { IntOffset(startX.toInt(), startY.toInt()) }
            .width((endX - startX).dp)
            .height((endY - startY).dp)
            .background(color.copy(alpha = 0.2f))
            .zIndex(999f)
    )
}

/**
 * Cursor manager for multiple users
 */
@Composable
fun CollaborativeCursorsLayer(
    cursors: Map<String, RemoteCursor>,
    getCursorPosition: (RemoteCursor) -> Pair<Float, Float>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        cursors.values.forEach { cursor ->
            cursor.position?.let {
                val (x, y) = getCursorPosition(cursor)
                CollaborativeCursor(
                    cursor = cursor,
                    offsetX = x,
                    offsetY = y
                )
            }
        }
    }
}

/**
 * Get user color based on userId
 * Ensures consistent colors for each user
 */
fun getUserColor(userId: String): Color {
    val colors = listOf(
        Color(0xFFEF4444), // Red
        Color(0xFFF97316), // Orange
        Color(0xFFFBBF24), // Yellow
        Color(0xFF10B981), // Green
        Color(0xFF14B8A6), // Teal
        Color(0xFF3B82F6), // Blue
        Color(0xFF6366F1), // Indigo
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899), // Pink
    )

    val hash = userId.hashCode()
    val index = (hash and Int.MAX_VALUE) % colors.size
    return colors[index]
}
