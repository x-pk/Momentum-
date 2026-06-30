package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Task
import com.example.data.TaskStep
import com.example.viewmodel.LifeSaverViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: LifeSaverViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasksState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    val categories by viewModel.categories.collectAsState()

    val filteredTasks = remember(tasks, selectedCategoryFilter) {
        val activeTasks = tasks.filter { !it.isCompleted }
        if (selectedCategoryFilter == "All") activeTasks else activeTasks.filter { it.category == selectedCategoryFilter }
    }

    val firstLeastTimeTask = remember(filteredTasks) {
        filteredTasks.filter { !it.isCompleted }.minByOrNull { it.deadline } ?: filteredTasks.firstOrNull()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    com.example.ui.components.LifeSaverLogo(size = 46.dp)
                    Column {
                        Text(
                            text = "MOMENTUM AI",
                            color = Color(0xFFF97316),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Commitments",
                            color = Color(0xFFF1F5F9),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Horizontal Filters - Side scrollable LazyRow with 4dp symmetrical padding
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategoryFilter == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                if (isSelected) Color.White else Color.White.copy(alpha = 0.03f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(100.dp)
                            )
                            .clickable { selectedCategoryFilter = cat }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color(0xFF0A0B10) else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                // Dynamic Add Category button at the end
                item {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFFF97316).copy(alpha = 0.15f))
                            .border(
                                width = 1.dp,
                                color = Color(0xFFF97316).copy(alpha = 0.4f),
                                shape = RoundedCornerShape(100.dp)
                            )
                            .clickable { showAddCategoryDialog = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "+ Add",
                            color = Color(0xFFF97316),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No commitments tracked yet",
                            color = Color(0xFF94A3B8),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the '+' button above to add an assignment or deadline.",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tasks_list"),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 240.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskItemRow(
                            task = task,
                            viewModel = viewModel,
                            initiallyExpanded = (task == firstLeastTimeTask),
                            onCompleteToggle = { isChecked ->
                                viewModel.completeTask(task, isChecked)
                            },
                            onDelete = {
                                viewModel.deleteTask(task)
                            }
                        )
                    }
                }
            }
        }

        // Beautiful glass Add Task Bottom Sheet (iOS style slide-up)
        if (showAddDialog) {
            AddTaskBottomSheet(
                categories = categories,
                onDismiss = { showAddDialog = false },
                onAdd = { title, desc, startTimeMs, deadlineMs, priority, category ->
                    viewModel.addTask(title, desc, startTimeMs, deadlineMs, priority, category)
                    showAddDialog = false
                    viewModel.speak("Task created. Generating your custom AI plan.")
                },
                onAddCategory = { viewModel.addCategory(it) }
            )
        }

        // Dialog for user to add custom category
        if (showAddCategoryDialog) {
            var newCategoryName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false },
                title = { Text("Add Custom Category", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Enter a name for your custom commitment category:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        TextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            placeholder = { Text("e.g. Coding, Fitness", color = Color(0xFF64748B)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color(0xFFF97316)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newCategoryName.trim().isNotEmpty()) {
                                viewModel.addCategory(newCategoryName.trim())
                                showAddCategoryDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                        enabled = newCategoryName.trim().isNotEmpty()
                    ) {
                        Text("Add", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCategoryDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                },
                containerColor = Color(0xFF0A0B10).copy(alpha = 0.95f),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            )
        }

        // Floating Action Button hovering in the bottom right
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = Color(0xFFF97316),
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 120.dp)
                .testTag("add_task_trigger")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add commitment",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun syncStartTimeWithReminder(startTimeMs: Long, hour: Int, minute: Int): Long {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = startTimeMs }
    cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
    cal.set(java.util.Calendar.MINUTE, minute)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun syncDeadlineWithReminder(deadlineMs: Long, hour: Int, minute: Int): Long {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = deadlineMs }
    cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
    cal.set(java.util.Calendar.MINUTE, minute)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

@Composable
fun TaskItemRow(
    task: Task,
    viewModel: LifeSaverViewModel,
    initiallyExpanded: Boolean = false,
    onCompleteToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember(task.id, initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    val steps by viewModel.getTaskStepsFlow(task.id).collectAsState(initial = emptyList())
    val generatingPlanId by viewModel.generatingPlanTaskId.collectAsState()
    val isGeneratingThisPlan = generatingPlanId == task.id
    var showEditDialog by remember { mutableStateOf(false) }
    var showTickOptions by remember { mutableStateOf(false) }
    var isReminderDropdownExpanded by remember(task.id) { mutableStateOf(false) }
    val categories by viewModel.categories.collectAsState()

    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(task.isCompleted) {
        if (!task.isCompleted) {
            while (true) {
                delay(1000)
                currentTime = System.currentTimeMillis()
            }
        }
    }
    val remainingMs = task.deadline - currentTime

    val formattedTimer = remember(remainingMs) {
        val absMs = kotlin.math.abs(remainingMs)
        val hrs = TimeUnit.MILLISECONDS.toHours(absMs)
        val mins = TimeUnit.MILLISECONDS.toMinutes(absMs) % 60
        val secs = TimeUnit.MILLISECONDS.toSeconds(absMs) % 60
        if (remainingMs <= 0) {
            String.format("-%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        }
    }
    
    // Calculate display duration
    val (remainingValue, remainingUnit) = remember(remainingMs) {
        if (remainingMs <= 0) {
            val overdueMs = kotlin.math.abs(remainingMs)
            val days = TimeUnit.MILLISECONDS.toDays(overdueMs)
            val hours = TimeUnit.MILLISECONDS.toHours(overdueMs)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(overdueMs)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(overdueMs)
            when {
                days > 0 -> "$days" to if (days == 1L) "DAY OVERDUE" else "DAYS OVERDUE"
                hours > 0 -> "$hours" to if (hours == 1L) "HR OVERDUE" else "HRS OVERDUE"
                minutes > 0 -> "$minutes" to if (minutes == 1L) "MIN OVERDUE" else "MINS OVERDUE"
                else -> "$seconds" to "SEC OVERDUE"
            }
        } else {
            val days = TimeUnit.MILLISECONDS.toDays(remainingMs)
            val hours = TimeUnit.MILLISECONDS.toHours(remainingMs)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMs)
            when {
                days > 0 -> "$days" to if (days == 1L) "DAY" else "DAYS"
                hours > 0 -> "$hours" to if (hours == 1L) "HOUR" else "HOURS"
                else -> "$minutes" to if (minutes == 1L) "MIN" else "MINS"
            }
        }
    }

    val itemBorderColor = if (task.priority == "HIGH" && !task.isCompleted) {
        Color(0xFFF97316).copy(alpha = 0.25f)
    } else {
        Color.White.copy(alpha = 0.05f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, itemBorderColor, RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
            .testTag("task_item_${task.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Task Checkbox
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (task.isCompleted || task.isStrikethrough) Color.White else Color.White.copy(alpha = 0.04f)
                        )
                        .border(
                            1.dp,
                            if (task.isCompleted || task.isStrikethrough) Color.White else Color.White.copy(alpha = 0.15f),
                            CircleShape
                        )
                        .clickable { showTickOptions = true }
                        .testTag("task_checkbox_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = if (task.isFailed) Icons.Default.Close else Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color(0xFF0A0B10),
                            modifier = Modifier.size(14.dp)
                        )
                    } else if (task.isStrikethrough) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Strikethrough",
                            tint = Color(0xFF0A0B10).copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Title, priority & category
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Category Label
                        if (task.category.isNotEmpty()) {
                            Text(
                                text = task.category.uppercase(),
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        
                        // Priority Dot
                        val dotColor = when (task.priority) {
                            "HIGH" -> Color(0xFFF97316)
                            "MEDIUM" -> Color(0xFFFCD34D)
                            else -> Color(0xFF10B981)
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(dotColor, CircleShape)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    val isTimeUp = !task.isCompleted && remainingMs <= 0
                    val shouldStrikeThrough = task.isCompleted || task.isStrikethrough || isTimeUp

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = task.title,
                            color = if (shouldStrikeThrough) Color(0xFF64748B) else Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            textDecoration = if (shouldStrikeThrough) TextDecoration.LineThrough else null,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (!task.isCompleted) {
                            val isOverdue = remainingMs <= 0
                            val timerColor = if (isOverdue) Color(0xFFF97316) else Color(0xFFFCD34D)
                            Text(
                                text = "⏱️ $formattedTimer",
                                color = timerColor,
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .background(timerColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Countdown badge from wireframe
                Box(
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = remainingValue,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = remainingUnit,
                            color = if (remainingUnit.contains("OVERDUE") || remainingUnit == "MIN" || remainingUnit == "MINS") Color(0xFFF97316) else Color(0xFF94A3B8),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            lineHeight = 8.sp
                        )
                    }
                }
            }

            // Expanded details: Tactical rescue plan, advice & actions
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (task.description.isNotEmpty()) {
                        Text(
                            text = task.description,
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    // Strategic AI Advice box
                    task.aiProactiveAdvice?.let { advice ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "💡 AI PROACTIVE ADVICE",
                                    color = Color(0xFFF97316),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = advice,
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Tactical sub-steps checklist
                    Text(
                        text = "TACTICAL RESCUE STEPS",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isGeneratingThisPlan) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFF97316),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else if (steps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.generateRescuePlan(task.id) }
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.01f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✨ Tap to Generate AI Rescue Plan",
                                color = Color(0xFFF97316),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Steps list
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            steps.forEach { step ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleStep(step, !step.isCompleted) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Check indicator
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .border(
                                                1.dp,
                                                if (step.isCompleted) Color(0xFFF97316) else Color.White.copy(alpha = 0.2f),
                                                CircleShape
                                            )
                                            .background(
                                                if (step.isCompleted) Color(0xFFF97316) else Color.Transparent,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (step.isCompleted) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Completed",
                                                tint = Color(0xFF0A0B10),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Step info (Time span badge + step text)
                                    Text(
                                        text = "${step.timeSpan} - ",
                                        color = Color(0xFFFCD34D),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = step.stepText,
                                        color = if (step.isCompleted) Color(0xFF64748B) else Color(0xFFE2E8F0),
                                        fontSize = 13.sp,
                                        textDecoration = if (step.isCompleted) TextDecoration.LineThrough else null,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Anti-Procrastinator Coach Settings
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.01f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Header row acting as a Toggle/Dropdown Menu
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isReminderDropdownExpanded = !isReminderDropdownExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⏰", fontSize = 16.sp)
                                    Column {
                                        Text(
                                            text = "Anti-Procrastinator Reminder",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Coach warns about different phases",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = task.isFocusReminderEnabled,
                                        onCheckedChange = { isEnabled ->
                                            viewModel.updateTaskAndSteps(
                                                task.copy(isFocusReminderEnabled = isEnabled),
                                                steps
                                            )
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFFF97316),
                                            uncheckedThumbColor = Color(0xFF64748B),
                                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = if (isReminderDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Toggle Settings",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            
                            if (isReminderDropdownExpanded && task.isFocusReminderEnabled) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color.White.copy(alpha = 0.05f))
                                )
                                
                                // Days of week selection
                                Column {
                                    Text(
                                        text = "SELECT DAYS TO REMIND",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                        val selectedDays = task.reminderDays.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
                                        daysOfWeek.forEach { day ->
                                            val isSelected = selectedDays.contains(day)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isSelected) Color(0xFFF97316) else Color.White.copy(alpha = 0.08f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .background(
                                                        color = if (isSelected) Color(0xFFF97316).copy(alpha = 0.12f) else Color.Transparent,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        val updated = selectedDays.toMutableSet()
                                                        if (updated.contains(day)) {
                                                            updated.remove(day)
                                                        } else {
                                                            updated.add(day)
                                                        }
                                                        viewModel.updateTaskAndSteps(
                                                            task.copy(reminderDays = updated.joinToString(",")),
                                                            steps
                                                        )
                                                    }
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = day,
                                                    color = if (isSelected) Color(0xFFF97316) else Color(0xFF94A3B8),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))

                                // Begin and Finish time selectors (Time Range picker using simple dropdown hours and minutes)
                                Column {
                                    Text(
                                        text = "DAILY ACTIVE TIME WINDOW",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Start hour/minute
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "WHEN TO BEGIN",
                                                color = Color(0xFF64748B),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Hour selector
                                                var startHourMenuExpanded by remember { mutableStateOf(false) }
                                                Box {
                                                    Box(
                                                        modifier = Modifier
                                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                                            .clickable { startHourMenuExpanded = true }
                                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = String.format("%02d hr", task.reminderStartHour),
                                                            color = Color.White,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                    DropdownMenu(
                                                        expanded = startHourMenuExpanded,
                                                        onDismissRequest = { startHourMenuExpanded = false }
                                                    ) {
                                                        (0..23).forEach { hour ->
                                                            DropdownMenuItem(
                                                                text = { Text(String.format("%02d:00", hour)) },
                                                                onClick = {
                                                                    val newStart = syncStartTimeWithReminder(task.startTime, hour, task.reminderStartMinute)
                                                                    viewModel.updateTaskAndSteps(
                                                                        task.copy(reminderStartHour = hour, startTime = newStart),
                                                                        steps
                                                                    )
                                                                    startHourMenuExpanded = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                                // Minute selector
                                                var startMinMenuExpanded by remember { mutableStateOf(false) }
                                                Box {
                                                    Box(
                                                        modifier = Modifier
                                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                                            .clickable { startMinMenuExpanded = true }
                                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = String.format("%02d min", task.reminderStartMinute),
                                                            color = Color.White,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                    DropdownMenu(
                                                        expanded = startMinMenuExpanded,
                                                        onDismissRequest = { startMinMenuExpanded = false }
                                                    ) {
                                                        listOf(0, 15, 30, 45).forEach { min ->
                                                            DropdownMenuItem(
                                                                text = { Text(String.format("%02d min", min)) },
                                                                onClick = {
                                                                    val newStart = syncStartTimeWithReminder(task.startTime, task.reminderStartHour, min)
                                                                    viewModel.updateTaskAndSteps(
                                                                        task.copy(reminderStartMinute = min, startTime = newStart),
                                                                        steps
                                                                    )
                                                                    startMinMenuExpanded = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // End hour/minute
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "AT WHAT TIME TO FINISH",
                                                color = Color(0xFF64748B),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Hour selector
                                                var endHourMenuExpanded by remember { mutableStateOf(false) }
                                                Box {
                                                    Box(
                                                        modifier = Modifier
                                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                                            .clickable { endHourMenuExpanded = true }
                                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = String.format("%02d hr", task.reminderEndHour),
                                                            color = Color.White,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                    DropdownMenu(
                                                        expanded = endHourMenuExpanded,
                                                        onDismissRequest = { endHourMenuExpanded = false }
                                                    ) {
                                                        (0..23).forEach { hour ->
                                                            DropdownMenuItem(
                                                                text = { Text(String.format("%02d:00", hour)) },
                                                                onClick = {
                                                                    val newEnd = syncDeadlineWithReminder(task.deadline, hour, task.reminderEndMinute)
                                                                    viewModel.updateTaskAndSteps(
                                                                        task.copy(reminderEndHour = hour, deadline = newEnd),
                                                                        steps
                                                                    )
                                                                    endHourMenuExpanded = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                                // Minute selector
                                                var endMinMenuExpanded by remember { mutableStateOf(false) }
                                                Box {
                                                    Box(
                                                        modifier = Modifier
                                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                                            .clickable { endMinMenuExpanded = true }
                                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = String.format("%02d min", task.reminderEndMinute),
                                                            color = Color.White,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                    DropdownMenu(
                                                        expanded = endMinMenuExpanded,
                                                        onDismissRequest = { endMinMenuExpanded = false }
                                                    ) {
                                                        listOf(0, 15, 30, 45).forEach { min ->
                                                            DropdownMenuItem(
                                                                text = { Text(String.format("%02d min", min)) },
                                                                onClick = {
                                                                    val newEnd = syncDeadlineWithReminder(task.deadline, task.reminderEndHour, min)
                                                                    viewModel.updateTaskAndSteps(
                                                                        task.copy(reminderEndMinute = min, deadline = newEnd),
                                                                        steps
                                                                    )
                                                                    endMinMenuExpanded = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Pulse Interval selector
                                Column {
                                    Text(
                                        text = "COACH REMINDER INTERVAL",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val intervals = remember(remainingMs, task.focusReminderIntervalSeconds) {
                                            val rawList = when {
                                                remainingMs <= 5 * 60 * 1000L -> {
                                                    listOf(
                                                        30 to "30s",
                                                        60 to "1m",
                                                        120 to "2m",
                                                        300 to "5m"
                                                    )
                                                }
                                                remainingMs <= 30 * 60 * 1000L -> {
                                                    listOf(
                                                        30 to "30s",
                                                        60 to "1m",
                                                        300 to "5m",
                                                        600 to "10m"
                                                    )
                                                }
                                                remainingMs <= 2 * 60 * 60 * 1000L -> {
                                                    listOf(
                                                        60 to "1m",
                                                        300 to "5m",
                                                        1800 to "30m",
                                                        3600 to "1h"
                                                    )
                                                }
                                                remainingMs <= 12 * 60 * 60 * 1000L -> {
                                                    listOf(
                                                        300 to "5m",
                                                        1800 to "30m",
                                                        3600 to "1h",
                                                        7200 to "2h"
                                                    )
                                                }
                                                else -> {
                                                    listOf(
                                                        1800 to "30m",
                                                        3600 to "1h",
                                                        7200 to "2h",
                                                        14400 to "4h"
                                                    )
                                                }
                                            }
                                            
                                            val baseList = listOf(10 to "10s (Test)") + rawList
                                            
                                            if (baseList.any { it.first == task.focusReminderIntervalSeconds }) {
                                                baseList
                                            } else {
                                                val currentSec = task.focusReminderIntervalSeconds
                                                val currentLabel = when {
                                                    currentSec < 60 -> "${currentSec}s"
                                                    currentSec < 3600 -> "${currentSec / 60}m"
                                                    else -> "${currentSec / 3600}h"
                                                }
                                                (baseList + (currentSec to currentLabel)).sortedBy { it.first }.distinctBy { it.first }
                                            }
                                        }
                                        intervals.forEach { (sec, label) ->
                                            val isSelected = task.focusReminderIntervalSeconds == sec
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isSelected) Color(0xFFF97316) else Color.White.copy(alpha = 0.08f),
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .background(
                                                        color = if (isSelected) Color(0xFFF97316).copy(alpha = 0.12f) else Color.Transparent,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable {
                                                        viewModel.updateTaskAndSteps(
                                                            task.copy(focusReminderIntervalSeconds = sec),
                                                            steps
                                                        )
                                                    }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSelected) Color(0xFFF97316) else Color(0xFF94A3B8),
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.05f))
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Edit and Delete buttons (symmetric and properly arranged)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showEditDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFCD34D)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFFCD34D).copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Commitment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFEF4444)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete Task", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditTaskDialog(
            task = task,
            steps = steps,
            categories = categories,
            onDismiss = { showEditDialog = false },
            onSave = { updatedTask, updatedSteps ->
                viewModel.updateTaskAndSteps(updatedTask, updatedSteps)
                showEditDialog = false
            }
        )
    }

    if (showTickOptions) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showTickOptions = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF111111)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Task Actions",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose an action for: '${task.title}'",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )

                    // Option 2: X Not Completed and send to task history
                    Button(
                        onClick = {
                            viewModel.updateTaskAndSteps(
                                task.copy(
                                    isCompleted = true,
                                    isFailed = true,
                                    isStrikethrough = false,
                                    completedAt = System.currentTimeMillis()
                                ),
                                steps
                            )
                            showTickOptions = false
                            viewModel.speak("Task marked as failed and archived.")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                            contentColor = Color(0xFFFCA5A5)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("❌", fontSize = 16.sp)
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "Mark Failed & Archive",
                                    fontSize = 14.sp,
                                    color = Color(0xFFFCA5A5),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Send to history as NOT completed",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFCA5A5).copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Option 3: Completed task and send to task history
                    Button(
                        onClick = {
                            viewModel.updateTaskAndSteps(
                                task.copy(
                                    isCompleted = true,
                                    isFailed = false,
                                    isStrikethrough = false,
                                    completedAt = System.currentTimeMillis()
                                ),
                                steps
                            )
                            showTickOptions = false
                            viewModel.speak("Excellent work! Commitment fulfilled and saved to archive.")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✅", fontSize = 16.sp)
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "Complete Task & Archive",
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Send to history as COMPLETED",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = { showTickOptions = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }
}

   @OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskBottomSheet(
    categories: List<String>,
    onDismiss: () -> Unit,
    onAdd: (title: String, desc: String, startTimeMs: Long, deadlineMs: Long, priority: String, category: String) -> Unit,
    onAddCategory: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("HIGH") }
    
    val selectableCategories = remember(categories) {
        categories.filter { it != "All" }
    }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showAddCategoryDialogInSheet by remember { mutableStateOf(false) }
    var newCategoryInSheet by remember { mutableStateOf("") }
    
    var selectedDurationIndex by remember { mutableStateOf(0) }

    val durations = listOf(
        "30 Min" to TimeUnit.MINUTES.toMillis(30),
        "45 Min" to TimeUnit.MINUTES.toMillis(45),
        "60 Min" to TimeUnit.MINUTES.toMillis(60),
        "2 Hr" to TimeUnit.HOURS.toMillis(2),
        "3 Hr" to TimeUnit.HOURS.toMillis(3),
        "Custom" to -1L
    )

    val calendar = remember { Calendar.getInstance() }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    val defaultDate = remember { dateFormat.format(calendar.time) }
    val defaultTime = remember { timeFormat.format(calendar.time) }
    
    var startDateInput by remember { mutableStateOf(defaultDate) }
    var startTimeInput by remember { mutableStateOf(defaultTime) }
    
    var deadlineDateInput by remember { mutableStateOf(defaultDate) }
    var deadlineTimeInput by remember { mutableStateOf(defaultTime) }

    val parsedStartMs: Long? = remember(startDateInput, startTimeInput) {
        try {
            val fullString = "${startDateInput.trim()} ${startTimeInput.trim()}"
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            sdf.isLenient = false
            sdf.parse(fullString)?.time
        } catch (e: Exception) {
            null
        }
    }

    val parsedDeadlineMs: Long? = remember(deadlineDateInput, deadlineTimeInput) {
        try {
            val fullString = "${deadlineDateInput.trim()} ${deadlineTimeInput.trim()}"
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            sdf.isLenient = false
            sdf.parse(fullString)?.time
        } catch (e: Exception) {
            null
        }
    }

    val selectedDuration = durations[selectedDurationIndex]
    val isCustomSelected = selectedDuration.first == "Custom"

    val activeStartMs = remember(selectedDurationIndex, parsedStartMs) {
        if (isCustomSelected) {
            parsedStartMs
        } else {
            System.currentTimeMillis()
        }
    }

    val activeDeadlineMs = remember(selectedDurationIndex, parsedDeadlineMs) {
        if (isCustomSelected) {
            parsedDeadlineMs
        } else {
            System.currentTimeMillis() + selectedDuration.second
        }
    }

    val scope = rememberCoroutineScope()
    var dismissAttemptCount by remember { mutableStateOf(0) }
    var forceDismiss by remember { mutableStateOf(false) }

    // Reset attempt count after some time if they don't try again
    LaunchedEffect(dismissAttemptCount) {
        if (dismissAttemptCount > 0) {
            delay(3500)
            dismissAttemptCount = 0
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            if (newValue == SheetValue.Hidden) {
                if (forceDismiss || dismissAttemptCount >= 1) {
                    true
                } else {
                    dismissAttemptCount = 1
                    false
                }
            } else {
                true
            }
        }
    )

    ModalBottomSheet(
        onDismissRequest = {
            if (forceDismiss || dismissAttemptCount >= 1) {
                onDismiss()
            } else {
                dismissAttemptCount = 1
            }
        },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) },
        containerColor = Color(0xFF0F0F15),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            if (dismissAttemptCount == 1) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "⚠️ Swipe down again (or click Cancel) to close and discard changes.",
                            color = Color(0xFFFCA5A5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Text(
                    text = "New Commitment",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Title
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("TITLE", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Task Title (e.g. Approve Budget)", color = Color(0xFF64748B)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.04f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color(0xFFF97316)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("task_title_input")
                    )
                }
            }

            // Description
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("DESCRIPTION", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    TextField(
                        value = desc,
                        onValueChange = { desc = it },
                        placeholder = { Text("Short Description (e.g. Due tonight)", color = Color(0xFF64748B)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.04f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color(0xFFF97316)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("task_desc_input")
                    )
                }
            }

            // Category
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CATEGORY", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(selectableCategories) { cat ->
                            val isSelected = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(if (isSelected) Color(0xFFFCD34D) else Color.White.copy(alpha = 0.03f))
                                    .clickable { selectedCategory = if (selectedCategory == cat) null else cat }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) Color(0xFF0A0B10) else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .clickable { showAddCategoryDialogInSheet = true }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("+ New", color = Color(0xFFFCD34D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (showAddCategoryDialogInSheet) {
                    AlertDialog(
                        onDismissRequest = { showAddCategoryDialogInSheet = false },
                        title = { Text("Add Custom Category", color = Color.White) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Enter a name for your custom commitment category:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                TextField(
                                    value = newCategoryInSheet,
                                    onValueChange = { newCategoryInSheet = it },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.White.copy(alpha = 0.04f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedIndicatorColor = Color(0xFFF97316)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (newCategoryInSheet.trim().isNotEmpty()) {
                                        onAddCategory(newCategoryInSheet.trim())
                                        selectedCategory = newCategoryInSheet.trim()
                                        newCategoryInSheet = ""
                                        showAddCategoryDialogInSheet = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFCD34D), contentColor = Color(0xFF0F0F15))
                            ) {
                                Text("Add")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddCategoryDialogInSheet = false }) {
                                Text("Cancel", color = Color(0xFF94A3B8))
                            }
                        },
                        containerColor = Color(0xFF151520)
                    )
                }
            }

            // Priority
            item {
                Column {
                    Text("PRIORITY LEVEL", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("HIGH", "MEDIUM", "LOW").forEach { priority ->
                            val isSelected = selectedPriority == priority
                            val pillBgColor = when {
                                !isSelected -> Color.White.copy(alpha = 0.03f)
                                priority == "HIGH" -> Color(0xFFF97316)
                                priority == "MEDIUM" -> Color(0xFFFCD34D)
                                else -> Color(0xFF10B981)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(pillBgColor)
                                    .clickable { selectedPriority = priority }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = priority,
                                    color = if (isSelected) Color(0xFF0A0B10) else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Due Date Selector
            item {
                Column {
                    Text("DUE IN (TIME BLOCKS)", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        durations.take(3).forEachIndexed { index, (label, _) ->
                            val isSelected = selectedDurationIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                    .border(1.dp, if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable { selectedDurationIndex = index }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        durations.drop(3).take(2).forEachIndexed { idx, (label, _) ->
                            val index = idx + 3
                            val isSelected = selectedDurationIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                    .border(1.dp, if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable { selectedDurationIndex = index }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isSelected = selectedDurationIndex == 5
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                .border(1.dp, if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { selectedDurationIndex = 5 }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Custom (Choose Date & Time)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Custom date and time fields
            if (isCustomSelected) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("START OF TASK", color = Color(0xFFF97316), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Start Date field
                            TextField(
                                value = startDateInput,
                                onValueChange = { startDateInput = it },
                                label = { Text("Start Date (dd/mm/yyyy)", color = Color(0xFF64748B), fontSize = 11.sp) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(alpha = 0.04f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color(0xFFF97316)
                                ),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .clip(RoundedCornerShape(12.dp)),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                            )

                            // Start Time field
                            TextField(
                                value = startTimeInput,
                                onValueChange = { startTimeInput = it },
                                label = { Text("Start Time (hr:min)", color = Color(0xFF64748B), fontSize = 11.sp) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(alpha = 0.04f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color(0xFFF97316)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp)),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                            )
                        }
                        
                        if (parsedStartMs == null) {
                            Text(
                                text = "Invalid start format. Expected dd/mm/yyyy and hh:mm",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("DEADLINE", color = Color(0xFFF97316), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Deadline Date field
                            TextField(
                                value = deadlineDateInput,
                                onValueChange = { deadlineDateInput = it },
                                label = { Text("Deadline Date (dd/mm/yyyy)", color = Color(0xFF64748B), fontSize = 11.sp) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(alpha = 0.04f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color(0xFFFCD34D)
                                ),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .clip(RoundedCornerShape(12.dp)),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                            )

                            // Deadline Time field
                            TextField(
                                value = deadlineTimeInput,
                                onValueChange = { deadlineTimeInput = it },
                                label = { Text("Deadline Time (hr:min)", color = Color(0xFF64748B), fontSize = 11.sp) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(alpha = 0.04f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color(0xFFFCD34D)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp)),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                            )
                        }
                        
                        if (parsedDeadlineMs == null) {
                            Text(
                                text = "Invalid deadline format. Expected dd/mm/yyyy and hh:mm",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            forceDismiss = true
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            if (title.isNotEmpty() && activeDeadlineMs != null) {
                                forceDismiss = true
                                onAdd(title, desc, activeStartMs ?: System.currentTimeMillis(), activeDeadlineMs, selectedPriority, selectedCategory ?: "")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        enabled = title.isNotEmpty() && activeDeadlineMs != null && (!isCustomSelected || activeStartMs != null),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("submit_task_button")
                    ) {
                        Text("Save Task", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: Task,
    steps: List<TaskStep>,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (updatedTask: Task, updatedSteps: List<TaskStep>) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var desc by remember { mutableStateOf(task.description) }
    var selectedPriority by remember { mutableStateOf(task.priority) }
    
    val selectableCategories = remember(categories) {
        categories.filter { it != "All" }
    }
    var selectedCategory by remember { mutableStateOf<String?>(task.category.ifEmpty { null }) }
    
    // Manage local list of steps for editing
    var editableSteps by remember { mutableStateOf(steps) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0A0B10).copy(alpha = 0.98f)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Edit Commitment",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Title
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("TITLE", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        TextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("Task Title", color = Color(0xFF64748B)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.04f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color(0xFFF97316)
                            ),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        )
                    }
                }

                // Description
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("DESCRIPTION", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        TextField(
                            value = desc,
                            onValueChange = { desc = it },
                            placeholder = { Text("Short Description", color = Color(0xFF64748B)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.04f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color(0xFFF97316)
                            ),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        )
                    }
                }

                // Category
                item {
                    Column {
                        Text("CATEGORY", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(selectableCategories) { cat ->
                                val isSelected = selectedCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(if (isSelected) Color(0xFFFCD34D) else Color.White.copy(alpha = 0.03f))
                                        .clickable { selectedCategory = if (selectedCategory == cat) null else cat }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        color = if (isSelected) Color(0xFF0A0B10) else Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Priority
                item {
                    Column {
                        Text("PRIORITY LEVEL", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("HIGH", "MEDIUM", "LOW").forEach { priority ->
                                val isSelected = selectedPriority == priority
                                val pillBgColor = when {
                                    !isSelected -> Color.White.copy(alpha = 0.03f)
                                    priority == "HIGH" -> Color(0xFFF97316)
                                    priority == "MEDIUM" -> Color(0xFFFCD34D)
                                    else -> Color(0xFF10B981)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(pillBgColor)
                                        .clickable { selectedPriority = priority }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = priority,
                                        color = if (isSelected) Color(0xFF0A0B10) else Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Steps list section
                if (editableSteps.isNotEmpty()) {
                    item {
                        Text(
                            text = "TACTICAL RESCUE STEPS",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    items(editableSteps.size) { index ->
                        val step = editableSteps[index]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Duration text field
                            TextField(
                                value = step.timeSpan,
                                onValueChange = { newVal ->
                                    editableSteps = editableSteps.toMutableList().apply {
                                        this[index] = step.copy(timeSpan = newVal)
                                    }
                                },
                                placeholder = { Text("Duration", color = Color(0xFF64748B)) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(alpha = 0.04f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color(0xFFFCD34D)
                                ),
                                modifier = Modifier.width(90.dp).clip(RoundedCornerShape(8.dp)),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            )

                            // Step Text field
                            TextField(
                                value = step.stepText,
                                onValueChange = { newVal ->
                                    editableSteps = editableSteps.toMutableList().apply {
                                        this[index] = step.copy(stepText = newVal)
                                    }
                                },
                                placeholder = { Text("Step text", color = Color(0xFF64748B)) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(alpha = 0.04f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color(0xFFFCD34D)
                                ),
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )

                            // Delete step button
                            IconButton(
                                onClick = {
                                    editableSteps = editableSteps.toMutableList().apply {
                                        removeAt(index)
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete step",
                                    tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    item {
                        // Add custom step button
                        TextButton(
                            onClick = {
                                editableSteps = editableSteps + TaskStep(
                                    taskId = task.id,
                                    timeSpan = "Step ${editableSteps.size + 1}",
                                    stepText = "",
                                    isCompleted = false
                                )
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFCD34D))
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Step", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Step", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", fontSize = 14.sp)
                        }

                        Button(
                            onClick = {
                                if (title.isNotEmpty()) {
                                    onSave(
                                        task.copy(
                                            title = title,
                                            description = desc,
                                            priority = selectedPriority,
                                            category = selectedCategory ?: ""
                                        ),
                                        editableSteps
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316), contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            enabled = title.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Changes", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
