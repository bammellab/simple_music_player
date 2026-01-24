package com.bammellab.musicplayer.data.model

import android.net.Uri

data class MusicFolder(
    val path: String,
    val displayName: String,
    val trackCount: Int,
    val albumArtUri: Uri? = null
)
