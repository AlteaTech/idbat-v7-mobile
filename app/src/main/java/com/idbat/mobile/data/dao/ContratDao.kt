package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.idbat.mobile.data.entities.ContratEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContratDao {
    @Upsert
    suspend fun insertContrats(contrats: List<ContratEntity>)

    @Upsert
    suspend fun insertContrat(contrat: ContratEntity)

    @Query("SELECT * FROM contrats")
    fun getAllContratsFlow(): Flow<List<ContratEntity>>

    @Query("SELECT count(1) FROM contrats")
    suspend fun count(): Long

    @Query("delete FROM contrats")
    suspend fun purge()

    @Query("SELECT * FROM contrats WHERE id = :id LIMIT 1")
    suspend fun getContratById(id: Long): ContratEntity?

    @Query("DELETE FROM contrats")
    suspend fun clearContrats()
}
