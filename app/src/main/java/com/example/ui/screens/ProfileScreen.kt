package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Task
import com.example.viewmodel.LifeSaverViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun ProfileScreen(
    viewModel: LifeSaverViewModel,
    onNavigateToTasks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasksState.collectAsState()
    val isTtsReady by viewModel.isTtsReady.collectAsState()
    val themeMode by viewModel.themeState.collectAsState()
    val currentVoice by viewModel.coachVoice.collectAsState()
    val currentStyle by viewModel.focusReminderStyle.collectAsState()

    val isDark = themeMode == "Dark"

    // Theme adaptive colors
    val textColorPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textColorSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBgColor = if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.03f)
    val cardBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)

    val completedTasks = remember(tasks) { tasks.filter { it.isCompleted } }
    val completedCount = completedTasks.size

    val savedUserName by viewModel.userName.collectAsState()
    val userName = savedUserName.ifEmpty { "Pratik Kumar" }
    var tempUserName by remember(savedUserName) { mutableStateOf(userName) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 240.dp)
    ) {
        // Spacing top
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Header Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.example.ui.components.LifeSaverLogo(size = 46.dp)
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulseScale"
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(scale)
                                .background(Color(0xFFF97316), shape = CircleShape)
                        )
                        Text(
                            text = "MOMENTUM AI ACTIVE",
                            color = Color(0xFFF97316),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Profile Settings",
                        color = textColorPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
        }

        // Profile Identity Card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorderColor, RoundedCornerShape(24.dp))
                    .background(cardBgColor, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large circular avatar with orange glow
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .border(1.dp, Color(0xFFF97316).copy(alpha = 0.3f), CircleShape)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFF97316), Color(0xFFFCD34D))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName,
                        color = textColorPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Elite Commitment Defender",
                        color = textColorSecondary,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = { showEditProfileDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, cardBorderColor, CircleShape)
                        .background(cardBgColor, CircleShape)
                        .testTag("edit_profile_trigger")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Name",
                        tint = textColorPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Task History (Redesigned to be clean, elegant, and non-congested)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "RECORDS & ARCHIVES",
                    color = textColorSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cardBorderColor, RoundedCornerShape(24.dp))
                        .background(cardBgColor, RoundedCornerShape(24.dp))
                        .clickable { viewModel.currentTab.value = "History" }
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFF97316).copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📜", fontSize = 18.sp)
                            }
                            Column {
                                Text(
                                    text = "Task History",
                                    color = textColorPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "View archives of completed and missed tasks.",
                                    color = textColorSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "View Task History",
                            tint = textColorSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // AI Coach Settings
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "AI COACH SETTINGS",
                    color = textColorSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // 1. Coach Voice Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cardBorderColor, RoundedCornerShape(24.dp))
                        .background(cardBgColor, RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Coach Voice",
                                color = textColorPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Choose the voice characteristics of your coach.",
                                color = textColorSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("Adam", "Eve").forEach { voice ->
                            val isSelected = currentVoice == voice
                            val activeColor = if (voice == "Adam") Color(0xFF3B82F6) else Color(0xFFEC4899)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) activeColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f))
                                    .border(1.dp, if (isSelected) activeColor else cardBorderColor, RoundedCornerShape(16.dp))
                                    .clickable { viewModel.setCoachVoice(voice) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (voice == "Adam") "👨 Adam" else "👩 Eve",
                                    color = if (isSelected) activeColor else textColorSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 2. Coach Tone Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cardBorderColor, RoundedCornerShape(24.dp))
                        .background(cardBgColor, RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AI Coach Tone",
                                color = textColorPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Set how the coach prompts you to stay focused.",
                                color = textColorSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Calm", "Confident", "Encouraging").forEach { tone ->
                            val isSelected = currentStyle == tone
                            val activeColor = when (tone) {
                                "Calm" -> Color(0xFF10B981)
                                "Confident" -> Color(0xFFF97316)
                                else -> Color(0xFFFCD34D)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) activeColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f))
                                    .border(1.dp, if (isSelected) activeColor else cardBorderColor, RoundedCornerShape(16.dp))
                                    .clickable { viewModel.setFocusReminderStyle(tone) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tone,
                                    color = if (isSelected) activeColor else textColorSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Profile Name Dialog
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Customize your agent profile name below:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    TextField(
                        value = tempUserName,
                        onValueChange = { tempUserName = it },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color(0xFFF97316)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_profile_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setUserName(tempUserName)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316))
                ) {
                    Text("Save", color = Color.White)
                }
            },
            containerColor = Color(0xFF0C0D16),
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
        )
    }
}
