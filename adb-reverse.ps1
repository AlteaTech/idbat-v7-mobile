& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8091 tcp:8091
Write-Host "adb reverse OK — port 8091 du device -> localhost:8091" -ForegroundColor Green
