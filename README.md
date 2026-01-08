# Music Player

A simple Android music player that plays audio files from a selected folder.

![Screenshot](./docs/screenshots/screenshot.png)

## Features

- Browse and select folders using Android's Storage Access Framework
- Play MP3, WAV, OGG, FLAC, M4A, and other common audio formats
- Playback controls: play/pause, previous/next track, volume up/down
- Shuffle mode with auto-scroll to current track
- Seek bar for track position
- Now Playing view with current track info

## Build

```bash
./gradlew assembleDebug
```

## Tech Stack

- Kotlin
- Jetpack Compose with Material 3
- Android MediaPlayer API
- ViewModel with StateFlow
