package com.bammellab.musicplayer.cast

import android.net.Uri
import android.util.Log
import com.bammellab.musicplayer.data.model.AudioFile
import com.bammellab.musicplayer.player.PlaybackState
import com.bammellab.musicplayer.player.PlayerController
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PlayerController implementation for Chromecast playback.
 * Controls playback on the remote Cast device via RemoteMediaClient.
 */
class CastRemotePlayer(
    private val castSessionManager: CastSessionManager,
    private val audioStreamServer: AudioStreamServer
) : PlayerController {

    companion object {
        private const val TAG = "CastRemotePlayer"
    }

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    override val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0)
    override val duration: StateFlow<Int> = _duration.asStateFlow()

    private var onCompletionCallback: (() -> Unit)? = null
    private var currentAudioFile: AudioFile? = null

    private val remoteMediaClientCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            updateStateFromRemote()
        }

        override fun onMediaError(error: com.google.android.gms.cast.MediaError) {
            Log.e(TAG, "Media error: ${error.reason}")
            _playbackState.value = PlaybackState.ERROR
        }
    }

    private fun getRemoteMediaClient(): RemoteMediaClient? {
        return castSessionManager.getCurrentSession()?.remoteMediaClient
    }

    private fun updateStateFromRemote() {
        val client = getRemoteMediaClient() ?: return
        val mediaStatus = client.mediaStatus

        if (mediaStatus != null) {
            // Update duration
            val mediaDuration = mediaStatus.mediaInfo?.streamDuration ?: 0L
            _duration.value = mediaDuration.toInt()

            // Update position
            _currentPosition.value = client.approximateStreamPosition.toInt()

            // Update playback state
            val newState = when (mediaStatus.playerState) {
                MediaStatus.PLAYER_STATE_PLAYING -> PlaybackState.PLAYING
                MediaStatus.PLAYER_STATE_PAUSED -> PlaybackState.PAUSED
                MediaStatus.PLAYER_STATE_BUFFERING -> PlaybackState.PLAYING
                MediaStatus.PLAYER_STATE_IDLE -> {
                    if (mediaStatus.idleReason == MediaStatus.IDLE_REASON_FINISHED) {
                        onCompletionCallback?.invoke()
                        PlaybackState.STOPPED
                    } else {
                        PlaybackState.IDLE
                    }
                }
                else -> PlaybackState.IDLE
            }
            _playbackState.value = newState
        }
    }

    /**
     * Play an audio file on the Cast device.
     * Registers the file with the HTTP server and sends the URL to Cast.
     */
    fun playAudioFile(audioFile: AudioFile) {
        currentAudioFile = audioFile
        play(audioFile.uri)
    }

    override fun play(uri: Uri) {
        val client = getRemoteMediaClient()
        if (client == null) {
            Log.e(TAG, "No remote media client available")
            _playbackState.value = PlaybackState.ERROR
            return
        }

        // Register callback
        client.registerCallback(remoteMediaClientCallback)

        // Register file with HTTP server to get streamable URL
        val streamUrl = audioStreamServer.registerFile(uri)
        Log.d(TAG, "Streaming URL: $streamUrl")

        // Build media metadata
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            currentAudioFile?.let { file ->
                putString(MediaMetadata.KEY_TITLE, file.displayName)
            }
        }

        // Build media info
        val mediaInfo = MediaInfo.Builder(streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(getMimeType(uri))
            .setMetadata(metadata)
            .build()

        // Load and play
        val loadRequest = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setAutoplay(true)
            .build()

        client.load(loadRequest)
            .addStatusListener { }

        _playbackState.value = PlaybackState.PLAYING
    }

    private fun getMimeType(uri: Uri): String {
        val fileName = uri.lastPathSegment?.lowercase() ?: ""
        return when {
            fileName.endsWith(".mp3") -> "audio/mpeg"
            fileName.endsWith(".m4a") -> "audio/mp4"
            fileName.endsWith(".aac") -> "audio/aac"
            fileName.endsWith(".ogg") -> "audio/ogg"
            fileName.endsWith(".flac") -> "audio/flac"
            fileName.endsWith(".wav") -> "audio/wav"
            fileName.endsWith(".wma") -> "audio/x-ms-wma"
            fileName.endsWith(".opus") -> "audio/opus"
            else -> "audio/mpeg"
        }
    }

    override fun pause() {
        getRemoteMediaClient()?.pause()
        _playbackState.value = PlaybackState.PAUSED
    }

    override fun resume() {
        getRemoteMediaClient()?.play()
        _playbackState.value = PlaybackState.PLAYING
    }

    override fun stop() {
        getRemoteMediaClient()?.stop()
        _playbackState.value = PlaybackState.STOPPED
    }

    override fun seekTo(position: Int) {
        val seekOptions = MediaSeekOptions.Builder()
            .setPosition(position.toLong())
            .build()
        getRemoteMediaClient()?.seek(seekOptions)
        _currentPosition.value = position
    }

    override fun setVolume(volume: Float) {
        val session = castSessionManager.getCurrentSession()
        try {
            session?.setVolume(volume.toDouble())
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume", e)
        }
    }

    override fun updateCurrentPosition() {
        val client = getRemoteMediaClient() ?: return
        _currentPosition.value = client.approximateStreamPosition.toInt()
    }

    override fun setOnCompletionListener(listener: () -> Unit) {
        onCompletionCallback = listener
    }

    override fun release() {
        getRemoteMediaClient()?.unregisterCallback(remoteMediaClientCallback)
        _playbackState.value = PlaybackState.IDLE
        _currentPosition.value = 0
        _duration.value = 0
        currentAudioFile = null
    }
}
