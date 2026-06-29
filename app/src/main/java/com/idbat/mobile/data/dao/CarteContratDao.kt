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

    @Query("""
        SELECT cc.* FROM carte_contrat cc
        INNER JOIN usager_cartes uc ON uc.carteId = cc.id
        WHERE uc.usagerId = :usagerId
          AND cc.type IN ('I', 'C')
          AND (uc.dateDebut IS NULL OR uc.dateDebut <= :now)
          AND (uc.dateFin IS NULL OR uc.dateFin >= :now)
        LIMIT 1
    """)
    suspend fun getFirstActiveCarteICForUsager(usagerId: Long, now: Long): CarteContratEntity?

    @Query("""
        SELECT cc.* FROM carte_contrat cc
        INNER JOIN usager_cartes uc ON uc.carteId = cc.id
        WHERE cc.type = 'I'
          AND cc.valeur = :immatriculation
          AND (uc.dateDebut IS NULL OR uc.dateDebut <= :now)
          AND (uc.dateFin IS NULL OR uc.dateFin >= :now)
        LIMIT 1
    """)
    suspend fun getActiveCarteIByImmatriculation(immatriculation: String, now: Long): CarteContratEntity?

    @Query("""
        SELECT cc.* FROM carte_contrat cc
        INNER JOIN usager_cartes uc ON uc.carteId = cc.id
        WHERE cc.type = 'C'
          AND cc.valeur = :codebarres
          AND (uc.dateDebut IS NULL OR uc.dateDebut <= :now)
          AND (uc.dateFin IS NULL OR uc.dateFin >= :now)
        LIMIT 1
    """)
    suspend fun getActiveCarteCByCodebarres(codebarres: String, now: Long): CarteContratEntity?

    @Query("SELECT * FROM carte_contrat WHERE contratId = :contratId")
    suspend fun getCartesByContrat(contratId: Long): List<CarteContratEntity>

    @Query("SELECT * FROM carte_contrat WHERE id = :id LIMIT 1")
    suspend fun getCarteById(id: Long): CarteContratEntity?

    @Query("DELETE FROM carte_contrat")
    suspend fun clearCartes()

    @Query("DELETE FROM carte_contrat WHERE id NOT IN (:ids)")
    suspend fun deleteCartesNotIn(ids: List<Long>)
}
