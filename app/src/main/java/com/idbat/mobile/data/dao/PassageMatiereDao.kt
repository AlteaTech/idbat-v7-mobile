package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.idbat.mobile.data.entities.PassageMatiereEntity

@Dao
interface PassageMatiereDao {
    @Insert
    suspend fun insertMatieres(matieres: List<PassageMatiereEntity>)

    @Query("SELECT * FROM passage_matiere WHERE passageId = :passageId")
    suspend fun getMatieresByPassage(passageId: Long): List<PassageMatiereEntity>
}
