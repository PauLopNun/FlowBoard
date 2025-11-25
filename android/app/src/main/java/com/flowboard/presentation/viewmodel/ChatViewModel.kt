package com.flowboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowboard.domain.model.*
import com.flowboard.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    // ==================== State ====================

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    // Chat rooms
    val chatRooms: StateFlow<List<ChatRoom>> = chatRepository.getAllChatRooms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val archivedChatRooms: StateFlow<List<ChatRoom>> = chatRepository.getArchivedChatRooms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalUnreadCount: StateFlow<Int> = chatRepository.getTotalUnreadCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Active chat data
    val activeChatRoom: StateFlow<ChatRoom?> = _activeChatId
        .flatMapLatest { chatId ->
            if (chatId != null) {
                chatRepository.getChatRoom(chatId)
            } else {
                flowOf(null)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val messages: StateFlow<List<Message>> = _activeChatId
        .flatMapLatest { chatId ->
            if (chatId != null) {
                chatRepository.getMessages(chatId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val participants: StateFlow<List<ChatParticipant>> = _activeChatId
        .flatMapLatest { chatId ->
            if (chatId != null) {
                chatRepository.getChatParticipants(chatId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val typingIndicators: StateFlow<List<TypingIndicator>> = _activeChatId
        .flatMapLatest { chatId ->
            if (chatId != null) {
                chatRepository.getTypingIndicators(chatId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ==================== Actions ====================

    fun selectChat(chatRoomId: String) {
        _activeChatId.value = chatRoomId
        markAsRead(chatRoomId)
        connectToChat(chatRoomId)
    }

    fun deselectChat() {
        _activeChatId.value?.let { disconnectFromChat(it) }
        _activeChatId.value = null
    }

    fun createDirectChat(userId: String, userName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = chatRepository.createChatRoom(
                type = ChatType.DIRECT,
                name = userName,
                participantIds = listOf(userId)
            )

            result.fold(
                onSuccess = { chatRoom ->
                    _activeChatId.value = chatRoom.id
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Chat created successfully"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to create chat"
                        )
                    }
                }
            )
        }
    }

    fun createGroupChat(name: String, participantIds: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = chatRepository.createChatRoom(
                type = ChatType.GROUP,
                name = name,
                participantIds = participantIds
            )

            result.fold(
                onSuccess = { chatRoom ->
                    _activeChatId.value = chatRoom.id
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Group created successfully"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to create group"
                        )
                    }
                }
            )
        }
    }

    fun createProjectChat(projectId: String, projectName: String, participantIds: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = chatRepository.createChatRoom(
                type = ChatType.PROJECT,
                name = projectName,
                participantIds = participantIds,
                resourceId = projectId,
                resourceType = ResourceType.PROJECT
            )

            result.fold(
                onSuccess = { chatRoom ->
                    _activeChatId.value = chatRoom.id
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Project chat created"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to create project chat"
                        )
                    }
                }
            )
        }
    }

    fun createTaskThread(taskId: String, taskTitle: String, participantIds: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = chatRepository.createChatRoom(
                type = ChatType.TASK_THREAD,
                name = "Task: $taskTitle",
                participantIds = participantIds,
                resourceId = taskId,
                resourceType = ResourceType.TASK
            )

            result.fold(
                onSuccess = { chatRoom ->
                    _activeChatId.value = chatRoom.id
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Task thread created"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to create task thread"
                        )
                    }
                }
            )
        }
    }

    fun sendMessage(
        content: String,
        type: MessageType = MessageType.TEXT,
        replyToId: String? = null,
        mentions: List<String> = emptyList()
    ) {
        val chatRoomId = _activeChatId.value ?: return

        viewModelScope.launch {
            val result = chatRepository.sendMessage(
                chatRoomId = chatRoomId,
                content = content,
                type = type,
                replyToId = replyToId,
                mentions = mentions
            )

            result.fold(
                onSuccess = { message ->
                    _uiState.update {
                        it.copy(successMessage = "Message sent")
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Failed to send message")
                    }
                }
            )
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        viewModelScope.launch {
            val result = chatRepository.editMessage(messageId, newContent)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(successMessage = "Message updated")
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Failed to edit message")
                    }
                }
            )
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessage(messageId)
            _uiState.update {
                it.copy(successMessage = "Message deleted")
            }
        }
    }

    fun markAsRead(chatRoomId: String) {
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(chatRoomId)
        }
    }

    fun addReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            chatRepository.addReaction(messageId, emoji)
        }
    }

    fun removeReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            chatRepository.removeReaction(messageId, emoji)
        }
    }

    fun archiveChat(chatRoomId: String, isArchived: Boolean = true) {
        viewModelScope.launch {
            chatRepository.archiveChatRoom(chatRoomId, isArchived)
            _uiState.update {
                it.copy(
                    successMessage = if (isArchived) "Chat archived" else "Chat unarchived"
                )
            }
        }
    }

    fun muteChat(chatRoomId: String, isMuted: Boolean = true) {
        viewModelScope.launch {
            chatRepository.muteChatRoom(chatRoomId, isMuted)
            _uiState.update {
                it.copy(
                    successMessage = if (isMuted) "Chat muted" else "Chat unmuted"
                )
            }
        }
    }

    fun deleteChat(chatRoomId: String) {
        viewModelScope.launch {
            chatRepository.deleteChatRoom(chatRoomId)
            if (_activeChatId.value == chatRoomId) {
                _activeChatId.value = null
            }
            _uiState.update {
                it.copy(successMessage = "Chat deleted")
            }
        }
    }

    fun addParticipant(userId: String) {
        val chatRoomId = _activeChatId.value ?: return

        viewModelScope.launch {
            val result = chatRepository.addParticipant(chatRoomId, userId)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(successMessage = "Participant added")
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Failed to add participant")
                    }
                }
            )
        }
    }

    fun removeParticipant(userId: String) {
        val chatRoomId = _activeChatId.value ?: return

        viewModelScope.launch {
            val result = chatRepository.removeParticipant(chatRoomId, userId)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(successMessage = "Participant removed")
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Failed to remove participant")
                    }
                }
            )
        }
    }

    fun sendTypingIndicator(isTyping: Boolean) {
        val chatRoomId = _activeChatId.value ?: return

        viewModelScope.launch {
            chatRepository.sendTypingIndicator(chatRoomId, isTyping)
        }
    }

    fun searchMessages(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }

            val results = chatRepository.searchMessages(query)

            _uiState.update {
                it.copy(
                    isSearching = false,
                    searchResults = results
                )
            }
        }
    }

    fun clearSearchResults() {
        _uiState.update {
            it.copy(searchResults = emptyList())
        }
    }

    private fun connectToChat(chatRoomId: String) {
        viewModelScope.launch {
            chatRepository.connectToChat(chatRoomId)
        }
    }

    private fun disconnectFromChat(chatRoomId: String) {
        viewModelScope.launch {
            chatRepository.disconnectFromChat(chatRoomId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        _activeChatId.value?.let { disconnectFromChat(it) }
    }
}

// ==================== UI State ====================

data class ChatUiState(
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val searchResults: List<MessageSearchResult> = emptyList()
)
