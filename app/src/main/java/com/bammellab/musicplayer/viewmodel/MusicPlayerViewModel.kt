package com.bammellab.musicplayer.viewmodel

import android.app.Application
import android.content.Context
import com.bammellab.musicplayer.cast.AudioStreamServer
import com.bammellab.musicplayer.cast.CastRemotePlayer
import com.bammellab.musicplayer.cast.CastSessionManager
import com.bammellab.musicplayer.cast.CastUiState
import com.bammellab.musicplayer.data.model.AudioFile
import com.bammellab.musicplayer.data.model.MusicFolder
import com.bammellab.musicplayer.data.repository.MediaStoreRepository
import com.bammellab.musicplayer.player.AudioPlayerManager
import com.bammellab.musicplayer.player.PlaybackState
import com.bammellab.musicplayer.player.PlayerController
import com.bammellab.musicplayer.player.ShuffleTracker
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MusicPlayerUiState(
    val audioFiles: List<AudioFile> = emptyList(),
    val currentTrackIndex: Int = -1,
    val shuffleEnabled: Boolean = false,
    val volume: Float = 0.5f,
    val selectedFolderPath: String? = null,
    val selectedFolderName: String = "No folder selected",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val allFolders: List<MusicFolder> = emptyList(),
    val showFolderBrowser: Boolean = true,
    val hasPermission: Boolean = false
) {
    val currentTrack: AudioFile?
        get() = if (currentTrackIndex in audioFiles.indices) audioFiles[currentTrackIndex] else null

    val hasFiles: Boolean
        get() = audioFiles.isNotEmpty()
}

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    // Local player
    private val localPlayer = AudioPlayerManager(application)

    // Cast components
    val castSessionManager = CastSessionManager(application)
    private val audioStreamServer = AudioStreamServer(application)
    private val castPlayer = CastRemotePlayer(castSessionManager, audioStreamServer)

    // Cast state for UI
    val castState: StateFlow<CastUiState> = castSessionManager.castState

    // MediaStore repository
    private val mediaStoreRepository = MediaStoreRepository(application)
    private var allFilesMap: Map<String, List<AudioFile>> = emptyMap()

    private val shuffleTracker = ShuffleTracker(application)
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(MusicPlayerUiState())
    val uiState: StateFlow<MusicPlayerUiState> = _uiState.asStateFlow()

    // Active player switches between local and cast
    private val activePlayer: PlayerController
        get() = if (castState.value.isCasting) castPlayer else localPlayer

    // Playback state from active player
    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration.asStateFlow()

    private var positionUpdateJob: Job? = null
    private var castStateObserverJob: Job? = null

    init {
        // Set completion listeners for both players
        localPlayer.setOnCompletionListener { playNext() }
        castPlayer.setOnCompletionListener { playNext() }

        // Observe cast state changes
        startCastStateObserver()
    }

    private fun startCastStateObserver() {
        castStateObserverJob = viewModelScope.launch {
            castState.collect { state ->
                if (state.isCasting) {
                    // Started casting - start HTTP server
                    startStreamServer()
                } else {
                    // Stopped casting - stop HTTP server
                    stopStreamServer()
                }
            }
        }
    }

    private fun startStreamServer() {
        try {
            if (!audioStreamServer.isAlive) {
                audioStreamServer.start()
            }
        } catch (e: Exception) {
            // Server may already be running
        }
    }

    private fun stopStreamServer() {
        try {
            audioStreamServer.stop()
            audioStreamServer.clearRegisteredFiles()
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasPermission = granted)
        if (granted) {
            loadMediaStoreAudio()
        }
    }

    fun loadMediaStoreAudio() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val result = withContext(Dispatchers.IO) {
                    mediaStoreRepository.queryAudioFiles()
                }

                allFilesMap = result.allFiles

                val savedFolderPath = getSavedFolderPath()
                val savedShuffleState = getSavedShuffleState()

                // Check if saved folder still exists
                val shouldRestoreFolder = savedFolderPath != null &&
                        result.folders.any { it.path == savedFolderPath }

                _uiState.value = _uiState.value.copy(
                    allFolders = result.folders,
                    isLoading = false,
                    hasPermission = true,
                    showFolderBrowser = !shouldRestoreFolder,
                    shuffleEnabled = savedShuffleState
                )

                // Restore saved folder if it exists
                if (shouldRestoreFolder) {
                    val folder = result.folders.first { it.path == savedFolderPath }
                    selectFolder(folder, restoreState = true)
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load music: ${e.message}"
                )
            }
        }
    }

    fun selectFolder(folder: MusicFolder, restoreState: Boolean = false) {
        viewModelScope.launch {
            // Stop current playback when changing folders
            stopPositionUpdates()
            activePlayer.stop()
            _playbackState.value = PlaybackState.IDLE
            _currentPosition.value = 0
            _duration.value = 0

            val files = mediaStoreRepository.getFilesForFolder(folder.path, allFilesMap)

            // Restore saved state if requested, otherwise select first track
            val savedTrackIndex = if (restoreState) getSavedTrackIndex() else -1
            val trackIndex = when {
                savedTrackIndex in files.indices -> savedTrackIndex
                files.isNotEmpty() -> 0
                else -> -1
            }

            _uiState.value = _uiState.value.copy(
                audioFiles = files,
                selectedFolderPath = folder.path,
                selectedFolderName = folder.displayName,
                currentTrackIndex = trackIndex,
                showFolderBrowser = false
            )

            // Save selected folder
            saveFolderPath(folder.path)

            // Initialize shuffle tracker for folder
            shuffleTracker.initialize(null, files.size)
        }
    }

    fun showFolderBrowser() {
        // Stop playback when going back to folder browser
        stopPositionUpdates()
        activePlayer.stop()
        _playbackState.value = PlaybackState.IDLE
        _currentPosition.value = 0
        _duration.value = 0

        _uiState.value = _uiState.value.copy(
            showFolderBrowser = true,
            audioFiles = emptyList(),
            currentTrackIndex = -1
        )
    }

    private fun saveFolderPath(path: String) {
        prefs.edit().putString(KEY_FOLDER_PATH, path).apply()
    }

    private fun getSavedFolderPath(): String? {
        return prefs.getString(KEY_FOLDER_PATH, null)
    }

    private fun clearSavedFolder() {
        prefs.edit()
            .remove(KEY_FOLDER_PATH)
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

    fun playTrack(index: Int) {
        val files = _uiState.value.audioFiles
        if (index !in files.indices) return

        _uiState.value = _uiState.value.copy(currentTrackIndex = index)

        val audioFile = files[index]

        if (castState.value.isCasting) {
            // Play on Chromecast
            castPlayer.playAudioFile(audioFile)
        } else {
            // Play locally
            localPlayer.play(audioFile.uri)
        }

        activePlayer.setVolume(_uiState.value.volume)
        startPositionUpdates()

        // Save last played track for persistence
        saveLastTrackIndex(index)

        // Mark track as played for shuffle tracking
        if (_uiState.value.shuffleEnabled) {
            shuffleTracker.markPlayed(index)
        }
    }

    fun togglePlayPause() {
        when (_playbackState.value) {
            PlaybackState.PLAYING -> {
                activePlayer.pause()
                stopPositionUpdates()
                _playbackState.value = PlaybackState.PAUSED
            }
            PlaybackState.PAUSED -> {
                activePlayer.resume()
                startPositionUpdates()
                _playbackState.value = PlaybackState.PLAYING
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
        activePlayer.setVolume(volume)
    }

    fun seekTo(position: Int) {
        activePlayer.seekTo(position)
        // Update UI immediately (important when paused, since position updates aren't running)
        _currentPosition.value = position
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                // Update from active player
                activePlayer.updateCurrentPosition()
                _playbackState.value = activePlayer.playbackState.value
                _currentPosition.value = activePlayer.currentPosition.value
                _duration.value = activePlayer.duration.value
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
        castStateObserverJob?.cancel()
        localPlayer.release()
        castPlayer.release()
        castSessionManager.release()
        stopStreamServer()
    }

    companion object {
        private const val PREFS_NAME = "music_player_prefs"
        private const val KEY_FOLDER_PATH = "selected_folder_path"
        private const val KEY_LAST_TRACK_INDEX = "last_track_index"
        private const val KEY_SHUFFLE_ENABLED = "shuffle_enabled"
    }
}
