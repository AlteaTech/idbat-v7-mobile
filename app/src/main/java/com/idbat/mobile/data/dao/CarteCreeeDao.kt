package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.idbat.mobile.data.entities.CarteCreeeEntity

@Dao
interface CarteCreeeDao {
    @Insert
    suspend fun insert(carte: CarteCreeeEntity): Long

    @Query("SELECT * FROM carte_creee ORDER BY dateCreation DESC")
    suspend fun getAll(): List<CarteCreeeEntity>

    // RG3 : cartes créées non encore envoyées au BO
    @Query("SELECT * FROM carte_creee WHERE sentAt IS NULL ORDER BY dateCreation ASC")
    suspend fun getUnsent(): List<CarteCreeeEntity>

    // RG3 : marque une carte créée comme envoyée avec succès
    @Query("UPDATE carte_creee SET sentAt = :sentAt WHERE id = :id")
    suspend fun markSent(id: Long, sentAt: Long)

    // RG3 : purge les cartes créées envoyées et saisies il y a plus de X jours
    @Query("DELETE FROM carte_creee WHERE sentAt IS NOT NULL AND dateCreation < :threshold")
    suspend fun deleteSentOlderThan(threshold: Long)

    @Query("DELETE FROM carte_creee WHERE id = :id")
    suspend fun deleteById(id: Long)
}
