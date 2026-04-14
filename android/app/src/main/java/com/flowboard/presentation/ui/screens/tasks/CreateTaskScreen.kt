package com.flowboard.presentation.ui.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flowboard.data.local.entities.TaskPriority
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    onCreateTask: (
        title: String,
        description: String,
        priority: TaskPriority,
        dueDate: LocalDateTime?,
        isEvent: Boolean,
        eventStartTime: LocalDateTime?,
        eventEndTime: LocalDateTime?,
        location: String?
    ) -> Unit,
    onNavigateBack: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var isEvent by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf("") }
    var showPriorityMenu by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val canCreate = title.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Task") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (canCreate) {
                                onCreateTask(
                                    title,
                                    description,
                                    priority,
                                    dueDate,
                                    isEvent,
                                    null, // eventStartTime - can be extended later
                                    null, // eventEndTime - can be extended later
                                    if (location.isBlank()) null else location
                                )
                            }
                        },
                        enabled = canCreate && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Create")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                placeholder = { Text("Enter task title") },
                leadingIcon = {
                    Icon(Icons.Default.Task, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("Enter task description") },
                leadingIcon = {
                    Icon(Icons.Default.Description, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                minLines = 4,
                maxLines = 8
            )

            // Priority Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val priorities = TaskPriority.values().toList()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        priorities.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { p ->
                                    FilterChip(
                                        modifier = Modifier.weight(1f),
                                        selected = priority == p,
                                        onClick = { priority = p },
                                        label = { Text(p.name) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = when (p) {
                                                    TaskPriority.LOW -> Icons.Default.ArrowDownward
                                                    TaskPriority.MEDIUM -> Icons.Default.Remove
                                                    TaskPriority.HIGH -> Icons.Default.ArrowUpward
                                                    TaskPriority.URGENT -> Icons.Default.Warning
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        enabled = !isLoading,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = when (p) {
                                                TaskPriority.LOW -> MaterialTheme.colorScheme.tertiary
                                                TaskPriority.MEDIUM -> MaterialTheme.colorScheme.primary
                                                TaskPriority.HIGH -> MaterialTheme.colorScheme.secondary
                                                TaskPriority.URGENT -> MaterialTheme.colorScheme.error
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Due Date
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Due Date", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = dueDate?.let { "${it.date.dayOfMonth}/${it.date.monthNumber}/${it.date.year}" } ?: "No date set",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (dueDate != null) {
                            IconButton(onClick = { dueDate = null }) {
                                Icon(Icons.Default.Clear, null, modifier = Modifier.size(20.dp))
                            }
                        }
                        FilledTonalButton(onClick = { showDatePicker = true }) {
                            Text(if (dueDate == null) "Set" else "Change")
                        }
                    }
                }
            }

            // Event Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Calendar Event",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Add to calendar with time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isEvent,
                        onCheckedChange = { isEvent = it },
                        enabled = !isLoading
                    )
                }
            }

            // Location (if event)
            if (isEvent) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    placeholder = { Text("Enter event location") },
                    leadingIcon = {
                        Icon(Icons.Default.Place, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )
            }

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Tasks are synced in real-time across all connected devices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val instant = Instant.fromEpochMilliseconds(millis)
                            dueDate = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

