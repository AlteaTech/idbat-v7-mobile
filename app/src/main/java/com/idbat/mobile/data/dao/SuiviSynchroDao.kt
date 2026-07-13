package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.idbat.mobile.data.entities.SuiviSynchroEntity
import java.util.Date

@Dao
interface SuiviSynchroDao {

    /** Crée la ligne de suivi au démarrage de la synchro. Retourne l'id local généré. */
    @Insert
    suspend fun insert(suivi: SuiviSynchroEntity): Long

    /** Clôture la synchro : renseigne le nombre envoyé et la date de fin. */
    @Query("UPDATE suivi_synchro SET nbEnvoye = :nbEnvoye, dateFin = :dateFin WHERE idTransaction = :idTransaction")
    suspend fun marquerFin(idTransaction: String, nbEnvoye: Int, dateFin: Date)

    @Query("SELECT * FROM suivi_synchro ORDER BY dateDebut DESC")
    suspend fun getAll(): List<SuiviSynchroEntity>

    @Query("SELECT * FROM suivi_synchro WHERE siteId = :siteId ORDER BY dateDebut DESC")
    suspend fun getForSite(siteId: Long): List<SuiviSynchroEntity>

    @Query("SELECT * FROM suivi_synchro WHERE idTransaction = :idTransaction LIMIT 1")
    suspend fun getByTransaction(idTransaction: String): SuiviSynchroEntity?

    // Envoi 1 : lignes pas encore transmises (sentAt1 null) + marquage
    @Query("SELECT * FROM suivi_synchro WHERE sentAt1 IS NULL ORDER BY dateDebut ASC")
    suspend fun getUnsent1(): List<SuiviSynchroEntity>

    @Query("UPDATE suivi_synchro SET sentAt1 = :sentAt1 WHERE id = :id")
    suspend fun markSent1(id: Long, sentAt1: Date)

    // Envoi 2 : lignes pas encore transmises (sentAt2 null) + marquage
    @Query("SELECT * FROM suivi_synchro WHERE sentAt2 IS NULL ORDER BY dateDebut ASC")
    suspend fun getUnsent2(): List<SuiviSynchroEntity>

    @Query("UPDATE suivi_synchro SET sentAt2 = :sentAt2 WHERE id = :id")
    suspend fun markSent2(id: Long, sentAt2: Date)

    // Purge : lignes de suivi dont le 2ᵉ envoi (sentAt2) est fait et date de plus de X (millis).
    @Query("DELETE FROM suivi_synchro WHERE sentAt2 IS NOT NULL AND sentAt2 < :threshold")
    suspend fun deleteSentOlderThan(threshold: Long)
}
