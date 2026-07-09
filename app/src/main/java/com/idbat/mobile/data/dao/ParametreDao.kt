package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.idbat.mobile.data.entities.ParametreEntity

@Dao
interface ParametreDao {

    @Insert
    suspend fun insertAll(parametres: List<ParametreEntity>)

    @Query("DELETE FROM parametre")
    suspend fun deleteAll()

    /**
     * Remplacement complet de la table (synchro descendante). Table de référence globale sans
     * FK ni enfants : le DELETE ne déclenche aucune cascade. Le vidage préalable évite un conflit
     * sur l'index unique `clef` si le back réattribue un `id` à une clef existante.
     */
    @Transaction
    suspend fun replaceAll(parametres: List<ParametreEntity>) {
        deleteAll()
        if (parametres.isNotEmpty()) insertAll(parametres)
    }

    @Query("SELECT * FROM parametre ORDER BY clef ASC")
    suspend fun getAll(): List<ParametreEntity>

    @Query("SELECT * FROM parametre WHERE clef = :clef LIMIT 1")
    suspend fun getByClef(clef: String): ParametreEntity?

    @Query("SELECT valeur FROM parametre WHERE clef = :clef LIMIT 1")
    suspend fun getValeur(clef: String): String?

    @Query("SELECT COUNT(*) FROM parametre")
    suspend fun count(): Long
}
