package com.bammellab.musicplayer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bammellab.musicplayer.player.PlaybackState

@Composable
fun PlayerControls(
    playbackState: PlaybackState,
    shuffleEnabled: Boolean,
    volume: Float,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffleToggle: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    isCurrentTrackPlayable: Boolean = true
) {
    // Determine icon based on state and playability
    val isPlaying = playbackState == PlaybackState.PLAYING
    val playPauseIcon = when {
        !isCurrentTrackPlayable -> Icons.Filled.Block
        isPlaying -> Icons.Filled.Pause
        else -> Icons.Filled.PlayArrow
    }
    val playPauseDescription = when {
        !isCurrentTrackPlayable -> "Cannot play this format"
        isPlaying -> "Pause"
        else -> "Play"
    }

    if (isCompact) {
        // Single row layout for compact mode (landscape phone)
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Volume down
            IconButton(
                onClick = onVolumeDown,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                    contentDescription = "Volume down",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Shuffle
            IconButton(
                onClick = onShuffleToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = if (shuffleEnabled) "Disable shuffle" else "Enable shuffle",
                    tint = if (shuffleEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            // Previous
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous track",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Play/Pause - use key to force recomposition
            key(isPlaying, isCurrentTrackPlayable) {
                FilledIconButton(
                    onClick = onPlayPause,
                    enabled = isCurrentTrackPlayable,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = playPauseIcon,
                        contentDescription = playPauseDescription,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Next
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next track",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Shuffle placeholder for symmetry - use volume up instead
            IconButton(
                onClick = onVolumeUp,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Volume up",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    } else {
        // Two row layout for normal mode (portrait)
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShuffleToggle) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = if (shuffleEnabled) "Disable shuffle" else "Enable shuffle",
                        tint = if (shuffleEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous track",
                        modifier = Modifier.size(36.dp)
                    )
                }

                key(isPlaying, isCurrentTrackPlayable) {
                    FilledIconButton(
                        onClick = onPlayPause,
                        enabled = isCurrentTrackPlayable,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = playPauseIcon,
                            contentDescription = playPauseDescription,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next track",
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onVolumeDown) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                        contentDescription = "Volume down"
                    )
                }

                LinearProgressIndicator(
                    progress = { volume },
                    modifier = Modifier
                        .width(120.dp)
                        .height(4.dp),
                )

                IconButton(onClick = onVolumeUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Volume up"
                    )
                }

                Text(
                    text = "${(volume * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(40.dp)
                )
            }
        }
    }
}
