package com.example.musicplayer.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlaybackState {
    IDLE,
    PLAYING,
    PAUSED,
    STOPPED,
    ERROR
}

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration.asStateFlow()

    private var onCompletionCallback: (() -> Unit)? = null

    fun setOnCompletionListener(callback: () -> Unit) {
        onCompletionCallback = callback
    }

    fun play(uri: Uri) {
        release()

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            try {
                setDataSource(context, uri)
                prepareAsync()

                setOnPreparedListener { mp ->
                    _duration.value = mp.duration
                    mp.start()
                    _playbackState.value = PlaybackState.PLAYING
                }

                setOnCompletionListener {
                    _playbackState.value = PlaybackState.STOPPED
                    onCompletionCallback?.invoke()
                }

                setOnErrorListener { _, _, _ ->
                    _playbackState.value = PlaybackState.ERROR
                    true
                }
            } catch (e: Exception) {
                _playbackState.value = PlaybackState.ERROR
            }
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _playbackState.value = PlaybackState.PAUSED
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            if (_playbackState.value == PlaybackState.PAUSED) {
                it.start()
                _playbackState.value = PlaybackState.PLAYING
            }
        }
    }

    fun stop() {
        mediaPlayer?.let {
            it.stop()
            _playbackState.value = PlaybackState.STOPPED
        }
    }

    fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(clampedVolume, clampedVolume)
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        _currentPosition.value = position
    }

    fun updateCurrentPosition() {
        mediaPlayer?.let {
            if (_playbackState.value == PlaybackState.PLAYING) {
                _currentPosition.value = it.currentPosition
            }
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        _playbackState.value = PlaybackState.IDLE
        _currentPosition.value = 0
        _duration.value = 0
    }
}
