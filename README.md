# Strummer (MVP)

Android practice app for guitar chord + strumming practice.

## Stack
- Kotlin + Jetpack Compose (Material 3)
- Min SDK 26
- Local JSON assets (`songs.json`, `patterns.json`)
- DataStore (Preferences) for user settings/recent selections
- Audio engine built on `AudioTrack` (`MODE_STREAM`) with generated PCM clicks

## Run
1. Open project in Android Studio (Ladybug+ recommended).
2. Let Gradle sync.
3. Run `app` on emulator/device (API 26+).

## Features implemented
- Songs tab:
  - song + section picker
  - large current chord display
  - upcoming bars row
  - current strum pattern with playhead highlight
  - transport (play/pause)
  - tempo 40-160 (slider + -5/-1/+1/+5)
  - practice ramp controls (start/end/increment/bars per increment)
- Custom Practice tab:
  - custom chord sequence input (comma or whitespace separated)
  - beats per bar (3 or 4)
  - preset pattern selector
  - DSL pattern editor
  - play/pause
- Chord Library tab:
  - open chord grid (A, Am, C, D, Dm, E, Em, F, G)
  - chord diagram rendered in Compose Canvas

## Pattern DSL
Supported tokens:
- `D` down
- `U` up
- `X` mute
- `-` rest

Rules:
- separate steps by spaces, e.g. `D D U U D U`
- extra whitespace is ignored
- `-` is parsed as rest

## JSON assets
Location: `app/src/main/assets/`

### `patterns.json`
```json
{
  "patterns": [
    {
      "id": "basic_8_down_up",
      "name": "Basic 8th Down/Up",
      "subdivision": 8,
      "steps": [{ "kind": "D" }, { "kind": "-" }]
    }
  ]
}
```

### `songs.json`
```json
{
  "songs": [
    {
      "title": "Song Name",
      "sections": [
        {
          "name": "Verse",
          "bars": [
            { "chord": "G", "beatsPerBar": 4, "patternId": "basic_8_down_up" }
          ]
        }
      ]
    }
  ]
}
```

## Tests
Unit tests added for:
- Pattern DSL parsing
- Timing math for 8th/16th subdivision and beat/downbeat progression

Run tests from Android Studio test runner.
