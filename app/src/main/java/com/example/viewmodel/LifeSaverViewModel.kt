package com.example.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Task
import com.example.data.TaskRepository
import com.example.data.TaskStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.TimeUnit

class LifeSaverViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val repository: TaskRepository
    val tasksState: StateFlow<List<Task>>

    // UI Tab state
    val currentTab = mutableStateOf("Tasks")

    // AI Briefing State
    private val _aiBriefing = MutableStateFlow<String>("Click 'Optimize Calendar' or request a strategic briefing in the AI Assist tab.")
    val aiBriefing: StateFlow<String> = _aiBriefing.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // Key of task that is currently generating an AI rescue plan
    private val _generatingPlanTaskId = MutableStateFlow<Int?>(null)
    val generatingPlanTaskId: StateFlow<Int?> = _generatingPlanTaskId.asStateFlow()

    // TextToSpeech Engine
    private var tts: TextToSpeech? = null
    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    // Focus Reminder System
    private val _focusReminderInterval = MutableStateFlow(10) // default 10 seconds for instant testing
    val focusReminderInterval: StateFlow<Int> = _focusReminderInterval.asStateFlow()

    private val _isFocusReminderEnabled = MutableStateFlow(false)
    val isFocusReminderEnabled: StateFlow<Boolean> = _isFocusReminderEnabled.asStateFlow()

    private val _focusReminderStyle = MutableStateFlow("Encouraging")
    val focusReminderStyle: StateFlow<String> = _focusReminderStyle.asStateFlow()

    private val _lastReminderMessage = MutableStateFlow("No reminders sent yet. Turn on Focus Pulse above to start!")
    val lastReminderMessage: StateFlow<String> = _lastReminderMessage.asStateFlow()

    // Persistent Theme Mode, Coach voice, and User Name
    private val sharedPrefs = application.getSharedPreferences("lifesaver_prefs", Context.MODE_PRIVATE)
    private val _themeState = MutableStateFlow(sharedPrefs.getString("theme_mode", "Dark") ?: "Dark")
    val themeState: StateFlow<String> = _themeState.asStateFlow()

    private val _coachVoice = MutableStateFlow(sharedPrefs.getString("coach_voice", "Adam") ?: "Adam")
    val coachVoice: StateFlow<String> = _coachVoice.asStateFlow()

    private val _userName = MutableStateFlow(sharedPrefs.getString("user_name", "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val prefsChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "last_reminder_message") {
            _lastReminderMessage.value = sharedPrefs.getString("last_reminder_message", "") ?: ""
        }
    }

    // Thread-safe Chat History State for AI Assist
    private val _chatHistory = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf("Hello! I am 'Life Saver', your elite tactical productivity AI. Ask me anything about your tasks, or click 'Optimize Calendar' above to analyze your schedule!" to false)
    )
    val chatHistoryState: StateFlow<List<Pair<String, Boolean>>> = _chatHistory.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // Dynamic list of categories
    private val _categories = MutableStateFlow(listOf("All", "Academic", "Health", "Personal"))
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    fun setTheme(theme: String) {
        _themeState.value = theme
        sharedPrefs.edit().putString("theme_mode", theme).apply()
    }

    fun setCoachVoice(voice: String) {
        _coachVoice.value = voice
        sharedPrefs.edit().putString("coach_voice", voice).apply()
        applyCoachVoiceSettings()
    }

    fun setUserName(name: String) {
        val trimmed = name.trim()
        _userName.value = trimmed
        sharedPrefs.edit().putString("user_name", trimmed).apply()
    }

    fun applyCoachVoiceSettings() {
        try {
            val voiceMode = _coachVoice.value
            val isMale = voiceMode == "Adam"
            
            // Set Pitch and SpeechRate as a base backup
            if (isMale) {
                tts?.setPitch(0.80f) // Deep masculine pitch
                tts?.setSpeechRate(0.92f) // Commanding pace
            } else {
                tts?.setPitch(1.15f) // Bright feminine pitch
                tts?.setSpeechRate(1.05f) // Energetic pace
            }

            val availableVoices = tts?.voices
            if (availableVoices != null) {
                // Find a US English voice matching the requested gender
                val targetVoice = availableVoices.firstOrNull { voice ->
                    val name = voice.name.lowercase()
                    val isUsEnglish = voice.locale.language == "en" && voice.locale.country == "US"
                    
                    if (isUsEnglish) {
                        if (isMale) {
                            name.contains("male") || name.contains("m-local") || name.contains("iom") || name.contains("sfg") || name.contains("jef")
                        } else {
                            name.contains("female") || name.contains("f-local") || name.contains("tpf") || name.contains("iol") || name.contains("sfn")
                        }
                    } else {
                        false
                    }
                }
                
                if (targetVoice != null) {
                    tts?.setVoice(targetVoice)
                    Log.d("LifeSaverViewModel", "Applied TTS voice: ${targetVoice.name}")
                } else {
                    // Fallback to any en-US voice if gender-specific is not found
                    val fallbackVoice = availableVoices.firstOrNull { 
                        it.locale.language == "en" && it.locale.country == "US" 
                    }
                    if (fallbackVoice != null) {
                        tts?.setVoice(fallbackVoice)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LifeSaverViewModel", "Failed to apply coach voice", e)
        }
    }

    fun sendChatMessage(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return
        
        // Add user message to history
        val currentList = _chatHistory.value.toMutableList()
        currentList.add(trimmed to true)
        _chatHistory.value = currentList
        
        _isChatLoading.value = true
        viewModelScope.launch {
            try {
                val activeTasks = tasksState.value.filter { !it.isCompleted }
                val tasksStr = if (activeTasks.isEmpty()) {
                    "No active tasks"
                } else {
                    activeTasks.joinToString("\n") { "- ${it.title} (${it.category}, priority: ${it.priority}, due in ${((it.deadline - System.currentTimeMillis()) / 60000)} mins)" }
                }
                
                val prompt = """
                    You are 'Life Saver', an elite, highly supportive but deeply tactical action-oriented productivity companion. 
                    The user is asking: "$trimmed"
                    
                    Active Tasks Backlog:
                    $tasksStr
                    
                    Provide an immediate, highly motivating, and practical tactical answer. Highlight if they are procrastinating. Limit response to 120 words. Keep it structured and elegant.
                """.trimIndent()
                
                val response = com.example.api.GeminiClient.generateText(prompt, "You are 'Life Saver', an elite productivity coach assistant. Speak directly with a highly encouraging and tactical tone.")
                
                val updatedList = _chatHistory.value.toMutableList()
                updatedList.add(response.trim() to false)
                _chatHistory.value = updatedList
            } catch (e: Exception) {
                Log.e("LifeSaverViewModel", "Failed to get AI chat response", e)
                val updatedList = _chatHistory.value.toMutableList()
                updatedList.add("Connection issues encountered. Please make sure your GEMINI_API_KEY is configured in the Secrets panel." to false)
                _chatHistory.value = updatedList
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun addCategory(categoryName: String) {
        val trimmed = categoryName.trim()
        if (trimmed.isNotEmpty() && !_categories.value.any { it.equals(trimmed, ignoreCase = true) }) {
            _categories.value = _categories.value + trimmed
        }
    }

    private var reminderJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao())
        
        tasksState = repository.getAllTasksFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // Initialize from persistent SharedPreferences
        _focusReminderStyle.value = sharedPrefs.getString("focus_reminder_style", "Encouraging") ?: "Encouraging"
        _lastReminderMessage.value = sharedPrefs.getString("last_reminder_message", "No reminders sent yet. Focus Coach is monitoring in the background!") ?: "No reminders sent yet. Focus Coach is monitoring in the background!"
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefsChangeListener)

        // Initialize Native Android Text To Speech
        try {
            tts = TextToSpeech(application, this)
        } catch (e: Exception) {
            Log.e("LifeSaverViewModel", "Failed to initialize TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("LifeSaverViewModel", "TTS US Language not supported")
            } else {
                _isTtsReady.value = true
                applyCoachVoiceSettings()
                Log.d("LifeSaverViewModel", "TTS initialized successfully!")
            }
        } else {
            Log.e("LifeSaverViewModel", "TTS initialization failed")
        }
    }

    /**
     * Speaks the specified text out loud.
     */
    fun speak(text: String) {
        if (_isTtsReady.value) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "LifeSaverSpeakId")
        } else {
            Log.w("LifeSaverViewModel", "TTS is not ready yet")
        }
    }

    fun addTask(title: String, description: String, startTimeMs: Long, deadlineMs: Long, priority: String, category: String) {
        viewModelScope.launch {
            // Smart sync: determine days and active daily reminder time window from the task timeframe
            val calendarStart = java.util.Calendar.getInstance().apply { timeInMillis = startTimeMs }
            val startHour = calendarStart.get(java.util.Calendar.HOUR_OF_DAY)
            val startMinute = calendarStart.get(java.util.Calendar.MINUTE)

            val calendarEnd = java.util.Calendar.getInstance().apply { timeInMillis = deadlineMs }
            val endHour = calendarEnd.get(java.util.Calendar.HOUR_OF_DAY)
            val endMinute = calendarEnd.get(java.util.Calendar.MINUTE)

            val daysToRemind = mutableSetOf<String>()
            val tempCalendar = java.util.Calendar.getInstance().apply { timeInMillis = startTimeMs }
            val daysLimit = 100 // safety limit
            var loopCount = 0
            while (tempCalendar.timeInMillis <= deadlineMs && loopCount < daysLimit) {
                val dayStr = when (tempCalendar.get(java.util.Calendar.DAY_OF_WEEK)) {
                    java.util.Calendar.MONDAY -> "Mon"
                    java.util.Calendar.TUESDAY -> "Tue"
                    java.util.Calendar.WEDNESDAY -> "Wed"
                    java.util.Calendar.THURSDAY -> "Thu"
                    java.util.Calendar.FRIDAY -> "Fri"
                    java.util.Calendar.SATURDAY -> "Sat"
                    java.util.Calendar.SUNDAY -> "Sun"
                    else -> ""
                }
                if (dayStr.isNotEmpty()) {
                    daysToRemind.add(dayStr)
                }
                tempCalendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                loopCount++
            }

            val finalDays = if (daysToRemind.isEmpty()) {
                "Mon,Tue,Wed,Thu,Fri,Sat,Sun"
            } else {
                val standardOrder = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                standardOrder.filter { daysToRemind.contains(it) }.joinToString(",")
            }

            val task = Task(
                title = title,
                description = description,
                startTime = startTimeMs,
                deadline = deadlineMs,
                priority = priority,
                category = category,
                isCompleted = false,
                reminderDays = finalDays,
                reminderStartHour = startHour,
                reminderStartMinute = startMinute,
                reminderEndHour = endHour,
                reminderEndMinute = endMinute
            )
            val newId = repository.insertTask(task)
            // Auto generate rescue plan when a task is created
            generateRescuePlan(newId)
        }
    }

    fun completeTask(task: Task, isCompleted: Boolean) {
        viewModelScope.launch {
            val updatedTask = task.copy(
                isCompleted = isCompleted,
                completedAt = if (isCompleted) System.currentTimeMillis() else null
            )
            repository.updateTask(updatedTask)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun toggleStep(step: TaskStep, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateStep(step.copy(isCompleted = isCompleted))
        }
    }

    fun updateTaskAndSteps(task: Task, steps: List<TaskStep>) {
        viewModelScope.launch {
            repository.updateTaskAndSteps(task, steps)
        }
    }

    fun getTaskStepsFlow(taskId: Int) = repository.getStepsForTaskFlow(taskId)

    fun generateRescuePlan(taskId: Int) {
        viewModelScope.launch {
            _generatingPlanTaskId.value = taskId
            try {
                repository.generateRescuePlan(taskId)
            } catch (e: Exception) {
                Log.e("LifeSaverViewModel", "Failed to generate rescue plan", e)
            } finally {
                _generatingPlanTaskId.value = null
            }
        }
    }

    fun runProactiveBacklogBriefing() {
        val tasks = tasksState.value
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val report = repository.generateProactiveAnalysis(tasks)
                _aiBriefing.value = report
            } catch (e: Exception) {
                _aiBriefing.value = "Error generating strategic briefing: ${e.localizedMessage}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun setFocusReminderStyle(style: String) {
        _focusReminderStyle.value = style
        sharedPrefs.edit().putString("focus_reminder_style", style).apply()
    }

    private val lastTaskReminderTimeMs = mutableMapOf<Int, Long>()

    private fun startFocusReminderLoop() {
        reminderJob?.cancel()
        reminderJob = viewModelScope.launch {
            // Warm up reminder timestamps so we don't spam on startup
            val startTime = System.currentTimeMillis()
            tasksState.value.forEach {
                if (it.isFocusReminderEnabled && !lastTaskReminderTimeMs.containsKey(it.id)) {
                    lastTaskReminderTimeMs[it.id] = startTime
                }
            }

            while (isActive) {
                delay(2000L) // Check every 2 seconds for high responsiveness
                
                val currentTime = System.currentTimeMillis()
                val calendar = java.util.Calendar.getInstance()
                val currentDayOfWeek = when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
                    java.util.Calendar.MONDAY -> "Mon"
                    java.util.Calendar.TUESDAY -> "Tue"
                    java.util.Calendar.WEDNESDAY -> "Wed"
                    java.util.Calendar.THURSDAY -> "Thu"
                    java.util.Calendar.FRIDAY -> "Fri"
                    java.util.Calendar.SATURDAY -> "Sat"
                    java.util.Calendar.SUNDAY -> "Sun"
                    else -> ""
                }
                
                val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(java.util.Calendar.MINUTE)
                val currentMinutesSinceMidnight = currentHour * 60 + currentMinute

                val activeTasks = tasksState.value.filter { !it.isCompleted }
                
                for (task in activeTasks) {
                    if (!task.isFocusReminderEnabled) continue
                    
                    // 1. Day of week check
                    val allowedDays = task.reminderDays.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (allowedDays.isNotEmpty() && !allowedDays.contains(currentDayOfWeek)) {
                        continue
                    }
                    
                    // 2. Start/End Time of day check
                    val startMinutes = task.reminderStartHour * 60 + task.reminderStartMinute
                    val endMinutes = task.reminderEndHour * 60 + task.reminderEndMinute
                    val isTimeInRange = if (startMinutes <= endMinutes) {
                        currentMinutesSinceMidnight in startMinutes..endMinutes
                    } else {
                        currentMinutesSinceMidnight >= startMinutes || currentMinutesSinceMidnight <= endMinutes
                    }
                    if (!isTimeInRange) {
                        continue
                    }
                    
                    val intervalMs = task.focusReminderIntervalSeconds * 1000L
                    val lastRemind = lastTaskReminderTimeMs[task.id]
                    
                    if (lastRemind == null) {
                        lastTaskReminderTimeMs[task.id] = currentTime
                        continue
                    }
                    
                    if (currentTime - lastRemind >= intervalMs) {
                        lastTaskReminderTimeMs[task.id] = currentTime
                        triggerTaskFocusReminder(task)
                    }
                }
            }
        }
    }

    private fun stopFocusReminderLoop() {
        reminderJob?.cancel()
        reminderJob = null
    }

    private fun triggerTaskFocusReminder(task: Task) {
        viewModelScope.launch {
            // Get steps for this task
            val steps = repository.getStepsForTask(task.id)
            val totalSteps = steps.size
            val completedSteps = steps.count { it.isCompleted }
            
            // Calculate task phase
            val now = System.currentTimeMillis()
            val remainingMs = task.deadline - now
            val durationDesc = if (remainingMs <= 0) {
                "OVERDUE"
            } else {
                val days = TimeUnit.MILLISECONDS.toDays(remainingMs)
                val hours = TimeUnit.MILLISECONDS.toHours(remainingMs) % 24
                val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMs) % 60
                when {
                    days > 0 -> "$days days and $hours hours"
                    hours > 0 -> "$hours hours and $minutes minutes"
                    else -> "$minutes minutes"
                }
            }
            
            val phaseDesc = when {
                totalSteps == 0 -> "Getting Started (Plan steps not generated yet)"
                completedSteps == 0 -> "Kick-off Phase (0 of $totalSteps steps completed)"
                completedSteps < totalSteps -> "Mid-Execution Phase ($completedSteps of $totalSteps steps completed)"
                else -> "Final Polish Phase (All steps completed)"
            }
            
            val coachName = _coachVoice.value
            val prompt = """
                Give me a short, highly motivating, punchy anti-procrastination reminder of under 20 words.
                The user is trying to complete this task: '${task.title}'
                Description: '${task.description}'
                Category: '${task.category}'
                Priority level: '${task.priority}'
                Current Phase: '$phaseDesc'
                Remaining Time to Deadline: '$durationDesc'
                
                The voice/tone style of the coach must be: '${_focusReminderStyle.value}'.
                The coach is named: '$coachName'. If the coach is Adam, speak in a strong, direct, commanding male voice. If the coach is Eve, speak in an encouraging, inspiring, bright female voice.
                Keep it under 20 words. Speak in exactly 1 punchy sentence. No hashtags or markdown.
            """.trimIndent()
            
            val instruction = "You are $coachName, an elite AI focus coach. Speak in exactly 1 highly motivating sentence of under 20 words. Do not use hashtags, stars, or markdown."
            
            val message = try {
                val response = com.example.api.GeminiClient.generateText(prompt, instruction)
                if (response.startsWith("API Key is missing") || response.startsWith("Error:")) {
                    getTaskFallbackMessage(_focusReminderStyle.value, task.title, coachName)
                } else {
                    response.trim().replace("\"", "")
                }
            } catch (e: Exception) {
                getTaskFallbackMessage(_focusReminderStyle.value, task.title, coachName)
            }
            
            _lastReminderMessage.value = "[$coachName on '${task.title}']: \"$message\""
            speak(message)
            showSystemNotification(message)
        }
    }

    private fun getTaskFallbackMessage(style: String, taskTitle: String, coachName: String): String {
        val coachPrefix = if (coachName == "Adam") "Adam here. " else "This is Eve. "
        return when (style) {
            "Confident" -> listOf(
                "${coachPrefix}Time is ticking for '$taskTitle'. Put distractions away and get it done now!",
                "${coachPrefix}Stop waiting! Every second wasted is a second of stress. Focus on '$taskTitle'!",
                "${coachPrefix}You have goals to hit. Focus and crush '$taskTitle' right this moment."
            ).random()
            "Encouraging" -> listOf(
                "${coachPrefix}You are fully capable of finishing '$taskTitle'! Take one small step now, you got this!",
                "${coachPrefix}Keep pushing! The hard part is starting. Focus on '$taskTitle' and keep going.",
                "${coachPrefix}Believe in yourself. You'll feel amazing once '$taskTitle' is complete. Let's do it!"
            ).random()
            "Calm" -> listOf(
                "${coachPrefix}Take a deep breath. Focus fully on '$taskTitle'. One mindful step at a time.",
                "${coachPrefix}Clear your mind of clutter. Center your energy and return to '$taskTitle'.",
                "${coachPrefix}In this moment, there is only you and your focus on '$taskTitle'. Proceed with calm strength."
            ).random()
            else -> "${coachPrefix}Focus on '$taskTitle' now. You can do this!"
        }
    }

    private fun showSystemNotification(message: String) {
        val context = getApplication<Application>()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "life_saver_focus_pulse"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AI Focus Pulse Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Provides AI-generated motivational focus reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🚨 Focus Pulse Reminder")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(4242, notification)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefsChangeListener)
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("LifeSaverViewModel", "Error shutting down TTS", e)
        }
    }
}
