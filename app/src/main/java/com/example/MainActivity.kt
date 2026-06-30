package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.AssistScreen
import com.example.ui.screens.TaskHistoryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LifeSaverViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {}.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Start background service to allow app execution in background
        try {
            val serviceIntent = android.content.Intent(this, BackgroundPulseService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error starting background service", e)
        }

        enableEdgeToEdge()
        setContent {
            val viewModel: LifeSaverViewModel = viewModel()
            val themeMode by viewModel.themeState.collectAsState()
            MyApplicationTheme(isDark = themeMode == "Dark") {
                MainContainer(viewModel)
            }
        }
    }
}

@Composable
fun MainContainer(viewModel: LifeSaverViewModel) {
    val currentTab by viewModel.currentTab
    val themeMode by viewModel.themeState.collectAsState()
    val isDark = themeMode == "Dark"

    val bgColor = if (isDark) Color.Black else Color(0xFFF8FAFC)
    val navBgColor = if (isDark) Color(0xFF111111).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)
    val navBorderColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Main Screen Switcher
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding() // Top padding to avoid status bar overlap on notched phones!
        ) {
            when (currentTab) {
                "Profile" -> ProfileScreen(
                    viewModel = viewModel,
                    onNavigateToTasks = { viewModel.currentTab.value = "Tasks" }
                )
                "Tasks" -> TasksScreen(viewModel = viewModel)
                "Assist" -> AssistScreen(viewModel = viewModel)
                "History" -> TaskHistoryScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.currentTab.value = "Profile" }
                )
            }
        }

        // Custom Navigation Bar - Hovering fully rounded pill shape
        if (currentTab != "History") {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding() // Bottom padding for gesture navigation bar on modern phones!
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp) // Beautiful hovering offset
                    .height(64.dp)
            ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.dp,
                        color = navBorderColor,
                        shape = RoundedCornerShape(32.dp)
                    )
                    .background(
                        color = navBgColor,
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavigationTabItem(
                        title = "Tasks",
                        icon = Icons.Default.List,
                        isSelected = currentTab == "Tasks",
                        isDark = isDark,
                        onClick = { viewModel.currentTab.value = "Tasks" },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_tasks")
                    )

                    NavigationTabItem(
                        title = "AI Assist",
                        icon = Icons.Default.Star,
                        isSelected = currentTab == "Assist",
                        isDark = isDark,
                        onClick = { viewModel.currentTab.value = "Assist" },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_assist")
                    )

                    NavigationTabItem(
                        title = "Profile",
                        icon = Icons.Default.Person,
                        isSelected = currentTab == "Profile",
                        isDark = isDark,
                        onClick = { viewModel.currentTab.value = "Profile" },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_profile")
                    )
                }
            }
        }
    }

        // Onboarding name screen overlay (if user's name is not configured yet)
        val userName by viewModel.userName.collectAsState()
        if (userName.isEmpty()) {
            var tempName by remember { mutableStateOf("") }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF07070A))
                    .padding(24.dp)
                    .clickable(enabled = false) {} // block clickthroughs
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                        .background(Color(0xFF111116), RoundedCornerShape(24.dp))
                        .padding(32.dp)
                ) {
                    Text(
                        text = "🛡️ LIFE SAVER AI",
                        color = Color(0xFFF97316),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    
                    Text(
                        text = "Your Identity",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Your elite tactical assistant needs a name to address you during reminders and coaching.",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    
                    TextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        placeholder = { Text("Enter your name...", color = Color(0xFF64748B)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.04f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color(0xFFF97316),
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("onboarding_name_input")
                    )
                    
                    Button(
                        onClick = {
                            val nameStr = tempName.trim()
                            if (nameStr.isNotEmpty()) {
                                viewModel.setUserName(nameStr)
                                viewModel.speak("Welcome $nameStr! I am Life Saver. Let's begin our commitments.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        enabled = tempName.trim().isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("onboarding_submit_button")
                    ) {
                        Text("Begin Commitment", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationTabItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedColor = if (isDark) Color.White else Color(0xFF0F172A)
    val unselectedColor = Color(0xFF64748B)
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) selectedColor else unselectedColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = title,
            color = if (isSelected) selectedColor else unselectedColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
