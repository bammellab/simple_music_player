package com.bammellab.musicplayer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bammellab.musicplayer.data.model.AudioFile
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip

@Composable
fun NowPlayingView(
    currentTrack: AudioFile?,
    currentPosition: Int,
    duration: Int,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val albumArtSize: Dp = if (isCompact) 56.dp else 120.dp

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large
    ) {
        if (isCompact) {
            // Horizontal layout for compact mode (landscape phone)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtImage(
                    uri = currentTrack?.uri,
                    albumArtUri = currentTrack?.albumArtUri,
                    size = albumArtSize,
                    fallbackIcon = Icons.Filled.Album,
                    showBackground = true,
                    modifier = Modifier.clip(MaterialTheme.shapes.medium)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = currentTrack?.displayName ?: "No track selected",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val hasValidDuration = duration > 0
                    Slider(
                        value = if (hasValidDuration) currentPosition.toFloat() else 0f,
                        onValueChange = { if (hasValidDuration) onSeek(it.toInt()) },
                        valueRange = 0f..(if (hasValidDuration) duration.toFloat() else 1f),
                        enabled = hasValidDuration,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatTime(duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Vertical layout for normal mode (portrait)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AlbumArtImage(
                    uri = currentTrack?.uri,
                    albumArtUri = currentTrack?.albumArtUri,
                    size = albumArtSize,
                    fallbackIcon = Icons.Filled.Album,
                    showBackground = true,
                    modifier = Modifier.clip(MaterialTheme.shapes.medium)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentTrack?.displayName ?: "No track selected",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                val hasValidDuration = duration > 0
                Slider(
                    value = if (hasValidDuration) currentPosition.toFloat() else 0f,
                    onValueChange = { if (hasValidDuration) onSeek(it.toInt()) },
                    valueRange = 0f..(if (hasValidDuration) duration.toFloat() else 1f),
                    enabled = hasValidDuration,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Int): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
