package com.idbat.mobile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.idbat.mobile.data.dao.UserDao
import com.idbat.mobile.data.entities.UserEntity

// On passe à la version 2 de la base de données
@Database(
    entities = [UserEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration manuelle de la version 1 à 2 car on modifie complètement la table (clés primaires)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Créer la nouvelle table avec le nouveau schéma
                database.execSQL(
                    """
                    CREATE TABLE new_users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        login TEXT NOT NULL,
                        pin TEXT NOT NULL,
                        token TEXT,
                        lastLoginDate INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                // (Optionnel) Copier les données de l'ancienne table si nécessaire
                // Ici, comme le schéma change radicalement (username -> login, ajout du PIN),
                // on pourrait ignorer l'ancienne table ou la mapper. Pour l'instant on repart à zéro.
                
                // Supprimer l'ancienne table
                database.execSQL("DROP TABLE users")
                
                // Renommer la nouvelle table
                database.execSQL("ALTER TABLE new_users RENAME TO users")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "idbat_database"
                )
                .addMigrations(MIGRATION_1_2) // Application de la migration
                .addCallback(object : RoomDatabase.Callback() {
                    // Cette méthode est appelée lors de la toute première création de la BDD
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Initialisation avec le compte admin par défaut
                        db.execSQL("INSERT INTO users (login, pin, lastLoginDate) VALUES ('admin', '1234', 0)")
                    }
                    
                    // Si on veut insérer l'admin même après une migration (s'il n'existe pas déjà)
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // On vérifie si l'admin existe, sinon on le crée
                        val cursor = db.query("SELECT COUNT(*) FROM users WHERE login = 'admin'")
                        cursor.moveToFirst()
                        if (cursor.getInt(0) == 0) {
                            db.execSQL("INSERT INTO users (login, pin, lastLoginDate) VALUES ('admin', '1234', 0)")
                        }
                        cursor.close()
                    }
                })
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
