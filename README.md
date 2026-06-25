# Ringtone ID

A modern Android recreation of the classic **LG Ringtone ID** feature — generates unique musical ringtones for your contacts based on their phone number digits.

## Features

- **Unique ringtones per contact** — maps phone number digits to musical notes (pentatonic scale) to create a personalized melody
- **Multiple audio formats** — WAV, M4A (AAC), and MIDI output
- **38 MIDI instruments** — Piano, Guitar, Strings, Brass, Synth, and more with categorized picker
- **Configurable per contact** — seed variation (prev/next), note length (4–16), format, and instrument
- **Animated waveform visualizer** with preview playback
- **Bulk operations** — Generate All / Remove All with progress tracking
- **Full cleanup** — Remove All deletes audio files, clears contact assignments, and purges database records
- **Auto-generate on launch** — automatically assigns ringtones to new contacts when the app opens
- **Background sync** — daily or weekly WorkManager task for new contacts
- **Settings** — global defaults for format, instrument, length, and automation toggles
- **Sample button** in settings to audition your defaults with a random seed
- **Extensible** — JSON properties field for per-contact settings without DB migrations

## Tech Stack

- **Kotlin** with **Jetpack Compose** and **Material3** (dynamic color)
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
3. Maps digits 0–9 to musical notes (C4 through E5)
4. Applies an optional seed for variation and instrument-specific waveform shaping
5. Generates an audio file (WAV/M4A/MIDI) and saves to MediaStore
6. Sets as the contact's custom ringtone via ContactsContract

## Building

```bash
# Requires JDK 21 and Android SDK (platform 34, build-tools 34.0.0)
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Permissions

- `READ_CONTACTS` — to list contacts and their phone numbers
- `WRITE_CONTACTS` — to set custom ringtones on contacts
- `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` — for older Android versions

## License

[MIT](LICENSE)
