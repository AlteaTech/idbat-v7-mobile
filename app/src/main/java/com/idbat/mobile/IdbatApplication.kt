package com.idbat.mobile

import android.app.Application
import com.idbat.mobile.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IdbatApplication : Application() {
    
    // Instance de la base de données
    val database by lazy { AppDatabase.getDatabase(this) }
    
    override fun onCreate() {
        super.onCreate()
        
        // Forcer la création de la base de données et l'exécution des migrations
        // dès le démarrage de l'application. On le fait dans une coroutine (thread IO)
        // pour ne pas bloquer l'interface utilisateur pendant le lancement.
        CoroutineScope(Dispatchers.IO).launch {
            // L'appel à writableDatabase force SQLite à créer physiquement le fichier
            // et Room à vérifier et appliquer le schéma (ou les migrations).
            database.openHelper.writableDatabase
        }
    }
}
