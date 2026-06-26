# Changelog

All notable changes to Ringtone ID are documented here.

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
