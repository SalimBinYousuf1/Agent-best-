package com.example.data.database

import kotlinx.coroutines.flow.Flow

class AssistantRepository(private val dao: AssistantDao) {
    val conversations: Flow<List<ConversationEntity>> = dao.getAllConversations()
    val actionHistory: Flow<List<ActionHistoryEntity>> = dao.getAllActionHistory()

    fun getMessages(conversationId: Long): Flow<List<MessageEntity>> {
        return dao.getMessagesForConversation(conversationId)
    }

    suspend fun getOrCreateDefaultConversation(): Long {
        val existing = dao.getConversationById(1L)
        return if (existing != null) {
            1L
        } else {
            dao.insertConversation(
                ConversationEntity(
                    id = 1L,
                    title = "General Assistance",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun addMessage(message: MessageEntity): Long {
        val id = dao.insertMessage(message)
        val conversation = dao.getConversationById(message.conversationId)
        if (conversation != null) {
            dao.updateConversation(conversation.copy(updatedAt = System.currentTimeMillis()))
        }
        return id
    }

    suspend fun recordAction(action: ActionHistoryEntity): Long {
        return dao.insertActionHistory(action)
    }

    suspend fun createConversation(title: String): Long {
        val id = System.currentTimeMillis()
        dao.insertConversation(
            ConversationEntity(
                id = id,
                title = title,
                createdAt = id,
                updatedAt = id
            )
        )
        return id
    }

    suspend fun deleteConversation(conversationId: Long) {
        dao.deleteMessagesForConversation(conversationId)
        dao.deleteConversationById(conversationId)
    }

    suspend fun updateConversationTitle(conversationId: Long, title: String) {
        val existing = dao.getConversationById(conversationId)
        if (existing != null) {
            dao.updateConversation(existing.copy(title = title, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun clearCurrentConversation(conversationId: Long = 1L) {
        dao.deleteMessagesForConversation(conversationId)
    }

    suspend fun clearAllConversations() {
        dao.deleteAllMessages()
        dao.deleteAllConversations()
    }

    suspend fun clearActionHistory() {
        dao.deleteAllActionHistory()
    }
}
