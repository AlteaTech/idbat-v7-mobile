package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.idbat.mobile.data.entities.PassageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PassageDao {
    @Insert
    suspend fun insertPassage(passage: PassageEntity): Long

    @Insert
    suspend fun insertPassages(passages: List<PassageEntity>): List<Long>

    @Query("SELECT * FROM passage ORDER BY dateHeure DESC")
    fun getAllPassagesFlow(): Flow<List<PassageEntity>>

    @Query("SELECT * FROM passage WHERE siteId = :siteId ORDER BY dateHeure DESC")
    fun getPassagesBySiteFlow(siteId: Long): Flow<List<PassageEntity>>

    @Query("SELECT COUNT(*) FROM passage")
    suspend fun count(): Long

    @Query("SELECT * FROM passage ORDER BY dateHeure ASC")
    suspend fun getAllPassages(): List<PassageEntity>

    @Query("DELETE FROM passage WHERE id = :id")
    suspend fun deleteById(id: Long)
}
