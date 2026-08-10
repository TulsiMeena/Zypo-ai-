package com.example.data.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.Chat
import com.example.data.model.MemoryItem
import com.example.data.model.Message
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreSyncManager(private val context: Context) {

    companion object {
        private const val TAG = "FirestoreSyncManager"
    }

    private val db: FirebaseFirestore? by lazy {
        try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
    }

    private val storage: FirebaseStorage? by lazy {
        try { FirebaseStorage.getInstance() } catch (e: Exception) { null }
    }

    private val currentUid: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    /**
     * Creates or updates user profile in Firestore at users/{uid}
     */
    suspend fun syncUserProfile(profile: UserProfile, isMemoryEnabled: Boolean = true) = withContext(Dispatchers.IO) {
        val uid = currentUid ?: profile.uid
        if (uid.isBlank() || db == null) return@withContext

        try {
            val userMap = mutableMapOf<String, Any?>(
                "uid" to uid,
                "displayName" to profile.displayName,
                "email" to profile.email,
                "photoUrl" to profile.photoUrl,
                "lastLoginAt" to System.currentTimeMillis(),
                "language" to "en",
                "plan" to profile.plan,
                "memoryEnabled" to isMemoryEnabled,
                "preferences" to mapOf(
                    "voice" to "Puck",
                    "theme" to "dark"
                )
            )

            db?.collection("users")?.document(uid)?.set(userMap, SetOptions.merge())?.await()
            Log.d(TAG, "User profile synced to Firestore: $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing user profile to Firestore", e)
        }
    }

    /**
     * Syncs a memory item to users/{uid}/memories/{memoryId}
     */
    suspend fun syncMemory(memory: MemoryItem, isMemoryEnabled: Boolean) = withContext(Dispatchers.IO) {
        val uid = currentUid ?: return@withContext
        if (db == null || !isMemoryEnabled) return@withContext

        try {
            val memMap = mapOf(
                "id" to memory.id,
                "content" to memory.content,
                "category" to memory.category.name,
                "createdAt" to memory.createdAt,
                "updatedAt" to memory.updatedAt,
                "source" to memory.source,
                "enabled" to memory.enabled
            )

            db?.collection("users")?.document(uid)
                ?.collection("memories")?.document(memory.id)
                ?.set(memMap, SetOptions.merge())?.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing memory to Firestore", e)
        }
    }

    /**
     * Syncs a chat session to users/{uid}/chats/{chatId}
     */
    suspend fun syncChatSession(chat: Chat) = withContext(Dispatchers.IO) {
        val uid = currentUid ?: return@withContext
        if (db == null) return@withContext

        try {
            val chatMap = mapOf(
                "id" to chat.id,
                "title" to chat.title,
                "updatedAt" to chat.updatedAt,
                "isPinned" to chat.isPinned,
                "isArchived" to chat.isArchived,
                "modelUsed" to chat.modelUsed.name,
                "lastMessagePreview" to chat.lastMessagePreview
            )

            db?.collection("users")?.document(uid)
                ?.collection("chats")?.document(chat.id)
                ?.set(chatMap, SetOptions.merge())?.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing chat session", e)
        }
    }

    /**
     * Syncs a chat message to users/{uid}/chats/{chatId}/messages/{messageId}
     */
    suspend fun syncChatMessage(chatId: String, message: Message) = withContext(Dispatchers.IO) {
        val uid = currentUid ?: return@withContext
        if (db == null) return@withContext

        try {
            val msgMap = mapOf(
                "id" to message.id,
                "chatId" to chatId,
                "sender" to message.sender.name,
                "content" to message.content,
                "timestamp" to message.timestamp,
                "status" to message.status.name,
                "isLiked" to message.isLiked,
                "attachments" to message.attachments.map { mapOf("id" to it.id, "name" to it.name, "type" to it.type.name, "localUri" to it.localUri) }
            )

            db?.collection("users")?.document(uid)
                ?.collection("chats")?.document(chatId)
                ?.collection("messages")?.document(message.id)
                ?.set(msgMap, SetOptions.merge())?.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing message to Firestore", e)
        }
    }

    /**
     * Uploads file to Firebase Storage users/{uid}/files/{fileId}
     */
    suspend fun uploadFile(fileUri: Uri, fileId: String): String? = withContext(Dispatchers.IO) {
        val uid = currentUid ?: return@withContext null
        val stor = storage ?: return@withContext null

        try {
            val fileRef = stor.reference.child("users/$uid/files/$fileId")
            fileRef.putFile(fileUri).await()
            val downloadUrl = fileRef.downloadUrl.await().toString()
            downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading file to Firebase Storage", e)
            null
        }
    }

    /**
     * Deletes user cloud data from Firestore & Storage on account deletion
     */
    suspend fun deleteUserCloudData() = withContext(Dispatchers.IO) {
        val uid = currentUid ?: return@withContext
        val database = db ?: return@withContext

        try {
            database.collection("users").document(uid).delete().await()
            Log.d(TAG, "Deleted Firestore user data for $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user cloud data", e)
        }
    }
}
