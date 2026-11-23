package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.idbat.mobile.data.entities.MotifAutorisationContratEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MotifAutorisationContratDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(motifs: List<MotifAutorisationContratEntity>)

    @Query("SELECT * FROM motif_autorisation_contrat WHERE contratId = :contratId ORDER BY libelle COLLATE NOCASE")
    fun getByContratFlow(contratId: Long): Flow<List<MotifAutorisationContratEntity>>

    @Query("DELETE FROM motif_autorisation_contrat")
    suspend fun clear()
}
