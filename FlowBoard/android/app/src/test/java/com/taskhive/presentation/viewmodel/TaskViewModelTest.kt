package com.flowboard.presentation.viewmodel

import com.flowboard.data.local.entities.TaskPriority
import com.flowboard.domain.model.Task
import com.flowboard.domain.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {

    @Mock
    private lateinit var taskRepository: TaskRepository

    private lateinit var viewModel: TaskViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val sampleTask = Task(
        id = "test-id",
        title = "Test Task",
        description = "Test Description",
        isCompleted = false,
        priority = TaskPriority.MEDIUM,
        dueDate = null,
        createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        updatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        assignedTo = null,
        projectId = null,
        tags = emptyList(),
        attachments = emptyList(),
        isEvent = false,
        eventStartTime = null,
        eventEndTime = null,
        location = null
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Setup default repository behavior
        whenever(taskRepository.getAllTasks()).thenReturn(flowOf(listOf(sampleTask)))
        whenever(taskRepository.getTasksByStatus(false)).thenReturn(flowOf(listOf(sampleTask)))
        whenever(taskRepository.getTasksByStatus(true)).thenReturn(flowOf(emptyList()))
        whenever(taskRepository.getOverdueTasks()).thenReturn(flowOf(emptyList()))
        
        viewModel = TaskViewModel(taskRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() {
        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertNull(uiState.selectedTask)
        assertNull(uiState.error)
        assertNull(uiState.message)
    }

    @Test
    fun `loadTaskById updates selected task`() = runTest {
        // Given
        whenever(taskRepository.getTaskById("test-id")).thenReturn(sampleTask)

        // When
        viewModel.loadTaskById("test-id")
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(sampleTask, uiState.selectedTask)
        assertNull(uiState.error)
    }

    @Test
    fun `loadTaskById handles task not found`() = runTest {
        // Given
        whenever(taskRepository.getTaskById("non-existent")).thenReturn(null)

        // When
        viewModel.loadTaskById("non-existent")
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertNull(uiState.selectedTask)
        assertNull(uiState.error)
    }

    @Test
    fun `createTask shows loading state`() = runTest {
        // Given
        whenever(taskRepository.createTask(any())).thenReturn(Result.success(sampleTask))

        // When
        viewModel.createTask("New Task", "Description")
        
        // Then (while loading)
        assertTrue(viewModel.uiState.value.isLoading)
        
        advanceUntilIdle()
        
        // Then (after completion)
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals("Task created successfully", uiState.message)
        assertNull(uiState.error)
    }

    @Test
    fun `createTask handles failure`() = runTest {
        // Given
        val exception = RuntimeException("Creation failed")
        whenever(taskRepository.createTask(any())).thenReturn(Result.failure(exception))

        // When
        viewModel.createTask("New Task", "Description")
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals("Creation failed", uiState.error)
        assertNull(uiState.message)
    }

    @Test
    fun `toggleTaskStatus calls repository`() = runTest {
        // Given
        whenever(taskRepository.toggleTaskStatus("test-id")).thenReturn(Result.success(sampleTask))

        // When
        viewModel.toggleTaskStatus("test-id")
        advanceUntilIdle()

        // Then
        verify(taskRepository).toggleTaskStatus("test-id")
        val uiState = viewModel.uiState.value
        assertEquals("Task status updated", uiState.message)
    }

    @Test
    fun `deleteTask shows success message`() = runTest {
        // Given
        whenever(taskRepository.deleteTask("test-id")).thenReturn(Result.success(Unit))

        // When
        viewModel.deleteTask("test-id")
        advanceUntilIdle()

        // Then
        verify(taskRepository).deleteTask("test-id")
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals("Task deleted successfully", uiState.message)
    }

    @Test
    fun `syncTasks handles success`() = runTest {
        // Given
        whenever(taskRepository.syncTasks()).thenReturn(Result.success(Unit))

        // When
        viewModel.syncTasks()
        advanceUntilIdle()

        // Then
        verify(taskRepository).syncTasks()
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals("Tasks synced successfully", uiState.message)
    }

    @Test
    fun `clearError resets error state`() {
        // Given - Set an error first
        viewModel.createTask("", "") // This should cause an error
        
        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `clearMessage resets message state`() = runTest {
        // Given - Set a message first
        whenever(taskRepository.createTask(any())).thenReturn(Result.success(sampleTask))
        viewModel.createTask("Test", "Description")
        advanceUntilIdle()

        // When
        viewModel.clearMessage()

        // Then
        assertNull(viewModel.uiState.value.message)
    }
}