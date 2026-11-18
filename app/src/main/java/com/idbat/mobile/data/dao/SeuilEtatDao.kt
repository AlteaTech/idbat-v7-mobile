package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.idbat.mobile.data.entities.SeuilEtatEntity
import com.idbat.mobile.data.model.SeuilAtteint
import kotlinx.coroutines.flow.Flow

@Dao
interface SeuilEtatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeuils(seuils: List<SeuilEtatEntity>)

    @Query("SELECT * FROM seuil_etat WHERE usagerId = :usagerId")
    suspend fun getSeuilsByUsager(usagerId: Long): List<SeuilEtatEntity>

    @Query("SELECT * FROM seuil_etat WHERE usagerId = :usagerId")
    fun getSeuilsByUsagerFlow(usagerId: Long): Flow<List<SeuilEtatEntity>>

    @Query("DELETE FROM seuil_etat")
    suspend fun clearSeuils()

    /**
     * #513 : compteurs locaux entre deux synchros — ajoute les unités du passage aux seuils « passage »
     * de l'usager et recalcule isAlerte / isPreventionAtteinte (cf. SeuilAtteint.apresPassage).
     * La synchro descendante écrase ces valeurs (back = source de vérité).
     */
    @Transaction
    suspend fun appliquerPassage(usagerId: Long, nbUnitesDecomptees: Int) {
        val maj = getSeuilsByUsager(usagerId)
            .filter { SeuilAtteint.comptePassages(it) }
            .map { SeuilAtteint.apresPassage(it, nbUnitesDecomptees) }
        if (maj.isNotEmpty()) insertSeuils(maj)
    }
}
