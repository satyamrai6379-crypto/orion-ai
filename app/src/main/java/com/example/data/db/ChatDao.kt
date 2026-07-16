package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity): Long

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChatById(chatId: Long)

    @Query("SELECT * FROM chats ORDER BY createdAt DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats")
    suspend fun getAllChatsList(): List<ChatEntity>

    @Query("SELECT * FROM messages")
    suspend fun getAllMessagesList(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: Long): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: Long)
}
