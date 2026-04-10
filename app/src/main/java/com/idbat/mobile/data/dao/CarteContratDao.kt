package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.idbat.mobile.data.entities.CarteContratEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CarteContratDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartes(cartes: List<CarteContratEntity>)

    @Query("SELECT * FROM carte_contrat")
    fun getAllCartesFlow(): Flow<List<CarteContratEntity>>

    @Query("SELECT * FROM carte_contrat WHERE contratId = :contratId")
    fun getCartesByContratFlow(contratId: Long): Flow<List<CarteContratEntity>>
    
    @Query("DELETE FROM carte_contrat")
    suspend fun clearCartes()
}
