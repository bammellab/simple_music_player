# Music Player Application Requirements

## Overview
An Android application that allows users to navigate to a folder in the Android file system and play audio files contained within that folder.

## Functional Requirements

### 1. Folder Navigation
- User can navigate to and select a folder in the Android file system
- Application reads audio files from the selected folder
- No subfolder traversal required (flat folder structure assumed)

### 2. Audio Playback
- Play audio files from the selected folder
- Support common audio formats (MP3, WAV, OGG, FLAC, M4A, etc.)
- Sequential playback through the file list

### 3. Playback Controls
- **Play**: Start or resume audio playback
- **Pause**: Pause current playback
- **Volume Up**: Increase playback volume
- **Volume Down**: Decrease playback volume

### 4. Shuffle Feature
- Randomize the play order of audio files
- Toggle shuffle mode on/off

### 5. User Interface
- **File List View**: Display all audio files in the selected folder
- **Now Playing View**: Sub-view showing the currently playing file
- Intuitive control buttons for playback management

## Technical Constraints
- Target Platform: Android
- Base Template: Hello World Android app (Jetpack Compose)
- Development Branch: `claudeopus45`

## Non-Functional Requirements
- Responsive UI that works on various screen sizes
- Efficient memory usage for file listing
- Smooth playback without stuttering

## Implementation Status

All core requirements have been implemented:
- Folder navigation via Storage Access Framework (SAF)
- Audio playback using MediaPlayer
- Play/Pause, Volume Up/Down controls
- Shuffle feature with toggle
- File list view with current track highlighting
- Now Playing view with track name and progress slider
- Previous/Next track navigation (bonus feature)
- Seek bar for track position (bonus feature)

### Architecture
- **UI Framework**: Jetpack Compose with Material 3
- **State Management**: ViewModel with StateFlow
- **Audio**: Android MediaPlayer API
- **File Access**: DocumentFile with SAF

### Project Structure
```
app/src/main/java/com/example/musicplayer/
├── MainActivity.kt                    # Entry point
├── data/model/
│   └── AudioFile.kt                   # Data model
├── player/
│   └── AudioPlayerManager.kt          # MediaPlayer wrapper
├── ui/
│   ├── components/
│   │   ├── FileListView.kt            # Track list
│   │   ├── NowPlayingView.kt          # Current track display
│   │   └── PlayerControls.kt          # Playback controls
│   ├── screens/
│   │   └── MusicPlayerScreen.kt       # Main screen
│   └── theme/                         # Material theme
└── viewmodel/
    └── MusicPlayerViewModel.kt        # Business logic
```

## Future Considerations (Out of Scope for Initial Version)
- Background playback service with notification controls
- Playlist saving
- Album art display from audio metadata
- Subfolder support
- Repeat modes (single, all, none)
- Audio focus handling
