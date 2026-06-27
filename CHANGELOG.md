# Changelog

All notable changes to Ringtone ID are documented here.

## [1.2] — 2026-06-26

### Added
- **Genre starter presets** — add ready-made styles (Chiptune, Synthwave, Jazz, Rock,
  Classical, Classic Cell Ringer, Ambient, Blues, Music Box) from Settings, then tweak
  them like any pool preset.
- **Wave & Swing tempo motion** — tempo can now breathe up and down (Wave, a sine LFO)
  or gallop slow/fast (Swing), in addition to Steady / Speed up / Slow down / Random.
- **Numeric tempo & length entry** — type exact Min/Max BPM and note count alongside
  the sliders.

### Changed
- **Default output format is now MIDI** for a noticeably better on-device sound.
- **Warmer non-MIDI timbres** — instruments are rebuilt from band-limited additive
  partials, reducing the harsh/aliased buzz of the old square/saw/pulse shapes.

### Fixed
- Contacts now reflect a saved ringtone immediately (the list observes ringtone state
  live instead of taking a one-shot snapshot).
- The preview play button resets to ▶ when playback finishes (detail screen, in-list
  preview, and Settings sample).
- Pressing Back while multi-selecting contacts cancels the selection instead of
  exiting the app.

## [1.1] — 2026-06-26

### Added
- **Preset pool** — maintain a pool of named, weighted generation presets; bulk
  generation (Generate All, on-launch, background sync) picks one at random per
  contact for variety instead of identical settings everywhere.
- **Per-contact variation history** — tunes you audition or set are recorded so you
  can restore an earlier variation; deduped and capped, with favorites preserved.
- **Favorites** — save variations you love and manage them on a dedicated screen
  (play, set as ringtone, unfavorite).
- **Share & set as default** — share a generated tone via the system share sheet, or
  set it as your default phone ringtone, notification, or alarm sound.
- **Contact multi-select** — pick a subset of contacts and generate for just them;
  filter by All / Needs tone / Has tone; search by name or phone number; play an
  assigned ringtone inline from the list.
- **Richer music engine** — musical styles/scales, key, tempo range with motion,
  melodic contour, octave, motif repeat, articulation, and harmony.

### Changed
- **Higher-quality synthesis** — ADSR amplitude envelope, a subtle reverb tail, and
  whole-tune peak normalization for a warmer, more consistent sound.
- Background sync now runs as a foreground service with a progress notification so
  long bulk runs aren't killed mid-way.

### Developer
- JVM unit tests for note generation, PCM sizing, and settings/preset/variation JSON
  round-trips.
- GitHub Actions CI that runs unit tests and builds the debug APK on every push/PR.

## [1.0]

- Initial release: per-contact musical ringtones from phone digits, multiple audio
  formats (WAV/M4A/MIDI), 38 GM instruments, preview with waveform visualizer,
  bulk generate/remove, auto-generate on launch, and daily/weekly background sync.
