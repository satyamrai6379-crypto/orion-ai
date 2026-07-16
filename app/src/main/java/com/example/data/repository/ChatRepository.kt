package com.example.data.repository

import com.example.data.api.RetrofitClient
import com.example.data.db.ChatDao
import com.example.data.db.ChatEntity
import com.example.data.db.MessageEntity
import com.example.data.models.Content
import com.example.data.models.GenerateContentRequest
import com.example.data.models.Part
import com.example.data.models.GenerationConfig
import com.example.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChatRepository(private val chatDao: ChatDao) {

    val allChats: Flow<List<ChatEntity>> = chatDao.getAllChats()

    fun getMessages(chatId: Long): Flow<List<MessageEntity>> {
        return chatDao.getMessagesForChat(chatId)
    }

    suspend fun createNewChat(title: String): Long {
        val chatId = chatDao.insertChat(ChatEntity(title = title))
        saveChatToFirestore(chatId, title, System.currentTimeMillis())
        return chatId
    }

    suspend fun updateChatTitle(chatId: Long, title: String) {
        chatDao.updateChat(ChatEntity(id = chatId, title = title))
        saveChatToFirestore(chatId, title, System.currentTimeMillis())
    }

    suspend fun deleteChat(chatId: Long) {
        chatDao.deleteChatById(chatId)
        chatDao.deleteMessagesForChat(chatId)
        
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("users").document(uid)
                .collection("chats").document(chatId.toString())
                .delete()
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Error deleting chat in Firestore", e)
        }
    }

    suspend fun saveMessage(chatId: Long, role: String, text: String): Long {
        val messageId = chatDao.insertMessage(
            MessageEntity(
                chatId = chatId,
                role = role,
                text = text
            )
        )
        saveMessageToFirestore(chatId, messageId, role, text, System.currentTimeMillis())
        return messageId
    }

    private fun saveChatToFirestore(chatId: Long, title: String, createdAt: Long) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            val firestore = FirebaseFirestore.getInstance()
            val chatData = hashMapOf(
                "title" to title,
                "createdAt" to createdAt
            )
            firestore.collection("users").document(uid)
                .collection("chats").document(chatId.toString())
                .set(chatData)
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Error saving chat to Firestore", e)
        }
    }

    private fun saveMessageToFirestore(chatId: Long, messageId: Long, role: String, text: String, timestamp: Long) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            val firestore = FirebaseFirestore.getInstance()
            val msgData = hashMapOf(
                "role" to role,
                "text" to text,
                "timestamp" to timestamp
            )
            firestore.collection("users").document(uid)
                .collection("chats").document(chatId.toString())
                .collection("messages").document(messageId.toString())
                .set(msgData)
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Error saving message to Firestore", e)
        }
    }

    suspend fun syncFromFirestore() = withContext(Dispatchers.IO) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext
        try {
            val firestore = FirebaseFirestore.getInstance()
            val chatsSnapshot = Tasks.await(
                firestore.collection("users").document(uid).collection("chats").get()
            )
            for (chatDoc in chatsSnapshot.documents) {
                val chatIdStr = chatDoc.id
                val chatId = chatIdStr.toLongOrNull() ?: continue
                val title = chatDoc.getString("title") ?: "Chat"
                val createdAt = chatDoc.getLong("createdAt") ?: System.currentTimeMillis()
                
                chatDao.insertChat(ChatEntity(id = chatId, title = title, createdAt = createdAt))
                
                val messagesSnapshot = Tasks.await(
                    firestore.collection("users").document(uid).collection("chats")
                        .document(chatIdStr).collection("messages").get()
                )
                for (msgDoc in messagesSnapshot.documents) {
                    val msgId = msgDoc.id.toLongOrNull() ?: continue
                    val role = msgDoc.getString("role") ?: "user"
                    val text = msgDoc.getString("text") ?: ""
                    val timestamp = msgDoc.getLong("timestamp") ?: System.currentTimeMillis()
                    
                    chatDao.insertMessage(
                        MessageEntity(
                            id = msgId,
                            chatId = chatId,
                            role = role,
                            text = text,
                            timestamp = timestamp
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Error syncing from Firestore", e)
        }
    }

    private fun getBase64FromUri(context: android.content.Context, uriString: String): String? {
        return try {
            val uri = android.net.Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            inputStream.close()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Error getting Base64 for $uriString", e)
            null
        }
    }

    suspend fun generateAiResponse(
        context: android.content.Context,
        systemInstruction: String,
        conversationHistory: List<MessageEntity>,
        model: String = "gemini-3.5-flash",
        temperature: Float = 0.7f
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Error: Gemini API Key is missing. Please configure it via the Secrets panel in AI Studio."
        }

        val contents = conversationHistory.map { msg ->
            if (msg.role == "user" && msg.text.startsWith("[attachment:")) {
                val endBracket = msg.text.indexOf(']')
                if (endBracket != -1) {
                    val contentStr = msg.text.substring(12, endBracket)
                    val partsList = contentStr.split(":", limit = 3)
                    if (partsList.size >= 3) {
                        val mimeType = partsList[0]
                        val uriString = partsList[1]
                        val actualText = msg.text.substring(endBracket + 1)
                        val base64 = getBase64FromUri(context, uriString)
                        if (base64 != null) {
                            return@map Content(
                                role = "user",
                                parts = listOf(
                                    Part(inlineData = com.example.data.models.InlineData(mimeType = mimeType, data = base64)),
                                    Part(text = actualText.ifEmpty { "Analyze this attachment" })
                                )
                            )
                        }
                    }
                }
            }

            Content(
                role = if (msg.role == "user") "user" else "model",
                parts = listOf(
                    Part(
                        text = if (msg.text.startsWith("[attachment:")) {
                            val endBracket = msg.text.indexOf(']')
                            if (endBracket != -1) msg.text.substring(endBracket + 1) else msg.text
                        } else msg.text
                    )
                )
            )
        }

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = Content(
                parts = listOf(Part(text = systemInstruction))
            ),
            generationConfig = GenerationConfig(
                temperature = temperature
            )
        )

        return try {
            val response = RetrofitClient.service.generateContent(model, apiKey, request)
            val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            reply ?: "Error: Received empty response from Orion AI engine."
        } catch (e: Exception) {
            "Error details: ${e.localizedMessage ?: "Unknown network error"}"
        }
    }
}
