package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.idbat.mobile.data.entities.SiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSites(sites: List<SiteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: SiteEntity)

    @Query("SELECT * FROM sites")
    fun getAllSitesFlow(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE id = :id LIMIT 1")
    suspend fun getSiteById(id: Long): SiteEntity?

    @Query("delete FROM sites")
    suspend fun purge()

    @Query("SELECT * FROM sites WHERE contratId = :contratId")
    fun getSitesByContratFlow(contratId: Long): Flow<List<SiteEntity>>

    @Query("SELECT count(1) FROM sites")
    suspend fun count(): Long

    @Query("DELETE FROM sites WHERE id NOT IN (:ids)")
    suspend fun deleteSitesNotIn(ids: List<Long>)
}
