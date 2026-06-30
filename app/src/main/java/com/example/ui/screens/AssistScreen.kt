package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
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
import com.example.viewmodel.LifeSaverViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistScreen(
    viewModel: LifeSaverViewModel,
    modifier: Modifier = Modifier
) {
    val aiBriefing by viewModel.aiBriefing.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val chatHistory by viewModel.chatHistoryState.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var chatMessage by remember { mutableStateOf("") }

    val quickCommands = listOf(
        "🔍 Audit My Schedule",
        "🔥 Show High Risk Deadlines",
        "🌱 Suggest a Micro-Habit"
    )

    // Scroll to bottom when new messages arrive
    LaunchedEffect(chatHistory.size, isChatLoading) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size + 4) // offset for headers
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 240.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Screen title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.example.ui.components.LifeSaverLogo(size = 46.dp)
                Column {
                    Text(
                        text = "MOMENTUM AI COACH",
                        color = Color(0xFFF97316),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "AI Assistance",
                        color = Color(0xFFF1F5F9),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Strategic Backlog Briefing Panel
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFF97316).copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f))
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STRATEGIC ANALYSIS",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                color = Color(0xFFF97316),
                                strokeWidth = 1.5.dp,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = aiBriefing,
                        color = Color(0xFFE2E8F0),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Light,
                        lineHeight = 20.sp,
                        modifier = Modifier.testTag("ai_briefing_text")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { 
                            viewModel.runProactiveBacklogBriefing()
                            viewModel.speak("Initiating strategic commitment analysis.")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF0A0B10)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("run_audit_button")
                    ) {
                        Text(
                            text = "Optimize Calendar & Backlog",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Conversational Chat History section header
        item {
            Text(
                text = "INTERACTIVE COMPANION",
                color = Color(0xFF64748B),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Conversational Chat Messages
        items(chatHistory) { (msg, isUser) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(
                            color = if (isUser) Color(0xFFF97316).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f),
                            shape = RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomStart = if (isUser) 20.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 20.dp
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = if (isUser) Color(0xFFF97316).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomStart = if (isUser) 20.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 20.dp
                            )
                        )
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = if (isUser) (userName.ifBlank { "You" }) else "Life Saver AI",
                            color = if (isUser) Color(0xFFF97316) else Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Chat loader indicator
        if (isChatLoading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(20.dp))
                            .padding(14.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFCD34D),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Quick action commands row (Scrollable row)
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickCommands) { cmd ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(100.dp))
                                .clickable {
                                    if (!isChatLoading) {
                                        viewModel.sendChatMessage(cmd)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = cmd,
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Message input textfield
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = chatMessage,
                    onValueChange = { chatMessage = it },
                    placeholder = { Text("Ask Life Saver anything...", color = Color(0xFF64748B), fontSize = 13.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field")
                )

                IconButton(
                    onClick = {
                        if (chatMessage.isNotBlank() && !isChatLoading) {
                            viewModel.sendChatMessage(chatMessage)
                            chatMessage = ""
                        }
                    },
                    enabled = chatMessage.isNotBlank() && !isChatLoading,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (chatMessage.isNotBlank() && !isChatLoading) Color(0xFFF97316) else Color.White.copy(alpha = 0.05f))
                        .testTag("send_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Msg",
                        tint = if (chatMessage.isNotBlank() && !isChatLoading) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
