package com.example.musicplayer.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.model.AudioFile
import com.example.musicplayer.player.AudioPlayerManager
import com.example.musicplayer.player.PlaybackState
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

    private val _uiState = MutableStateFlow(MusicPlayerUiState())
    val uiState: StateFlow<MusicPlayerUiState> = _uiState.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = audioPlayerManager.playbackState
    val currentPosition: StateFlow<Int> = audioPlayerManager.currentPosition
    val duration: StateFlow<Int> = audioPlayerManager.duration

    private var shuffledIndices: List<Int> = emptyList()
    private var currentShufflePosition: Int = 0
    private var positionUpdateJob: Job? = null

    private val audioExtensions = setOf(
        "mp3", "wav", "ogg", "m4a", "aac", "flac", "wma", "opus"
    )

    init {
        audioPlayerManager.setOnCompletionListener {
            playNext()
        }
    }

    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val contentResolver = getApplication<Application>().contentResolver
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

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

                _uiState.value = _uiState.value.copy(
                    audioFiles = files,
                    selectedFolderUri = uri,
                    selectedFolderName = folderName,
                    currentTrackIndex = -1,
                    isLoading = false
                )

                regenerateShuffleOrder()

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

        if (_uiState.value.shuffleEnabled) {
            currentShufflePosition = shuffledIndices.indexOf(index)
            if (currentShufflePosition == -1) currentShufflePosition = 0
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
            currentShufflePosition = (currentShufflePosition + 1) % shuffledIndices.size
            shuffledIndices[currentShufflePosition]
        } else {
            (_uiState.value.currentTrackIndex + 1) % files.size
        }

        playTrack(nextIndex)
    }

    fun playPrevious() {
        val files = _uiState.value.audioFiles
        if (files.isEmpty()) return

        val prevIndex = if (_uiState.value.shuffleEnabled) {
            currentShufflePosition = if (currentShufflePosition > 0) {
                currentShufflePosition - 1
            } else {
                shuffledIndices.size - 1
            }
            shuffledIndices[currentShufflePosition]
        } else {
            val current = _uiState.value.currentTrackIndex
            if (current > 0) current - 1 else files.size - 1
        }

        playTrack(prevIndex)
    }

    fun toggleShuffle() {
        val newShuffleState = !_uiState.value.shuffleEnabled
        _uiState.value = _uiState.value.copy(shuffleEnabled = newShuffleState)

        if (newShuffleState) {
            regenerateShuffleOrder()
        }
    }

    private fun regenerateShuffleOrder() {
        val files = _uiState.value.audioFiles
        shuffledIndices = files.indices.shuffled()
        currentShufflePosition = 0
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
}
