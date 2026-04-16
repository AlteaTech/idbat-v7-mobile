package com.idbat.mobile.singleton

import android.util.Log
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.entities.LastSynchroHistoryEntity
import com.idbat.mobile.data.entities.SiteEntity
import com.idbat.mobile.data.entities.TypeSynchro
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class SyncManager @Inject constructor(
    private val database: AppDatabase
) {
    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState

    data class SyncState(
        val lastSynchroDateEnvoi: Date? = null,
        val lastSynchroDateReception: Date? = null,
        val isTransferring: Boolean = false
    )

    suspend fun loadSyncDatesForSite(site: SiteEntity) {
        try {
            val dao = database.lastSynchroHistoryDao()
            val lastEnvoi = dao.getLastSynchroForSiteAndType(site.id, TypeSynchro.ENVOI)
            val lastReception = dao.getLastSynchroForSiteAndType(site.id, TypeSynchro.RECEPTION)

            _syncState.value = _syncState.value.copy(
                lastSynchroDateEnvoi = lastEnvoi?.date,
                lastSynchroDateReception = lastReception?.date
            )
        } catch (e: Exception) {
            Log.e("SYNC_MANAGER", "Erreur lors du chargement des dates", e)
        }
    }

    suspend fun executeTransfer(site: SiteEntity) {
        _syncState.value = _syncState.value.copy(isTransferring = true)

        val tente = Random.nextLong(1, 200)
        val reussie = Random.nextLong(1, tente)
        try {
            Log.d("SYNC_MANAGER", "Mon token est : ${ConfigSingleton.tokenApi}")

            val dateExec = Date()
            val dao = database.lastSynchroHistoryDao()

            // Mise à jour ou création des enregistrements de synchro
            val lastEnvoi = dao.getLastSynchroForSiteAndType(site.id, TypeSynchro.ENVOI)
            val lastReception = dao.getLastSynchroForSiteAndType(site.id, TypeSynchro.RECEPTION)

            if (lastEnvoi != null) {
                lastEnvoi.date = dateExec
                dao.updateSynchro(lastEnvoi)
            } else {
                dao.insertSynchro(
                    LastSynchroHistoryEntity(
                        siteId = site.id,
                        date = dateExec,
                        type = TypeSynchro.ENVOI,
                        operationsTentees = tente,
                        operationsReussies = reussie
                    )
                )
            }

            if (lastReception != null) {
                lastReception.date = dateExec
                dao.updateSynchro(lastReception)
            } else {
                dao.insertSynchro(
                    LastSynchroHistoryEntity(
                        siteId = site.id,
                        date = dateExec,
                        type = TypeSynchro.RECEPTION,
                        operationsTentees = 1,
                        operationsReussies = 1
                    )
                )
            }

            _syncState.value = _syncState.value.copy(
                lastSynchroDateEnvoi = dateExec,
                lastSynchroDateReception = dateExec,
                isTransferring = false
            )

            Log.d("SYNC_MANAGER", "Synchronisation terminée avec succès")

        } catch (e: Exception) {
            Log.e("SYNC_MANAGER", "Erreur lors du transfert", e)
            _syncState.value = _syncState.value.copy(isTransferring = false)
        }
    }

    fun clearSyncData() {
        _syncState.value = SyncState()
    }
}