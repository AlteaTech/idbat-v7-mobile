package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.idbat.mobile.data.entities.PassageInconnuEntity

@Dao
interface PassageInconnuDao {

    @Insert
    suspend fun insert(entity: PassageInconnuEntity): Long

    @Query("SELECT * FROM passage_inconnu WHERE sentAt IS NULL ORDER BY dateHeure ASC")
    suspend fun getUnsent(): List<PassageInconnuEntity>

    @Query("SELECT COUNT(*) FROM passage_inconnu")
    suspend fun count(): Long

    @Query("SELECT COUNT(*) FROM passage_inconnu WHERE sentAt IS NULL")
    suspend fun countUnsent(): Long

    @Query("UPDATE passage_inconnu SET sentAt = :sentAt WHERE id = :id")
    suspend fun markSent(id: Long, sentAt: Long)

    // RG3 (rétention) : purge des lignes envoyées et saisies il y a plus de X (millis)
    @Query("DELETE FROM passage_inconnu WHERE sentAt IS NOT NULL AND dateHeure < :threshold")
    suspend fun deleteSentOlderThan(threshold: Long)
}
