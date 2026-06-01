package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.idbat.mobile.data.entities.PassageDocumentEntity

@Dao
interface PassageDocumentDao {
    @Insert
    suspend fun insertDocuments(documents: List<PassageDocumentEntity>)

    @Query("SELECT * FROM passage_document WHERE passageId = :passageId")
    suspend fun getDocumentsByPassage(passageId: Long): List<PassageDocumentEntity>
}
