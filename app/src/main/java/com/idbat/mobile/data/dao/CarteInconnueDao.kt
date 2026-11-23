package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.idbat.mobile.data.entities.CarteInconnueEntity

@Dao
interface CarteInconnueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cartes: List<CarteInconnueEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(carte: CarteInconnueEntity)

    @Query("SELECT * FROM carte_inconnue WHERE UPPER(valeur) = UPPER(:immatriculation) LIMIT 1")
    suspend fun getByImmatriculation(immatriculation: String): CarteInconnueEntity?

    // RG7 : un passage de plus pour cette immatriculation (compteur local en attendant la synchro)
    @Query("UPDATE carte_inconnue SET nbPassages = nbPassages + 1 WHERE UPPER(valeur) = UPPER(:immatriculation)")
    suspend fun incrementerPassages(immatriculation: String): Int

    @Query("DELETE FROM carte_inconnue")
    suspend fun clear()
}
