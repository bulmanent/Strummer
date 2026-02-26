# Strummer (Local Practice Library)

Android practice app for manual guitar practice against user-imported local audio.

## Stack
- Kotlin + Jetpack Compose (Material 3)
- Min SDK 26
- Local persisted JSON library in app storage
- DataStore (Preferences) for user settings
- `MediaPlayer`-based audio playback with variable speed and pitch-preserving attempt

## Run
1. Open project in Android Studio (Ladybug+ recommended).
2. Let Gradle sync.
3. Run `app` on emulator/device (API 26+).

## Practice Library Workflow
1. Open the `Songs` tab.
2. In `Import Song`, set title/optional artist.
3. Tap `Pick MP3`, choose a local audio file.
4. Tap `Import`.
5. Add chord events manually (`timestampMs + chord + optional note`).
6. Play, seek, and practice with speed controls.
7. Optionally enable stepped speed mode (loop + auto-increment).

Library starts empty by design.

## External Audio Download (example)
`yt-dlp` is external to this app. Example command:

```bash
yt-dlp -x --audio-format mp3 "<url>"
```

## Supported Audio Formats
- `.mp3` (primary target)
- `.m4a`
- `.aac`
- `.wav`
- `.ogg`

Imported files are copied into app-managed storage.

## Storage and Backup
- Library metadata file:
  - `files/practice-library/library_state.json`
- Metadata backup file:
  - `files/practice-library/library_state.json.bak`
- Imported audio files:
  - `files/practice-library/audio/`

To back up practice data, copy the `practice-library` folder from app internal files.

## Features
- Manual song library (no bundled songbook required)
- Song metadata editor (title, artist, source path shown)
- Chord timeline CRUD:
  - add at current playback time
  - add at explicit timestamp
  - edit timestamp/chord/note
  - delete and auto-sort by time
- Chord cues while playing:
  - current chord
  - next chord and time-to-next
- Playback controls:
  - play/pause
  - seek
  - speed slider `0.5x` to `1.25x`
  - quick speed buttons (`0.6x` to `1.0x`)
- Stepped speed practice:
  - start speed
  - increment step
  - target speed
  - loops per speed
  - optional loop range
  - reset to start speed
- Auto-detect chords (beta):
  - optional async analysis on selected song audio
  - draft timestamped suggestions with confidence
  - review/edit/include-exclude before apply
  - accept all / accept selected / discard
  - merge into timeline (append or replace mode)

## Validation and Error Handling
- Import validates supported extension.
- Song/chord/practice profile input is validated before persistence.
- Missing/moved audio files are detected and surfaced with recovery guidance.
- Corrupt metadata falls back to backup file when available.
- Playback and import paths emit logs (`SongRepository`, `PlaybackService`, `PracticeLibraryStore`).
- Detection failures are non-blocking and keep manual workflow available.

## Auto-Detect Chords (Beta)
- What it does:
  - Generates draft chord-change suggestions (`timestamp + chord + confidence`) for review.
- What it does not do:
  - Does not detect strumming patterns.
  - Does not auto-apply suggestions without user confirmation.
- Accuracy expectations:
  - This is a first-pass heuristic detector; manual correction is expected.
  - Low-confidence runs show: `Low-confidence result. Please review before applying.`
- Performance:
  - Runs in background with progress updates and cancel action.
  - Large files take longer.

## Detection Troubleshooting
- `Audio file not found`: song path is stale; re-import the track.
- `Unsupported format`: use one of `.mp3`, `.m4a`, `.aac`, `.wav`, `.ogg`.
- `No chord suggestions produced`: try cleaner audio or shorter loop sections.
- `Chord detection failed`: detector error occurred; continue manual timeline editing.

## Known Limitations
- No strumming-pattern inference.
- Pitch-preserving speed depends on device playback capabilities.
- No instrumentation UI flow test is included yet; current coverage is unit-test focused.

## Tests
Unit tests cover:
- Chord event sorting and cue activation
- Validation rules for timestamps and stepped-speed config
- Stepped speed progression logic
- Persistence round-trip for song/chord/profile data
- Missing audio file behavior

Run:

```bash
./gradlew test
```
