param(
    [string] $HostIp = "192.168.0.14",
    [int] $PairPort = 0,
    [string] $PairCode = "",
    [switch] $ListMunkz,
    [switch] $InstallSetlistDebug,
    [switch] $InstallAllDebug,
    [switch] $UninstallPlaySetlist,
    [switch] $LaunchSetlist,
    [string] $Serial = ""
)

$ErrorActionPreference = "Stop"

$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    throw "ADB was not found at $adb"
}

function Invoke-Adb {
    & $adb @args
}

function Invoke-DeviceAdb {
    if ($Serial) {
        & $adb -s $Serial @args
    } else {
        & $adb @args
    }
}

if ($PairPort -gt 0 -and $PairCode) {
    Write-Host "Pairing $HostIp`:$PairPort ..."
    Invoke-Adb pair "$HostIp`:$PairPort" $PairCode
}

Write-Host "Looking for Wear OS wireless debugging services ..."
$services = Invoke-Adb mdns services
$connectLine = $services |
    Select-String -Pattern "_adb-tls-connect\._tcp\s+$([regex]::Escape($HostIp)):(\d+)" |
    Select-Object -First 1

if ($connectLine) {
    $connectPort = [regex]::Match($connectLine.Line, "$([regex]::Escape($HostIp)):(\d+)").Groups[1].Value
    if (-not $Serial) {
        $Serial = "$HostIp`:$connectPort"
    }
    Write-Host "Connecting $HostIp`:$connectPort ..."
    Invoke-Adb connect "$HostIp`:$connectPort"
} else {
    Write-Host "No mDNS connect service found for $HostIp."
    Write-Host "If the watch shows an IP address & port, run:"
    Write-Host "  .\tools\connect-watch.ps1 -HostIp $HostIp"
    Write-Host "If pairing is required, run:"
    Write-Host "  .\tools\connect-watch.ps1 -HostIp $HostIp -PairPort PORT -PairCode CODE"
}

Write-Host ""
Invoke-Adb devices -l

if ($ListMunkz) {
    Write-Host ""
    Write-Host "Installed Munkz/Pulse packages:"
    Invoke-DeviceAdb shell pm list packages |
        Select-String -Pattern "munkz|bpm|pulse"
}

if ($UninstallPlaySetlist) {
    Write-Host ""
    Write-Host "Uninstalling existing Setlist package ..."
    Invoke-DeviceAdb uninstall bpm.munkz.pulse_wear.os.playlist
}

if ($InstallSetlistDebug) {
    $apk = Join-Path (Get-Location) "app\build\outputs\apk\playlist\debug\app-playlist-debug.apk"
    if (-not (Test-Path $apk)) {
        throw "Setlist debug APK was not found at $apk. Build it with .\gradlew.bat :app:assemblePlaylistDebug"
    }

    Write-Host ""
    Write-Host "Installing Setlist debug APK ..."
    Invoke-DeviceAdb install -r $apk

    Write-Host "Launching Setlist ..."
    Invoke-DeviceAdb shell monkey -p bpm.munkz.pulse_wear.os.playlist -c android.intent.category.LAUNCHER 1
}

if ($InstallAllDebug) {
    $apkPaths = @(
        "app\build\outputs\apk\bpm\debug\app-bpm-debug.apk",
        "app\build\outputs\apk\tune\debug\app-tune-debug.apk",
        "app\build\outputs\apk\rhythm\debug\app-rhythm-debug.apk",
        "app\build\outputs\apk\playlist\debug\app-playlist-debug.apk",
        "app\build\outputs\apk\pro\debug\app-pro-debug.apk",
        "app\build\outputs\apk\fidgettoy\debug\app-fidgettoy-debug.apk",
        "app\build\outputs\apk\hearnoevil\debug\app-hearnoevil-debug.apk",
        "watchface\build\outputs\apk\debug\watchface-debug.apk"
    )

    foreach ($relativePath in $apkPaths) {
        $apk = Join-Path (Get-Location) $relativePath
        if (-not (Test-Path $apk)) {
            throw "APK was not found at $apk. Build debug APKs before installing."
        }

        Write-Host ""
        Write-Host "Installing $relativePath ..."
        Invoke-DeviceAdb install -r $apk
    }
}

if ($LaunchSetlist) {
    Write-Host ""
    Write-Host "Launching Setlist ..."
    Invoke-DeviceAdb shell monkey -p bpm.munkz.pulse_wear.os.playlist -c android.intent.category.LAUNCHER 1
}
