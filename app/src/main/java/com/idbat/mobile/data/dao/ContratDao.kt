package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.idbat.mobile.data.entities.ContratEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContratDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContrats(contrats: List<ContratEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContrat(contrat: ContratEntity)

    @Query("SELECT * FROM contrats")
    fun getAllContratsFlow(): Flow<List<ContratEntity>>
    
    @Query("SELECT * FROM contrats WHERE id = :id LIMIT 1")
    suspend fun getContratById(id: Long): ContratEntity?

    @Query("DELETE FROM contrats")
    suspend fun clearContrats()
}
