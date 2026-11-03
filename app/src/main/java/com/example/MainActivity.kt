package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.AuthUiState
import com.example.ui.screens.auth.SignInScreen
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.chats.ChatListScreen
import com.example.ui.screens.conversation.ConversationScreen
import com.example.ui.screens.dialogs.NewChatDialog
import com.example.ui.screens.dialogs.ReportDialog
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.theme.SATMessengerTheme
import com.example.ui.viewmodel.MessengerViewModel
import com.example.ui.viewmodel.ScreenState

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SATMessengerTheme {
                val context = LocalContext.current
                val viewModel: MessengerViewModel = viewModel {
                    MessengerViewModel(context.applicationContext)
                }

                val authState by viewModel.authState.collectAsState()
                val currentScreen by viewModel.currentScreen.collectAsState()
                val activeChat by viewModel.activeChat.collectAsState()
                val userProfile by viewModel.userProfile.collectAsState()
                val chats by viewModel.chats.collectAsState()
                val messages by viewModel.messages.collectAsState()
                val directoryUsers by viewModel.directoryUsers.collectAsState()
                val blockedUsers by viewModel.blockedUsers.collectAsState()
                val privacySettings by viewModel.privacySettings.collectAsState()
                val replyingTo by viewModel.replyingTo.collectAsState()
                val showNewChatDialog by viewModel.showNewChatDialog.collectAsState()
                val showReportDialog by viewModel.showReportDialog.collectAsState()
                val reportedTarget by viewModel.reportedTarget.collectAsState()
                val toastMessage by viewModel.toastMessage.collectAsState()

                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(toastMessage) {
                    toastMessage?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        viewModel.clearToast()
                    }
                }

                if (authState is AuthUiState.Authenticated) {
                    val uid = (authState as AuthUiState.Authenticated).user.uid

                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                ScreenState.CHATS -> {
                                    ChatListScreen(
                                        currentUserId = uid,
                                        userProfile = userProfile,
                                        chats = chats,
                                        directoryUsers = directoryUsers,
                                        onChatClick = { chat -> viewModel.openChat(chat) },
                                        onProfileClick = { viewModel.navigateTo(ScreenState.PROFILE) },
                                        onNewChatClick = { viewModel.openNewChatDialog() }
                                    )
                                }

                                ScreenState.CONVERSATION -> {
                                    activeChat?.let { chat ->
                                        ChatScreen(
                                            chat = chat,
                                            currentUserId = uid,
                                            messages = messages,
                                            registeredUsers = directoryUsers,
                                            replyingTo = replyingTo,
                                            onBack = { viewModel.leaveChat() },
                                            onSendMessage = { text, type, attachmentUrl ->
                                                viewModel.sendMessage(text, type, attachmentUrl)
                                            },
                                            onSwitchUser = { targetUser ->
                                                viewModel.openDirectChatWithUser(targetUser)
                                            },
                                            onSetReplyingTo = { viewModel.setReplyingTo(it) },
                                            onDeleteMessage = { viewModel.deleteMessage(it) },
                                            onBlockUser = { targetId, targetName ->
                                                viewModel.blockUser(targetId, targetName)
                                            },
                                            onReportUser = { targetId, targetName ->
                                                viewModel.openReportDialog(targetId, targetName)
                                            },
                                            onTyping = { isTyping ->
                                                viewModel.setTyping(isTyping)
                                            }
                                        )
                                    } ?: run {
                                        viewModel.navigateTo(ScreenState.CHATS)
                                    }
                                }

                                ScreenState.PROFILE -> {
                                    ProfileScreen(
                                        userProfile = userProfile,
                                        privacySettings = privacySettings,
                                        blockedUsers = blockedUsers,
                                        onBack = { viewModel.navigateTo(ScreenState.CHATS) },
                                        onUpdateProfile = { name, status ->
                                            viewModel.updateProfile(name, status)
                                        },
                                        onUpdatePrivacy = { readReceipts, lastSeenVisible ->
                                            viewModel.updatePrivacy(readReceipts, lastSeenVisible)
                                        },
                                        onUnblockUser = { blockedId ->
                                            viewModel.unblockUser(blockedId)
                                        },
                                        onSignOut = { viewModel.signOut() },
                                        onDeleteAccount = { viewModel.deleteAccount() }
                                    )
                                }
                            }

                            // Modals
                            if (showNewChatDialog) {
                                NewChatDialog(
                                    users = directoryUsers,
                                    onDismiss = { viewModel.closeNewChatDialog() },
                                    onSelectUser = { targetUser ->
                                        viewModel.openDirectChatWithUser(targetUser)
                                    },
                                    onCreateGroup = { groupName, memberIds ->
                                        viewModel.createGroupChat(groupName, memberIds)
                                    }
                                )
                            }

                            if (showReportDialog && reportedTarget != null) {
                                ReportDialog(
                                    reportedUserName = reportedTarget!!.second,
                                    onDismiss = { viewModel.closeReportDialog() },
                                    onSubmit = { reason, details ->
                                        viewModel.submitReport(reason, details)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    SignInScreen(
                        authState = authState,
                        onSignInClick = { activity ->
                            viewModel.signInWithGoogle(activity)
                        }
                    )
                }
            }
        }
    }
}
