package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.idbat.mobile.data.entities.UsagerCarteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsagerCarteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsagerCartes(usagerCartes: List<UsagerCarteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsagerCarte(usagerCarte: UsagerCarteEntity)

    @Query("SELECT * FROM usager_cartes")
    fun getAllUsagerCartesFlow(): Flow<List<UsagerCarteEntity>>

    @Query("SELECT * FROM usager_cartes WHERE usagerId = :usagerId")
    fun getCartesForUsagerFlow(usagerId: Long): Flow<List<UsagerCarteEntity>>

    @Query("SELECT * FROM usager_cartes WHERE carteId = :carteId")
    fun getUsagersForCarteFlow(carteId: Long): Flow<List<UsagerCarteEntity>>

    @Query("DELETE FROM usager_cartes")
    suspend fun clearUsagerCartes()
}
