package com.flowboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowboard.domain.model.Task
import com.flowboard.domain.repository.TaskRepository
import com.flowboard.data.local.entities.TaskPriority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    val allTasks = taskRepository.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pendingTasks = taskRepository.getTasksByStatus(false)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val completedTasks = taskRepository.getTasksByStatus(true)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val overdueTasks = taskRepository.getOverdueTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun loadTaskById(taskId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val task = taskRepository.getTaskById(taskId)
                _uiState.update { 
                    it.copy(
                        selectedTask = task,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun createTask(
        title: String,
        description: String,
        priority: TaskPriority = TaskPriority.MEDIUM,
        dueDate: LocalDateTime? = null,
        isEvent: Boolean = false,
        eventStartTime: LocalDateTime? = null,
        eventEndTime: LocalDateTime? = null,
        location: String? = null,
        projectId: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    description = description,
                    priority = priority,
                    dueDate = dueDate,
                    createdAt = now,
                    updatedAt = now,
                    isEvent = isEvent,
                    eventStartTime = eventStartTime,
                    eventEndTime = eventEndTime,
                    location = location,
                    projectId = projectId
                )

                taskRepository.createTask(task).fold(
                    onSuccess = {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                message = "Task created successfully"
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = exception.message
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val updatedTask = task.copy(updatedAt = now)

                taskRepository.updateTask(updatedTask).fold(
                    onSuccess = {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                message = "Task updated successfully"
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = exception.message
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun toggleTaskStatus(taskId: String) {
        viewModelScope.launch {
            try {
                taskRepository.toggleTaskStatus(taskId).fold(
                    onSuccess = {
                        _uiState.update { 
                            it.copy(message = "Task status updated")
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update { 
                            it.copy(error = exception.message)
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message)
                }
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                taskRepository.deleteTask(taskId).fold(
                    onSuccess = {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                message = "Task deleted successfully"
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = exception.message
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun syncTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                taskRepository.syncTasks().fold(
                    onSuccess = {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                message = "Tasks synced successfully"
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "Sync failed"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Sync failed"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

data class TaskUiState(
    val isLoading: Boolean = false,
    val selectedTask: Task? = null,
    val error: String? = null,
    val message: String? = null
)