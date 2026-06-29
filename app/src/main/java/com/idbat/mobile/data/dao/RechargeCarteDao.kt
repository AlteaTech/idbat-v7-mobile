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

    @Query("DELETE FROM recharge_carte WHERE id = :id")
    suspend fun deleteById(id: Long)
}
