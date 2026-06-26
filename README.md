# Ringtone ID

[![Android CI](https://github.com/N6UDP/ringtoneid/actions/workflows/android.yml/badge.svg)](https://github.com/N6UDP/ringtoneid/actions/workflows/android.yml)

A modern Android recreation of the classic **LG Ringtone ID** feature — generates unique musical ringtones for your contacts based on their phone number digits.

## Features

- **Unique ringtones per contact** — maps phone number digits onto a chosen musical scale to create a personalized, in-key melody
- **Rich music engine** — musical styles/scales, key, tempo range with motion (steady/accelerate/decelerate/random), melodic contour, octave, motif repeat, articulation, and harmony
- **High-quality synthesis** — ADSR envelope, a subtle reverb tail, and peak normalization for a warm, consistent sound
- **Multiple audio formats** — WAV, M4A (AAC), and MIDI output
- **38 MIDI instruments** — Piano, Guitar, Strings, Brass, Synth, and more with a categorized picker
- **Preset pool** — a pool of named, weighted presets; bulk generation picks one at random per contact for variety
- **Per-contact history & favorites** — restore an earlier variation you liked, and save favorites to a dedicated screen
- **Share & set as default** — share a tone via the system share sheet, or set it as the default phone ringtone, notification, or alarm sound
- **Smart contact list** — multi-select subset generation, filter by All/Needs/Has tone, search by name or number, and inline ringtone playback
- **Animated waveform visualizer** with preview playback
- **Bulk operations** — Generate All / Remove All with progress tracking and full cleanup
- **Automation** — auto-generate on launch and a daily/weekly background sync (runs as a foreground service)
- **Extensible** — JSON properties field for per-contact settings without DB migrations

## Tech Stack

- **Kotlin** with **Jetpack Compose** and **Material3** (dark mode + Material You dynamic color)
- **Hilt** for dependency injection
- **Room** for database persistence
- **Compose Navigation** for screen routing
- **WorkManager** for background sync
- **MediaCodec** for AAC encoding
- **AudioTrack** for PCM preview, **MediaPlayer** for MIDI preview
- Clean MVVM architecture (domain/data/ui layers)
- Min SDK 26 (Android 8.0+), Target SDK 34

## How It Works

1. Reads your contacts (with permission)
2. Takes the digits of each contact's phone number
3. Maps each digit onto a scale degree in the chosen musical style and key
4. Applies an optional seed for variation plus contour, octave, tempo, articulation, and harmony
5. Generates an audio file (WAV/M4A/MIDI) and saves to MediaStore
6. Sets as the contact's custom ringtone via ContactsContract

## Building

```bash
# Requires JDK 17 and Android SDK (platform 34, build-tools 34.0.0)
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

Run the unit tests with:

```bash
./gradlew testDebugUnitTest
```

## Permissions

- `READ_CONTACTS` — to list contacts and their phone numbers
- `WRITE_CONTACTS` — to set custom ringtones on contacts
- `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` — for older Android versions
- `WRITE_SETTINGS` — to set the default phone ringtone / notification / alarm sound
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` / `POST_NOTIFICATIONS` — for the background sync notification

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## License

[MIT](LICENSE)
