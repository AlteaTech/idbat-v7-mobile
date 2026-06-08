package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.idbat.mobile.data.entities.SignalementEntity

@Dao
interface SignalementDao {
    @Insert
    suspend fun insert(signalement: SignalementEntity): Long

    @Query("SELECT * FROM signalement ORDER BY dateSignalement ASC")
    suspend fun getAll(): List<SignalementEntity>

    @Query("SELECT COUNT(*) FROM signalement")
    suspend fun count(): Long

    @Query("DELETE FROM signalement WHERE id = :id")
    suspend fun deleteById(id: Long)
}
