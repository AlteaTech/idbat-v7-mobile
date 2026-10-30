package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.idbat.mobile.data.entities.PassageSeuilEntity

@Dao
interface PassageSeuilDao {
    @Insert
    suspend fun insertSeuils(seuils: List<PassageSeuilEntity>)

    @Query("SELECT * FROM passage_seuil WHERE passageId = :passageId")
    suspend fun getSeuilsByPassage(passageId: Long): List<PassageSeuilEntity>
}
