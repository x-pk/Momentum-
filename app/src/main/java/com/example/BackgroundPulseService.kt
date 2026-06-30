package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.AppDatabase
import com.example.data.Task
import com.example.data.TaskStep
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.TimeUnit

class BackgroundPulseService : Service(), TextToSpeech.OnInitListener {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reminderJob: Job? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val lastTaskReminderTimeMs = mutableMapOf<Int, Long>()
    private val lastTaskGeminiCallTimeMs = mutableMapOf<Int, Long>()
    private val lastTaskGeminiMessage = mutableMapOf<Int, String>()
    private val activeTasks = Collections.synchronizedList(mutableListOf<Task>())

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        startServiceForeground()
        
        // Initialize TTS
        try {
            tts = TextToSpeech(this, this)
        } catch (e: Exception) {
            Log.e("BackgroundPulseService", "Failed to initialize TTS", e)
        }

        // Observe active tasks from Database
        startObservingTasks()

        // Start checking reminders
        startReminderLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("BackgroundPulseService", "TTS US Language not supported in background")
            } else {
                isTtsReady = true
                Log.d("BackgroundPulseService", "TTS initialized successfully in background")
            }
        } else {
            Log.e("BackgroundPulseService", "TTS initialization failed in background")
        }
    }

    private fun startServiceForeground() {
        val channelId = "life_saver_background_service"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Background Pulse Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps AI Focus Pulse alive in the background"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("AI Focus Pulse Active")
            .setContentText("Your AI Focus Coach is monitoring your tasks in the background.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                9999,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(9999, notification)
        }
    }

    private fun startObservingTasks() {
        serviceScope.launch {
            try {
                val database = AppDatabase.getDatabase(this@BackgroundPulseService)
                database.taskDao().getAllTasksFlow().collect { tasks ->
                    synchronized(activeTasks) {
                        activeTasks.clear()
                        activeTasks.addAll(tasks.filter { !it.isCompleted })
                    }
                }
            } catch (e: Exception) {
                Log.e("BackgroundPulseService", "Error collecting tasks flow", e)
            }
        }
    }

    private fun startReminderLoop() {
        reminderJob?.cancel()
        reminderJob = serviceScope.launch {
            // Warm up reminder timestamps
            val startTime = System.currentTimeMillis()
            
            while (isActive) {
                delay(2000L) // Check every 2 seconds for high responsiveness

                val currentTime = System.currentTimeMillis()
                val calendar = Calendar.getInstance()
                val currentDayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> "Mon"
                    Calendar.TUESDAY -> "Tue"
                    Calendar.WEDNESDAY -> "Wed"
                    Calendar.THURSDAY -> "Thu"
                    Calendar.FRIDAY -> "Fri"
                    Calendar.SATURDAY -> "Sat"
                    Calendar.SUNDAY -> "Sun"
                    else -> ""
                }

                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(Calendar.MINUTE)
                val currentMinutesSinceMidnight = currentHour * 60 + currentMinute

                val tasksToCheck = synchronized(activeTasks) { ArrayList(activeTasks) }

                for (task in tasksToCheck) {
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

    private fun triggerTaskFocusReminder(task: Task) {
        serviceScope.launch {
            try {
                val database = AppDatabase.getDatabase(this@BackgroundPulseService)
                val steps = database.taskDao().getStepsForTask(task.id)
                val totalSteps = steps.size
                val completedSteps = steps.count { it.isCompleted }

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

                val sharedPrefs = getSharedPreferences("lifesaver_prefs", Context.MODE_PRIVATE)
                val coachName = sharedPrefs.getString("coach_voice", "Adam") ?: "Adam"
                val style = sharedPrefs.getString("focus_reminder_style", "Direct & Urgent") ?: "Direct & Urgent"

                val prompt = """
                    Give me a short, highly motivating, punchy anti-procrastination reminder of under 20 words.
                    The user is trying to complete this task: '${task.title}'
                    Description: '${task.description}'
                    Category: '${task.category}'
                    Priority level: '${task.priority}'
                    Current Phase: '$phaseDesc'
                    Remaining Time to Deadline: '$durationDesc'
                    
                    The voice/tone style of the coach must be: '$style'.
                    The coach is named: '$coachName'. If the coach is Adam, speak in a strong, direct, commanding male voice. If the coach is Eve, speak in an encouraging, inspiring, bright female voice.
                    Keep it under 20 words. Speak in exactly 1 punchy sentence. No hashtags or markdown.
                """.trimIndent()

                val instruction = "You are $coachName, an elite AI focus coach. Speak in exactly 1 highly motivating sentence of under 20 words. Do not use hashtags, stars, or markdown."

                val nowTime = System.currentTimeMillis()
                val lastCallTime = lastTaskGeminiCallTimeMs[task.id] ?: 0L
                val cachedMsg = lastTaskGeminiMessage[task.id]

                val message = if (nowTime - lastCallTime < 60000L && cachedMsg != null) {
                    cachedMsg
                } else {
                    try {
                        val response = com.example.api.GeminiClient.generateText(prompt, instruction)
                        if (response.startsWith("API Key is missing") || response.startsWith("Error:")) {
                            cachedMsg ?: getTaskFallbackMessage(style, task.title, coachName)
                        } else {
                            val cleaned = response.trim().replace("\"", "")
                            lastTaskGeminiCallTimeMs[task.id] = nowTime
                            lastTaskGeminiMessage[task.id] = cleaned
                            cleaned
                        }
                    } catch (e: Exception) {
                        cachedMsg ?: getTaskFallbackMessage(style, task.title, coachName)
                    }
                }

                // Persist the message so VM can display it
                val formattedMsg = "[$coachName on '${task.title}']: \"$message\""
                sharedPrefs.edit().putString("last_reminder_message", formattedMsg).apply()

                // High energy activation alert
                playHighEnergyFocusSound()
                triggerVibration()

                // Speak & notify
                speak(message)
                showSystemNotification(task.title, message)
            } catch (e: Exception) {
                Log.e("BackgroundPulseService", "Error triggering focus reminder", e)
            }
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

    private fun speak(text: String) {
        if (isTtsReady) {
            try {
                val params = android.os.Bundle()
                params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "FocusReminderTTS")
            } catch (e: Exception) {
                Log.e("BackgroundPulseService", "Error during speak", e)
            }
        }
    }

    private fun playHighEnergyFocusSound() {
        try {
            val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
            toneG.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 150)
            
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                toneG.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 150)
                
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    toneG.startTone(android.media.ToneGenerator.TONE_CDMA_HIGH_L, 500)
                }, 200)
            }, 200)
        } catch (e: Exception) {
            Log.e("BackgroundPulseService", "Failed to play custom tone", e)
        }
    }

    private fun triggerVibration() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 150, 100, 150), -1)
            }
        } catch (e: Exception) {
            Log.e("BackgroundPulseService", "Failed to vibrate", e)
        }
    }

    private fun showSystemNotification(title: String, message: String) {
        val channelId = "life_saver_focus_reminders"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Focus Pulse Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Focus Coach Reminders"
                setSound(null, null) // Bypasses system sound in favor of our high energy ToneGenerator synth
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            2,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🚨 Focus Coach: $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(4242, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        reminderJob?.cancel()
        serviceScope.cancel()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("BackgroundPulseService", "Error shutting down TTS", e)
        }
    }
}
