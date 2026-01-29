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
./gradlew testDebugUnitTest --tests "com.bammellab.musicplayer.release.ReleaseBuildTest"

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

- **ViewModel** (`MusicPlayerViewModel`): Central business logic hub. Manages UI state via `MusicPlayerUiState` data class and `StateFlow`. Handles folder browsing, track playback coordination, shuffle logic, volume control, and player switching between local and cast playback.

- **Data Layer** (`data/`):
  - `MediaStoreRepository`: Queries MediaStore for all audio files on device, groups by folder
  - `AudioFile`: Data class with uri, displayName, duration, albumId, albumArtUri, artist, album, folderPath
  - `MusicFolder`: Data class with path, displayName, trackCount, albumArtUri
  - `AlbumArtFetcher`: Custom Coil fetcher for album art (MediaStore URIs and embedded art)

- **Player Abstraction** (`PlayerController` interface): Defines common playback operations (play, pause, resume, stop, seek, volume). Two implementations:
  - `AudioPlayerManager`: Local MediaPlayer wrapper
  - `CastRemotePlayer`: Chromecast playback via Google Cast SDK

- **Cast System** (`cast/`): Components for Chromecast support:
  - `CastSessionManager`: Manages Cast sessions and device discovery
  - `CastRemotePlayer`: Implements `PlayerController` for remote playback
  - `AudioStreamServer`: NanoHTTPD server that streams audio files to Cast devices

- **UI** (`ui/`): Jetpack Compose components that observe ViewModel state. `MusicPlayerScreen` is the main composable with three modes:
  - Permission request UI (first launch)
  - Folder browser (`FolderListView`) showing all folders with music
  - Track list (`FileListView`) with `NowPlayingView`, `PlayerControls`, and `CastButton`
  - Supports landscape layouts with side-by-side file list and player panel

### Data Flow

1. App requests READ_MEDIA_AUDIO permission (API 33+) or READ_EXTERNAL_STORAGE (older)
2. On permission grant → `loadMediaStoreAudio()` queries MediaStore, groups files by folder
3. User browses folders → taps folder → `selectFolder()` loads tracks for that folder
4. User taps track → `playTrack()` → routes to `activePlayer` (local or cast) → state updates flow to UI
5. Track completion triggers `onCompletionCallback` → `playNext()` respects shuffle state
6. For Chromecast: audio files are served via HTTP to the Cast device

### State Management

- `MusicPlayerUiState` contains:
  - `allFolders`: List of MusicFolder from MediaStore
  - `audioFiles`: Tracks in selected folder
  - `currentTrackIndex`, `shuffleEnabled`, `volume`
  - `selectedFolderPath`, `selectedFolderName`
  - `showFolderBrowser`: Whether showing folder list or track list
  - `hasPermission`, `isLoading`, `errorMessage`
- `CastUiState` contains: casting status, device name, connection state
- `PlaybackState` enum: IDLE, PLAYING, PAUSED, STOPPED, ERROR
- `activePlayer` property dynamically returns local or cast player based on `castState.isCasting`
- Position updates run on coroutine job every 500ms during playback
- Last selected folder and track index persist across app restarts

### Album Art

Album art loading uses a priority chain:
1. MediaStore album art URI (`content://media/external/audio/albumart/<albumId>`)
2. Embedded art extracted via MediaMetadataRetriever
3. Fallback icon

`AlbumArtImage` composable handles this with `albumArtUri` and `uri` parameters.

## Tech Stack

- Kotlin 2.3, Compose BOM 2026.01, Material 3
- Target SDK 36, Min SDK 24
- MediaStore API for audio file discovery and album art
- Android MediaPlayer for local audio playback
- Google Cast SDK + NanoHTTPD for Chromecast streaming
- Coil for album art loading with custom `AlbumArtFetcher`
