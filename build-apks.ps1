# build-apks.ps1 — Génère les APK pour chaque environnement (OVH, REC, INT, PROD)
# Usage : .\build-apks.ps1   (depuis la racine du projet)

$ErrorActionPreference = 'Stop'

$root        = $PSScriptRoot
$apkDir      = Join-Path $root 'Apk'
$gradleFile  = Join-Path $root 'app\build.gradle.kts'
$configFile  = Join-Path $root 'app\src\main\java\com\idbat\mobile\singleton\ConfigSingleton.kt'
$apkOutput   = Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'
$gradlew     = Join-Path $root 'gradlew.bat'
$utf8NoBom   = New-Object System.Text.UTF8Encoding($false)

function Read-TextFile([string]$path) { [System.IO.File]::ReadAllText($path, $utf8NoBom) }
function Write-TextFile([string]$path, [string]$content) { [System.IO.File]::WriteAllText($path, $content, $utf8NoBom) }

# 1. Dossier Apk : création ou vidage
if (Test-Path $apkDir) {
    Get-ChildItem -Path $apkDir -Force | Remove-Item -Recurse -Force -Confirm:$false
    Write-Host "Dossier Apk vidé." -ForegroundColor DarkGray
} else {
    New-Item -ItemType Directory -Path $apkDir | Out-Null
    Write-Host "Dossier Apk créé." -ForegroundColor DarkGray
}

# 2. Version à builder → versionName dans build.gradle.kts (recherche par regex, pas par n° de ligne)
$version = (Read-Host "Version à builder (ex. 7.2.6)").Trim()
if ([string]::IsNullOrWhiteSpace($version)) { throw "Version vide, abandon." }

$versionRegex = 'versionName\s*=\s*"[^"]*"'
$gradle = Read-TextFile $gradleFile
if ($gradle -notmatch $versionRegex) { throw "versionName introuvable dans $gradleFile" }
$gradle = [regex]::Replace($gradle, $versionRegex, "versionName = `"$version`"", 1)
Write-TextFile $gradleFile $gradle
Write-Host "versionName = `"$version`" écrit dans build.gradle.kts" -ForegroundColor Green

# 3. Build par environnement (baseUrl modifié dans ConfigSingleton puis restauré à la fin)
$baseUrlRegex   = 'val\s+baseUrl\s*=\s*BASE_URL_\w+'
$originalConfig = Read-TextFile $configFile
if ($originalConfig -notmatch $baseUrlRegex) { throw "Ligne 'val baseUrl = BASE_URL_...' introuvable dans $configFile" }

$targets = @(
    @{ Const = 'BASE_URL_OVH';  Name = "$version-OVH.apk" },
    @{ Const = 'BASE_URL_REC';  Name = "$version-REC.apk" },
    @{ Const = 'BASE_URL_INT';  Name = "$version-INT.apk" },
    @{ Const = 'BASE_URL_PROD'; Name = "$version.apk" }
)

try {
    foreach ($t in $targets) {
        Write-Host ""
        Write-Host "=== Build $($t.Name) (baseUrl = $($t.Const)) ===" -ForegroundColor Cyan

        $config = [regex]::Replace($originalConfig, $baseUrlRegex, "val baseUrl = $($t.Const)", 1)
        Write-TextFile $configFile $config

        if (Test-Path $apkOutput) { Remove-Item $apkOutput -Force }

        # Équivalent CLI d'un "Gradle sync" : (re)configure le projet et rafraîchit les dépendances
        Write-Host "--- Gradle sync ---" -ForegroundColor DarkCyan
        & $gradlew -p $root --refresh-dependencies help
        if ($LASTEXITCODE -ne 0) { throw "Échec du Gradle sync pour $($t.Const) (code $LASTEXITCODE)" }

        & $gradlew -p $root assembleDebug
        if ($LASTEXITCODE -ne 0) { throw "Échec du build pour $($t.Const) (code $LASTEXITCODE)" }
        if (-not (Test-Path $apkOutput)) { throw "APK introuvable après build : $apkOutput" }

        Copy-Item $apkOutput (Join-Path $apkDir $t.Name) -Force
        Write-Host "-> Apk\$($t.Name)" -ForegroundColor Green
    }
}
finally {
    # Remet ConfigSingleton dans son état d'origine (même en cas d'échec)
    Write-TextFile $configFile $originalConfig
    Write-Host ""
    Write-Host "ConfigSingleton restauré." -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "Terminé. APK générés dans $apkDir :" -ForegroundColor Green
Get-ChildItem $apkDir | ForEach-Object { Write-Host "  $($_.Name)" }
