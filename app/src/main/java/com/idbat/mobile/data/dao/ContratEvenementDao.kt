package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.idbat.mobile.data.entities.ContratEvenementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContratEvenementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvenements(evenements: List<ContratEvenementEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvenement(evenement: ContratEvenementEntity)

    // Saisie signalement : seuls les événements actifs (isEnable = 1) sont proposés
    @Query("SELECT * FROM contrat_evenements WHERE contratId = :contratId AND isEnable = 1")
    fun getEvenementsByContratFlow(contratId: Long): Flow<List<ContratEvenementEntity>>

    @Query("DELETE FROM contrat_evenements")
    suspend fun clearEvenements()
}
