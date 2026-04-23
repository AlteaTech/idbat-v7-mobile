package com.idbat.mobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.idbat.mobile.data.entities.UtilisateurTPEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UtilisateurTPDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUtilisateur(utilisateur: UtilisateurTPEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUtilisateurs(utilisateurs: List<UtilisateurTPEntity>)

    @Query("SELECT * FROM utilisateurs_tp WHERE login = :login LIMIT 1")
    suspend fun getUtilisateurByLogin(login: String): UtilisateurTPEntity?

    @Query("delete FROM utilisateurs_tp")
    suspend fun purge()

    @Query("SELECT * FROM utilisateurs_tp ORDER BY lastLoginDate DESC LIMIT 1")
    fun getLastLoggedInUtilisateurFlow(): Flow<UtilisateurTPEntity?>

    @Query("SELECT * FROM utilisateurs_tp")
    fun getAllUtilisateurTPFlow(): Flow<List<UtilisateurTPEntity>>

    @Query("DELETE FROM utilisateurs_tp")
    suspend fun clearUtilisateurs()
}
