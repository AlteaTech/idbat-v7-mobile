package com.idbat.mobile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.idbat.mobile.data.dao.CarteContratDao
import com.idbat.mobile.data.dao.ContratDao
import com.idbat.mobile.data.dao.MatiereSiteDao
import com.idbat.mobile.data.dao.MotifListeNoireContratDao
import com.idbat.mobile.data.dao.SiteDao
import com.idbat.mobile.data.dao.UtilisateurTPDao
import com.idbat.mobile.data.entities.CarteContratEntity
import com.idbat.mobile.data.entities.ContratEntity
import com.idbat.mobile.data.entities.MatiereSiteEntity
import com.idbat.mobile.data.entities.MotifListeNoireContratEntity
import com.idbat.mobile.data.entities.SiteEntity
import com.idbat.mobile.data.entities.UtilisateurTPEntity

// On passe à la version 9 de la base de données
@Database(
    entities = [
        UtilisateurTPEntity::class, 
        ContratEntity::class, 
        SiteEntity::class, 
        MotifListeNoireContratEntity::class,
        CarteContratEntity::class,
        MatiereSiteEntity::class
    ],
    version = 9,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun utilisateurTPDao(): UtilisateurTPDao
    abstract fun contratDao(): ContratDao
    abstract fun siteDao(): SiteDao
    abstract fun motifListeNoireContratDao(): MotifListeNoireContratDao
    abstract fun carteContratDao(): CarteContratDao
    abstract fun matiereSiteDao(): MatiereSiteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ... (MIGRATION_1_2 à MIGRATION_7_8)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE new_users (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, login TEXT NOT NULL, pin TEXT NOT NULL, token TEXT, lastLoginDate INTEGER NOT NULL)")
                db.execSQL("DROP TABLE users")
                db.execSQL("ALTER TABLE new_users RENAME TO users")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `contrats` (`id` INTEGER NOT NULL, `trigramme` TEXT NOT NULL, `nom` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT OR IGNORE INTO contrats (id, trigramme, nom) VALUES (1, 'VEO', 'Veolia Default')")
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
                
                db.execSQL("INSERT OR IGNORE INTO motifs_liste_noire_contrat (id, libelle, contratId, motifListeNoireId) VALUES (1, 'Client injoignable', 1, 101)")
                db.execSQL("INSERT OR IGNORE INTO motifs_liste_noire_contrat (id, libelle, contratId, motifListeNoireId) VALUES (2, 'Refus de paiement', 1, 102)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users RENAME TO utilisateurs_tp")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Table carte_contrat
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
                
                // Mocks pour carte_contrat
                db.execSQL("INSERT OR IGNORE INTO carte_contrat (id, libelle, contratId, carteId) VALUES (1, 'Carte Accès Principal', 1, 501)")
                db.execSQL("INSERT OR IGNORE INTO carte_contrat (id, libelle, contratId, carteId) VALUES (2, 'Carte Déchetterie', 1, 502)")

                // Table matiere_site
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
                
                // Mocks pour matiere_site (Liés au site ID 1)
                db.execSQL("INSERT OR IGNORE INTO matiere_site (id, libelle, siteId, matiereId) VALUES (1, 'Gravats', 1, 201)")
                db.execSQL("INSERT OR IGNORE INTO matiere_site (id, libelle, siteId, matiereId) VALUES (2, 'Bois', 1, 202)")
            }
        }

        // Migration manuelle de la version 8 à 9 pour ajouter des colonnes à carte_contrat
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ajout des colonnes à la table carte_contrat
                db.execSQL("ALTER TABLE carte_contrat ADD COLUMN type TEXT")
                db.execSQL("ALTER TABLE carte_contrat ADD COLUMN valeur TEXT")
                db.execSQL("ALTER TABLE carte_contrat ADD COLUMN uidRfid TEXT")
                db.execSQL("ALTER TABLE carte_contrat ADD COLUMN isCreationByQRCode INTEGER NOT NULL DEFAULT 0") // SQLite stocke les booléens comme des entiers (0 = false, 1 = true)
                db.execSQL("ALTER TABLE carte_contrat ADD COLUMN carteGriseJ1 TEXT")
                db.execSQL("ALTER TABLE carte_contrat ADD COLUMN carteGriseF3 INTEGER")

                // Mise à jour des mocks existants avec les nouvelles valeurs
                db.execSQL("UPDATE carte_contrat SET type = 'Paiement', valeur = '100', uidRfid = 'A1B2C3D4', isCreationByQRCode = 1, carteGriseJ1 = 'VP', carteGriseF3 = 1500 WHERE id = 1")
                db.execSQL("UPDATE carte_contrat SET type = 'Acces', valeur = 'Illimite', uidRfid = 'E5F6G7H8', isCreationByQRCode = 0, carteGriseJ1 = 'CTTE', carteGriseF3 = 3500 WHERE id = 2")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "idbat_bdd"
                )
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, 
                    MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, 
                    MIGRATION_7_8, MIGRATION_8_9
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Note: On utilise le nouveau nom de table
                        db.execSQL("INSERT INTO utilisateurs_tp (login, pin, lastLoginDate) VALUES ('admin', '1234', 0)")
                        db.execSQL("INSERT INTO contrats (id, trigramme, nom) VALUES (1, 'VEO', 'Veolia Default')")
                        db.execSQL("INSERT INTO sites (id, trigramme, nom, contratId) VALUES (1, 'STP', 'Site Test Paris', 1)")
                        
                        db.execSQL("INSERT INTO motifs_liste_noire_contrat (id, libelle, contratId, motifListeNoireId) VALUES (1, 'Client injoignable', 1, 101)")
                        db.execSQL("INSERT INTO motifs_liste_noire_contrat (id, libelle, contratId, motifListeNoireId) VALUES (2, 'Refus de paiement', 1, 102)")
                        
                        // Mocks initiaux pour les nouvelles tables
                        db.execSQL("INSERT INTO carte_contrat (id, libelle, contratId, carteId, type, valeur, uidRfid, isCreationByQRCode, carteGriseJ1, carteGriseF3) VALUES (1, 'Carte Accès Principal', 1, 501, 'Paiement', '100', 'A1B2C3D4', 1, 'VP', 1500)")
                        db.execSQL("INSERT INTO carte_contrat (id, libelle, contratId, carteId, type, valeur, uidRfid, isCreationByQRCode, carteGriseJ1, carteGriseF3) VALUES (2, 'Carte Déchetterie', 1, 502, 'Acces', 'Illimite', 'E5F6G7H8', 0, 'CTTE', 3500)")
                        db.execSQL("INSERT INTO matiere_site (id, libelle, siteId, matiereId) VALUES (1, 'Gravats', 1, 201)")
                        db.execSQL("INSERT INTO matiere_site (id, libelle, siteId, matiereId) VALUES (2, 'Bois', 1, 202)")
                    }
                    
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // On utilise le nouveau nom de table pour la vérification
                        val cursorUser = db.query("SELECT COUNT(*) FROM utilisateurs_tp WHERE login = 'admin'")
                        cursorUser.moveToFirst()
                        if (cursorUser.getInt(0) == 0) {
                            db.execSQL("INSERT INTO utilisateurs_tp (login, pin, lastLoginDate) VALUES ('admin', '1234', 0)")
                        }
                        cursorUser.close()

                        db.execSQL("INSERT OR IGNORE INTO motifs_liste_noire_contrat (id, libelle, contratId, motifListeNoireId) VALUES (1, 'Client injoignable', 1, 101)")
                        db.execSQL("INSERT OR IGNORE INTO motifs_liste_noire_contrat (id, libelle, contratId, motifListeNoireId) VALUES (2, 'Refus de paiement', 1, 102)")
                        
                        // Sécurité pour l'ouverture
                        db.execSQL("INSERT OR IGNORE INTO carte_contrat (id, libelle, contratId, carteId, type, valeur, uidRfid, isCreationByQRCode, carteGriseJ1, carteGriseF3) VALUES (1, 'Carte Accès Principal', 1, 501, 'Paiement', '100', 'A1B2C3D4', 1, 'VP', 1500)")
                        db.execSQL("INSERT OR IGNORE INTO carte_contrat (id, libelle, contratId, carteId, type, valeur, uidRfid, isCreationByQRCode, carteGriseJ1, carteGriseF3) VALUES (2, 'Carte Déchetterie', 1, 502, 'Acces', 'Illimite', 'E5F6G7H8', 0, 'CTTE', 3500)")
                        db.execSQL("INSERT OR IGNORE INTO matiere_site (id, libelle, siteId, matiereId) VALUES (1, 'Gravats', 1, 201)")
                        db.execSQL("INSERT OR IGNORE INTO matiere_site (id, libelle, siteId, matiereId) VALUES (2, 'Bois', 1, 202)")
                    }
                })
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
