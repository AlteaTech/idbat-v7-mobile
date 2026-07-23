package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.idbat.mobile.data.entities.SignalementEntity

@Dao
interface SignalementDao {
    @Insert
    suspend fun insert(signalement: SignalementEntity): Long

    @Query("SELECT * FROM signalement ORDER BY dateSignalement ASC")
    suspend fun getAll(): List<SignalementEntity>

    // RG3 : signalements non encore envoyés au BO
    @Query("SELECT * FROM signalement WHERE sentAt IS NULL ORDER BY dateSignalement ASC")
    suspend fun getUnsent(): List<SignalementEntity>

    @Query("SELECT COUNT(*) FROM signalement")
    suspend fun count(): Long

    // RG3 : nombre de signalements restant à envoyer (bloque la synchro descendante si > 0)
    @Query("SELECT COUNT(*) FROM signalement WHERE sentAt IS NULL")
    suspend fun countUnsent(): Long

    // RG3 : marque un signalement comme envoyé avec succès
    @Query("UPDATE signalement SET sentAt = :sentAt WHERE id = :id")
    suspend fun markSent(id: Long, sentAt: Long)

    // RG3 : purge les signalements envoyés et saisis il y a plus de X jours (cascade documents)
    @Query("DELETE FROM signalement WHERE sentAt IS NOT NULL AND dateSignalement < :threshold")
    suspend fun deleteSentOlderThan(threshold: Long)

    @Query("DELETE FROM signalement WHERE id = :id")
    suspend fun deleteById(id: Long)
}
