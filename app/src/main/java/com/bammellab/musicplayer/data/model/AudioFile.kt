package com.bammellab.musicplayer.data.model

import android.net.Uri

data class AudioFile(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val size: Long,
    val duration: Long = 0L,
    val albumId: Long = 0L,
    val albumArtUri: Uri? = null,
    val artist: String = "",
    val album: String = "",
    val folderPath: String = ""
) {
    val formattedDuration: String
        get() {
            if (duration <= 0) return "--:--"
            val minutes = duration / 1000 / 60
            val seconds = (duration / 1000) % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    /**
     * Check if this audio file can be played by Android's MediaPlayer.
     * Some formats like WMA and DRM-protected M4P files are not supported.
     */
    val isPlayable: Boolean
        get() {
            val lowerName = displayName.lowercase()
            val lowerMime = mimeType.lowercase()

            // Check for unplayable extensions
            if (lowerName.endsWith(".wma") ||
                lowerName.endsWith(".m4p") ||
                lowerName.endsWith(".asf")) {
                return false
            }

            // Check for unplayable MIME types
            if (lowerMime.contains("x-ms-wma") ||
                lowerMime.contains("x-ms-asf")) {
                return false
            }

            return true
        }
}
