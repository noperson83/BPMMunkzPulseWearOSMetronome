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

## App Page Rundown

The app uses four horizontal Wear OS pages. The screen is built for round watches, so most actions are compact buttons, popups, and overlays instead of deep phone-style menus.

### Page 1: Main Clock

The main page is the fast performance view.

- Shows the selected clock image, current time, and current song context.
- Start/stop control is centered for quick access.
- Edit opens playlist/song controls.
- Tuner and spectrum tools can be opened from the audio controls.
- The quick-stop overlay keeps stop access available while the metronome is running.
- Uses the selected app theme colors, clock image, clock hand color, and ring color.

### Page 2: Rhythm

The rhythm page is the main metronome setup surface.

- Large BPM readout opens the tap-tempo popup.
- Beat count and subdivision are shown in the center.
- Beat circles around the dial show accent placement.
- Beat accents support big, medium, little, and silent states.
- Tuner and spectrum buttons can open analyzer overlays above the rhythm editor.
- Edit Rhythm opens song/rhythm editing without leaving the watch workflow.
- The big ring pulse can be shown for all beats, big beats only, or turned off.

### Page 3: Settings

The settings page controls app behavior, audio feel, visuals, and language.

- Beep toggle controls audible metronome tone.
- Vibration toggle controls haptic pulse.
- Intensity opens a picker for big, medium, little, and silent accent ranges.
- A4 reference adjusts tuner pitch reference from 400 to 480 Hz.
- Diagnostics show app CPU usage for performance checks.
- Keep screen on offers App Open, Playing, and Timeout modes.
- Theme controls main color and background color.
- Tap ring controls the big ring color and flash mode.
- Clock controls watch/app clock hand color and clock image.
- Clock image choices include Rainb, Blue, Green, Orange, Purple, White, Munk, Sax, Piano, Gtr, Trum, and Rock.
- Language switches compact UI labels between English and Spanish.
- A right-side scroll indicator shows position on the settings page.

### Page 4: Tap Tempo

The tap tempo page is a dedicated tempo capture surface.

- Tap repeatedly to estimate BPM.
- Use -/+ for single BPM nudges.
- Long controls support larger 5 BPM changes.
- Start/stop is available from the same flow.
- The page keeps the large ring visual available without forcing that animation into every popup.

## Analyzer Overlays

Analyzer overlays sit above the current page so the user can dip into audio tools and return quickly.

### Tuner

- Shows detected note, frequency, cents, recent notes, guessed key, and likely chords.
- Supports listen profiles for different analysis behavior.
- `Save BPM` saves the detected tempo to the current playlist song.
- `Save Key` saves the guessed key to the current playlist song.
- This page stays song-focused and does not overwrite the watch-face study slot.

### Spectrum

- Shows spectrum bars, peak frequency, note band, detected BPM, and guessed key.
- Tapping a latest BPM/key complication on the watch face opens the app directly into this overlay.
- When both BPM and key are available, the `Clock` action appears at the top-left.
- `Clock` saves the latest BPM/key pair to the watch face study slot, requests a complication refresh, and returns to the watch face.
- This latest study save is separate from playlists and rewrites the same single slot every use.

## Playlist And Song Data

Playlists are saved inside the app package and are separate from the watch-face latest reading.

Each saved song can include:

- Song name.
- BPM.
- Beats per measure.
- Accent beat.
- Subdivision count.
- Per-beat accent types.
- Accent intensity mode.
- Musical key.
- Note.

## Watch Face Rundown

The `:watchface` module is a separate Watch Face Format package. It is intended to work alongside the app, not replace it.

- Package id: `bpm.munkz.pulse_wear.os.watchface`.
- Format version is declared in `watchface/src/main/AndroidManifest.xml`.
- Dial styles: all colors, blue, green, orange, purple, and white.
- Hand color configuration: green, white, blue, orange, and purple.
- Latest key complication sits on the left side of the top dial area.
- Latest BPM complication sits on the right side of the top dial area.
- Key and BPM are aligned to grow inward so they avoid covering the 11 and 1 areas.
- Tapping either latest-reading complication opens BPM Munkz Pulse into the spectrum workflow.
- The app publishes latest readings through:

```text
LatestBpmComplicationDataSourceService
LatestKeyComplicationDataSourceService
```

## Technical Specs

- App module: `:app`.
- Watch-face module: `:watchface`.
- App package id: `bpm.munkz.pulse_wear.os.metronome`.
- Watch-face package id: `bpm.munkz.pulse_wear.os.watchface`.
- BPM range: 30 to 240.
- Audio sample rate: 44,100 Hz.
- Audio frame size: 2,048 samples.
- Spectrum bar count: 28.
- A4 reference range: 400 to 480 Hz.
- Default A4 reference: 440 Hz.
- UI languages: English and Spanish.
- Wear OS tile: included in `BpmMunkzTileService`.
- Metronome timing: foreground service in `MetronomeService`.

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
bpm.munkz.pulse_wear.os.metronome
```

The app declares permissions for foreground service playback, notifications, microphone input, wake lock, and vibration. Microphone permission is used for tuner and spectrum analysis.

## Watch-Face Package

The watch-face package id is:

```text
bpm.munkz.pulse_wear.os.watchface
```

The app queries this package so it can help the user open/select the BPM Munkz Pulse watch face when installed.

The watch face can show latest BPM and latest key from the app via complication providers:

```text
bpm.munkz.pulse_wear.os.metronome.presentation.LatestBpmComplicationDataSourceService
bpm.munkz.pulse_wear.os.metronome.presentation.LatestKeyComplicationDataSourceService
```

If a fresh install still shows `--` or `...`, long-press the watch face and confirm that the two complication slots are assigned to `Latest BPM` and `Latest Key`. Some Wear OS/Samsung face instances keep old complication assignments after reinstalling a debug APK.

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
