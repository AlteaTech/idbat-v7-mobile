package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.idbat.mobile.data.entities.RechargeCarteEntity

@Dao
interface RechargeCarteDao {
    @Insert
    suspend fun insert(recharge: RechargeCarteEntity): Long

    @Query("SELECT * FROM recharge_carte ORDER BY dateRecharge DESC")
    suspend fun getAll(): List<RechargeCarteEntity>

    // RG3 : rechargements non encore envoyés au BO
    @Query("SELECT * FROM recharge_carte WHERE sentAt IS NULL ORDER BY dateRecharge ASC")
    suspend fun getUnsent(): List<RechargeCarteEntity>

    // Suivi : nombre total de rechargements en base (envoyés non purgés + non envoyés)
    @Query("SELECT COUNT(*) FROM recharge_carte")
    suspend fun count(): Long

    // Suivi : nombre de rechargements restant à transférer
    @Query("SELECT COUNT(*) FROM recharge_carte WHERE sentAt IS NULL")
    suspend fun countUnsent(): Long

    // RG3 : marque un rechargement comme envoyé avec succès
    @Query("UPDATE recharge_carte SET sentAt = :sentAt WHERE id = :id")
    suspend fun markSent(id: Long, sentAt: Long)

    // RG3 : purge les rechargements envoyés et saisis il y a plus de X jours
    @Query("DELETE FROM recharge_carte WHERE sentAt IS NOT NULL AND dateRecharge < :threshold")
    suspend fun deleteSentOlderThan(threshold: Long)

    @Query("DELETE FROM recharge_carte WHERE id = :id")
    suspend fun deleteById(id: Long)
}
