# CLAUDE.md — Base de connaissance du projet idbat-v7-mobile

## Vue d'ensemble

Application Android native pour les techniciens de terrain Veolia. Elle permet de gérer les sites de collecte de déchets (contrats, matières, usagers), de saisir des signalements et de synchroniser les opérations avec le back-end.

**Stack principale :** Kotlin · Jetpack Compose · Room · Retrofit · Hilt · Kotlin Coroutines/Flow  
**API min/target :** 24 / 34  
**Java compatibility :** 17

---

## Architecture

### Couches

```
ui/           → Composables + ViewModels (MVVM)
singleton/    → Managers métier (AuthManager, SyncManager, ApiClient, ConfigSingleton)
data/         → Entités Room, DAOs, Repositories, Converters
di/           → Modules Hilt
generated/    → Client API auto-généré (OpenAPI)
utils/        → Utilitaires divers
```

### Flux de données

```
Composable → ViewModel (StateFlow) → Manager singleton → Repository → Room DAO
                                                        ↘ ApiClient (Retrofit)
```

Les Managers (`AuthManager`, `SyncManager`) exposent des `StateFlow` consommés par les ViewModels, qui les propagent aux Composables via `collectAsState()`.

---

## Écrans et navigation

La navigation est conditionnelle (pas de NavHost) : `MainScreen` route vers `LoginScreen` ou `HomeScreen` selon `authState.isLoggedIn`. Les navigations secondaires (ex. `PocScreen`) sont gérées par état local dans le composable parent (`var showPocScreen by remember { mutableStateOf(false) }` dans `HomeScreen`).

| Écran | Rôle |
|---|---|
| `MainScreen` | Hub d'initialisation, routing authentification |
| `LoginScreen` | Sélection site + utilisateur TP + saisie PIN |
| `HomeScreen` | Tableau de bord : infos site, dates synchro, actions |
| `PocScreen` | Page POC accessible depuis `HomeScreen` → bottom sheet "Gestion des cartes" |

**Actions du HomeScreen :**
- Suivi des opérations
- Transférer (synchronisation)
- Saisie des signalements
- Gestion des cartes → ouvre `CarteActionSheet` (bottom sheet Material3)

**`CarteActionSheet`** propose : Créer une carte · Recharger une carte · POC (navigue vers `PocScreen`)

**`PocScreen`** contient 4 onglets : PHOTO · CB · RFID Lecture · RFID Écriture  
- Onglet **PHOTO** : `PhotoPickerComponent` (état hoisted) + bouton compteur  
- Onglet **CB** : `CardScanComponent` (OCR ML Kit) + affichage des champs extraits

---

## Base de données locale (Room)

**Nom :** `idbat_bdd` — **Version actuelle :** 15

| Entité | Description |
|---|---|
| `UtilisateurTPEntity` | Techniciens (login, PIN, token) |
| `ContratEntity` | Contrats (trigramme, nom) |
| `SiteEntity` | Sites de collecte (adresse, horaires, imprimante…) |
| `MatiereSiteEntity` | Matières collectées par site (clé composite matiereId+siteId) |
| `CarteContratEntity` | Cartes/passes par contrat (RFID, QR code…) |
| `MotifListeNoireContratEntity` | Motifs de liste noire par contrat |
| `UsagerEntity` | Usagers/clients (import en batch de 2000) |
| `ContratEvenementEntity` | Événements liés aux contrats |
| `LastSynchroHistoryEntity` | Historique de synchronisation (ENVOI / RECEPTION) |

**Migrations :** trajet complet v1→v15 documenté dans `AppDatabase.kt`.  
**Init :** un utilisateur admin par défaut (login `admin`, PIN `1234`) est créé au premier accès.

---

## API back-end

**Protocole :** REST — client généré par OpenAPI Generator dans `generated/client/`

**URLs :**
- Dev (émulateur) : `http://10.0.2.2:8091/`
- Staging (commenté) : `https://idbat-mobile-rec.recyclage.veolia.fr/`

**Interfaces principales :**

| Interface | Endpoints clés |
|---|---|
| `AuthMobileControllerApi` | `authenticateUser()` → token Bearer |
| `ContratsControllerApi` | `getByDevice()` → contrat complet avec toutes les sous-entités |
| `SmartphonesMobileControllerApi` | `checkSmartphoneExists()`, `creerSmartphone()` |

**Auth :** token Bearer injecté automatiquement via un intercepteur OkHttp dans `ApiClient`.  
**Enregistrement device :** au premier lancement, le smartphone est enregistré avec sa position GPS (permissions `ACCESS_FINE_LOCATION` et `ACCESS_COARSE_LOCATION`).

---

## State management

### `AuthManager.AuthState`
```kotlin
isLoggedIn, isInitialized, loginError,
loggedInSite, availableSites,
availableUtilisateursTps, showValidationError
```

### `SyncManager.SyncState`
```kotlin
lastSynchroDateEnvoi, lastSynchroDateReception, isTransferring
```

`MainViewModel` fusionne ces deux états pour l'UI principale.

---

## Composants UI réutilisables (`ui/components/`)

| Composant | Rôle |
|---|---|
| `MainSiteCard` | Carte principale (nom site, dates synchro, boutons Suivi/Transférer) |
| `ActionRowButton` | Bouton ligne blanc arrondi avec icône droite (`iconVector` ou `iconResId`) |
| `BottomLargeButton` | Grand bouton bas d'écran corail avec image droite |
| `CarteActionSheet` | Bottom sheet Material3 avec 3 actions carte |
| `PhotoPickerComponent` | Prise/sélection photos, thumbnails suppressibles — état **hoisted** (`photos`, `onPhotosChange`) |
| `CardScanComponent` | Scan CB via caméra/galerie + OCR ML Kit → retourne `CardData` via `onCardDataExtracted` |
| `CameraUtils` | `createCameraUri(context)` — helper interne partagé pour FileProvider |
| `ToastHost` / `rememberToastState` | Système de toasts custom avec titre + contenu rich text |

**`CardData`** (défini dans `CardScanComponent.kt`) :
```kotlin
data class CardData(val cardNumber: String?, val expiryDate: String?, val cardholderName: String?)
```

---

## Permissions et FileProvider

Permissions déclarées dans `AndroidManifest.xml` :
- `INTERNET`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` (existantes)
- `CAMERA` (ajoutée pour la prise de photo)

FileProvider configuré : autorité `${applicationId}.fileprovider`, chemins dans `res/xml/file_paths.xml` (cache `images/`). Utilisé par `createCameraUri()` pour les photos caméra.

---

## Dépendances clés (versions dans `gradle/libs.versions.toml`)

| Lib | Version |
|---|---|
| Compose BOM | 2024.02.00 |
| Hilt | 2.50 |
| Room | 2.6.1 |
| Retrofit | 2.11.0 |
| Moshi | 1.15.2 |
| OkHttp | 4.12.0 |
| Lifecycle | 2.7.0 |
| Navigation Compose | 2.7.7 |
| Coroutines | 1.8.1 |
| Coil Compose | 2.6.0 |
| ML Kit Text Recognition | 16.0.0 |
| Material Icons Extended | 1.6.1 |

---

## Points d'attention pour les modifications

- **Migrations Room** : toute modification de schéma doit s'accompagner d'une migration incrémentale dans `AppDatabase.kt` et d'un bump de version. Ne pas utiliser `fallbackToDestructiveMigration` en prod.
- **Client API généré** : le dossier `generated/client/` ne se modifie pas à la main — regénérer depuis la spec OpenAPI.
- **ConfigSingleton** : contient l'URL de base et le flag `webEnabled`. Changer l'URL ici pour switcher d'environnement.
- **Imports en batch** : `UsagerEntity` est inséré par tranches de 2000 ; ne pas casser cette logique lors de refactorisations.
- **Permissions Android** : `INTERNET` + localisation + `CAMERA` déclarées dans `AndroidManifest.xml` ; la localisation est utilisée à l'enregistrement du device uniquement ; la caméra est demandée à la volée (runtime permission) dans `PhotoPickerComponent` et `CardScanComponent`.
- **FileProvider** : toute nouvelle fonctionnalité utilisant `TakePicture()` doit passer par `createCameraUri()` (`CameraUtils.kt`) — ne pas créer de URI caméra directement.
- **State hoisting** : les composants photo (`PhotoPickerComponent`, `CardScanComponent`) n'ont pas d'état interne — l'état est géré par le parent. Ne pas ré-internaliser cet état.
- **Navigation secondaire** : pas de NavHost — utiliser `var showXxx by remember { mutableStateOf(false) }` dans le composable parent et un `if (showXxx) { XxxScreen(onBack = { showXxx = false }) ; return }` pour les nouvelles pages.

---

## Tests

Les dépendances de test sont configurées (JUnit 4, Espresso, Compose UI Test) mais **aucun test automatisé n'existe** actuellement.

---

## Lancer l'application

```bash
# Depuis Android Studio ou en ligne de commande
./gradlew assembleDebug
./gradlew installDebug
```

L'émulateur doit cibler API 24+ et avoir accès à `10.0.2.2:8091` (back-end local) pour que la synchro fonctionne en dev.
