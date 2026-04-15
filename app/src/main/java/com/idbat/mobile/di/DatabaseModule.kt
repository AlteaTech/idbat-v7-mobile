package com.idbat.mobile.di

import android.content.Context
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.dao.CarteContratDao
import com.idbat.mobile.data.dao.ContratDao
import com.idbat.mobile.data.dao.LastSynchroHistoryDao
import com.idbat.mobile.data.dao.MatiereSiteDao
import com.idbat.mobile.data.dao.MotifListeNoireContratDao
import com.idbat.mobile.data.dao.SiteDao
import com.idbat.mobile.data.dao.UtilisateurTPDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideUtilisateurTPDao(database: AppDatabase): UtilisateurTPDao {
        return database.utilisateurTPDao()
    }

    @Provides
    fun provideContratDao(database: AppDatabase): ContratDao {
        return database.contratDao()
    }

    @Provides
    fun provideSiteDao(database: AppDatabase): SiteDao {
        return database.siteDao()
    }

    @Provides
    fun provideMotifListeNoireContratDao(database: AppDatabase): MotifListeNoireContratDao {
        return database.motifListeNoireContratDao()
    }

    @Provides
    fun provideCarteContratDao(database: AppDatabase): CarteContratDao {
        return database.carteContratDao()
    }

    @Provides
    fun provideMatiereSiteDao(database: AppDatabase): MatiereSiteDao {
        return database.matiereSiteDao()
    }

    @Provides
    fun provideLastSynchroHistoryDao(database: AppDatabase): LastSynchroHistoryDao {
        return database.lastSynchroHistoryDao()
    }
}
