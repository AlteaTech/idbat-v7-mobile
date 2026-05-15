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

    @Query("SELECT * FROM contrat_evenements")
    fun getAllEvenementsFlow(): Flow<List<ContratEvenementEntity>>

    @Query("SELECT * FROM contrat_evenements WHERE contratId = :contratId")
    fun getEvenementsByContratFlow(contratId: Long): Flow<List<ContratEvenementEntity>>
    
    @Query("DELETE FROM contrat_evenements")
    suspend fun purge()
}
