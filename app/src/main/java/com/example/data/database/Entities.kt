package com.example.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "messages",
    indices = [Index(value = ["conversationId"])],
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long = 1L,
    val sender: String, // "USER" or "ASSISTANT" or "SYSTEM"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val kind: String = "CONVERSATION", // CONVERSATION, ACTION, CLARIFICATION, UNSUPPORTED
    val actionType: String = "NONE",
    val actionStatus: String = "NONE", // EXECUTED, FALLBACK, FAILED, NONE
    val actionDetails: String? = null
)

@Entity(tableName = "action_history")
data class ActionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionType: String,
    val target: String,
    val parametersJson: String,
    val status: String, // "AUTONOMOUS", "FALLBACK_LAUNCHED", "DENIED", "ERROR"
    val timestamp: Long = System.currentTimeMillis(),
    val executionMessage: String
)
