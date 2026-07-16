package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.ChatEntity
import com.example.data.db.MessageEntity
import com.example.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository
    private val chatPrefs = application.getSharedPreferences("orion_chat_settings", Context.MODE_PRIVATE)

    var messageCountForAds = 0
        private set

    fun incrementMessageCount() {
        messageCountForAds++
    }

    fun resetMessageCountForAds() {
        messageCountForAds = 0
    }

    // State flows
    val allChats: StateFlow<List<ChatEntity>>
    
    private val _currentChat = MutableStateFlow<ChatEntity?>(null)
    val currentChat: StateFlow<ChatEntity?> = _currentChat.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _splashActive = MutableStateFlow(true)
    val splashActive: StateFlow<Boolean> = _splashActive.asStateFlow()

    private var messageCollectionJob: Job? = null

    enum class AiMode(val displayName: String, val systemInstruction: String) {
        GENERAL(
            "General Assistant",
            "You are Orion AI, a premium, highly advanced, and supportive conversational AI companion.\nYour visual environment is a deep, starry space-themed modern interface.\nYour output should be articulate, formatted with beautiful, elegant Markdown. Use headings (##, ###), bullet lists, and code blocks with language identifiers where appropriate.\nAlways keep a friendly, brilliant, and sophisticated tone."
        ),
        CODING(
            "Coding Assistant",
            "You are Orion Coding Assistant, an elite, world-class software engineering companion.\nProvide extremely precise, optimized, and beautifully formatted code snippets.\nUse Markdown formatting with explicit language identifiers for code blocks.\nExplain your architectural decisions and reasoning concisely.\nAlways guide the user toward clean, maintainable, and modern software design patterns."
        ),
        STUDY(
            "Study Assistant",
            "You are Orion Study Assistant, a patient, encouraging, and brilliant academic mentor.\nBreak down complex subjects into highly digestible, structured sections.\nUse bullet points, bold key concepts, and structured analogies.\nOffer to summarize concepts, generate flashcard-style outlines, or quiz the user to reinforce learning.\nKeep your explanations clear, structured, and educational."
        ),
        WRITING(
            "Writing Assistant",
            "You are Orion Writing Assistant, an elite editor, creative writing partner, and copywriter.\nFocus on improving prose style, rhythm, vocabulary, clarity, and tone.\nHelp the user write, draft, proofread, or expand their essays, emails, stories, or copy.\nAlways provide constructive critique and present alternative formulations to let the user choose."
        )
    }

    private val _activeMode = MutableStateFlow(AiMode.GENERAL)
    val activeMode: StateFlow<AiMode> = _activeMode.asStateFlow()

    fun setActiveMode(mode: AiMode) {
        _activeMode.value = mode
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _searchMessageQuery = MutableStateFlow("")
    val searchMessageQuery: StateFlow<String> = _searchMessageQuery.asStateFlow()

    fun setSearchMessageQuery(query: String) {
        _searchMessageQuery.value = query
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ChatRepository(database.chatDao())
        
        // Expose chats
        allChats = MutableStateFlow<List<ChatEntity>>(emptyList())
        viewModelScope.launch {
            repository.allChats.collectLatest {
                (allChats as MutableStateFlow).value = it
            }
        }

        // Sync with cloud on start if user is authenticated
        if (FirebaseAuth.getInstance().currentUser != null) {
            syncWithCloud()
        }

        // Handle splash delay
        viewModelScope.launch {
            delay(2200) // Show splash for 2.2s for deep constellation intro
            _splashActive.value = false
        }
    }

    fun getSelectedModel(): String {
        return chatPrefs.getString("selected_model", "gemini-3.5-flash") ?: "gemini-3.5-flash"
    }

    fun setSelectedModel(model: String) {
        chatPrefs.edit().putString("selected_model", model).apply()
    }

    fun getTemperature(): Float {
        return chatPrefs.getFloat("temperature", 0.7f)
    }

    fun setTemperature(temp: Float) {
        chatPrefs.edit().putFloat("temperature", temp).apply()
    }

    private val _selectedTheme = MutableStateFlow(chatPrefs.getString("selected_theme", "space_dark") ?: "space_dark")
    val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

    fun setSelectedTheme(theme: String) {
        chatPrefs.edit().putString("selected_theme", theme).apply()
        _selectedTheme.value = theme
    }

    private val _selectedLanguage = MutableStateFlow(chatPrefs.getString("selected_language", "en") ?: "en")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    fun setSelectedLanguage(language: String) {
        chatPrefs.edit().putString("selected_language", language).apply()
        _selectedLanguage.value = language
    }

    fun syncWithCloud() {
        viewModelScope.launch {
            repository.syncFromFirestore()
        }
    }

    fun selectChat(chat: ChatEntity) {
        _currentChat.value = chat
        observeMessages(chat.id)
    }

    fun startNewChat() {
        _currentChat.value = null
        _messages.value = emptyList()
        messageCollectionJob?.cancel()
    }

    private fun observeMessages(chatId: Long) {
        messageCollectionJob?.cancel()
        messageCollectionJob = viewModelScope.launch {
            repository.getMessages(chatId).collectLatest { msgs ->
                _messages.value = msgs
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.trim().isEmpty()) return

        viewModelScope.launch {
            var chatId = _currentChat.value?.id
            
            if (chatId == null) {
                // Determine a fitting title from first prompt
                val chatTitle = if (text.length > 25) text.take(22) + "..." else text
                chatId = repository.createNewChat(chatTitle)
                val newChat = ChatEntity(id = chatId, title = chatTitle)
                _currentChat.value = newChat
                observeMessages(chatId)
            }

            // Save user message
            repository.saveMessage(chatId, "user", text)
            
            // Trigger AI response
            getAiResponse(chatId)
        }
    }

    private suspend fun getAiResponse(chatId: Long) {
        _isTyping.value = true
        
        // Let the state update so the user message displays and typing begins
        delay(500)

        // Retrieve full conversation up to this point
        val history = _messages.value
        
        val model = getSelectedModel()
        val temp = getTemperature()
        val response = repository.generateAiResponse(getApplication(), _activeMode.value.systemInstruction, history, model, temp)
        
        // Save model response
        repository.saveMessage(chatId, "model", response)
        
        _isTyping.value = false
    }

    fun regenerateLastResponse() {
        val chatId = _currentChat.value?.id ?: return
        val currentList = _messages.value
        if (currentList.isEmpty()) return

        viewModelScope.launch {
            val lastMsg = currentList.last()
            val historyUpToRegen = if (lastMsg.role == "model") {
                currentList.dropLast(1)
            } else {
                currentList
            }

            _messages.value = historyUpToRegen
            _isTyping.value = true

            val model = getSelectedModel()
            val temp = getTemperature()
            val response = repository.generateAiResponse(getApplication(), _activeMode.value.systemInstruction, historyUpToRegen, model, temp)
            
            repository.saveMessage(chatId, "model", response)
            _isTyping.value = false
        }
    }

    fun deleteChat(chatId: Long) {
        viewModelScope.launch {
            if (_currentChat.value?.id == chatId) {
                startNewChat()
            }
            repository.deleteChat(chatId)
        }
    }

    fun exportChatsToBackupJson(context: Context, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val dao = AppDatabase.getDatabase(context).chatDao()
                val chats = dao.getAllChatsList()
                val messages = dao.getAllMessagesList()
                
                val backupJson = JSONObject()
                val chatsArray = JSONArray()
                for (chat in chats) {
                    val chatObj = JSONObject()
                    chatObj.put("id", chat.id)
                    chatObj.put("title", chat.title)
                    chatObj.put("createdAt", chat.createdAt)
                    chatsArray.put(chatObj)
                }
                backupJson.put("chats", chatsArray)
                
                val messagesArray = JSONArray()
                for (msg in messages) {
                    val msgObj = JSONObject()
                    msgObj.put("id", msg.id)
                    msgObj.put("chatId", msg.chatId)
                    msgObj.put("role", msg.role)
                    msgObj.put("text", msg.text)
                    msgObj.put("timestamp", msg.timestamp)
                    messagesArray.put(msgObj)
                }
                backupJson.put("messages", messagesArray)
                
                val contentResolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "orion_chats_backup_${System.currentTimeMillis()}.json")
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                
                val uri = contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(backupJson.toString(2).toByteArray())
                    }
                    android.widget.Toast.makeText(context, "Chats backup exported to Downloads!", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "Failed to create backup file.", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Backup failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                onComplete()
            }
        }
    }

    fun importChatsFromBackupJson(context: Context, uri: android.net.Uri, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                } ?: throw Exception("File is empty or inaccessible")
                
                val backupJson = JSONObject(jsonString)
                val chatsArray = backupJson.getJSONArray("chats")
                val messagesArray = backupJson.getJSONArray("messages")
                
                val chatsList = mutableListOf<ChatEntity>()
                for (i in 0 until chatsArray.length()) {
                    val chatObj = chatsArray.getJSONObject(i)
                    chatsList.add(
                        ChatEntity(
                            id = chatObj.getLong("id"),
                            title = chatObj.getString("title"),
                            createdAt = chatObj.getLong("createdAt")
                        )
                    )
                }
                
                val messagesList = mutableListOf<MessageEntity>()
                for (i in 0 until messagesArray.length()) {
                    val msgObj = messagesArray.getJSONObject(i)
                    messagesList.add(
                        MessageEntity(
                            id = msgObj.getLong("id"),
                            chatId = msgObj.getLong("chatId"),
                            role = msgObj.getString("role"),
                            text = msgObj.getString("text"),
                            timestamp = msgObj.getLong("timestamp")
                        )
                    )
                }
                
                val dao = AppDatabase.getDatabase(context).chatDao()
                dao.insertChats(chatsList)
                dao.insertMessages(messagesList)
                
                android.widget.Toast.makeText(context, "Successfully imported ${chatsList.size} chats!", android.widget.Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Import failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
            } finally {
                onComplete()
            }
        }
    }
}
