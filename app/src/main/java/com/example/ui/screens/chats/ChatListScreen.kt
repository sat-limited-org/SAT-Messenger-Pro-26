package com.example.ui.screens.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Chat
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateView
import com.example.ui.components.UserAvatar
import com.example.ui.theme.SatElectricBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    currentUserId: String,
    userProfile: UserProfile?,
    chats: List<Chat>,
    directoryUsers: List<UserProfile>,
    onChatClick: (Chat) -> Unit,
    onProfileClick: () -> Unit,
    onNewChatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredChats = remember(chats, searchQuery, directoryUsers) {
        if (searchQuery.isBlank()) {
            chats
        } else {
            chats.filter { chat ->
                val title = if (chat.type == "group") {
                    chat.name ?: "Group Chat"
                } else {
                    val otherMemberId = chat.memberIds.firstOrNull { it != currentUserId }
                    val otherUser = directoryUsers.firstOrNull { it.userId == otherMemberId }
                    otherUser?.displayName ?: "Direct Message"
                }
                title.contains(searchQuery, ignoreCase = true) ||
                        (chat.lastMessageText?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search messages & chats...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_search_input")
                        )
                    } else {
                        Column {
                            Text(
                                text = "SAT Messenger",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "SAT LIMITED COMMUNICATIONS",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        },
                        modifier = Modifier.testTag("toggle_search_button")
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { onProfileClick() }
                            .testTag("open_profile_button")
                    ) {
                        UserAvatar(
                            name = userProfile?.displayName ?: "Me",
                            avatarUrl = userProfile?.avatarUrl,
                            isOnline = userProfile?.isOnline ?: true,
                            size = 36.dp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewChatClick,
                containerColor = SatElectricBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("new_chat_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Chat",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (filteredChats.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.ChatBubbleOutline,
                    title = if (searchQuery.isNotBlank()) "No Matching Chats" else "No Conversations Yet",
                    message = if (searchQuery.isNotBlank()) "Try a different search keyword."
                    else "Connect with your SAT Limited team members. Tap the '+' button below to start a direct message or create a group.",
                    actionLabel = if (searchQuery.isBlank()) "Start Conversation" else null,
                    onAction = if (searchQuery.isBlank()) onNewChatClick else null,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredChats, key = { it.id }) { chat ->
                        val isGroup = chat.type == "group"
                        val otherMemberId = chat.memberIds.firstOrNull { it != currentUserId }
                        val otherUser = directoryUsers.firstOrNull { it.userId == otherMemberId }

                        val title = if (isGroup) {
                            chat.name ?: "SAT Group"
                        } else {
                            otherUser?.displayName ?: "SAT Colleague"
                        }

                        val isOtherOnline = otherUser?.isOnline ?: false
                        val timeStr = formatTimestamp(chat.lastTimestamp?.toDate())

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChatClick(chat) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("chat_item_${chat.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                name = title,
                                avatarUrl = otherUser?.avatarUrl,
                                isOnline = isOtherOnline,
                                isGroup = isGroup,
                                size = 52.dp
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (timeStr.isNotBlank()) {
                                        Text(
                                            text = timeStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = chat.lastMessageText ?: "No messages yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            modifier = Modifier.padding(start = 82.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(date: Date?): String {
    if (date == null) return ""
    val now = Date()
    val diffMs = now.time - date.time
    val oneDayMs = 24 * 60 * 60 * 1000L
    return when {
        diffMs < oneDayMs -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        diffMs < 7 * oneDayMs -> SimpleDateFormat("EEE", Locale.getDefault()).format(date)
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}
