package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.idbat.mobile.data.entities.LastSynchroHistoryEntity
import com.idbat.mobile.data.entities.TypeSynchro
import kotlinx.coroutines.flow.Flow

@Dao
interface LastSynchroHistoryDao {
    @Query("SELECT * FROM last_synchro_history_sites WHERE siteId = :siteId ORDER BY date DESC LIMIT 1")
    suspend fun getLastSynchroForSite(siteId: Long): LastSynchroHistoryEntity?

    @Query("SELECT * FROM last_synchro_history_sites WHERE siteId = :siteId AND type = :type ORDER BY date DESC LIMIT 1")
    suspend fun getLastSynchroForSiteAndType(siteId: Long, type: TypeSynchro): LastSynchroHistoryEntity?

    @Query("SELECT * FROM last_synchro_history_sites")
    fun getAllSynchroHistoryFlow(): Flow<List<LastSynchroHistoryEntity>>

    @Insert
    suspend fun insertSynchro(synchro: LastSynchroHistoryEntity)

    @Update
    suspend fun updateSynchro(synchro: LastSynchroHistoryEntity)

    // Nouvelles méthodes pour les statistiques
    @Query("SELECT SUM(operationsTentees) FROM last_synchro_history_sites WHERE siteId = :siteId")
    suspend fun getTotalOperationsTentees(siteId: Long): Long?

    @Query("SELECT SUM(operationsReussies) FROM last_synchro_history_sites WHERE siteId = :siteId")
    suspend fun getTotalOperationsReussies(siteId: Long): Long?
}