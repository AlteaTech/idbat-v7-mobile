package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.idbat.mobile.data.entities.SignalementDocumentEntity

@Dao
interface SignalementDocumentDao {
    @Insert
    suspend fun insertDocuments(documents: List<SignalementDocumentEntity>)

    @Query("SELECT * FROM signalement_document WHERE signalementId = :signalementId")
    suspend fun getDocumentsBySignalement(signalementId: Long): List<SignalementDocumentEntity>
}
