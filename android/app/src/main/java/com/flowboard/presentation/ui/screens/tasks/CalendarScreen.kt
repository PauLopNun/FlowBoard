package com.flowboard.presentation.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowboard.data.local.entities.TaskEntity
import com.flowboard.data.local.entities.TaskPriority
import com.flowboard.presentation.viewmodel.TaskViewModel
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onTaskClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()

    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    var currentMonth by remember { mutableStateOf(today.toYearMonth()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(today) }

    // Map: date -> list of tasks
    val tasksByDate: Map<LocalDate, List<TaskEntity>> = remember(allTasks) {
        val result = mutableMapOf<LocalDate, MutableList<TaskEntity>>()
        allTasks.forEach { task ->
            val date = task.dueDate?.date ?: task.eventStartTime?.date
            if (date != null) result.getOrPut(date) { mutableListOf() }.add(task)
        }
        result
    }

    val selectedTasks = selectedDate?.let { tasksByDate[it] } ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Month navigation
            MonthHeader(
                yearMonth = currentMonth,
                onPrevious = { currentMonth = currentMonth.minus(1) },
                onNext = { currentMonth = currentMonth.plus(1) }
            )

            // Day-of-week labels
            DayOfWeekHeader()

            // Calendar grid
            CalendarGrid(
                yearMonth = currentMonth,
                today = today,
                selectedDate = selectedDate,
                tasksByDate = tasksByDate,
                onDayClick = { date ->
                    selectedDate = if (selectedDate == date) null else date
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Tasks for selected day
            if (selectedDate != null) {
                Text(
                    text = if (selectedTasks.isEmpty()) "No tasks on this day"
                           else "${selectedTasks.size} task${if (selectedTasks.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedTasks, key = { it.id }) { task ->
                    CalendarTaskItem(task = task, onClick = { onTaskClick(task.id) })
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    yearMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, "Previous month")
        }
        Text(
            text = "${yearMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${yearMonth.year}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, "Next month")
        }
    }
}

@Composable
private fun DayOfWeekHeader() {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        days.forEach { day ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate?,
    tasksByDate: Map<LocalDate, List<TaskEntity>>,
    onDayClick: (LocalDate) -> Unit
) {
    val firstDay = LocalDate(yearMonth.year, yearMonth.month, 1)
    // Monday=1 .. Sunday=7; offset so Monday is column 0
    val startOffset = (firstDay.dayOfWeek.isoDayNumber - 1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - startOffset + 1
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayNumber in 1..daysInMonth) {
                            val date = LocalDate(yearMonth.year, yearMonth.month, dayNumber)
                            val hasTasks = tasksByDate.containsKey(date)
                            val isToday = date == today
                            val isSelected = date == selectedDate

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primaryContainer
                                            else -> androidx.compose.ui.graphics.Color.Transparent
                                        }
                                    )
                                    .clickable { onDayClick(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dayNumber.toString(),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    if (hasTasks) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.primary
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarTaskItem(task: TaskEntity, onClick: () -> Unit) {
    val priorityColor = when (task.priority) {
        TaskPriority.URGENT -> MaterialTheme.colorScheme.error
        TaskPriority.HIGH -> MaterialTheme.colorScheme.tertiary
        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.primary
        TaskPriority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(priorityColor)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isCompleted)
                        androidx.compose.ui.text.style.TextDecoration.LineThrough
                    else null
                )
                if (task.isEvent && task.eventStartTime != null) {
                    Text(
                        text = "${task.eventStartTime.hour.toString().padStart(2,'0')}:${task.eventStartTime.minute.toString().padStart(2,'0')}" +
                               (task.eventEndTime?.let { " – ${it.hour.toString().padStart(2,'0')}:${it.minute.toString().padStart(2,'0')}" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (task.isCompleted) {
                Icon(Icons.Default.CheckCircle, null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

data class YearMonth(val year: Int, val month: Month) {
    fun minus(months: Int): YearMonth {
        var m = month.number - months
        var y = year
        while (m < 1) { m += 12; y-- }
        return YearMonth(y, Month(m))
    }
    fun plus(months: Int): YearMonth {
        var m = month.number + months
        var y = year
        while (m > 12) { m -= 12; y++ }
        return YearMonth(y, Month(m))
    }
    fun lengthOfMonth(): Int = when (month) {
        Month.JANUARY, Month.MARCH, Month.MAY, Month.JULY,
        Month.AUGUST, Month.OCTOBER, Month.DECEMBER -> 31
        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        Month.FEBRUARY -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    }
}

fun LocalDate.toYearMonth() = YearMonth(year, month)
