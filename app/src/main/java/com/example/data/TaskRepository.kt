package com.example.data

import com.example.api.GeminiClient
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import android.util.Log

class TaskRepository(private val taskDao: TaskDao) {

    fun getAllTasksFlow(): Flow<List<Task>> = taskDao.getAllTasksFlow()

    suspend fun getTaskById(taskId: Int): Task? = taskDao.getTaskById(taskId)

    fun getStepsForTaskFlow(taskId: Int): Flow<List<TaskStep>> = taskDao.getStepsForTaskFlow(taskId)

    suspend fun getStepsForTask(taskId: Int): List<TaskStep> = taskDao.getStepsForTask(taskId)

    suspend fun insertTask(task: Task): Int {
        return taskDao.insertTask(task).toInt()
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteStepsForTask(task.id)
        taskDao.deleteTask(task)
    }

    suspend fun updateStep(step: TaskStep) {
        taskDao.updateStep(step)
    }

    suspend fun updateTaskAndSteps(task: Task, steps: List<TaskStep>) {
        taskDao.updateTask(task)
        taskDao.deleteStepsForTask(task.id)
        if (steps.isNotEmpty()) {
            taskDao.insertSteps(steps)
        }
    }

    private fun isAmbiguousOrGibberish(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return true
        
        // No letters or digits (e.g., only emojis, symbols, or punctuation like '🤷‍♂️😂' or '!!!')
        val hasLettersOrDigits = trimmed.any { it.isLetterOrDigit() }
        if (!hasLettersOrDigits) return true
        
        val lower = trimmed.lowercase()
        // Common gibberish words and keyboard mash patterns
        val keyboardMashes = listOf("ihfifhso", "asdf", "qwer", "zxcv", "dfgh", "ghjk", "jkl;", "xcvb", "cvbn")
        if (keyboardMashes.any { lower.contains(it) }) return true
        
        // Single word of length >= 5 with no vowels
        if (!trimmed.contains(" ") && trimmed.length >= 5) {
            val hasVowel = trimmed.any { "aeiouy".contains(it.lowercaseChar()) }
            if (!hasVowel) return true
        }
        
        return false
    }

    /**
     * Uses Gemini to generate a tailored rescue plan for a task and saves the steps to the DB.
     */
    suspend fun generateRescuePlan(taskId: Int): Boolean {
        val task = taskDao.getTaskById(taskId) ?: return false
        val now = System.currentTimeMillis()
        val durationMs = task.deadline - now

        val durationDesc = if (durationMs <= 0) {
            "OVERDUE"
        } else {
            val days = TimeUnit.MILLISECONDS.toDays(durationMs)
            val hours = TimeUnit.MILLISECONDS.toHours(durationMs) % 24
            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
            when {
                days > 0 -> "$days days and $hours hours"
                hours > 0 -> "$hours hours and $minutes minutes"
                else -> "$minutes minutes"
            }
        }

        var updatedTitle = task.title
        val isAmbiguous = isAmbiguousOrGibberish(task.title)
        if (isAmbiguous) {
            val categoryTaskName = when (task.category.lowercase(java.util.Locale.getDefault())) {
                "academic" -> "Prepare for exam"
                "health" -> "Meal prep"
                "personal", "person" -> "Organize room"
                else -> "Strategic Focus Planning"
            }
            updatedTitle = "Random Task #$taskId - $categoryTaskName"
        }

        val prompt = """
            You are 'Life Saver', an elite AI deadline rescue coach. 
            The user is in danger of missing an important commitment. Proactively create a custom, high-impact tactical plan of action to rescue this task before it is too late.
            
            Task Information:
            - Title: $updatedTitle
            - Description: ${task.description}
            - Priority: ${task.priority}
            - Category: ${task.category}
            - Time Remaining: $durationDesc
            
            CRITICAL INSTRUCTION FOR PLAN GENERATION:
            You MUST prioritize the task's Title and Description over its Category! The user might have selected the wrong category by accident (e.g., they chose 'Academic' category for a task called 'Buy groceries' or 'Clean sink').
            If the Title or Description is clear (e.g. contains action verbs like buy, clean, gym, cook, work, study, etc.), generate a plan that is 100% tailored to the Title and Description. Completely ignore the selected Category if it conflicts with the Title/Description's actual purpose. Only focus on the Category as a hint if the Title and Description are highly vague, ambiguous, or empty.
            
            Based on the remaining time, partition the work into exactly 3 to 5 realistic, sequential action items. 
            If the time remaining is more than 24 hours, partition the steps by day (e.g. 'Day 1', 'Day 2', 'Day 3').
            If the time remaining is less than 24 hours, partition the steps by time duration or specific countdown (e.g. 'First 15 mins', 'Next 30 mins', 'Final 10 mins').
            
            You MUST output the steps in the exact format shown below, with one step per line. Do NOT output any markdown tags, introduction text, list numbers, or final conversational remarks.
            Format to follow exactly:
            TIME_SEGMENT | ACTION_STEP
            
            Example output for long duration:
            Day 1 | Gather background data and reference sheets
            Day 2 | Draft core bullet points and main outline
            Day 3 | Polish wording and verify final submission guidelines
            
            Example output for short duration:
            First 10m | Clean workspace and disable social media alerts
            Next 30m | Direct draft formulation with zero-stops editing
            Final 15m | Final grammatical check and submit instantly
            
            Now generate the tactical rescue plan for this task:
        """.trimIndent()

        val systemInstruction = "You are 'Life Saver', an elite, direct, action-oriented productivity coach. You only answer in the exact requested schema 'TIME_SEGMENT | ACTION_STEP' with one line per step. No extra talk."

        val steps = mutableListOf<TaskStep>()
        
        try {
            val response = GeminiClient.generateText(prompt, systemInstruction)
            Log.d("TaskRepository", "Gemini rescue plan response: $response")

            val lines = response.lines()
            for (line in lines) {
                if (line.isBlank()) continue
                val parts = line.split("|", limit = 2)
                if (parts.size == 2) {
                    val timeSpan = parts[0].trim().replace("^-\\s*".toRegex(), "") // Clean list markers if any
                    val stepText = parts[1].trim()
                    if (stepText.isNotEmpty()) {
                        steps.add(
                            TaskStep(
                                taskId = taskId,
                                stepText = stepText,
                                timeSpan = timeSpan,
                                isCompleted = false
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TaskRepository", "Gemini failed, using local fallback steps", e)
        }

        // Fallback if empty or failed
        if (steps.isEmpty()) {
            val titleAndDesc = (task.title + " " + task.description).lowercase(java.util.Locale.getDefault())
            
            val detectedCategory = when {
                // Academic indicators
                titleAndDesc.contains("study") || titleAndDesc.contains("exam") || titleAndDesc.contains("test") ||
                titleAndDesc.contains("homework") || titleAndDesc.contains("class") || titleAndDesc.contains("lecture") ||
                titleAndDesc.contains("read") || titleAndDesc.contains("book") || titleAndDesc.contains("quiz") ||
                titleAndDesc.contains("assignment") || titleAndDesc.contains("grade") -> "academic"
                
                // Health indicators
                titleAndDesc.contains("health") || titleAndDesc.contains("diet") || titleAndDesc.contains("workout") ||
                titleAndDesc.contains("gym") || titleAndDesc.contains("med") || titleAndDesc.contains("doctor") ||
                titleAndDesc.contains("pill") || titleAndDesc.contains("run") || titleAndDesc.contains("walk") ||
                titleAndDesc.contains("meal") || titleAndDesc.contains("food") || titleAndDesc.contains("cook") -> "health"
                
                // Personal / Chores indicators
                titleAndDesc.contains("clean") || titleAndDesc.contains("organize") || titleAndDesc.contains("grocery") ||
                titleAndDesc.contains("groceries") || titleAndDesc.contains("room") || titleAndDesc.contains("sink") ||
                titleAndDesc.contains("buy") || titleAndDesc.contains("laundry") || titleAndDesc.contains("wash") ||
                titleAndDesc.contains("vacuum") || titleAndDesc.contains("trash") -> "personal"
                
                // Default to selected category
                else -> task.category.lowercase(java.util.Locale.getDefault())
            }

            val fallback = when (detectedCategory) {
                "academic" -> listOf(
                    "First 15m" to "Clean workspace and gather study notes",
                    "Next 45m" to "Focus-review core definitions and high-yield concepts",
                    "Final 15m" to "Complete mock practice questions and summarize errors"
                )
                "health" -> listOf(
                    "First 10m" to "Set clean dietary priorities and draft grocery list",
                    "Next 30m" to "Direct food preparation and label storage containers",
                    "Final 15m" to "Clean kitchen surfaces and log nutrient metrics"
                )
                "personal", "person" -> listOf(
                    "First 15m" to "Segregate clutter and prioritize workspace organization",
                    "Next 30m" to "Systematic cleaning and returning items to designated spots",
                    "Final 15m" to "Final room aesthetic adjustment and checklist completion"
                )
                else -> listOf(
                    "First 15m" to "Deep-focus objective definitions and roadblocking analysis",
                    "Next 35m" to "Dedicated execute loops with complete digital silence",
                    "Final 10m" to "Verify quality parameters and record completion history"
                )
            }
            
            for ((timeSpan, stepText) in fallback) {
                steps.add(
                    TaskStep(
                        taskId = taskId,
                        stepText = stepText,
                        timeSpan = timeSpan,
                        isCompleted = false
                    )
                )
            }
        }

        taskDao.deleteStepsForTask(taskId)
        taskDao.insertSteps(steps)
        
        // Generate short advice as well
        val finalTask = task.copy(title = updatedTitle)
        val advicePrompt = "In 25 words or less, give a highly motivating, critical piece of advice to help the user complete: '${finalTask.title}' which is due in $durationDesc."
        val advice = try {
            GeminiClient.generateText(advicePrompt, "You are a motivating productivity coach. Answer in one punchy sentence.")
        } catch (e: Exception) {
            "Time block allocated. Minimize distractions and execute immediately!"
        }
        
        taskDao.updateTask(finalTask.copy(aiProactiveAdvice = advice.trim()))
        return true
    }

    /**
     * Get personalized AI assistant analysis for all tasks.
     */
    suspend fun generateProactiveAnalysis(tasks: List<Task>): String {
        if (tasks.isEmpty()) {
            return "No pending tasks! You are currently safe and fully rescued. Keep it up!"
        }
        val activeTasks = tasks.filter { !it.isCompleted }
        if (activeTasks.isEmpty()) {
            return "All current tasks are successfully completed! The Life Saver database is clear. You've avoided all last-minute disasters."
        }

        val tasksSummary = activeTasks.joinToString("\n") { task ->
            val hoursLeft = (task.deadline - System.currentTimeMillis()) / (1000 * 60 * 60)
            "- '${task.title}' (${task.category}, Priority: ${task.priority}) due in $hoursLeft hours."
        }

        val prompt = """
            You are 'Life Saver' AI. Analyze the user's active task backlog and provide an urgent, highly strategic, personalized prioritization briefing. 
            Highlight the single most critical task they are in danger of missing, outline how to avoid failure, and suggest a smart schedule optimization or micro-habit.
            
            Active Task Backlog:
            $tasksSummary
            
            Keep your briefing punchy, direct, and structured with clean sections using emojis. Limit response to 120 words.
        """.trimIndent()

        return GeminiClient.generateText(prompt, "You are an elite productivity briefing assistant.")
    }
}
