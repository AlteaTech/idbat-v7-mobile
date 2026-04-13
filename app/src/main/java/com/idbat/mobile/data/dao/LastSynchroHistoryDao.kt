package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.idbat.mobile.data.entities.LastSynchroHistoryEntity
import com.idbat.mobile.data.entities.TypeSynchro
import kotlinx.coroutines.flow.Flow

@Dao
interface LastSynchroHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSynchroHistory(history: LastSynchroHistoryEntity)

    @Query("SELECT * FROM last_synchro_history_sites WHERE siteId = :siteId ORDER BY date DESC LIMIT 1")
    suspend fun getLastSynchroForSite(siteId: Long): LastSynchroHistoryEntity?

    @Query("SELECT * FROM last_synchro_history_sites WHERE siteId = :siteId AND type = :type ORDER BY date DESC LIMIT 1")
    suspend fun getLastSynchroForSiteAndType(siteId: Long, type: TypeSynchro): LastSynchroHistoryEntity?

    @Query("SELECT * FROM last_synchro_history_sites")
    fun getAllSynchroHistoryFlow(): Flow<List<LastSynchroHistoryEntity>>
    
    @Query("DELETE FROM last_synchro_history_sites")
    suspend fun clearHistory()
}
