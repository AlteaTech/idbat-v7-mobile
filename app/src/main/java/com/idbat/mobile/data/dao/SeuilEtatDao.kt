package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.idbat.mobile.data.entities.SeuilEtatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeuilEtatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeuils(seuils: List<SeuilEtatEntity>)

    @Query("SELECT * FROM seuil_etat WHERE usagerId = :usagerId")
    suspend fun getSeuilsByUsager(usagerId: Long): List<SeuilEtatEntity>

    @Query("SELECT * FROM seuil_etat WHERE usagerId = :usagerId")
    fun getSeuilsByUsagerFlow(usagerId: Long): Flow<List<SeuilEtatEntity>>

    @Query("DELETE FROM seuil_etat")
    suspend fun clearSeuils()
}
