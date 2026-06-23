# CLAUDE.md — Base de connaissance du projet idbat-v7-mobile

> ⚠️ **RÈGLE DE TRAVAIL — à respecter systématiquement**
> **Avant de commencer chaque nouveau sujet**, mettre à jour cette base de connaissance (`CLAUDE.md`) pour qu'elle reflète l'état réel du code (entités, version BDD, écrans, flux, conventions). On documente d'abord, on code ensuite. Toute fonctionnalité ajoutée doit être reportée ici.

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
singleton/    → Managers métier (AuthManager, SyncManager, ConfigSingleton, TokenStore)
data/
  entities/   → Entités Room
  dao/        → Interfaces Room DAO
  repository/ → Repositories (Room + API)
  model/      → Modèles métier purs (CartePuce, BarcodeFormat)
  nfc/        → Logique NFC : NfcRepository, NfcConfig
di/           → Modules Hilt (DatabaseModule, ApiModule)
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

`MainScreen` utilise un **NavHost** (routes `AppDestination` : Login / Home / Poc). Il demande d'abord les permissions de localisation, puis appelle `viewModel.initializeApp()`, et route vers `Login` ou `Home` selon `authState.isLoggedIn`. Les **navigations secondaires** (Dépôt, Saisie signalement, Création carte…) restent gérées par **état local** dans `HomeScreen` (`var showXxx by remember { mutableStateOf(false) }` + `if (showXxx) { XxxScreen(...) ; return }`).

| Écran | Rôle |
|---|---|
| `MainScreen` | Hub d'initialisation, permissions localisation, NavHost, AlertDialog erreur synchro |
| `LoginScreen` | Sélection site + utilisateur TP + saisie PIN |
| `HomeScreen` | Tableau de bord : infos site, badges synchro, actions |
| `DepotScreen` | Choix du type de carte (2 gros boutons empilés : Carte à puce / Autres) — fond blanc pur, affiché selon flags contrat |
| `AutresCartesScreen` | Code-barres / Immatriculation (uppercase alphanumérique only) / Sélection usager |
| `PassageInfoScreen` | Infos carte + **seuils/alertes** ; bouton "Fermer" si usager bloqué (seuil atteint) |
| `SaisieMatiereScreen` | Saisie des matières du passage (quantité obligatoire) |
| `TerminerPassageScreen` | Photos + signature + email → enregistrement passage en BDD (resize photos ≤4000px) |
| `SaisieSignalementScreen` | Événement + commentaire (max 50) + photos → signalement en BDD (offline-first) |
| `CreationCarteScreen` | Scan QR (192 car.) pour créer une carte à puce |
| `CarteCreationInfoScreen` | Affiche les infos parsées du QR + bouton Valider |
| `EcriturePuceScreen` | Écriture NFC de la carte (reader mode + états Idle/Writing/Success/Error) |
| `PocScreen` | Page POC (bac à sable NFC/CB/photo/signature) |

**Actions du HomeScreen :**
- Suivi des opérations · Transférer (synchronisation, loader pendant `isTransferring`)
- Saisie des signalements → `SaisieSignalementScreen`
- **Gestion des cartes** → `CarteActionSheet` — **visible uniquement si `contrat.hasPuce == true`**
- Passage en déchetterie → `DepotScreen`

**`CarteActionSheet`** propose : Créer une carte (→ `CreationCarteScreen`) · Recharger une carte · POC · **Test Volume Passage** (génère des passages de test en masse via `TestVolumeViewModel`)

**`PocScreen`** : 5 onglets PHOTO · CB · RFID Lecture · RFID Écriture · Signature (état hoisted dans le parent).

### Flux création de carte à puce
`Gestion des cartes → Créer une carte → CreationCarteScreen (scan QR) → CarteCreationInfoScreen (Valider) → EcriturePuceScreen (tap NFC → écriture)`
- Le QR contient une trame fixe **192 caractères** parsée par `CarteCreationQr.parse()` (`data/model`).
- `CarteCreationQr.toCartePuce(uid, numeroSerie)` mappe vers `CartePuce` ; le **CRC est recalculé** dans `CartePuce.serialize()` à partir du `numeroSerie` réel (lu du tag au tap).
- L'écriture passe par `EcriturePuceViewModel` → `NfcRepository.writeCartePuce()`.

---

## Base de données locale (Room)

**Nom :** `idbat_bdd` — **Version actuelle :** 27

| Entité | Description |
|---|---|
| `UtilisateurTPEntity` | Techniciens (login, PIN, token) |
| `ContratEntity` | Contrats (trigramme, nom, flags `hasPuce`/`hasCodebarres`/`hasImmatriculation`/`hasSelectionusager`/signatures) |
| `SiteEntity` | Sites de collecte (adresse, horaires, imprimante…) — DAO en `@Upsert` (pas `REPLACE`, voir note cascade) |
| `MatiereSiteEntity` | Matières collectées par site (clé composite matiereId+siteId) |
| `CarteContratEntity` | Cartes/passes par contrat + **liste noire** (`isEnListeNoire`, `dtEntreeListeNoire`, `dtSortieListeNoire`, `motifListeNoireContratId`, `motifListeNoireLibelle`) |
| `MotifListeNoireContratEntity` | Motifs de liste noire par contrat |
| `UsagerEntity` | Usagers/clients (import en batch de 2000) |
| `ContratEvenementEntity` | Événements liés aux contrats |
| `LastSynchroHistoryEntity` | Historique de synchronisation (ENVOI / RECEPTION), stats par site |
| `UsagerCarteEntity` | Liaison usager↔carte (dates début/fin) |
| `SeuilEtatEntity` | Seuils/plafonds par usager (PK composite usagerId+seuilId, FK usager CASCADE) + champs `seuilDetail*` |
| `PassageEntity` | Passage déchetterie (outbox) — `transactionId` UUID, `userTpId` (user connecté), `sentAt` (RG3) |
| `PassageMatiereEntity` | Matières d'un passage (FK passage CASCADE) |
| `PassageDocumentEntity` | Photos/signature d'un passage en base64 (FK passage CASCADE) |
| `SignalementEntity` | Signalement (outbox, **sans FK** pour survivre aux diffs) — `transactionId`, `agentId` (user connecté), `sentAt` (RG3) |
| `SignalementDocumentEntity` | Photos d'un signalement en base64 (FK signalement CASCADE) |
| `CarteCreeeEntity` | Journal des cartes créées (outbox, **sans FK**) — `userTpId` (user connecté), `sentAt` (RG3) |

**Migrations :** trajet complet (**version actuelle : 31**) documenté dans `AppDatabase.kt`. **Règle : migration non bloquante** — tout `ADD COLUMN NOT NULL` doit avoir un `DEFAULT` (les colonnes `sentAt` RG3 sont nullable, donc sans `DEFAULT`).  
**Init :** un utilisateur admin par défaut (login `admin`, PIN `1234`) est créé au premier accès.

> **Piège cascade Room** : `ContratDao`/`SiteDao` utilisent `@Upsert` (et non `@Insert(REPLACE)`). `REPLACE` fait DELETE+INSERT → déclenche `ON DELETE CASCADE` et efface l'historique de synchro / sous-entités. `@Upsert` met à jour en place sans cascade.

### Lecture de gros base64 (CursorWindow)
Les colonnes `base64` (photos) dépassent le `CursorWindow` SQLite par défaut (2 Mo). Pour la synchro montante, `SyncManager` lit les documents via un curseur brut avec `CursorWindow` agrandi à 10 Mo (`getDocumentsForSync` / `getSignalementDocumentsForSync`).

---

## API back-end

**Protocole :** REST — client généré par OpenAPI Generator dans `generated/client/`

**URLs (`ConfigSingleton`)** — détection auto émulateur vs device physique :
- Émulateur : `BASE_URL_DEV_EMULATOR` = `http://10.0.2.2:8091/`
- Device physique : `BASE_URL_DEV_DEVICE` = `http://localhost:8091/` (nécessite `adb reverse tcp:8091 tcp:8091` — script `adb-reverse.ps1` à la racine)
- Staging : `BASE_URL_STAGING`
- `baseUrl` est un getter qui choisit selon `isEmulator` (heuristique `Build.FINGERPRINT`/`MODEL`/`PRODUCT`).

**Interfaces principales :**

| Interface | Endpoints clés |
|---|---|
| `AuthMobileControllerApi` | `authenticateUser()` → token Bearer |
| `ContratsControllerApi` | `getByDevice()` → `ContratDmo` complet (sites, usagers, cartes, événements, **seuils**) |
| `SmartphonesMobileControllerApi` | `checkSmartphoneExists()`, `creerSmartphone()` (avec GPS) |
| `PassagesControllerApi` | `creer(CreerPassageRequest)` — envoi passage (matières + documents + `transactionId`) |
| `SignalementsControllerApi` | `creerSignalement(CreerSignalementRequest)` — envoi signalement + photos (`FileData`) |
| `CarteCreationControllerApi` | `marquerCarteCreationParQrCode(MarquerCarteQrCodeRequest)` — `POST api/carte-creation` (uid, numeroIdentification, `userTpId`, dateCreationMobile) |

**Adapters Moshi (`ApiModule`)** : `LocalDate`, `OffsetDateTime` (sérialisé **sans offset** → `LocalDateTime` attendu par le back .NET), `BigDecimal` (via `toPlainString()`, pas de notation scientifique).

**Auth :** token Bearer injecté via intercepteur OkHttp dans `ApiModule` (lu depuis `TokenStore`).  
**Enregistrement device :** au premier lancement, smartphone enregistré avec position GPS. `getCurrentLocation()` demande une position fraîche (`requestSingleUpdate`, timeout 5 s) puis fallback sur le cache de tous les providers. Les permissions sont demandées dans `MainScreen` avant `initializeApp()`.

---

## State management

### `AuthManager.AuthState`
```kotlin
isLoggedIn, isInitialized, loginError,
loggedInSite, loggedInContrat, loggedInUtilisateurTp,
availableSites, availableUtilisateursTps,
isLoadingContracts, showValidationError
```
`refreshLoggedInContrat()` recharge le contrat depuis la BDD après une synchro (les flags `hasPuce`… sont mis à jour pour l'UI). Appelé par `SyncService` après le transfert.

**Reconnexion obligatoire (retour de veille)** : `AuthManager` mémorise, au passage en arrière-plan/veille (`onAppBackgrounded()`, uniquement si une session est ouverte), **deux horodatages** : `SystemClock.elapsedRealtime()` (horloge monotone, deep sleep inclus) et `System.currentTimeMillis()` (horloge murale). Au retour au premier plan (`onAppForegrounded()`), `logout()` est appelé si **l'une** des deux règles est vraie :
1. **Premier accès de la journée** (indépendant du paramètre) : le **jour calendaire** a changé entre la mise en veille et le retour (`isSameCalendarDay()` via `Calendar`, année + jour de l'année — robuste API 24). Reproduit le comportement v6 (comparaison à la sortie de veille).
2. **Délai d'inactivité** : le temps écoulé (`elapsedRealtime`) dépasse `ConfigSingleton.sessionTimeoutMinutes` (défaut **10 min**).

`logout()` met `isLoggedIn=false` → `MainScreen` re-route vers `LoginScreen` avec `popUpTo(0) { inclusive = true }` (l'écran précédent n'est PAS restauré). Les deux hooks sont déclenchés par `MainActivity.onStop()` / `onStart()`. La règle (1) utilise l'horloge murale (jour réel) ; la règle (2) l'horloge monotone (robuste au changement d'heure). *Limite connue (déjà présente en v6) : si l'app reste au premier plan toute la nuit sans veille ni écran éteint, aucun `onStop` n'est émis et le changement de jour n'est pas détecté.*

### `SyncManager.SyncState`
```kotlin
lastSynchroDateEnvoi, lastSynchroDateReception, isTransferring,
syncError, lastEnvoiSuccess  // null=jamais, true=tout OK (réussies==tentées), false=échecs
```

`MainViewModel` fusionne ces deux états pour l'UI principale.

---

## Synchronisation

`SyncManager.executeTransfer(site)` enchaîne (selon `ConfigSingleton.IsSyncAscEnable/IsSyncDescEnable`) puis appelle `purgeOldSyncedData()` :

1. **Synchro montante (`synchroMontante`)** : envoie un par un les **passages**, **signalements** puis **cartes créées** qui restent à transmettre (`sentAt IS NULL` via `getUnsentXxx()`). Après `200 OK`, la ligne n'est **pas supprimée** mais **marquée** `markSent(id, now)` (RG3, cf. ci-dessous). Stats `(tentées, réussies)` cumulées **par site** → `LastSynchroHistoryEntity` type `ENVOI`. Badge "Envoi" vert si `réussies == tentées`. **« Rien à envoyer » = contrôle d'envoi réussi** : l'horodatage `ENVOI` du site courant est quand même rafraîchi (compteur d'opérations conservé) — sinon la date paraît périmée.
2. **Synchro descendante (`synchroDescendante`)** : bloquée si des passages/signalements restent **à envoyer** (`countUnsent() > 0`, pas `count()` — les lignes en rétention ne bloquent pas). Sinon `getByDevice()` → `applyDiff(dmo)` dans une **transaction Room**. La date `RECEPTION` est rafraîchie à **chaque** réception réussie (pas de court-circuit « rien à recevoir »).

**RG3 — rétention locale (`ConfigSingleton.dataRetentionDays`, défaut 2 j)** : les données saisies ne sont supprimées que **X jours après leur saisie ET une fois envoyées**. Mécanique : `sentAt` (horodatage d'envoi, `null` = non envoyé) sur `passage`/`signalement`/`carte_creee` ; `purgeOldSyncedData()` (appelée à chaque transfert) fait `deleteSentOlderThan(now − X j)` sur les 3 tables (`WHERE sentAt IS NOT NULL AND <horodatage saisie> < seuil`). Sert au petit reporting / réédition de bons (hors MVP). ⚠️ `PassageEntity` a des **FK CASCADE** vers Site/Contrat : un passage en rétention peut être supprimé prématurément si la descendante retire son site/contrat (`signalement`/`carte_creee` sont sans FK, donc protégés).

**Traçabilité (user connecté + device)** : chaque ligne d'outbox porte l'**id du TP connecté** (`AuthManager.loggedInUtilisateurTp.id`) — `userTpId` (passage), `agentId` (signalement), `userTpId` (carte créée, → `MarquerCarteQrCodeRequest.userTpId`). L'**identifiant du device n'est PAS envoyé** dans les payloads : il est **déduit du token** côté back (le login se fait avec `idMobile = ANDROID_ID`).

**Auto-synchro périodique** : `MainViewModel` (boucle `viewModelScope`) déclenche `executeTransfer()` toutes les `ConfigSingleton.syncIntervalMinutes` minutes, si connecté et `!isTransferring`. Tourne tant que le process vit (pas de WorkManager → s'arrête si le process est tué).

**Suivi (popup du bouton « Suivi »)** : `MainViewModel.getSuiviContentAsync` calcule les compteurs à partir des **enregistrements en base** (pas de l'historique) : **« Opérations »** = `count()` cumulé sur les outbox (`passage`+`signalement`+`carte_creee` ; inclut RG3 : envoyé-non-purgé + non-envoyé) ; **« Opérations non transférées »** = `countUnsent()` cumulé (`sentAt IS NULL`, ≈ 0 après une synchro).

**Robustesse écran éteint / doze :**
- `executeTransfer` tient un `PARTIAL_WAKE_LOCK` + `WifiLock` (timeout 10 min).
- Le transfert tourne dans un **foreground service** `SyncService` (type `dataSync`, notification persistante) lancé par `MainViewModel.executeTransfer()` → survit au deep doze. L'UI observe toujours `syncState` (singleton partagé).
- `transactionId` (UUID) sur `PassageEntity`/`SignalementEntity` → anti-doublon si la réponse réseau est perdue (le back doit dédupliquer dessus).

> **Ajouter un nouveau type d'opération outbox** (ex. rechargement carte, maj e-mail usager, maj uid RFID) : (1) entité avec `userTpId` + `sentAt` ; (2) migration Room (+ bump version) ; (3) DAO `getUnsent` / `markSent` / `countUnsent` / `count` / `deleteSentOlderThan` ; (4) envoi dans `synchroMontante` (markSent, pas delete) ; (5) purge dans `purgeOldSyncedData()` ; (6) ajouter `count()`/`countUnsent()` aux sommes du Suivi (`MainViewModel.getSuiviContentAsync`).

**Première synchro** : `AuthManager.saveContractToDatabase()` (chemin séparé de `applyDiff`) — toute modif de mapping DMO→entité doit être répercutée **dans les deux** (`AuthManager` ET `SyncManager.applyDiff`).

---

## Composants UI réutilisables (`ui/components/`)

| Composant | Rôle |
|---|---|
| `MainSiteCard` | Carte principale (nom site, dates synchro, boutons Suivi/Transférer) |
| `ActionRowButton` | Bouton ligne blanc arrondi avec icône droite (`iconVector` ou `iconResId`) |
| `BottomLargeButton` | Grand bouton bas d'écran corail avec image droite |
| `CarteActionSheet` | Bottom sheet Material3 avec 3 actions carte |
| `PhotoPickerComponent` | Prise/sélection photos, thumbnails suppressibles + **visualisation plein écran au clic** (Dialog) — état **hoisted** (`photos`, `onPhotosChange`) |
| `BarcodeScannerComponent` | Scan code-barres/QR via caméra + ML Kit. **Ne lance la caméra qu'au clic sur "Scanner"** ; params `title`/`subtitle` pour éviter le double-cadre ; callback `onBarcodeDetected(value, format)` |
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

**`CarteCreationQr`** (package `data.model`) : contenu d'un QR de création de carte (trame fixe **192 caractères**). `parse(raw)` découpe par offsets, décode les modes de paiement (hex). `toCartePuce(uid, numeroSerie)` mappe vers `CartePuce` pour l'écriture NFC.

**NFC** (`data.nfc`) :
- `NfcConfig.kt` — constantes `NFC_TRIPLE_DES_KEY` et `NFC_TRIPLE_DES_IV` (visibilité `internal`)
- `NfcRepository` — `@Singleton @Inject constructor()`, expose `suspend readCartePuce(tag)` et `suspend writeCartePuce(tag, carte)` avec dispatching IO interne (`withContext(Dispatchers.IO)`)

**`PocViewModel`** (`ui/viewmodel`) — `@HiltViewModel` qui injecte `NfcRepository` et l'expose aux composants Mifare. Les composants gèrent le cycle de vie NFC adapter (`DisposableEffect` → `enableReaderMode`/`disableReaderMode`) mais délèguent tout le protocole I/O au repository.

---

## Permissions et FileProvider

Permissions déclarées dans `AndroidManifest.xml` :
- `INTERNET`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `CAMERA`, `NFC`
- `WAKE_LOCK`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `POST_NOTIFICATIONS` (pour `SyncService`)

Service déclaré : `.service.SyncService` (`foregroundServiceType="dataSync"`, `exported=false`).

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
- **ConfigSingleton** : contient l'URL de base, le flag `webEnabled`, les intervalles (`syncIntervalMinutes`, `dataRetentionDays`) et le **délai d'inactivité avant reconnexion** `sessionTimeoutMinutes` (défaut 10 min, cf. State management). Changer l'URL ici pour switcher d'environnement.
- **Imports en batch** : `UsagerEntity` est inséré par tranches de 2000 ; ne pas casser cette logique lors de refactorisations.
- **Permissions Android** : `INTERNET` + localisation + `CAMERA` déclarées dans `AndroidManifest.xml` ; la localisation est utilisée à l'enregistrement du device uniquement ; la caméra est demandée à la volée (runtime permission) dans `PhotoPickerComponent` et `CardScanComponent`.
- **FileProvider** : toute nouvelle fonctionnalité utilisant `TakePicture()` doit passer par `createCameraUri()` (`CameraUtils.kt`) — ne pas créer de URI caméra directement.
- **State hoisting** : `PhotoPickerComponent`, `CardScanComponent`, `SignatureComponent` et `MifareReaderComponent` n'ont pas d'état interne — l'état est géré par le parent (`PocScreen`). Ne pas ré-internaliser cet état.
- **Couleurs** : ne jamais écrire `Color(0x...)` en inline — voir section "Design system — Couleurs" ci-dessus.
- **Logique NFC** : toute opération Mifare (lecture/écriture de blocs, authentification, sérialisation) doit passer par `NfcRepository`. Les composants `MifareReaderComponent` et `MifareWriterComponent` ne font que gérer le cycle de vie NFC adapter et les callbacks UI.
- **Modèles métier** : les modèles sans persistance Room vont dans `data/model/`, pas dans `ui/components/`. Ex : `CartePuce`, `BarcodeFormat`.
- **Clés cryptographiques** : les constantes sensibles sont dans `data/nfc/NfcConfig.kt` avec visibilité `internal`. Ne pas les dupliquer ou les déplacer dans `BuildConfig` sans concertation (la clé est partagée avec le back-end .NET).
- **Réseau (Retrofit/OkHttp)** : toute la configuration HTTP est dans `di/ApiModule.kt`. Ne jamais recréer de client Retrofit ailleurs. Les API interfaces (`AuthMobileControllerApi`, `ContratsControllerApi`, `SmartphonesMobileControllerApi`) sont des singletons Hilt injectés directement dans les Managers.
- **Token API** : le token Bearer est géré par `singleton/TokenStore.kt` (`@Singleton @Inject`). L'intercepteur `ApiModule` le lit, `AuthManager` l'écrit après authentification. Ne jamais stocker le token dans une variable globale ou `ConfigSingleton`.
- **URLs** : dans `ConfigSingleton` (`BASE_URL_DEV_EMULATOR`, `BASE_URL_DEV_DEVICE`, `BASE_URL_STAGING`). `baseUrl` est un getter auto émulateur/device. Test sur device → lancer `adb-reverse.ps1` (port-forward 8091, volatil : refait à chaque rebranchement/redémarrage adb).
- **Formats de code-barres** : la fonction `Int.toFormatName()` est dans `data/model/BarcodeFormat.kt`. Ne pas la redéfinir localement dans les composants.
- **Navigation** : `MainScreen` = NavHost (Login/Home/Poc). Navigations secondaires depuis `HomeScreen` = état local `var showXxx by remember { mutableStateOf(false) }` + `if (showXxx) { XxxScreen(onBack = { showXxx = false }) ; return }`.
- **Contrat dans les écrans (jamais en paramètre figé)** : aucun écran ne reçoit un `ContratEntity` en paramètre. Les écrans qui ont besoin du contrat (`HomeScreen`, `DepotScreen`, `AutresCartesScreen`, `PassageInfoScreen`, `SaisieMatiereScreen`, `ConfirmationPassageScreen`) prennent un `contratId: Long` et l'observent via `ContratViewModel` (`hiltViewModel()` partagé sur le `ViewModelStoreOwner` de la route Home) : `LaunchedEffect(contratId) { contratVm.setContratId(contratId) }` + `val contrat by contratVm.contrat.collectAsStateWithLifecycle()`. `ContratViewModel.contrat` est un **flux live** issu de `ContratDao.getContratByIdFlow(id)` → le contrat est toujours rechargé/à jour depuis la BDD quand on (re)vient sur l'écran (utile après une synchro descendante qui change les flags). Ne jamais réintroduire un paramètre `contrat: ContratEntity?`.
- **Synchro DMO→entité** : tout nouveau champ d'un DMO doit être mappé **dans les deux chemins** : `AuthManager.saveContractToDatabase` (1ère synchro) ET `SyncManager.applyDiff` (descendante).
- **Photos** : redimensionnées avant stockage base64 (resize côté ViewModels passage/signalement). Stockées en base64 dans `*_document` ; lues en synchro via curseur `CursorWindow` 10 Mo.
- **Champ immatriculation** : filtrer la saisie en uppercase alphanumérique non-accenté (`.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }`) + `KeyboardOptions` Characters/Ascii.
- **Écriture carte à puce** : trame QR 192 car. → `CarteCreationQr` → `CartePuce` ; CRC recalculé dans `serialize()` au tap (dépend du `numeroSerie` réel). Toujours passer par `NfcRepository.writeCartePuce`.
- **`fillMaxSize` + boutons bas** : pour un écran scrollable avec boutons toujours visibles → zone centrale `Modifier.weight(1f).verticalScroll(...)`, boutons hors du scroll, et `navigationBarsPadding()` sur la Column racine (sinon boutons sous la barre système).

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

**Sur device physique USB** : lancer `adb-reverse.ps1` (à la racine) pour rediriger `localhost:8091` du téléphone vers la machine. À refaire à chaque débranchement/redémarrage du serveur adb. Vérifier avec `adb reverse --list` (doit afficher `UsbFfs tcp:8091 tcp:8091`).
