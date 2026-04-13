package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.idbat.mobile.data.entities.MatiereSiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatiereSiteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatieres(matieres: List<MatiereSiteEntity>)

    @Query("SELECT * FROM matiere_site")
    fun getAllMatieresFlow(): Flow<List<MatiereSiteEntity>>

    @Query("SELECT * FROM matiere_site WHERE siteId = :siteId")
    fun getMatieresBySiteFlow(siteId: Long): Flow<List<MatiereSiteEntity>>
    
    @Query("DELETE FROM matiere_site")
    suspend fun clearMatieres()
}
