package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BlockedUser
import com.example.data.model.Chat
import com.example.data.model.Message
import com.example.data.model.PrivacySettings
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthUiState
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenState {
    CHATS,
    CONVERSATION,
    PROFILE
}

class MessengerViewModel(
    context: Context,
    private val authRepository: AuthRepository = AuthRepository(context),
    private val userRepository: UserRepository = UserRepository(context),
    private val chatRepository: ChatRepository = ChatRepository(context)
) : ViewModel() {

    val authState: StateFlow<AuthUiState> = authRepository.authState

    private val _currentScreen = MutableStateFlow(ScreenState.CHATS)
    val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

    private val _activeChat = MutableStateFlow<Chat?>(null)
    val activeChat: StateFlow<Chat?> = _activeChat.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _directoryUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val directoryUsers: StateFlow<List<UserProfile>> = _directoryUsers.asStateFlow()

    private val _blockedUsers = MutableStateFlow<List<BlockedUser>>(emptyList())
    val blockedUsers: StateFlow<List<BlockedUser>> = _blockedUsers.asStateFlow()

    private val _privacySettings = MutableStateFlow(PrivacySettings())
    val privacySettings: StateFlow<PrivacySettings> = _privacySettings.asStateFlow()

    private val _replyingTo = MutableStateFlow<Message?>(null)
    val replyingTo: StateFlow<Message?> = _replyingTo.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Dialog flags
    private val _showNewChatDialog = MutableStateFlow(false)
    val showNewChatDialog: StateFlow<Boolean> = _showNewChatDialog.asStateFlow()

    private val _showReportDialog = MutableStateFlow(false)
    val showReportDialog: StateFlow<Boolean> = _showReportDialog.asStateFlow()

    private val _reportedTarget = MutableStateFlow<Pair<String, String>?>(null)
    val reportedTarget: StateFlow<Pair<String, String>?> = _reportedTarget.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var messagesJob: Job? = null
    private var chatsJob: Job? = null
    private var profileJob: Job? = null
    private var activeChatJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.attemptAutoSignIn(viewModelScope)
        }

        viewModelScope.launch {
            authState.collectLatest { state ->
                if (state is AuthUiState.Authenticated) {
                    onUserAuthenticated(state.user)
                } else {
                    onUserSignedOut()
                }
            }
        }
    }

    private fun onUserAuthenticated(user: FirebaseUser) {
        val uid = user.uid
        val displayName = user.displayName ?: user.email?.substringBefore("@") ?: "SAT Member"

        // Setup profile in Firestore if not already present
        viewModelScope.launch {
            userRepository.saveProfile(
                UserProfile(
                    userId = uid,
                    displayName = displayName,
                    email = user.email ?: "",
                    avatarUrl = user.photoUrl?.toString(),
                    isOnline = true
                )
            )
        }

        // Observe own profile
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            userRepository.observeUserProfile(uid).collectLatest { profile ->
                _userProfile.value = profile
            }
        }

        // Observe chats
        chatsJob?.cancel()
        chatsJob = viewModelScope.launch {
            chatRepository.observeUserChats(uid).collectLatest { list ->
                _chats.value = list
            }
        }

        // Observe colleague directory
        viewModelScope.launch {
            userRepository.searchUsers("").collectLatest { users ->
                _directoryUsers.value = users.filter { it.userId != uid }
            }
        }

        // Observe privacy settings
        viewModelScope.launch {
            userRepository.observePrivacySettings(uid).collectLatest { settings ->
                _privacySettings.value = settings
            }
        }

        // Observe blocked users
        viewModelScope.launch {
            userRepository.observeBlockedUsers(uid).collectLatest { list ->
                _blockedUsers.value = list
            }
        }
    }

    private fun onUserSignedOut() {
        messagesJob?.cancel()
        chatsJob?.cancel()
        profileJob?.cancel()
        _chats.value = emptyList()
        _messages.value = emptyList()
        _userProfile.value = null
        _activeChat.value = null
        _currentScreen.value = ScreenState.CHATS
    }

    fun signInWithGoogle(activity: Activity) {
        authRepository.signInWithGoogle(activity, viewModelScope) { err ->
            _toastMessage.value = err
        }
    }

    fun signOut() {
        viewModelScope.launch {
            userRepository.updatePresence(false)
            authRepository.signOut(viewModelScope)
        }
    }

    fun navigateTo(screen: ScreenState) {
        _currentScreen.value = screen
    }

    fun openChat(chat: Chat) {
        _activeChat.value = chat
        _currentScreen.value = ScreenState.CONVERSATION
        _replyingTo.value = null

        activeChatJob?.cancel()
        activeChatJob = viewModelScope.launch {
            chatRepository.observeChat(chat.id).collectLatest { updatedChat ->
                if (updatedChat != null) {
                    _activeChat.value = updatedChat
                }
            }
        }

        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepository.observeMessages(chat.id).collectLatest { list ->
                _messages.value = list
            }
        }

        // Mark messages as read
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(chat.id)
        }
    }

    fun setTyping(isTyping: Boolean) {
        val chat = _activeChat.value ?: return
        viewModelScope.launch {
            chatRepository.setTyping(chat.id, isTyping)
        }
    }

    fun leaveChat() {
        val chat = _activeChat.value
        if (chat != null) {
            viewModelScope.launch {
                chatRepository.setTyping(chat.id, false)
            }
        }
        activeChatJob?.cancel()
        _activeChat.value = null
        _currentScreen.value = ScreenState.CHATS
    }

    fun openDirectChatWithUser(targetUser: UserProfile) {
        val current = _userProfile.value ?: return
        viewModelScope.launch {
            val result = chatRepository.getOrCreateDirectChat(
                targetUserId = targetUser.userId,
                targetUserName = targetUser.displayName,
                currentUserName = current.displayName
            )
            result.onSuccess { chatId ->
                val chat = Chat(
                    id = chatId,
                    type = "direct",
                    memberIds = listOf(current.userId, targetUser.userId),
                    name = targetUser.displayName
                )
                openChat(chat)
                closeNewChatDialog()
            }.onFailure {
                _toastMessage.value = "Failed to start conversation: ${it.localizedMessage}"
            }
        }
    }

    fun createGroupChat(name: String, selectedMemberIds: List<String>) {
        val current = _userProfile.value ?: return
        if (name.isBlank()) {
            _toastMessage.value = "Group name cannot be empty"
            return
        }
        viewModelScope.launch {
            val result = chatRepository.createGroupChat(
                name = name,
                selectedMemberIds = selectedMemberIds,
                currentUserName = current.displayName
            )
            result.onSuccess { chatId ->
                val chat = Chat(
                    id = chatId,
                    type = "group",
                    name = name,
                    memberIds = selectedMemberIds + current.userId
                )
                openChat(chat)
                closeNewChatDialog()
            }.onFailure {
                _toastMessage.value = "Failed to create group: ${it.localizedMessage}"
            }
        }
    }

    fun sendMessage(text: String, type: String = "text", attachmentUrl: String? = null) {
        val chat = _activeChat.value ?: return
        val current = _userProfile.value ?: return
        val reply = _replyingTo.value

        if (text.isBlank() && attachmentUrl == null) return

        viewModelScope.launch {
            chatRepository.setTyping(chat.id, false)
            chatRepository.sendMessage(
                chatId = chat.id,
                senderName = current.displayName,
                text = text,
                type = type,
                attachmentUrl = attachmentUrl,
                replyTo = reply
            )
            _replyingTo.value = null
        }
    }

    fun setReplyingTo(message: Message?) {
        _replyingTo.value = message
    }

    fun deleteMessage(message: Message) {
        val chat = _activeChat.value ?: return
        viewModelScope.launch {
            chatRepository.deleteMessage(chat.id, message.id)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openNewChatDialog() {
        _showNewChatDialog.value = true
    }

    fun closeNewChatDialog() {
        _showNewChatDialog.value = false
    }

    fun openReportDialog(userId: String, userName: String) {
        _reportedTarget.value = Pair(userId, userName)
        _showReportDialog.value = true
    }

    fun closeReportDialog() {
        _showReportDialog.value = false
        _reportedTarget.value = null
    }

    fun submitReport(reason: String, details: String) {
        val target = _reportedTarget.value ?: return
        viewModelScope.launch {
            val result = userRepository.reportUser(target.first, reason, details)
            if (result.isSuccess) {
                _toastMessage.value = "Report submitted securely to SAT Compliance."
                closeReportDialog()
            } else {
                _toastMessage.value = "Failed to submit report."
            }
        }
    }

    fun blockUser(userId: String, userName: String) {
        viewModelScope.launch {
            val result = userRepository.blockUser(userId, userName)
            if (result.isSuccess) {
                _toastMessage.value = "User blocked."
                if (_currentScreen.value == ScreenState.CONVERSATION) {
                    _currentScreen.value = ScreenState.CHATS
                }
            }
        }
    }

    fun unblockUser(userId: String) {
        viewModelScope.launch {
            userRepository.unblockUser(userId)
            _toastMessage.value = "User unblocked."
        }
    }

    fun updateProfile(displayName: String, statusMessage: String) {
        val current = _userProfile.value ?: return
        viewModelScope.launch {
            userRepository.saveProfile(
                current.copy(
                    displayName = displayName.trim(),
                    statusMessage = statusMessage.trim()
                )
            )
            _toastMessage.value = "Profile updated."
        }
    }

    fun updatePrivacy(readReceipts: Boolean, lastSeenVisible: Boolean) {
        val current = _userProfile.value ?: return
        viewModelScope.launch {
            userRepository.updatePrivacySettings(
                PrivacySettings(
                    userId = current.userId,
                    readReceipts = readReceipts,
                    lastSeenVisible = lastSeenVisible
                )
            )
            _toastMessage.value = "Privacy settings updated."
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val result = userRepository.deleteAccount()
            if (result.isSuccess) {
                authRepository.signOut(viewModelScope)
            } else {
                _toastMessage.value = "Account deletion failed."
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
