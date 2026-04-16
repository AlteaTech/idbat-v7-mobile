package com.idbat.mobile.data.repository

import com.idbat.mobile.data.dao.UtilisateurTPDao
import com.idbat.mobile.data.entities.UtilisateurTPEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UtilisateurRepository @Inject constructor(
    private val utilisateurDao: UtilisateurTPDao
) {
    suspend fun checkCredentials(login: String, pin: String): UtilisateurTPEntity? {
        val user = utilisateurDao.getUtilisateurByLogin(login)
        return if (user != null && user.pin == pin) {
            user
        } else {
            null
        }
    }

    suspend fun updateLastLogin(user: UtilisateurTPEntity) {
        utilisateurDao.insertUtilisateur(user.copy(lastLoginDate = System.currentTimeMillis()))
    }
}
