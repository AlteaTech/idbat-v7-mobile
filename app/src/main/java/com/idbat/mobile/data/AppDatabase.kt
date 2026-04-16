package com.idbat.mobile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.idbat.mobile.data.converters.Converters
import com.idbat.mobile.data.dao.*
import com.idbat.mobile.data.entities.*

@Database(
    entities = [
        UtilisateurTPEntity::class,
        ContratEntity::class,
        SiteEntity::class,
        MotifListeNoireContratEntity::class,
        CarteContratEntity::class,
        MatiereSiteEntity::class,
        LastSynchroHistoryEntity::class
    ],
    version = 12,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun utilisateurTPDao(): UtilisateurTPDao
    abstract fun contratDao(): ContratDao
    abstract fun siteDao(): SiteDao
    abstract fun motifListeNoireContratDao(): MotifListeNoireContratDao
    abstract fun carteContratDao(): CarteContratDao
    abstract fun matiereSiteDao(): MatiereSiteDao
    abstract fun lastSynchroHistoryDao(): LastSynchroHistoryDao

    companion object {
        private const val DATABASE_NAME = "idbat_bdd"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(*getAllMigrations())
                .addCallback(DatabaseCallback())
                .build()
                
                INSTANCE = instance
                instance
            }
        }

        private fun getAllMigrations(): Array<Migration> = arrayOf(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, 
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, 
            MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, 
            MIGRATION_10_11, MIGRATION_11_12
        )

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE new_users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        login TEXT NOT NULL, 
                        pin TEXT NOT NULL, 
                        token TEXT, 
                        lastLoginDate INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("DROP TABLE users")
                db.execSQL("ALTER TABLE new_users RENAME TO users")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `contrats` (
                        `id` INTEGER NOT NULL, 
                        `trigramme` TEXT NOT NULL, 
                        `nom` TEXT NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sites` (
                        `id` INTEGER NOT NULL, 
                        `trigramme` TEXT NOT NULL, 
                        `nom` TEXT NOT NULL, 
                        `adresse1` TEXT, 
                        `adresse2` TEXT, 
                        `codePostal` TEXT, 
                        `ville` TEXT, 
                        `typeImprimante` TEXT, 
                        `macImprimante` TEXT, 
                        `horairesOuverture` TEXT, 
                        `destinatairesMailTransfertTP` TEXT, 
                        `contratId` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`), 
                        FOREIGN KEY(`contratId`) REFERENCES `contrats`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sites_contratId` ON `sites` (`contratId`)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `motifs_liste_noire_contrat` (
                        `id` INTEGER NOT NULL, 
                        `libelle` TEXT NOT NULL, 
                        `contratId` INTEGER NOT NULL, 
                        `motifListeNoireId` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`), 
                        FOREIGN KEY(`contratId`) REFERENCES `contrats`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_motifs_liste_noire_contrat_contratId` ON `motifs_liste_noire_contrat` (`contratId`)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users RENAME TO utilisateurs_tp")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `carte_contrat` (
                        `id` INTEGER NOT NULL, 
                        `libelle` TEXT NOT NULL, 
                        `contratId` INTEGER NOT NULL, 
                        `carteId` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`), 
                        FOREIGN KEY(`contratId`) REFERENCES `contrats`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_carte_contrat_contratId` ON `carte_contrat` (`contratId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `matiere_site` (
                        `id` INTEGER NOT NULL, 
                        `libelle` TEXT NOT NULL, 
                        `siteId` INTEGER NOT NULL, 
                        `matiereId` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`), 
                        FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_matiere_site_siteId` ON `matiere_site` (`siteId`)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE carte_contrat ADD COLUMN type TEXT")
                db.execSQL("ALTER TABLE carte_contrat ADD COLUMN valeur TEXT")
                db.execSQL("ALTER TABLE carte_contrat ADD COLUMN uidRfid TEXT")
                db.execSQL("ALTER TABLE carte_contrat ADD COLUMN isCreationByQRCode INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE carte_contrat ADD COLUMN carteGriseJ1 TEXT")
                db.execSQL("ALTER TABLE carte_contrat ADD COLUMN carteGriseF3 INTEGER")

                updateMockData(db)
            }

            private fun updateMockData(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    UPDATE carte_contrat 
                    SET type = 'Paiement', valeur = '100', uidRfid = 'A1B2C3D4', 
                        isCreationByQRCode = 1, carteGriseJ1 = 'VP', carteGriseF3 = 1500 
                    WHERE id = 1
                """.trimIndent())
                
                db.execSQL("""
                    UPDATE carte_contrat 
                    SET type = 'Acces', valeur = 'Illimite', uidRfid = 'E5F6G7H8', 
                        isCreationByQRCode = 0, carteGriseJ1 = 'CTTE', carteGriseF3 = 3500 
                    WHERE id = 2
                """.trimIndent())
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `last_synchro_history_sites` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `siteId` INTEGER NOT NULL, 
                        `date` INTEGER NOT NULL, 
                        FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_last_synchro_history_sites_siteId` ON `last_synchro_history_sites` (`siteId`)")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE last_synchro_history_sites ADD COLUMN type TEXT NOT NULL DEFAULT 'ENVOI'")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE last_synchro_history_sites ADD COLUMN operationsTentees INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE last_synchro_history_sites ADD COLUMN operationsReussies INTEGER NOT NULL DEFAULT 0")
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                insertDefaultUser(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                ensureDefaultUserExists(db)
            }

            private fun insertDefaultUser(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    INSERT INTO utilisateurs_tp (login, pin, lastLoginDate) 
                    VALUES ('admin', '1234', 0)
                """.trimIndent())
            }

            private fun ensureDefaultUserExists(db: SupportSQLiteDatabase) {
                val cursor = db.query("SELECT COUNT(*) FROM utilisateurs_tp WHERE login = 'admin'")
                cursor.use {
                    if (it.moveToFirst() && it.getInt(0) == 0) {
                        insertDefaultUser(db)
                    }
                }
            }
        }
    }
}