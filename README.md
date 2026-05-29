# BPM Munkz Pulse

BPM Munkz Pulse is a Wear OS tempo, tuner, spectrum, set-list, tile, and watch-face project for musicians. The repository contains two Android packages:

- `:app` - the Wear OS app, quick-launch tile, foreground metronome service, audio tuner/spectrum UI, and latest BPM/key complication providers.
- `:watchface` - the separate BPM Munkz Pulse watch-face package with selectable dial artwork and complication slots.

## Current Features

- Tap tempo and manual BPM control from 30 to 240 BPM.
- Foreground metronome service for more reliable timing while the app is running.
- Visual pulse, optional beep, optional haptics, beat accents, subdivisions, and adjustable accent intensity.
- Saved tempo playlists with song name, BPM, musical key, and notes.
- Tuner and spectrum overlays with note detection, key guessing, A4 reference adjustment, and save-to-clock support.
- Latest BPM and latest key complication data sources for the watch face.
- Quick-launch Wear OS tile with BPM Munkz Pulse branding.
- English and Spanish UI labels.
- Custom app colors, clock dial images, watch-face dial styles, and hand/ring colors.
- Separate watch-face package with all-colors, blue, green, orange, purple, and white dials.

## Project Layout

```text
app/
  src/main/java/com/example/bpmmunkzpulse/presentation/
    MainActivity.kt                         Wear Compose UI
    MetronomeService.kt                     Foreground metronome timing service
    BpmMunkzTileService.kt                  Wear OS tile
    LatestMusicComplicationDataSourceService.kt
                                           Latest BPM/key complication providers
  src/main/res/                            App strings, drawables, tile preview, launcher assets

watchface/
  src/main/res/raw/watchface.xml           Watch Face Format scene and complication slots
  src/main/res/drawable-nodpi/             Watch-face dial artwork
  src/main/res/drawable/                   Watch hands and preview

remote-live/
  Staged Django files/assets used for the LabMunkz product page.
```

## Build Requirements

- Android Studio or the bundled Gradle wrapper.
- Android Gradle Plugin configured by `gradle/libs.versions.toml`.
- Wear OS target SDK support for SDK 36.
- A Wear OS emulator/device for install testing.

`local.properties` should stay local and should not be committed.

## Common Commands

Build the Wear OS app:

```powershell
.\gradlew.bat :app:assembleDebug
```

Build the watch-face package:

```powershell
.\gradlew.bat :watchface:assembleDebug
```

Install both packages to the connected Wear OS device:

```powershell
.\gradlew.bat installDebugWithWatchFace
```

Run checks/builds before publishing changes:

```powershell
.\gradlew.bat :app:assembleDebug :watchface:assembleDebug
```

## App Package

The app package id is:

```text
com.example.bpmmunkzpulse
```

The app declares permissions for foreground service playback, notifications, microphone input, wake lock, and vibration. Microphone permission is used for tuner and spectrum analysis.

## Watch-Face Package

The watch-face package id is:

```text
com.example.bpmmunkzface
```

The app queries this package so it can help the user open/select the BPM Munkz Pulse watch face when installed.

The watch face can show latest BPM and latest key from the app via complication providers:

```text
com.example.bpmmunkzpulse.presentation.LatestBpmComplicationDataSourceService
com.example.bpmmunkzpulse.presentation.LatestKeyComplicationDataSourceService
```

## Website Page

The product page is staged under:

```text
remote-live/
```

The live Django route is intended to be:

```text
https://www.labmunkz.com/BPMMunkzPulse
```

When Django `.py` files are updated on the live server, restart the Gunicorn app service. Template-only edits generally do not require a server restart unless template caching or a stale worker process is involved.

Typical live check sequence:

```bash
cd /var/www/labmunkz
sudo -u nope .venv/bin/python manage.py check
systemctl restart gunicorn-labmunkz
systemctl is-active nginx gunicorn-labmunkz
```

If static assets are updated and production serves from `STATIC_ROOT`, run:

```bash
cd /var/www/labmunkz
sudo -u nope .venv/bin/python manage.py collectstatic --noinput
```

## Git Notes

Before committing, review local changes with:

```powershell
git status -sb
git diff --stat
```

Generated build output, IDE state, and local machine config should stay out of commits.
