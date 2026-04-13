package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.idbat.mobile.data.entities.MotifListeNoireContratEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MotifListeNoireContratDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMotifs(motifs: List<MotifListeNoireContratEntity>)

    @Query("SELECT * FROM motifs_liste_noire_contrat")
    fun getAllMotifsFlow(): Flow<List<MotifListeNoireContratEntity>>

    @Query("SELECT * FROM motifs_liste_noire_contrat WHERE contratId = :contratId")
    fun getMotifsByContratFlow(contratId: Long): Flow<List<MotifListeNoireContratEntity>>
    
    @Query("DELETE FROM motifs_liste_noire_contrat")
    suspend fun clearMotifs()
}
