package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.idbat.mobile.data.entities.UsagerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsagerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsagers(usagers: List<UsagerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsager(usager: UsagerEntity)

    @Query("SELECT * FROM usagers")
    fun getAllUsagersFlow(): Flow<List<UsagerEntity>>

    @Query("SELECT * FROM usagers WHERE contratId = :contratId")
    fun getUsagersByContratFlow(contratId: Long): Flow<List<UsagerEntity>>
    
    @Query("DELETE FROM usagers")
    suspend fun purge()
}