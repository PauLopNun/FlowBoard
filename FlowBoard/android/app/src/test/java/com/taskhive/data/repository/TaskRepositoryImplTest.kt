package com.flowboard.data.repository

import com.flowboard.data.local.dao.TaskDao
import com.flowboard.data.local.entities.TaskEntity
import com.flowboard.data.local.entities.TaskPriority
import com.flowboard.data.remote.api.TaskApiService
import com.flowboard.domain.model.Task
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.util.*

class TaskRepositoryImplTest {

    @Mock
    private lateinit var taskDao: TaskDao

    @Mock
    private lateinit var taskApiService: TaskApiService

    private lateinit var repository: TaskRepositoryImpl

    private val sampleTaskEntity = TaskEntity(
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
        location = null,
        isSync = true,
        lastSyncAt = null
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = TaskRepositoryImpl(taskDao, taskApiService)
    }

    @Test
    fun `getAllTasks returns flow of domain tasks`() = runBlocking {
        // Given
        whenever(taskDao.getAllTasks()).thenReturn(flowOf(listOf(sampleTaskEntity)))

        // When
        val result = repository.getAllTasks()

        // Then
        result.collect { tasks ->
            assertEquals(1, tasks.size)
            assertEquals("Test Task", tasks[0].title)
            assertEquals("Test Description", tasks[0].description)
        }
    }

    @Test
    fun `createTask inserts entity and attempts sync`() = runBlocking {
        // Given
        val task = Task(
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

        whenever(taskApiService.createTask(any())).thenReturn(sampleTaskEntity)

        // When
        val result = repository.createTask(task)

        // Then
        assertTrue(result.isSuccess)
        verify(taskDao).insertTask(any())
        verify(taskApiService).createTask(any())
    }

    @Test
    fun `createTask handles network failure gracefully`() = runBlocking {
        // Given
        val task = Task(
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

        whenever(taskApiService.createTask(any())).thenThrow(RuntimeException("Network error"))

        // When
        val result = repository.createTask(task)

        // Then
        assertTrue(result.isSuccess) // Should still succeed locally
        verify(taskDao).insertTask(any())
        verify(taskApiService).createTask(any())
        verify(taskDao, never()).markTaskAsSynced(any(), any())
    }

    @Test
    fun `getTaskById returns correct task`() = runBlocking {
        // Given
        whenever(taskDao.getTaskById("test-id")).thenReturn(sampleTaskEntity)

        // When
        val result = repository.getTaskById("test-id")

        // Then
        assertNotNull(result)
        assertEquals("Test Task", result?.title)
        assertEquals("test-id", result?.id)
    }

    @Test
    fun `getTaskById returns null for non-existent task`() = runBlocking {
        // Given
        whenever(taskDao.getTaskById("non-existent")).thenReturn(null)

        // When
        val result = repository.getTaskById("non-existent")

        // Then
        assertNull(result)
    }

    @Test
    fun `toggleTaskStatus updates task completion status`() = runBlocking {
        // Given
        val incompleteTask = sampleTaskEntity.copy(isCompleted = false)
        whenever(taskDao.getTaskById("test-id")).thenReturn(incompleteTask)
        whenever(taskApiService.updateTask(any(), any())).thenReturn(sampleTaskEntity)

        // When
        val result = repository.toggleTaskStatus("test-id")

        // Then
        assertTrue(result.isSuccess)
        verify(taskDao).updateTask(argThat { task -> task.isCompleted == true })
    }
}