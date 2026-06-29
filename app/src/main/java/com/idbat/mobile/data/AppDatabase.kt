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
        LastSynchroHistoryEntity::class,
        UsagerEntity::class,
        ContratEvenementEntity::class,
        UsagerCarteEntity::class,
        PassageEntity::class,
        PassageMatiereEntity::class,
        PassageDocumentEntity::class,
        SignalementEntity::class,
        SignalementDocumentEntity::class,
        SeuilEtatEntity::class,
        CarteCreeeEntity::class,
        RechargeCarteEntity::class
    ],
    version = 35,
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
    abstract fun usagerDao(): UsagerDao
    abstract fun contratEvenementDao(): ContratEvenementDao
    abstract fun usagerCarteDao(): UsagerCarteDao
    abstract fun passageDao(): PassageDao
    abstract fun passageMatiereDao(): PassageMatiereDao
    abstract fun passageDocumentDao(): PassageDocumentDao
    abstract fun signalementDao(): SignalementDao
    abstract fun signalementDocumentDao(): SignalementDocumentDao
    abstract fun seuilEtatDao(): SeuilEtatDao
    abstract fun carteCreeeDao(): CarteCreeeDao
    abstract fun rechargeCarteDao(): RechargeCarteDao

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
            MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
            MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
            MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22,
            MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27,
            MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32,
            MIGRATION_32_33, MIGRATION_33_34, MIGRATION_34_35
        )

        private val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recharge_carte` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `contratId` INTEGER NOT NULL,
                        `siteId` INTEGER NOT NULL,
                        `uid` TEXT NOT NULL,
                        `numeroIdentification` TEXT NOT NULL,
                        `identClient` TEXT NOT NULL,
                        `ancienSolde` INTEGER NOT NULL,
                        `pointsRecharges` INTEGER NOT NULL,
                        `nouveauSolde` INTEGER NOT NULL,
                        `dateRecharge` INTEGER NOT NULL,
                        `transactionId` TEXT NOT NULL DEFAULT '',
                        `sentAt` INTEGER
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE contrats ADD COLUMN hasPrepaiementParticuliers INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE contrats ADD COLUMN hasPrepaiementProfessionnels INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE matieres_site ADD COLUMN isEnable INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE contrat_evenements ADD COLUMN isEnable INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
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
                db.execSQL("DROP TABLE users")
                db.execSQL("ALTER TABLE new_users RENAME TO users")
            }
        }

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
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
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
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sites_contratId` ON `sites` (`contratId`)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `motifs_liste_noire_contrat` (
                        `id` INTEGER NOT NULL, 
                        `libelle` TEXT NOT NULL, 
                        `contratId` INTEGER NOT NULL, 
                        `motifListeNoireId` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`), 
                        FOREIGN KEY(`contratId`) REFERENCES `contrats`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent()
                )
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
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `carte_contrat` (
                        `id` INTEGER NOT NULL, 
                        `libelle` TEXT NOT NULL, 
                        `contratId` INTEGER NOT NULL, 
                        `carteId` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`), 
                        FOREIGN KEY(`contratId`) REFERENCES `contrats`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_carte_contrat_contratId` ON `carte_contrat` (`contratId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `matiere_site` (
                        `id` INTEGER NOT NULL, 
                        `libelle` TEXT NOT NULL, 
                        `siteId` INTEGER NOT NULL, 
                        `matiereId` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`), 
                        FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent()
                )
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
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `last_synchro_history_sites` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `siteId` INTEGER NOT NULL, 
                        `date` INTEGER NOT NULL, 
                        FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent()
                )
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

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `usagers` (
                        `id` INTEGER NOT NULL, 
                        `nom` TEXT NOT NULL, 
                        `prenom` TEXT NOT NULL, 
                        `contratId` INTEGER NOT NULL, 
                        `refClientIdBat` INTEGER, 
                        PRIMARY KEY(`id`), 
                        FOREIGN KEY(`contratId`) REFERENCES `contrats`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_usagers_contratId` ON `usagers` (`contratId`)")
            }
        }
        
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `matiere_site`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `matieres_site` (
                        `matiereId` INTEGER NOT NULL, 
                        `siteId` INTEGER NOT NULL, 
                        `libelle` TEXT NOT NULL, 
                        `unitesDesApportId` INTEGER NOT NULL, 
                        `unitesDesApportLibelle` TEXT NOT NULL, 
                        PRIMARY KEY(`matiereId`, `siteId`), 
                        FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_matieres_site_siteId` ON `matieres_site` (`siteId`)")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `contrat_evenements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `evenementId` INTEGER NOT NULL, 
                        `libelle` TEXT NOT NULL, 
                        `jointureId` INTEGER NOT NULL, 
                        `contratId` INTEGER NOT NULL, 
                        FOREIGN KEY(`contratId`) REFERENCES `contrats`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_contrat_evenements_contratId` ON `contrat_evenements` (`contratId`)")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE contrats ADD COLUMN hasPuce INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE contrats ADD COLUMN hasCodebarres INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE contrats ADD COLUMN hasImmatriculation INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE contrats ADD COLUMN hasSelectionusager INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `usager_cartes` (
                        `usagerId` INTEGER NOT NULL, 
                        `carteId` INTEGER NOT NULL, 
                        `dateDebut` INTEGER, 
                        `dateFin` INTEGER, 
                        PRIMARY KEY(`usagerId`, `carteId`), 
                        FOREIGN KEY(`usagerId`) REFERENCES `usagers`(`id`) ON DELETE CASCADE, 
                        FOREIGN KEY(`carteId`) REFERENCES `carte_contrat`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_usager_cartes_usagerId` ON `usager_cartes` (`usagerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_usager_cartes_carteId` ON `usager_cartes` (`carteId`)")
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE passage ADD COLUMN transactionId TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE carte_creee ADD COLUMN siteId INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE carte_creee ADD COLUMN userTpId INTEGER NOT NULL DEFAULT 0")
            }
        }

        // RG3 : colonne d'horodatage d'envoi (null = non envoyé) pour différer la purge
        private val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE passage ADD COLUMN sentAt INTEGER")
                db.execSQL("ALTER TABLE signalement ADD COLUMN sentAt INTEGER")
                db.execSQL("ALTER TABLE carte_creee ADD COLUMN sentAt INTEGER")
            }
        }

        private val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE contrats ADD COLUMN hasAccessSimpleParticuliers INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE contrats ADD COLUMN hasAccessSimpleProfessionnels INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `carte_creee` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `contratId` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `succes` INTEGER NOT NULL DEFAULT 1,
                        `uid` TEXT NOT NULL,
                        `numeroSerie` TEXT NOT NULL,
                        `numeroIdentification` TEXT NOT NULL,
                        `motDePasse` TEXT NOT NULL,
                        `societe` TEXT NOT NULL,
                        `interne` INTEGER NOT NULL DEFAULT 0,
                        `prepaiement` INTEGER NOT NULL DEFAULT 0,
                        `facturation` INTEGER NOT NULL DEFAULT 0,
                        `gratuit` INTEGER NOT NULL DEFAULT 0,
                        `nomPrenom` TEXT NOT NULL,
                        `identClient` TEXT NOT NULL,
                        `soldePoints` TEXT NOT NULL,
                        `cumulPoints` TEXT NOT NULL,
                        `dateCreation` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE carte_contrat ADD COLUMN isEnListeNoire INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE carte_contrat ADD COLUMN dtEntreeListeNoire INTEGER")
                db.execSQL("ALTER TABLE carte_contrat ADD COLUMN dtSortieListeNoire INTEGER")
                db.execSQL("ALTER TABLE carte_contrat ADD COLUMN motifListeNoireContratId INTEGER")
                db.execSQL("ALTER TABLE carte_contrat ADD COLUMN motifListeNoireLibelle TEXT")
            }
        }

        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE seuil_etat ADD COLUMN seuilDetailNom TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE seuil_etat ADD COLUMN seuilDetailType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE seuil_etat ADD COLUMN seuilDetailPeriode TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE seuil_etat ADD COLUMN seuilDetailNbPassage INTEGER")
                db.execSQL("ALTER TABLE seuil_etat ADD COLUMN seuilDetailSeuilPrevention INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE seuil_etat ADD COLUMN seuilDetailContratId INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `seuil_etat` (
                        `usagerId` INTEGER NOT NULL,
                        `seuilId` INTEGER NOT NULL,
                        `nom` TEXT NOT NULL,
                        `nbPassagesAutorises` INTEGER NOT NULL,
                        `nbPassagesEffectues` INTEGER NOT NULL,
                        `isAlerte` INTEGER NOT NULL,
                        PRIMARY KEY(`usagerId`, `seuilId`),
                        FOREIGN KEY(`usagerId`) REFERENCES `usagers`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_seuil_etat_usagerId` ON `seuil_etat` (`usagerId`)")
            }
        }

        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `signalement` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `siteId` INTEGER NOT NULL,
                        `contratId` INTEGER NOT NULL,
                        `evenementContratId` INTEGER NOT NULL,
                        `agentId` INTEGER NOT NULL,
                        `dateSignalement` INTEGER NOT NULL,
                        `commentaire` TEXT,
                        `transactionId` TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `signalement_document` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `signalementId` INTEGER NOT NULL,
                        `base64` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `nomFichier` TEXT NOT NULL,
                        FOREIGN KEY(`signalementId`) REFERENCES `signalement`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_signalement_document_signalementId` ON `signalement_document` (`signalementId`)")
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE passage ADD COLUMN emailUsager TEXT")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `passage_document` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `passageId` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `base64` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `nomFichier` TEXT NOT NULL,
                        FOREIGN KEY(`passageId`) REFERENCES `passage`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_passage_document_passageId` ON `passage_document` (`passageId`)")
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE passage ADD COLUMN commentaire TEXT")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `passage_matiere` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `passageId` INTEGER NOT NULL,
                        `matiereId` INTEGER NOT NULL,
                        `siteId` INTEGER NOT NULL,
                        `libelle` TEXT NOT NULL,
                        `quantite` TEXT NOT NULL,
                        `tarif` REAL NOT NULL,
                        `unitesLibelle` TEXT NOT NULL,
                        FOREIGN KEY(`passageId`) REFERENCES `passage`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_passage_matiere_passageId` ON `passage_matiere` (`passageId`)")
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE matieres_site ADD COLUMN tarif REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE usagers ADD COLUMN typeApporteurIsPro INTEGER")
                db.execSQL("ALTER TABLE contrats ADD COLUMN hasSignatureParticuliers INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE contrats ADD COLUMN hasSignatureProfessionnels INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `passage` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dateHeure` INTEGER NOT NULL,
                        `contratId` INTEGER NOT NULL,
                        `siteId` INTEGER NOT NULL,
                        `carteId` INTEGER,
                        `userTpId` INTEGER NOT NULL,
                        `numeroBonPassage` TEXT NOT NULL,
                        FOREIGN KEY(`contratId`) REFERENCES `contrats`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`siteId`) REFERENCES `sites`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`userTpId`) REFERENCES `utilisateurs_tp`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_passage_contratId` ON `passage` (`contratId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_passage_siteId` ON `passage` (`siteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_passage_userTpId` ON `passage` (`userTpId`)")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE usagers ADD COLUMN raisonSociale TEXT")
                db.execSQL("ALTER TABLE usagers ADD COLUMN typeApporteurLibelle TEXT")
                db.execSQL("ALTER TABLE usagers ADD COLUMN couriel TEXT")
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
                db.execSQL(
                    """
                    INSERT INTO utilisateurs_tp (login, pin, lastLoginDate) 
                    VALUES ('admin', '1234', 0)
                """.trimIndent()
                )
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
