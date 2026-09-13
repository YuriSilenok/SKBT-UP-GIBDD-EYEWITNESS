package com.example.skbt_up_gibdd_eyewitness.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY createdAtEpochMillis, id")
    fun observeAll(): Flow<List<MessageEntity>>

    @Upsert
    suspend fun upsert(message: MessageEntity)

    @Upsert
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(messages: List<MessageEntity>) {
        deleteAll()
        upsertAll(messages)
    }
}
