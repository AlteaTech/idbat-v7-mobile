package com.idbat.mobile.data.model

import com.idbat.mobile.data.entities.SiteEntity
import com.idbat.mobile.data.entities.UtilisateurTPEntity

data class LoginFormState(
    val selectedSite: SiteEntity? = null,
    val selectedUser: UtilisateurTPEntity? = null,
    val password: String = "",
    val expandedSite: Boolean = false,
    val expandedUser: Boolean = false,
)