package com.idbat.mobile.data.repository

import com.idbat.mobile.data.dao.LastSynchroHistoryDao
import com.idbat.mobile.data.entities.LastSynchroHistoryEntity
import com.idbat.mobile.data.entities.TypeSynchro
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SynchroRepository @Inject constructor(
    private val synchroDao: LastSynchroHistoryDao
) {
    suspend fun getLastSynchroForSite(siteId: Long, type: TypeSynchro): LastSynchroHistoryEntity? {
        return synchroDao.getLastSynchroForSiteAndType(siteId, type)
    }

    suspend fun recordSynchro(siteId: Long, type: TypeSynchro) {
        val history = LastSynchroHistoryEntity(
            siteId = siteId,
            date = Date(),
            type = type
        )
        synchroDao.insertSynchroHistory(history)
    }
}
