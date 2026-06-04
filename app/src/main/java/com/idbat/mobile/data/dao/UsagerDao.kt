package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.idbat.mobile.data.entities.UsagerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsagerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsagers(usagers: List<UsagerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsager(usager: UsagerEntity)

    @Query("SELECT * FROM usagers")
    fun getAllUsagersFlow(): Flow<List<UsagerEntity>>

    @Query("SELECT * FROM usagers WHERE contratId = :contratId")
    fun getUsagersByContratFlow(contratId: Long): Flow<List<UsagerEntity>>

    @Query("""
        SELECT DISTINCT u.* FROM usagers u
        INNER JOIN usager_cartes uc ON uc.usagerId = u.id
        INNER JOIN carte_contrat cc ON cc.id = uc.carteId
        WHERE u.contratId = :contratId
          AND cc.type IN ('I', 'C')
          AND (uc.dateDebut IS NULL OR uc.dateDebut <= :now)
          AND (uc.dateFin IS NULL OR uc.dateFin >= :now)
    """)
    fun getUsagersWithActiveCarteICFlow(contratId: Long, now: Long): Flow<List<UsagerEntity>>
    
    @Query("""
        SELECT u.* FROM usagers u
        INNER JOIN usager_cartes uc ON uc.usagerId = u.id
        WHERE uc.carteId = :carteId
        LIMIT 1
    """)
    suspend fun getUsagerByCarte(carteId: Long): UsagerEntity?

    @Query("UPDATE usagers SET couriel = :couriel WHERE id = :id")
    suspend fun updateCouriel(id: Long, couriel: String)

    @Query("DELETE FROM usagers")
    suspend fun purge()

    @Query("DELETE FROM usagers WHERE id NOT IN (:ids)")
    suspend fun deleteUsagersNotIn(ids: List<Long>)
}