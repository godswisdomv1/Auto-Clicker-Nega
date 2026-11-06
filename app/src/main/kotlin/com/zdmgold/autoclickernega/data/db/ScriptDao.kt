package com.zdmgold.autoclickernega.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {
    @Query("SELECT id, name, mode, itemCount, updatedAt FROM scripts ORDER BY updatedAt DESC")
    fun summaries(): Flow<List<ScriptSummaryRow>>

    @Query("SELECT * FROM scripts WHERE id = :id")
    suspend fun get(id: String): ScriptEntity?

    @Upsert
    suspend fun upsert(entity: ScriptEntity)

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun delete(id: String): Int
}
