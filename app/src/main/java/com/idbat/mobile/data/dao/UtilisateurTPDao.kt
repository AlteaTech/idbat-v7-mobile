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

    @Query("SELECT * FROM utilisateurs_tp WHERE login = :login LIMIT 1")
    suspend fun getUtilisateurByLogin(login: String): UtilisateurTPEntity?

    @Query("SELECT * FROM utilisateurs_tp ORDER BY lastLoginDate DESC LIMIT 1")
    fun getLastLoggedInUtilisateurFlow(): Flow<UtilisateurTPEntity?>
    
    @Query("DELETE FROM utilisateurs_tp")
    suspend fun clearUtilisateurs()
}
