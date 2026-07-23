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

    // UID normalisé : la carte est stockée avec espaces ("C6 BA E7 2B") alors que le lecteur
    // fournit "C6BAE72B" → on compare sans espaces et sans distinction de casse.
    @Query("SELECT * FROM carte_contrat WHERE UPPER(REPLACE(uidRfid, ' ', '')) = :uidRfid LIMIT 1")
    suspend fun getCarteByUidRfid(uidRfid: String): CarteContratEntity?

    // Carte à puce active (type 'P') rattachée à un usager, retrouvée par l'UID lu du tag
    @Query("""
        SELECT cc.* FROM carte_contrat cc
        INNER JOIN usager_cartes uc ON uc.carteId = cc.id
        WHERE UPPER(REPLACE(cc.uidRfid, ' ', '')) = :uidRfid
          AND cc.type = 'P'
          AND (uc.dateDebut IS NULL OR uc.dateDebut <= :now)
          AND (uc.dateFin IS NULL OR uc.dateFin >= :now)
        LIMIT 1
    """)
    suspend fun getActiveCartePByUid(uidRfid: String, now: Long): CarteContratEntity?

    @Query("DELETE FROM carte_contrat")
    suspend fun clearCartes()

    @Query("DELETE FROM carte_contrat WHERE id NOT IN (:ids)")
    suspend fun deleteCartesNotIn(ids: List<Long>)
}
