package com.idbat.mobile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.idbat.mobile.data.dao.ContratDao
import com.idbat.mobile.data.dao.UserDao
import com.idbat.mobile.data.entities.ContratEntity
import com.idbat.mobile.data.entities.UserEntity

// On passe à la version 4 de la base de données
@Database(
    entities = [UserEntity::class, ContratEntity::class],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun contratDao(): ContratDao // Ajout du DAO pour la table contrats

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration manuelle de la version 1 à 2 car on modifie complètement la table (clés primaires)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Créer la nouvelle table avec le nouveau schéma
                db.execSQL(
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
                // Supprimer l'ancienne table
                db.execSQL("DROP TABLE users")
                
                // Renommer la nouvelle table
                db.execSQL("ALTER TABLE new_users RENAME TO users")
            }
        }

        // Migration manuelle de la version 2 à 3 pour ajouter la table contrats
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `contrats` (
                        `id` INTEGER NOT NULL, 
                        `trigramme` TEXT NOT NULL, 
                        `nom` TEXT NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT OR IGNORE INTO contrats (id, trigramme, nom) VALUES (1, 'VEO', 'Veolia Default')")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "idbat_bdd"
                )
                // Application des migrations dans l'ordre
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .addCallback(object : Callback() {
                    // Cette méthode est appelée lors de la toute première création de la BDD
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT INTO users (login, pin, lastLoginDate) VALUES ('admin', '1234', 0)")
                        db.execSQL("INSERT INTO contrats (id, trigramme, nom) VALUES (1, 'VEO', 'Veolia Default')")
                    }

                })
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
