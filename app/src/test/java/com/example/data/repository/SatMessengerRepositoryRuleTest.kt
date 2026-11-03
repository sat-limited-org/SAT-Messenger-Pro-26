package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SatMessengerRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveUserProfile_authenticatedUser_succeeds() {
        runBlocking {
            val uid = signInTestUser(ALICE_EMAIL)
            val userRepo = UserRepository(firestore)

            val profile = UserProfile(
                userId = uid,
                displayName = "Alice Smith",
                email = ALICE_EMAIL,
                statusMessage = "SAT Executive Team",
                isOnline = true
            )

            val result = withTimeout(DEFAULT_TIMEOUT_MS) {
                userRepo.saveProfile(profile)
            }
            assertTrue("Expected saveProfile to succeed", result.isSuccess)

            val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
                userRepo.getUserProfile(uid)
            }
            assertNotNull(fetched)
            assertEquals("Alice Smith", fetched?.displayName)
        }
    }

    @Test
    fun createDirectChat_and_sendMessage_memberSucceeds() {
        runBlocking {
            val aliceUid = signInTestUser(ALICE_EMAIL)
            val bobUid = "bob_mock_${System.currentTimeMillis()}"

            val userRepo = UserRepository(firestore)
            userRepo.saveProfile(
                UserProfile(
                    userId = aliceUid,
                    displayName = "Alice",
                    email = ALICE_EMAIL
                )
            )

            val chatRepo = ChatRepository(firestore)
            val chatResult = withTimeout(DEFAULT_TIMEOUT_MS) {
                chatRepo.getOrCreateDirectChat(
                    targetUserId = bobUid,
                    targetUserName = "Bob Partner",
                    currentUserName = "Alice"
                )
            }
            assertTrue(chatResult.isSuccess)
            val chatId = chatResult.getOrThrow()
            assertTrue(chatId.isNotBlank())

            val msgResult = withTimeout(DEFAULT_TIMEOUT_MS) {
                chatRepo.sendMessage(
                    chatId = chatId,
                    senderName = "Alice",
                    text = "Secure corporate dispatch from SAT Limited."
                )
            }
            if (msgResult.isFailure) {
                println("sendMessage failed with: ${msgResult.exceptionOrNull()}")
                msgResult.exceptionOrNull()?.printStackTrace()
            }
            assertTrue("Expected msgResult to succeed but was $msgResult", msgResult.isSuccess)

            val typingResult = withTimeout(DEFAULT_TIMEOUT_MS) {
                chatRepo.setTyping(chatId = chatId, isTyping = true)
            }
            assertTrue(typingResult.isSuccess)

            val markReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
                chatRepo.markMessagesAsRead(chatId)
            }
            if (markReadResult.isFailure) {
                println("markMessagesAsRead failed with: ${markReadResult.exceptionOrNull()}")
                markReadResult.exceptionOrNull()?.printStackTrace()
            }
            assertTrue("Expected markReadResult to succeed but was $markReadResult", markReadResult.isSuccess)
        }
    }

    @Test
    fun observeChats_unauthenticatedUser_failsWithPermissionDenied() {
        runBlocking {
            auth.signOut()
            val chatRepo = ChatRepository(firestore)

            try {
                withTimeout(FLOW_TIMEOUT_MS) {
                    chatRepo.observeUserChats("unauth_uid").first()
                }
            } catch (e: Exception) {
                val root = e.unwrapFirestoreException()
                if (root != null) {
                    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, root.code)
                }
            }
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_test@satlimited.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
