package com.example.musicplayer.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.model.AudioFile
import com.example.musicplayer.player.AudioPlayerManager
import com.example.musicplayer.player.PlaybackState
import com.example.musicplayer.player.ShuffleTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MusicPlayerUiState(
    val audioFiles: List<AudioFile> = emptyList(),
    val currentTrackIndex: Int = -1,
    val shuffleEnabled: Boolean = false,
    val volume: Float = 0.5f,
    val selectedFolderUri: Uri? = null,
    val selectedFolderName: String = "No folder selected",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val currentTrack: AudioFile?
        get() = if (currentTrackIndex in audioFiles.indices) audioFiles[currentTrackIndex] else null

    val hasFiles: Boolean
        get() = audioFiles.isNotEmpty()
}

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val audioPlayerManager = AudioPlayerManager(application)
    private val shuffleTracker = ShuffleTracker(application)
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(MusicPlayerUiState())
    val uiState: StateFlow<MusicPlayerUiState> = _uiState.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = audioPlayerManager.playbackState
    val currentPosition: StateFlow<Int> = audioPlayerManager.currentPosition
    val duration: StateFlow<Int> = audioPlayerManager.duration

    private var positionUpdateJob: Job? = null

    private val audioExtensions = setOf(
        "mp3", "wav", "ogg", "m4a", "aac", "flac", "wma", "opus"
    )

    init {
        audioPlayerManager.setOnCompletionListener {
            playNext()
        }
        // Restore previously selected folder on startup
        restoreSavedFolder()
    }

    private fun restoreSavedFolder() {
        val savedUriString = prefs.getString(KEY_FOLDER_URI, null) ?: return

        try {
            val uri = savedUriString.toUri()

            // Check if we still have permission to access this URI
            val contentResolver = getApplication<Application>().contentResolver
            val persistedUris = contentResolver.persistedUriPermissions
            val hasPermission = persistedUris.any {
                it.uri == uri && it.isReadPermission
            }

            if (hasPermission) {
                // Reload the folder and restore saved state
                loadFolder(uri, restoreState = true)
            } else {
                // Permission lost, clear saved URI
                clearSavedFolder()
            }
        } catch (e: Exception) {
            clearSavedFolder()
        }
    }

    private fun saveFolder(uri: Uri) {
        prefs.edit().putString(KEY_FOLDER_URI, uri.toString()).apply()
    }

    private fun clearSavedFolder() {
        prefs.edit()
            .remove(KEY_FOLDER_URI)
            .remove(KEY_LAST_TRACK_INDEX)
            .apply()
    }

    private fun saveLastTrackIndex(index: Int) {
        prefs.edit().putInt(KEY_LAST_TRACK_INDEX, index).apply()
    }

    private fun getSavedTrackIndex(): Int {
        return prefs.getInt(KEY_LAST_TRACK_INDEX, -1)
    }

    private fun saveShuffleState(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHUFFLE_ENABLED, enabled).apply()
    }

    private fun getSavedShuffleState(): Boolean {
        return prefs.getBoolean(KEY_SHUFFLE_ENABLED, false)
    }

    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                // Take persistable permission for future app sessions
                val contentResolver = getApplication<Application>().contentResolver
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                // Save the folder URI for persistence
                saveFolder(uri)
            } catch (e: Exception) {
                // Permission may already be taken, continue loading
            }
            loadFolder(uri)
        }
    }

    private fun loadFolder(uri: Uri, restoreState: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val documentFile = DocumentFile.fromTreeUri(getApplication(), uri)
                val folderName = documentFile?.name ?: "Unknown Folder"

                val files = documentFile?.listFiles()
                    ?.filter { file ->
                        file.isFile &&
                        file.name != null &&
                        audioExtensions.any { ext ->
                            file.name!!.lowercase().endsWith(".$ext")
                        }
                    }
                    ?.map { file ->
                        AudioFile(
                            uri = file.uri,
                            displayName = file.name ?: "Unknown",
                            mimeType = file.type ?: "audio/*",
                            size = file.length()
                        )
                    }
                    ?.sortedBy { it.displayName.lowercase() }
                    ?: emptyList()

                // Restore saved state if requested
                val savedTrackIndex = if (restoreState) getSavedTrackIndex() else -1
                val savedShuffleState = if (restoreState) getSavedShuffleState() else false
                val trackIndex = if (savedTrackIndex in files.indices) savedTrackIndex else -1

                _uiState.value = _uiState.value.copy(
                    audioFiles = files,
                    selectedFolderUri = uri,
                    selectedFolderName = folderName,
                    currentTrackIndex = trackIndex,
                    shuffleEnabled = savedShuffleState,
                    isLoading = false
                )

                // Initialize shuffle tracker for folder (preserves played list if same folder)
                shuffleTracker.initialize(uri, files.size)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load folder: ${e.message}"
                )
            }
        }
    }

    fun playTrack(index: Int) {
        val files = _uiState.value.audioFiles
        if (index !in files.indices) return

        _uiState.value = _uiState.value.copy(currentTrackIndex = index)
        audioPlayerManager.play(files[index].uri)
        audioPlayerManager.setVolume(_uiState.value.volume)
        startPositionUpdates()

        // Save last played track for persistence
        saveLastTrackIndex(index)

        // Mark track as played for shuffle tracking
        if (_uiState.value.shuffleEnabled) {
            shuffleTracker.markPlayed(index)
        }
    }

    fun togglePlayPause() {
        when (playbackState.value) {
            PlaybackState.PLAYING -> {
                audioPlayerManager.pause()
                stopPositionUpdates()
            }
            PlaybackState.PAUSED -> {
                audioPlayerManager.resume()
                startPositionUpdates()
            }
            PlaybackState.STOPPED, PlaybackState.IDLE -> {
                val index = if (_uiState.value.currentTrackIndex >= 0) {
                    _uiState.value.currentTrackIndex
                } else {
                    0
                }
                if (_uiState.value.audioFiles.isNotEmpty()) {
                    playTrack(index)
                }
            }
            PlaybackState.ERROR -> {
                if (_uiState.value.currentTrackIndex >= 0) {
                    playTrack(_uiState.value.currentTrackIndex)
                }
            }
        }
    }

    fun playNext() {
        val files = _uiState.value.audioFiles
        if (files.isEmpty()) return

        val nextIndex = if (_uiState.value.shuffleEnabled) {
            // Check if all tracks have been played
            if (shuffleTracker.isAllPlayed()) {
                shuffleTracker.reset()
            }
            // Get next unplayed track
            shuffleTracker.getNextUnplayedIndex() ?: 0
        } else {
            (_uiState.value.currentTrackIndex + 1) % files.size
        }

        playTrack(nextIndex)
    }

    fun playPrevious() {
        val files = _uiState.value.audioFiles
        if (files.isEmpty()) return

        // Previous always goes sequentially (even in shuffle mode)
        val current = _uiState.value.currentTrackIndex
        val prevIndex = if (current > 0) current - 1 else files.size - 1

        playTrack(prevIndex)
    }

    fun toggleShuffle() {
        val newShuffleState = !_uiState.value.shuffleEnabled
        _uiState.value = _uiState.value.copy(shuffleEnabled = newShuffleState)

        // Save shuffle state for persistence
        saveShuffleState(newShuffleState)

        // Reset shuffle tracker when shuffle is turned on
        if (newShuffleState) {
            shuffleTracker.reset()
        }
    }

    fun volumeUp() {
        val newVolume = (_uiState.value.volume + 0.1f).coerceAtMost(1f)
        setVolume(newVolume)
    }

    fun volumeDown() {
        val newVolume = (_uiState.value.volume - 0.1f).coerceAtLeast(0f)
        setVolume(newVolume)
    }

    private fun setVolume(volume: Float) {
        _uiState.value = _uiState.value.copy(volume = volume)
        audioPlayerManager.setVolume(volume)
    }

    fun seekTo(position: Int) {
        audioPlayerManager.seekTo(position)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                audioPlayerManager.updateCurrentPosition()
                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPositionUpdates()
        audioPlayerManager.release()
    }

    companion object {
        private const val PREFS_NAME = "music_player_prefs"
        private const val KEY_FOLDER_URI = "selected_folder_uri"
        private const val KEY_LAST_TRACK_INDEX = "last_track_index"
        private const val KEY_SHUFFLE_ENABLED = "shuffle_enabled"
    }
}
