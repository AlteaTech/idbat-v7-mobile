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
}
