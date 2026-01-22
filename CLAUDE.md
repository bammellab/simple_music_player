# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run specific unit test class
./gradlew testDebugUnitTest --tests "com.bammellab.musicplayer.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Clean and rebuild
./gradlew clean assembleDebug
```

## Architecture Overview

This is an Android music player app using Jetpack Compose and MVVM architecture with Chromecast support.

### Key Layers

- **ViewModel** (`MusicPlayerViewModel`): Central business logic hub. Manages UI state via `MusicPlayerUiState` data class and `StateFlow`. Handles folder selection, track playback coordination, shuffle logic, volume control, and player switching between local and cast playback.

- **Player Abstraction** (`PlayerController` interface): Defines common playback operations (play, pause, resume, stop, seek, volume). Two implementations:
  - `AudioPlayerManager`: Local MediaPlayer wrapper
  - `CastRemotePlayer`: Chromecast playback via Google Cast SDK

- **Cast System** (`cast/`): Components for Chromecast support:
  - `CastSessionManager`: Manages Cast sessions and device discovery
  - `CastRemotePlayer`: Implements `PlayerController` for remote playback
  - `AudioStreamServer`: NanoHTTPD server that streams audio files to Cast devices

- **UI** (`ui/`): Jetpack Compose components that observe ViewModel state. `MusicPlayerScreen` is the main composable, composed of `FileListView`, `NowPlayingView`, `PlayerControls`, and `CastButton`. Supports landscape layouts with side-by-side file list and player panel.

### Data Flow

1. User selects folder via SAF (Storage Access Framework) → `onFolderSelected()` parses audio files
2. User taps track → `playTrack()` → routes to `activePlayer` (local or cast) → state updates flow to UI
3. Track completion triggers `onCompletionCallback` → `playNext()` respects shuffle state
4. For Chromecast: audio files are served via HTTP to the Cast device

### State Management

- `MusicPlayerUiState` contains: audio files list, current track index, shuffle state, volume, folder info, loading/error states
- `CastUiState` contains: casting status, device name, connection state
- `PlaybackState` enum: IDLE, PLAYING, PAUSED, STOPPED, ERROR
- `activePlayer` property dynamically returns local or cast player based on `castState.isCasting`
- Position updates run on coroutine job every 500ms during playback

## Tech Stack

- Kotlin 2.3, Compose BOM 2026.01, Material 3
- Target SDK 36, Min SDK 24
- AndroidX DocumentFile for SAF file access
- Android MediaPlayer for local audio playback
- Google Cast SDK + NanoHTTPD for Chromecast streaming
- Coil for album art loading
