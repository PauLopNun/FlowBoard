package com.flowboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowboard.domain.model.*
import com.flowboard.domain.repository.NotificationRepository
import com.flowboard.notification.FlowBoardNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing notifications
 */
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val notificationManager: FlowBoardNotificationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val currentUserId = MutableStateFlow<String?>(null)

    val allNotifications: StateFlow<List<Notification>> = currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            notificationRepository.getAllNotifications(userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unreadNotifications: StateFlow<List<Notification>> = currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            notificationRepository.getUnreadNotifications(userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unreadCount: StateFlow<Int> = currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            notificationRepository.getUnreadCount(userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    /**
     * Set current user
     */
    fun setCurrentUser(userId: String) {
        currentUserId.value = userId
    }

    /**
     * Mark notification as read
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.markAsRead(notificationId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to mark as read")
                }
            }
        }
    }

    /**
     * Mark all notifications as read
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            currentUserId.value?.let { userId ->
                try {
                    notificationRepository.markAllAsRead(userId)
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(error = e.message ?: "Failed to mark all as read")
                    }
                }
            }
        }
    }

    /**
     * Delete notification
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.deleteNotification(notificationId)
                notificationManager.cancelNotification(notificationId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to delete notification")
                }
            }
        }
    }

    /**
     * Delete all notifications
     */
    fun deleteAllNotifications() {
        viewModelScope.launch {
            currentUserId.value?.let { userId ->
                try {
                    notificationRepository.deleteAllNotifications(userId)
                    notificationManager.cancelAllNotifications()
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(error = e.message ?: "Failed to delete all notifications")
                    }
                }
            }
        }
    }

    /**
     * Clean up old notifications
     */
    fun cleanupOldNotifications() {
        viewModelScope.launch {
            currentUserId.value?.let { userId ->
                try {
                    notificationRepository.deleteOldNotifications(userId)
                } catch (e: Exception) {
                    // Silent failure for cleanup
                }
            }
        }
    }

    /**
     * Get grouped notifications for display
     */
    fun getGroupedNotifications(): StateFlow<List<NotificationGroup>> {
        return allNotifications.map { notifications ->
            notifications
                .groupBy { it.type }
                .map { (type, typeNotifications) ->
                    NotificationGroup(
                        type = type,
                        count = typeNotifications.size,
                        latestNotification = typeNotifications.first(),
                        notifications = typeNotifications
                    )
                }
                .sortedByDescending { it.latestNotification.createdAt }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    /**
     * Create and show a notification
     */
    fun createNotification(
        type: NotificationType,
        title: String,
        message: String,
        priority: NotificationPriority = NotificationPriority.MEDIUM,
        resourceId: String? = null,
        resourceType: ResourceType? = null,
        actionUserId: String? = null,
        actionUserName: String? = null,
        metadata: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch {
            currentUserId.value?.let { userId ->
                val notification = Notification(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = userId,
                    type = type,
                    priority = priority,
                    title = title,
                    message = message,
                    resourceId = resourceId,
                    resourceType = resourceType,
                    actionUserId = actionUserId,
                    actionUserName = actionUserName,
                    metadata = metadata,
                    createdAt = System.currentTimeMillis()
                )

                try {
                    notificationRepository.createNotification(notification)
                    notificationManager.showNotification(notification)
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(error = e.message ?: "Failed to create notification")
                    }
                }
            }
        }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI state for notifications
 */
data class NotificationUiState(
    val error: String? = null
)
