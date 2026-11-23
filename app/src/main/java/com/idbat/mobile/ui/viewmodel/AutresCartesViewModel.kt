package com.idbat.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.entities.UsagerEntity
import com.idbat.mobile.data.model.InfoCartePassage
import com.idbat.mobile.data.model.PassageInconnuInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutresCartesViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val _contratId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _passageInfo = MutableStateFlow<InfoCartePassage?>(null)
    private val _carteInconnue = MutableStateFlow(false)
    private val _passageInconnu = MutableStateFlow<PassageInconnuInfo?>(null)

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val passageInfo: StateFlow<InfoCartePassage?> = _passageInfo.asStateFlow()
    val carteInconnue: StateFlow<Boolean> = _carteInconnue.asStateFlow()
    // #533 : immatriculation inconnue acceptée en passage exceptionnel (null = pas ce cas)
    val passageInconnu: StateFlow<PassageInconnuInfo?> = _passageInconnu.asStateFlow()

    val usagers: StateFlow<List<UsagerEntity>> = _contratId
        .filterNotNull()
        .flatMapLatest { contratId ->
            val now = System.currentTimeMillis()
            database.usagerDao().getUsagersWithActiveCarteICFlow(contratId, now)
        }
        .combine(_searchQuery) { list, query ->
            val filtered = if (query.isBlank()) list
            else list.filter { u ->
                u.nom.contains(query, ignoreCase = true) ||
                u.prenom.contains(query, ignoreCase = true)
            }
            filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, UsagerEntity::nom)
                .thenBy(String.CASE_INSENSITIVE_ORDER, UsagerEntity::prenom))
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setContratId(id: Long) {
        _contratId.value = id
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun clearPassageInfo() {
        _passageInfo.value = null
    }

    fun clearCarteInconnue() {
        _carteInconnue.value = false
    }

    fun clearPassageInconnu() {
        _passageInconnu.value = null
    }

    fun valider(usager: UsagerEntity?, codebarres: String, immatriculation: String) {
        viewModelScope.launch {
            val info = when {
                usager != null -> buildInfoFromUsager(usager)
                codebarres.isNotBlank() -> buildInfoFromCodebarres(codebarres)
                immatriculation.isNotBlank() -> buildInfoFromImmatriculation(immatriculation)
                else -> null
            }
            if (info != null) _passageInfo.value = info
        }
    }

    private suspend fun buildInfoFromCodebarres(codebarres: String): InfoCartePassage? {
        val now = System.currentTimeMillis()
        val carte = database.carteContratDao().getActiveCarteCByCodebarres(codebarres, now)
        if (carte == null) {
            _carteInconnue.value = true
            return null
        }
        val usager = database.usagerDao().getUsagerByCarte(carte.id)
        return InfoCartePassage(
            societe = usager?.raisonSociale?.takeIf { it.isNotBlank() },
            nomTitulaire = usager?.let { "${it.nom} ${it.prenom}" },
            numeroCarte = carte.valeur,
            typeApporteur = usager?.typeApporteurLibelle,
            contact = usager?.couriel,
            carteId = carte.id,
            typeApporteurIsPro = usager?.typeApporteurIsPro,
            usagerId = usager?.id
        )
    }

    private suspend fun buildInfoFromImmatriculation(immatriculation: String): InfoCartePassage? {
        val now = System.currentTimeMillis()
        val carte = database.carteContratDao().getActiveCarteIByImmatriculation(immatriculation, now)
        if (carte == null) {
            // #533 : contrat acceptant des passages inconnus → écran dédié plutôt que « Carte inconnue »
            val inconnu = buildPassageInconnu(immatriculation)
            if (inconnu != null) _passageInconnu.value = inconnu
            else _carteInconnue.value = true
            return null
        }
        val usager = database.usagerDao().getUsagerByCarte(carte.id)
        return InfoCartePassage(
            societe = usager?.raisonSociale?.takeIf { it.isNotBlank() },
            nomTitulaire = usager?.let { "${it.nom} ${it.prenom}" },
            numeroCarte = carte.valeur,
            typeApporteur = usager?.typeApporteurLibelle,
            contact = usager?.couriel,
            carteId = carte.id,
            typeApporteurIsPro = usager?.typeApporteurIsPro,
            usagerId = usager?.id
        )
    }

    /**
     * #533 RG1/RG5/RG8 : null si le contrat n'accepte pas de passages inconnus
     * (`nbPassagesInconnusAcceptes <= 0`) → comportement « Carte inconnue » inchangé.
     */
    private suspend fun buildPassageInconnu(immatriculation: String): PassageInconnuInfo? {
        val contratId = _contratId.value ?: return null
        val contrat = database.contratDao().getContratById(contratId) ?: return null
        if (contrat.nbPassagesInconnusAcceptes <= 0) return null
        val connue = database.carteInconnueDao().getByImmatriculation(immatriculation)
        return PassageInconnuInfo(
            immatriculation = immatriculation.uppercase(),
            nbPassagesDeja = connue?.nbPassages ?: 0,
            nbPassagesAutorises = contrat.nbPassagesInconnusAcceptes,
            motifAutorisationContratId = connue?.motifAutorisationContratId,
            motifAutorisationLibelle = connue?.motifAutorisationLibelle
        )
    }

    private suspend fun buildInfoFromUsager(usager: UsagerEntity): InfoCartePassage {
        val now = System.currentTimeMillis()
        val carte = database.carteContratDao().getFirstActiveCarteICForUsager(usager.id, now)
        return InfoCartePassage(
            societe = usager.raisonSociale?.takeIf { it.isNotBlank() },
            nomTitulaire = "${usager.nom} ${usager.prenom}",
            numeroCarte = carte?.valeur,
            typeApporteur = usager.typeApporteurLibelle,
            contact = usager.couriel,
            carteId = carte?.id,
            typeApporteurIsPro = usager.typeApporteurIsPro,
            usagerId = usager.id
        )
    }
}
