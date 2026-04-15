package com.idbat.mobile.data.repository

import com.idbat.mobile.data.dao.UtilisateurTPDao
import com.idbat.mobile.data.entities.UtilisateurTPEntity
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val utilisateurDao: UtilisateurTPDao
) {
    suspend fun getUserByLogin(username: String): UtilisateurTPEntity? {
        return utilisateurDao.getUtilisateurByLogin(username)
    }

    suspend fun updateLastLogin(user: UtilisateurTPEntity) {
        utilisateurDao.insertUtilisateur(user.copy(lastLoginDate = System.currentTimeMillis()))
    }
}
