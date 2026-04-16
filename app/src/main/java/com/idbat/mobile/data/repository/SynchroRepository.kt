package com.idbat.mobile.data.repository

import com.idbat.mobile.data.dao.LastSynchroHistoryDao
import com.idbat.mobile.data.entities.LastSynchroHistoryEntity
import com.idbat.mobile.data.entities.TypeSynchro
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SynchroRepository @Inject constructor(
    private val synchroDao: LastSynchroHistoryDao
) {
    suspend fun getLastSynchroForSite(siteId: Long, type: TypeSynchro): LastSynchroHistoryEntity? {
        return synchroDao.getLastSynchroForSiteAndType(siteId, type)
    }

    suspend fun recordSynchro(siteId: Long, type: TypeSynchro, operationsTentees : Long, operationsReussies : Long) {
        val history = LastSynchroHistoryEntity(
            siteId = siteId,
            date = Date(),
            type = type,
            operationsTentees = operationsTentees,
            operationsReussies = operationsReussies
        )
        synchroDao.insertSynchroHistory(history)
    }
}
