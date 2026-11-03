package com.example.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Chat
import com.example.data.model.Message
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MessageStatusIndicator
import com.example.ui.components.UserAvatar
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatIncomingBubbleDark
import com.example.ui.theme.SatIncomingBubbleLight
import com.example.ui.theme.SatOnlineGreen
import com.example.ui.theme.SatOutgoingBubble
import com.example.ui.theme.SatSlate400
import com.example.ui.theme.SatTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chat: Chat,
    currentUserId: String,
    messages: List<Message>,
    registeredUsers: List<UserProfile>,
    replyingTo: Message?,
    onBack: () -> Unit,
    onSendMessage: (text: String, type: String, attachmentUrl: String?) -> Unit,
    onSwitchUser: (UserProfile) -> Unit,
    onSetReplyingTo: (Message?) -> Unit,
    onDeleteMessage: (Message) -> Unit,
    onBlockUser: (String, String) -> Unit,
    onReportUser: (String, String) -> Unit,
    onTyping: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showRegisteredBar by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    val isGroup = chat.type == "group"
    val otherUserId = chat.memberIds.firstOrNull { it != currentUserId }
    val otherUser = registeredUsers.firstOrNull { it.userId == otherUserId }

    val title = if (isGroup) chat.name ?: "SAT Group" else otherUser?.displayName ?: "SAT Colleague"
    val isOnline = otherUser?.isOnline == true

    // Real-time detection of other people typing in this chat
    val typingUsers = remember(chat.isTyping, registeredUsers, currentUserId) {
        chat.isTyping.filter { it.key != currentUserId && it.value }.keys.mapNotNull { uid ->
            registeredUsers.firstOrNull { it.userId == uid }?.displayName ?: "Colleague"
        }
    }
    val isSomeoneTyping = typingUsers.isNotEmpty()
    val typingStatusText = when {
        typingUsers.size == 1 -> "${typingUsers.first()} is typing..."
        typingUsers.size == 2 -> "${typingUsers[0]} and ${typingUsers[1]} are typing..."
        typingUsers.size > 2 -> "Multiple people are typing..."
        else -> ""
    }

    // Reset typing status when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            onTyping(false)
        }
    }

    // Debounced typing indicator updater for current user
    LaunchedEffect(inputText) {
        if (inputText.isNotBlank()) {
            onTyping(true)
            delay(2500)
            onTyping(false)
        } else {
            onTyping(false)
        }
    }

    val statusSubtitle = if (isGroup) {
        "${chat.memberIds.size} registered members"
    } else if (isOnline) {
        "Active Now • SAT Limited"
    } else {
        val lastSeenDate = otherUser?.lastSeen?.toDate()
        if (lastSeenDate != null) "Last seen ${formatMessageTime(lastSeenDate)}" else "Offline • SAT Limited"
    }

    // Scroll to latest message smoothly on arrival
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(
                                name = title,
                                avatarUrl = otherUser?.avatarUrl,
                                isOnline = isOnline,
                                isGroup = isGroup,
                                size = 40.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSomeoneTyping) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .background(SatElectricBlue, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                    } else if (isOnline && !isGroup) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .background(SatOnlineGreen, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                    }
                                    Text(
                                        text = if (isSomeoneTyping) typingStatusText else statusSubtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSomeoneTyping) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSomeoneTyping) SatElectricBlue else if (isOnline) SatTeal else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showRegisteredBar = !showRegisteredBar },
                            modifier = Modifier.testTag("toggle_registered_users_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Show registered colleagues",
                                tint = if (showRegisteredBar) SatElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { showMenu = !showMenu },
                            modifier = Modifier.testTag("chat_menu_button")
                        ) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (!isGroup && otherUserId != null) {
                                DropdownMenuItem(
                                    text = { Text("Block Contact") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Block, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onBlockUser(otherUserId, title)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Report to SAT Compliance") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Report, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onReportUser(otherUserId, title)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Real-time Registered Colleagues Carousel for Instant Switching
                AnimatedVisibility(visible = showRegisteredBar) {
                    RegisteredColleaguesBar(
                        registeredUsers = registeredUsers,
                        selectedUserId = otherUserId,
                        onSelectUser = onSwitchUser
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .imePadding()
            ) {
                // Reply Preview Bar
                if (replyingTo != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = null,
                                tint = SatElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Replying to ${replyingTo.senderName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SatElectricBlue
                                )
                                Text(
                                    text = if (replyingTo.deleted) "Deleted message" else replyingTo.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onSetReplyingTo(null) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel reply",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Chat Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        IconButton(
                            onClick = { showAttachmentMenu = !showAttachmentMenu },
                            modifier = Modifier.testTag("chat_attachment_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach file or photo",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        DropdownMenu(
                            expanded = showAttachmentMenu,
                            onDismissRequest = { showAttachmentMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Send Corporate Photo") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Image, contentDescription = null)
                                },
                                onClick = {
                                    showAttachmentMenu = false
                                    onSendMessage(
                                        "Project visual overview",
                                        "image",
                                        "https://picsum.photos/600/400"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Send SAT Document") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.AttachFile, contentDescription = null)
                                },
                                onClick = {
                                    showAttachmentMenu = false
                                    onSendMessage(
                                        "SAT_Network_Specifications.pdf",
                                        "file",
                                        null
                                    )
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Type a message to $title...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SatElectricBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("message_input_field")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText, "text", null)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                if (inputText.isNotBlank()) SatElectricBlue
                                else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = if (inputText.isNotBlank()) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    icon = Icons.Default.People,
                    title = "Start the Conversation",
                    message = "Connected to $title in real time via Cloud Firestore. Send a message to begin.",
                    actionLabel = "Say Hello 👋",
                    onAction = {
                        onSendMessage("Hello! Connecting from SAT Limited.", "text", null)
                    }
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    RealtimeSecurityBadge()
                    Spacer(modifier = Modifier.height(14.dp))
                }

                items(messages, key = { it.id }) { message ->
                    ChatMessageBubble(
                        message = message,
                        isOutgoing = message.senderId == currentUserId,
                        isGroup = isGroup,
                        onReply = { onSetReplyingTo(message) },
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Message", message.text))
                            Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = { onDeleteMessage(message) },
                        onReport = {
                            onReportUser(message.senderId, message.senderName)
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (isSomeoneTyping) {
                    item(key = "typing_bubble") {
                        TypingIndicatorBubble(
                            typingUserNames = typingUsers,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun RegisteredColleaguesBar(
    registeredUsers: List<UserProfile>,
    selectedUserId: String?,
    onSelectUser: (UserProfile) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            Text(
                text = "REGISTERED ON SAT MESSENGER (${registeredUsers.size})",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(registeredUsers, key = { it.userId }) { user ->
                    val isSelected = user.userId == selectedUserId
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) SatElectricBlue.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .clickable { onSelectUser(user) }
                            .padding(4.dp)
                            .testTag("registered_user_${user.userId}")
                    ) {
                        Box {
                            UserAvatar(
                                name = user.displayName,
                                avatarUrl = user.avatarUrl,
                                isOnline = user.isOnline,
                                size = 42.dp
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .border(2.dp, SatElectricBlue, CircleShape)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = user.displayName.split(" ").firstOrNull() ?: user.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 60.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatMessageBubble(
    message: Message,
    isOutgoing: Boolean,
    isGroup: Boolean,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit
) {
    var showActionMenu by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF070F1EUL

    val bubbleColor = when {
        isOutgoing -> SatOutgoingBubble
        isDark -> SatIncomingBubbleDark
        else -> SatIncomingBubbleLight
    }

    val textColor = when {
        isOutgoing -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    val alignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isOutgoing) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Box {
            Card(
                colors = CardDefaults.cardColors(containerColor = bubbleColor),
                shape = shape,
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showActionMenu = true }
                    )
                    .testTag("chat_bubble_${message.id}")
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (isGroup && !isOutgoing) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SatTeal,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    // Quoted reply snippet
                    if (!message.replyToText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = (if (isOutgoing) Color.White else MaterialTheme.colorScheme.surface).copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(
                                    text = message.replyToSender ?: "Colleague",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOutgoing) Color.White else SatElectricBlue
                                )
                                Text(
                                    text = message.replyToText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = (if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.8f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Image attachment if available
                    if (message.type == "image" && !message.attachmentUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = message.attachmentUrl,
                            contentDescription = "Shared photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Message Text
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (message.deleted) SatSlate400 else textColor,
                        fontStyle = if (message.deleted) FontStyle.Italic else FontStyle.Normal
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Timestamp and Delivery Status
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatMessageTime(message.createdAt?.toDate()),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = (if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f)
                        )
                        if (isOutgoing) {
                            val isSeenByOthers = message.readBy.filterKeys { it != message.senderId }.values.any { it } || message.status == "read"
                            Spacer(modifier = Modifier.width(4.dp))
                            MessageStatusIndicator(status = message.status, isOutgoing = true, isRead = isSeenByOthers)
                        }
                    }
                }
            }

            // Context Action Menu
            DropdownMenu(
                expanded = showActionMenu,
                onDismissRequest = { showActionMenu = false }
            ) {
                if (isOutgoing) {
                    val readCount = message.readBy.count { it.key != message.senderId && it.value }
                    DropdownMenuItem(
                        text = {
                            Text(if (readCount > 0) "Seen by $readCount colleague${if (readCount > 1) "s" else ""}" else "Not seen yet")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                tint = if (readCount > 0) SatTeal else SatSlate400
                            )
                        },
                        onClick = { showActionMenu = false }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Reply") },
                    leadingIcon = {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Reply, contentDescription = null)
                    },
                    onClick = {
                        showActionMenu = false
                        onReply()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Copy Message") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                    },
                    onClick = {
                        showActionMenu = false
                        onCopy()
                    }
                )
                if (isOutgoing && !message.deleted) {
                    DropdownMenuItem(
                        text = { Text("Delete for Everyone") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        },
                        onClick = {
                            showActionMenu = false
                            onDelete()
                        }
                    )
                }
                if (!isOutgoing) {
                    DropdownMenuItem(
                        text = { Text("Report to Compliance") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Report, contentDescription = null)
                        },
                        onClick = {
                            showActionMenu = false
                            onReport()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RealtimeSecurityBadge() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚡ Real-Time Cloud Firestore Sync",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Live updates with registered SAT Limited colleagues.",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatMessageTime(date: Date?): String {
    if (date == null) return ""
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
}

@Composable
fun TypingIndicatorBubble(
    typingUserNames: List<String>,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF070F1EUL
    val bubbleColor = if (isDark) SatIncomingBubbleDark else SatIncomingBubbleLight

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            modifier = Modifier.testTag("typing_indicator_bubble")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = typingUserNames.firstOrNull()?.let { "$it is typing" } ?: "Typing",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                TypingDotsAnimation()
            }
        }
    }
}

@Composable
fun TypingDotsAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")
    val dots = (0..2).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -6f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1000
                    0f at 0
                    -6f at (index * 150 + 200)
                    0f at (index * 150 + 400)
                    0f at 1000
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "dot_$index"
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dots.forEach { anim ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer { translationY = anim.value }
                    .background(SatElectricBlue, CircleShape)
            )
        }
    }
}
