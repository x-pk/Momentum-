package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val startTime: Long = System.currentTimeMillis(), // timestamp in ms
    val deadline: Long, // timestamp in ms
    val priority: String, // "HIGH", "MEDIUM", "LOW"
    val category: String, // "Work", "Study", "Finance", "Personal", "Health"
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val aiProactiveAdvice: String? = null,
    val isStrikethrough: Boolean = false,
    val isFailed: Boolean = false,
    val isFocusReminderEnabled: Boolean = true,
    val focusReminderIntervalSeconds: Int = 300, // default to 5 minutes
    val reminderDays: String = "Mon,Tue,Wed,Thu,Fri,Sat,Sun",
    val reminderStartHour: Int = 9,
    val reminderStartMinute: Int = 0,
    val reminderEndHour: Int = 17,
    val reminderEndMinute: Int = 0
)

@Entity(tableName = "task_steps")
data class TaskStep(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,
    val stepText: String,
    val isCompleted: Boolean = false,
    val timeSpan: String // e.g., "10 mins", "Day 1", "Step 1"
)
