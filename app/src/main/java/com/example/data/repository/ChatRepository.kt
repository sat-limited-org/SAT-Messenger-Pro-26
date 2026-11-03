package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Chat
import com.example.data.model.Message
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing chats.")
    }

    fun observeUserChats(userId: String): Flow<List<Chat>> {
        val path = "chats"
        return db.collection(path)
            .whereArrayContains("memberIds", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Chat::class.java).sortedByDescending {
                    it.lastTimestamp?.toDate()?.time ?: it.createdAt?.toDate()?.time ?: 0L
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    fun observeChat(chatId: String): Flow<Chat?> {
        val path = "chats/$chatId"
        return db.collection("chats").document(chatId)
            .snapshots()
            .map { it.toObject(Chat::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                emit(null)
            }
    }

    suspend fun setTyping(chatId: String, isTyping: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No current user"))
        val path = "chats/$chatId"
        return try {
            db.collection("chats").document(chatId).update(
                mapOf(
                    "isTyping.$uid" to isTyping
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            try {
                db.collection("chats").document(chatId).set(
                    mapOf("isTyping" to mapOf(uid to isTyping)),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
                Result.success(Unit)
            } catch (mergeError: Exception) {
                handleFirestoreError(mergeError, OperationType.UPDATE, path)
                Result.failure(mergeError)
            }
        }
    }

    suspend fun getOrCreateDirectChat(
        targetUserId: String,
        targetUserName: String,
        currentUserName: String
    ): Result<String> {
        val uid = requireUserId()
        val path = "chats"
        return try {
            val existing = db.collection(path)
                .whereArrayContains("memberIds", uid)
                .get()
                .await()

            val found = existing.toObjects(Chat::class.java).firstOrNull {
                it.type == "direct" && it.memberIds.contains(targetUserId)
            }

            if (found != null && found.id.isNotBlank()) {
                Result.success(found.id)
            } else {
                val chatId = UUID.randomUUID().toString()
                val payload = mutableMapOf<String, Any>(
                    "id" to chatId,
                    "type" to "direct",
                    "memberIds" to listOf(uid, targetUserId),
                    "adminIds" to listOf(uid),
                    "lastMessageText" to "Conversation started",
                    "lastSenderId" to uid,
                    "lastSenderName" to currentUserName,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastTimestamp" to FieldValue.serverTimestamp()
                )
                db.collection(path).document(chatId).set(payload).await()
                Result.success(chatId)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createGroupChat(
        name: String,
        selectedMemberIds: List<String>,
        currentUserName: String
    ): Result<String> {
        val uid = requireUserId()
        val chatId = UUID.randomUUID().toString()
        val path = "chats/$chatId"
        val allMembers = (selectedMemberIds + uid).distinct()

        return try {
            val payload = mutableMapOf<String, Any>(
                "id" to chatId,
                "type" to "group",
                "name" to name.trim(),
                "memberIds" to allMembers,
                "adminIds" to listOf(uid),
                "lastMessageText" to "$currentUserName created group \"$name\"",
                "lastSenderId" to uid,
                "lastSenderName" to currentUserName,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "lastTimestamp" to FieldValue.serverTimestamp()
            )
            db.collection("chats").document(chatId).set(payload).await()
            Result.success(chatId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeMessages(chatId: String): Flow<List<Message>> {
        val path = "chats/$chatId/messages"
        return db.collection("chats").document(chatId).collection("messages")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Message::class.java).sortedBy {
                    it.createdAt?.toDate()?.time ?: 0L
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun sendMessage(
        chatId: String,
        senderName: String,
        text: String,
        type: String = "text",
        attachmentUrl: String? = null,
        replyTo: Message? = null
    ): Result<String> {
        val uid = requireUserId()
        val messageId = UUID.randomUUID().toString()
        val path = "chats/$chatId/messages/$messageId"

        return try {
            val chatDoc = db.collection("chats").document(chatId).get().await()
            val chat = chatDoc.toObject(Chat::class.java)
                ?: throw IllegalStateException("Chat not found")

            val messagePayload = mutableMapOf<String, Any>(
                "id" to messageId,
                "chatId" to chatId,
                "senderId" to uid,
                "senderName" to senderName,
                "text" to text.trim(),
                "type" to type,
                "status" to "sent",
                "readBy" to mapOf(uid to true),
                "memberIds" to chat.memberIds,
                "deleted" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )

            attachmentUrl?.let { messagePayload["attachmentUrl"] = it }
            replyTo?.let {
                messagePayload["replyToId"] = it.id
                messagePayload["replyToText"] = if (it.deleted) "Deleted message" else it.text.take(120)
                messagePayload["replyToSender"] = it.senderName
            }

            db.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .set(messagePayload).await()

            // Update parent chat snippet
            val previewText = when (type) {
                "image" -> if (text.isNotBlank()) "📷 $text" else "📷 Photo"
                "file" -> if (text.isNotBlank()) "📎 $text" else "📎 Attachment"
                else -> text.trim()
            }

            db.collection("chats").document(chatId).update(
                mapOf(
                    "lastMessageText" to previewText,
                    "lastSenderId" to uid,
                    "lastSenderName" to senderName,
                    "lastTimestamp" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(messageId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markMessagesAsRead(chatId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No current user"))
        val path = "chats/$chatId/messages"
        return try {
            val unreadSnapshot = db.collection("chats").document(chatId).collection("messages")
                .get().await()

            for (doc in unreadSnapshot.documents) {
                val readByMap = (doc.get("readBy") as? Map<*, *>) ?: emptyMap<Any, Any>()
                val isAlreadyRead = readByMap[uid] == true
                if (!isAlreadyRead) {
                    doc.reference.update(
                        mapOf(
                            "readBy.$uid" to true,
                            "status" to "read"
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(chatId: String, messageId: String): Result<Unit> {
        val uid = requireUserId()
        val path = "chats/$chatId/messages/$messageId"
        return try {
            db.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update(
                    mapOf(
                        "deleted" to true,
                        "text" to "This message was deleted"
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
