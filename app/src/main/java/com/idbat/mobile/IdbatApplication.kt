package com.idbat.mobile

import android.app.Application
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.utils.FileLogger
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class IdbatApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()

        // Journalisation fichier : purge les jours précédents + capture le logcat du process
        FileLogger.init(this)

        CoroutineScope(Dispatchers.IO).launch {
            database.openHelper.writableDatabase
        }
    }
}
