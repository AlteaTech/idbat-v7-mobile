package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.idbat.mobile.data.entities.PassageRefuseEntity

@Dao
interface PassageRefuseDao {

    @Insert
    suspend fun insert(passageRefuse: PassageRefuseEntity): Long

    @Query("SELECT * FROM passage_refuse ORDER BY dateHeure DESC")
    suspend fun getAll(): List<PassageRefuseEntity>

    // RG4 : passages refusés pas encore transmis au BO
    @Query("SELECT * FROM passage_refuse WHERE sentAt IS NULL ORDER BY dateHeure ASC")
    suspend fun getUnsent(): List<PassageRefuseEntity>

    // Suivi : nombre total en base (envoyés non purgés + non envoyés)
    @Query("SELECT COUNT(*) FROM passage_refuse")
    suspend fun count(): Long

    // Suivi : nombre restant à transférer
    @Query("SELECT COUNT(*) FROM passage_refuse WHERE sentAt IS NULL")
    suspend fun countUnsent(): Long

    // RG4 : marque un passage refusé comme envoyé avec succès
    @Query("UPDATE passage_refuse SET sentAt = :sentAt WHERE id = :id")
    suspend fun markSent(id: Long, sentAt: Long)

    // RG3 (rétention) : purge des passages refusés envoyés et saisis il y a plus de X (millis)
    @Query("DELETE FROM passage_refuse WHERE sentAt IS NOT NULL AND dateHeure < :threshold")
    suspend fun deleteSentOlderThan(threshold: Long)
}
