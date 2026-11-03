package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val statusMessage: String? = "Available",
    val isOnline: Boolean = false,
    @ServerTimestamp val lastSeen: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PrivacySettings(
    val userId: String = "",
    val readReceipts: Boolean = true,
    val lastSeenVisible: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BlockedUser(
    val blockedUserId: String = "",
    val blockedUserName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Chat(
    val id: String = "",
    val type: String = "direct", // "direct" or "group"
    val name: String? = null,
    val memberIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val lastMessageText: String? = null,
    val lastSenderId: String? = null,
    val lastSenderName: String? = null,
    val isTyping: Map<String, Boolean> = emptyMap(),
    @ServerTimestamp val lastTimestamp: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val type: String = "text", // "text", "image", "file"
    val attachmentUrl: String? = null,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val status: String = "sent", // "sent", "delivered", "read"
    val readBy: Map<String, Boolean> = emptyMap(),
    val memberIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    val deleted: Boolean = false
)

@IgnoreExtraProperties
data class Report(
    val id: String = "",
    val reporterId: String = "",
    val reportedUserId: String = "",
    val reason: String = "",
    val details: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)
