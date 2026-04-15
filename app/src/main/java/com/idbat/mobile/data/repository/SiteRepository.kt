package com.idbat.mobile.data.repository

import com.idbat.mobile.data.dao.SiteDao
import com.idbat.mobile.data.entities.SiteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiteRepository @Inject constructor(
    private val siteDao: SiteDao
) {
    fun getAllSites(): Flow<List<SiteEntity>> {
        return siteDao.getAllSitesFlow()
    }
}
