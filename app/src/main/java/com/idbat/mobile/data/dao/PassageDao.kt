package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.idbat.mobile.data.entities.PassageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PassageDao {
    @Insert
    suspend fun insertPassage(passage: PassageEntity): Long

    @Insert
    suspend fun insertPassages(passages: List<PassageEntity>): List<Long>

    @Query("SELECT * FROM passage ORDER BY dateHeure DESC")
    fun getAllPassagesFlow(): Flow<List<PassageEntity>>

    @Query("SELECT * FROM passage WHERE siteId = :siteId ORDER BY dateHeure DESC")
    fun getPassagesBySiteFlow(siteId: Long): Flow<List<PassageEntity>>

    @Query("SELECT COUNT(*) FROM passage")
    suspend fun count(): Long

    // RG3 : nombre de passages restant à envoyer (bloque la synchro descendante si > 0)
    @Query("SELECT COUNT(*) FROM passage WHERE sentAt IS NULL")
    suspend fun countUnsent(): Long

    @Query("SELECT * FROM passage ORDER BY dateHeure ASC")
    suspend fun getAllPassages(): List<PassageEntity>

    // RG3 : passages non encore envoyés au BO
    @Query("SELECT * FROM passage WHERE sentAt IS NULL ORDER BY dateHeure ASC")
    suspend fun getUnsentPassages(): List<PassageEntity>

    // RG3 : marque un passage comme envoyé avec succès
    @Query("UPDATE passage SET sentAt = :sentAt WHERE id = :id")
    suspend fun markSent(id: Long, sentAt: Long)

    // RG3 : purge les passages envoyés et saisis il y a plus de X jours (cascade matières/documents)
    @Query("DELETE FROM passage WHERE sentAt IS NOT NULL AND dateHeure < :threshold")
    suspend fun deleteSentOlderThan(threshold: Long)

    @Query("DELETE FROM passage WHERE id = :id")
    suspend fun deleteById(id: Long)
}
