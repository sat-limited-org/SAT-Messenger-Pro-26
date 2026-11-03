package com.example.ui.screens.conversation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Chat
import com.example.data.model.Message
import com.example.data.model.UserProfile
import com.example.ui.components.MessageStatusIndicator
import com.example.ui.components.UserAvatar
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatIncomingBubbleDark
import com.example.ui.theme.SatIncomingBubbleLight
import com.example.ui.theme.SatOutgoingBubble
import com.example.ui.theme.SatSlate400
import com.example.ui.theme.SatTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    chat: Chat,
    currentUserId: String,
    messages: List<Message>,
    directoryUsers: List<UserProfile>,
    replyingTo: Message?,
    onBack: () -> Unit,
    onSendMessage: (text: String, type: String, attachmentUrl: String?) -> Unit,
    onSetReplyingTo: (Message?) -> Unit,
    onDeleteMessage: (Message) -> Unit,
    onBlockUser: (String, String) -> Unit,
    onReportUser: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val isGroup = chat.type == "group"
    val otherUserId = chat.memberIds.firstOrNull { it != currentUserId }
    val otherUser = directoryUsers.firstOrNull { it.userId == otherUserId }

    val title = if (isGroup) chat.name ?: "SAT Group" else otherUser?.displayName ?: "SAT Colleague"
    val subtitle = if (isGroup) "${chat.memberIds.size} members"
    else if (otherUser?.isOnline == true) "Online" else "SAT Limited"

    // Scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
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
                            isOnline = otherUser?.isOnline ?: false,
                            isGroup = isGroup,
                            size = 38.dp
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
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (otherUser?.isOnline == true) SatTeal else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showMenu = !showMenu },
                        modifier = Modifier.testTag("conversation_menu_button")
                    ) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu")
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
                                text = { Text("Report to Compliance") },
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

                // Input Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        IconButton(
                            onClick = { showAttachmentMenu = !showAttachmentMenu },
                            modifier = Modifier.testTag("attachment_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach file",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        DropdownMenu(
                            expanded = showAttachmentMenu,
                            onDismissRequest = { showAttachmentMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share Corporate Photo") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Image, contentDescription = null)
                                },
                                onClick = {
                                    showAttachmentMenu = false
                                    // Send simulated corporate diagram image
                                    onSendMessage(
                                        "Project architecture diagram",
                                        "image",
                                        "https://picsum.photos/600/400"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Document") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.AttachFile, contentDescription = null)
                                },
                                onClick = {
                                    showAttachmentMenu = false
                                    onSendMessage(
                                        "SAT_Quarterly_Strategy_Doc.pdf",
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
                        placeholder = { Text("Type an enterprise message...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp),
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
                            .size(44.dp)
                            .background(
                                if (inputText.isNotBlank()) SatElectricBlue
                                else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SecurityNoticeBanner()
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(messages, key = { it.id }) { message ->
                MessageBubbleItem(
                    message = message,
                    isOutgoing = message.senderId == currentUserId,
                    isGroup = isGroup,
                    onReply = { onSetReplyingTo(message) },
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Message", message.text))
                        Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = { onDeleteMessage(message) },
                    onReport = {
                        onReportUser(message.senderId, message.senderName)
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubbleItem(
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
                    .widthIn(max = 300.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showActionMenu = true }
                    )
                    .testTag("message_bubble_${message.id}")
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Sender Name for Groups
                    if (isGroup && !isOutgoing) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SatTeal,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    // Quoted Reply Box
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

                    // Image attachment if present
                    if (message.type == "image" && !message.attachmentUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = message.attachmentUrl,
                            contentDescription = "Photo attachment",
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

            // Message Context Menu
            DropdownMenu(
                expanded = showActionMenu,
                onDismissRequest = { showActionMenu = false }
            ) {
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
                    text = { Text("Copy Text") },
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
                        text = { Text("Report Message") },
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
private fun SecurityNoticeBanner() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔒 SAT Limited Secure Channel",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Messages are verified with zero-trust Firestore security rules. Authorized SAT Limited personnel only.",
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
