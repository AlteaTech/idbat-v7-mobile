package com.idbat.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.data.entities.SiteEntity
import com.idbat.mobile.data.entities.UtilisateurTPEntity
import com.idbat.mobile.singleton.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginFormState(
    val selectedSite: SiteEntity? = null,
    val selectedUser: UtilisateurTPEntity? = null,
    val password: String = "",
    val expandedSite: Boolean = false,
    val expandedUser: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    val authState = authManager.authState

    private val _formState = MutableStateFlow(LoginFormState())
    val formState: StateFlow<LoginFormState> = _formState.asStateFlow()

    fun selectSite(site: SiteEntity) =
        _formState.update { it.copy(selectedSite = site, expandedSite = false) }

    fun selectUser(user: UtilisateurTPEntity) =
        _formState.update { it.copy(selectedUser = user, expandedUser = false) }

    fun setPassword(password: String) = _formState.update { it.copy(password = password) }
    fun toggleSiteExpanded() = _formState.update { it.copy(expandedSite = !it.expandedSite) }
    fun toggleUserExpanded() = _formState.update { it.copy(expandedUser = !it.expandedUser) }
    fun dismissSite() = _formState.update { it.copy(expandedSite = false) }
    fun dismissUser() = _formState.update { it.copy(expandedUser = false) }

    fun submit() {
        val form = _formState.value
        val user = form.selectedUser ?: return
        viewModelScope.launch {
            authManager.login(user.login, form.password, form.selectedSite?.id)
        }
    }
}
