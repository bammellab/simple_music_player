package com.bammellab.musicplayer.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.bammellab.musicplayer.data.AlbumArtRequest

/**
 * A composable that displays album art from an audio file URI.
 * Shows a fallback icon if no album art is available or during loading.
 *
 * @param uri The URI of the audio file to extract embedded album art from
 * @param albumArtUri Optional MediaStore album art URI (takes priority over embedded art)
 * @param modifier Modifier for the component
 * @param size The size of the image/icon
 * @param fallbackIcon Icon to show when no album art is available
 * @param showBackground Whether to show a background behind the fallback icon
 */
@Composable
fun AlbumArtImage(
    uri: Uri?,
    modifier: Modifier = Modifier,
    albumArtUri: Uri? = null,
    size: Dp = 48.dp,
    fallbackIcon: ImageVector = Icons.Filled.MusicNote,
    showBackground: Boolean = true
) {
    // Priority: albumArtUri (MediaStore) → uri (embedded) → fallback
    val imageUri = albumArtUri ?: uri

    // Wrap in fixed-size Box to prevent layout shifts during loading/transitions
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (imageUri == null) {
            FallbackIcon(
                icon = fallbackIcon,
                size = size,
                showBackground = showBackground
            )
        } else {
            SubcomposeAsyncImage(
                model = AlbumArtRequest(imageUri),
                contentDescription = "Album art",
                modifier = Modifier
                    .size(size)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop,
                loading = {
                    FallbackIcon(icon = fallbackIcon, size = size, showBackground = showBackground)
                },
                error = {
                    // If albumArtUri failed, try embedded art from uri
                    if (albumArtUri != null && uri != null) {
                        SubcomposeAsyncImage(
                            model = AlbumArtRequest(uri),
                            contentDescription = "Album art",
                            modifier = Modifier
                                .size(size)
                                .clip(MaterialTheme.shapes.small),
                            contentScale = ContentScale.Crop,
                            loading = {
                                FallbackIcon(icon = fallbackIcon, size = size, showBackground = showBackground)
                            },
                            error = {
                                FallbackIcon(icon = fallbackIcon, size = size, showBackground = showBackground)
                            }
                        )
                    } else {
                        FallbackIcon(icon = fallbackIcon, size = size, showBackground = showBackground)
                    }
                }
            )
        }
    }
}

@Composable
private fun FallbackIcon(
    icon: ImageVector,
    size: Dp,
    showBackground: Boolean,
    modifier: Modifier = Modifier
) {
    if (showBackground) {
        Surface(
            modifier = modifier.size(size),
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(size * 0.5f),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    } else {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = modifier.size(size),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
