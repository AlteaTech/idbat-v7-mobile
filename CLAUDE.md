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
data/
  entities/   → Entités Room
  dao/        → Interfaces Room DAO
  repository/ → Repositories (Room + API)
  model/      → Modèles métier purs (ex. CartePuce)
  nfc/        → Logique NFC : NfcRepository, NfcConfig
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

**`PocScreen`** contient 5 onglets : PHOTO · CB · RFID Lecture · RFID Écriture · Signature  
- Onglet **PHOTO** : `PhotoPickerComponent` (état hoisted) + bouton compteur  
- Onglet **CB** : `BarcodeScannerComponent` + `OutlinedTextField` affichant la valeur scannée  
- Onglet **RFID Lecture** : `MifareReaderComponent` — résultat hoisted dans `rfidCard`  
- Onglet **RFID Écriture** : formulaire `OutlinedTextField` pré-rempli depuis `rfidCard` + `MifareWriterComponent`  
- Onglet **Signature** : `SignatureComponent` — image validée hoisted dans `signatureImage`

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
| `BarcodeScannerComponent` | Scan code-barres via caméra/galerie + ML Kit → callback `onBarcodeDetected(value, format)` |
| `MifareReaderComponent` | Lecture NFC Mifare Classic → callback `onCardRead: (CartePuce) -> Unit` |
| `MifareWriterComponent` | Écriture NFC Mifare Classic → lambda `buildCarte: (uid, numSerie) -> CartePuce?` appelé au moment du tap |
| `SignatureComponent` | Canvas de dessin (bezier quadratique) → callback `onValidate: ((ImageBitmap) -> Unit)?` |
| `CardScanComponent` | Scan CB via caméra/galerie + OCR ML Kit → retourne `CardData` via `onCardDataExtracted` |
| `CameraUtils` | `createCameraUri(context)` — helper interne partagé pour FileProvider |
| `ToastHost` / `rememberToastState` | Système de toasts custom avec titre + contenu rich text |

**`CardData`** (défini dans `CardScanComponent.kt`) :
```kotlin
data class CardData(val cardNumber: String?, val expiryDate: String?, val cardholderName: String?)
```

**`CartePuce`** (package `data.model`) : modèle métier carte Mifare avec `uid`, `numeroSerie`, `numeroIdentification`, `motDePasse`, `societe`, `nomPrenom`, `identClient`, `soldePoints`, `cumulPoints`, `paiementComptant`, flags booléens (`interne`, `prepaiement`, `facturation`, `gratuit`), `crc`. Méthodes : `serialize()` (240 octets ISO-8859-1), `parse(uid, numSerie, content)`, `isCrcValid`, `computeCrc()` (Triple-DES CBC).

**NFC** (`data.nfc`) :
- `NfcConfig.kt` — constantes `NFC_TRIPLE_DES_KEY` et `NFC_TRIPLE_DES_IV` (visibilité `internal`)
- `NfcRepository` — `@Singleton @Inject constructor()`, expose `suspend readCartePuce(tag)` et `suspend writeCartePuce(tag, carte)` avec dispatching IO interne (`withContext(Dispatchers.IO)`)

**`PocViewModel`** (`ui/viewmodel`) — `@HiltViewModel` qui injecte `NfcRepository` et l'expose aux composants Mifare. Les composants gèrent le cycle de vie NFC adapter (`DisposableEffect` → `enableReaderMode`/`disableReaderMode`) mais délèguent tout le protocole I/O au repository.

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

## Design system — Couleurs

### Tokens (`ui/theme/Color.kt`)

Toutes les couleurs du projet sont centralisées dans `Color.kt`. **Ne jamais écrire `Color(0x...)` en inline dans un composable — utiliser toujours un token nommé.**

| Token | Valeur | Usage |
|---|---|---|
| `VeoliaPrincipal` | `0xFFD96A56` | Interactions, boutons, tabs actifs |
| `VeoliaCoral` | `0xFFF27059` | Dégradés décoratifs, accents |
| `VeoliaCoralLight` | `0xFFFF9A82` | Fin de dégradé bouton photo |
| `VeoliaGradientTop` | `0xFFF8A282` | Haut du dégradé LoginScreen |
| `VeoliaGradientBot` | `0xFFE96D71` | Bas du dégradé LoginScreen |
| `VeoliaCoralText` | `0xFFEA6E72` | Texte corail sur fond blanc (bouton connexion) |
| `VeoliaSuccess` | `0xFF4CAF50` | Icônes / textes succès NFC |
| `VeoliaSuccessDark` | `0xFF007F2D` | Badge StatusBadge "Envoi/Réception" OK |
| `VeoliaWarning` | `0xFFFF9800` | Avertissement CRC |
| `VeoliaErrorDark` | `0xFFC60000` | Badge StatusBadge en échec, texte erreur |
| `VeoliaAlertOrange` | `0xFFF57C00` | Icône ValidationErrorDialog |
| `VeoliaInk` | `0xFF1C1C1E` | Trait de signature (canvas) |
| `VeoliaDisabled` | `0xFFE0E0E0` | Éléments inactifs / chips désactivés |
| `VeoliaSubtle` | `0xFFDDDDDD` | Bordures et séparateurs |
| `VeoliaPlaceholder` | `0xFFBBBBBB` | Placeholder zone signature |
| `VeoliaDrawingBg` | `0xFFFAFAFA` | Fond zone de dessin |
| `VeoliaGray` | `0xFF333333` | Texte secondaire foncé |
| `VeoliaLightGray` | `0xFFF5F5F5` | Fond général, surfaceVariant light |
| `White` / `Black` | — | Alias explicites |

### Règles d'import et d'utilisation

- **Import** : utiliser `import com.idbat.mobile.ui.theme.*` (wildcard) dans tout fichier UI utilisant des tokens couleur.
- **Texte principal** : `MaterialTheme.colorScheme.onSurface` (jamais `Color.Black` hardcodé).
- **Texte secondaire / labels** : `MaterialTheme.colorScheme.onSurfaceVariant` (jamais `Color.Gray` hardcodé — contraste insuffisant sur `surfaceVariant` en light).
- **Valeur par défaut de `CartePuceField`** : passer `MaterialTheme.colorScheme.onSurface` plutôt que `Color.Unspecified`.
- **Surfaces** : ne jamais passer `color = Color.White` à une `Surface` — laisser le thème gérer via `MaterialTheme.colorScheme.surface`.
- **Champs "mot de passe" non-secret** (ex. code carte RFID) : toujours ajouter `visualTransformation = VisualTransformation.None` pour éviter que l'IME Android masque le texte automatiquement sur détection du label.

### Thème (`ui/theme/Theme.kt`)

- **Dark mode commenté** : le vrai `DarkColorScheme` (avec `darkColorScheme(...)`) est présent mais commenté dans `Theme.kt`. **Ne pas le supprimer.** La variable `DarkColorScheme` pointe actuellement sur `lightColorScheme(...)` identique au light — les deux modes affichent le même rendu en attendant la validation du dark mode.
- `surfaceVariant` light = `VeoliaLightGray` (0xFFF5F5F5) ; `onSurfaceVariant` light = `VeoliaGray` (0xFF333333).

---

## Points d'attention pour les modifications

- **Migrations Room** : toute modification de schéma doit s'accompagner d'une migration incrémentale dans `AppDatabase.kt` et d'un bump de version. Ne pas utiliser `fallbackToDestructiveMigration` en prod.
- **Client API généré** : le dossier `generated/client/` ne se modifie pas à la main — regénérer depuis la spec OpenAPI.
- **ConfigSingleton** : contient l'URL de base et le flag `webEnabled`. Changer l'URL ici pour switcher d'environnement.
- **Imports en batch** : `UsagerEntity` est inséré par tranches de 2000 ; ne pas casser cette logique lors de refactorisations.
- **Permissions Android** : `INTERNET` + localisation + `CAMERA` déclarées dans `AndroidManifest.xml` ; la localisation est utilisée à l'enregistrement du device uniquement ; la caméra est demandée à la volée (runtime permission) dans `PhotoPickerComponent` et `CardScanComponent`.
- **FileProvider** : toute nouvelle fonctionnalité utilisant `TakePicture()` doit passer par `createCameraUri()` (`CameraUtils.kt`) — ne pas créer de URI caméra directement.
- **State hoisting** : `PhotoPickerComponent`, `CardScanComponent`, `SignatureComponent` et `MifareReaderComponent` n'ont pas d'état interne — l'état est géré par le parent (`PocScreen`). Ne pas ré-internaliser cet état.
- **Couleurs** : ne jamais écrire `Color(0x...)` en inline — voir section "Design system — Couleurs" ci-dessus.
- **Logique NFC** : toute opération Mifare (lecture/écriture de blocs, authentification, sérialisation) doit passer par `NfcRepository`. Les composants `MifareReaderComponent` et `MifareWriterComponent` ne font que gérer le cycle de vie NFC adapter et les callbacks UI.
- **Modèles métier** : les modèles sans persistance Room vont dans `data/model/`, pas dans `ui/components/`.
- **Clés cryptographiques** : les constantes sensibles sont dans `data/nfc/NfcConfig.kt` avec visibilité `internal`. Ne pas les dupliquer ou les déplacer dans `BuildConfig` sans concertation (la clé est partagée avec le back-end .NET).
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
