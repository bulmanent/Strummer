# Strummer (Bar-Based Practice Library)

Android practice app for manual guitar practice against user-imported local audio.

## Core Workflow
1. Open `Songs`.
2. Enter song title.
3. Tap `Add` and choose local audio (MP3 recommended).
4. Build a looped bar timeline:
   - set tempo + time signature
   - add steps (`Chord + Bars`)
5. Press `Play` and follow live chord prompts by bar.

Library starts empty by design.

## Features
- Local song library (user-imported audio files)
- Simplified song control box:
  - `Title`
  - `Add`
  - `Select`
  - `Play/Pause`
- Song metadata view + delete
- Bar-based chord timeline editor:
  - time signature (top editable, bottom fixed to 4)
  - tempo (BPM)
  - add/update/delete steps
  - each step = `Chord + Bars`
- Playback prompts:
  - current chord
  - next chord
  - current absolute bar + loop bar index
  - bars until next change
- Speed control `0.5x..1.25x` with quick buttons

## External Audio Download (example)
`yt-dlp` is external to this app:

```bash
yt-dlp -x --audio-format mp3 "<url>"
```

## Supported Audio Formats
- `.mp3` (primary target)
- `.m4a`
- `.aac`
- `.wav`
- `.ogg`

## Storage and Backup
- Metadata: `files/practice-library/library_state.json`
- Backup: `files/practice-library/library_state.json.bak`
- Imported audio: `files/practice-library/audio/`

## Notes on Timing
Bar prompts are estimated from:
- song playback position
- configured tempo
- configured time signature
- your looped step sequence

If prompts drift from the recording, adjust BPM/time signature/step bar lengths.

## Tests
Run:

```bash
./gradlew test
```
