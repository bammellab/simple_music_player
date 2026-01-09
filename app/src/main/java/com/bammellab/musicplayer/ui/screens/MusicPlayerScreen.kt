package com.bammellab.musicplayer.ui.screens

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bammellab.musicplayer.data.model.AudioFile
import com.bammellab.musicplayer.player.PlaybackState
import com.bammellab.musicplayer.ui.components.CastButton
import com.bammellab.musicplayer.ui.components.FileListView
import com.bammellab.musicplayer.ui.components.MusicLoadingIndicator
import com.bammellab.musicplayer.ui.components.NowPlayingView
import com.bammellab.musicplayer.ui.components.PlayerControls
import com.bammellab.musicplayer.util.StorageHelper
import com.bammellab.musicplayer.util.StorageOption
import com.bammellab.musicplayer.viewmodel.MusicPlayerViewModel
import com.bammellab.musicplayer.viewmodel.MusicPlayerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    viewModel: MusicPlayerViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val castState by viewModel.castState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showStorageDialog by remember { mutableStateOf(false) }
    val storageOptions = remember { StorageHelper.getStorageOptions(context) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.onFolderSelected(it) }
    }

    val launchFolderPicker: (Uri?) -> Unit = { initialUri ->
        folderPickerLauncher.launch(initialUri)
    }

    val onSelectFolder: () -> Unit = {
        if (storageOptions.size > 1) {
            showStorageDialog = true
        } else {
            launchFolderPicker(storageOptions.firstOrNull()?.initialUri)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val fileListState = rememberLazyListState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.currentTrackIndex) {
        if (uiState.currentTrackIndex >= 0) {
            val viewportHeight = fileListState.layoutInfo.viewportEndOffset -
                    fileListState.layoutInfo.viewportStartOffset
            fileListState.animateScrollToItem(
                index = uiState.currentTrackIndex,
                scrollOffset = -viewportHeight / 2
            )
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Music Player")
                        if (castState.isCasting) {
                            Text(
                                text = "Casting to ${castState.castDeviceName ?: "device"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // Cast button - shows when Cast devices are available
                    CastButton(modifier = Modifier.size(48.dp))

                    IconButton(
                        onClick = onSelectFolder
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = "Select folder"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                MusicLoadingIndicator(
                    message = "Loading music files..."
                )
            }
        } else {
            if (isLandscape) {
                LandscapeLayout(
                    uiState = uiState,
                    playbackState = playbackState,
                    currentPosition = currentPosition,
                    duration = duration,
                    fileListState = fileListState,
                    onSelectFolder = onSelectFolder,
                    onTrackSelected = { viewModel.playTrack(it) },
                    onSeek = { viewModel.seekTo(it) },
                    onPlayPause = { viewModel.togglePlayPause() },
                    onPrevious = { viewModel.playPrevious() },
                    onNext = { viewModel.playNext() },
                    onShuffleToggle = { viewModel.toggleShuffle() },
                    onVolumeUp = { viewModel.volumeUp() },
                    onVolumeDown = { viewModel.volumeDown() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            } else {
                PortraitLayout(
                    uiState = uiState,
                    playbackState = playbackState,
                    currentPosition = currentPosition,
                    duration = duration,
                    fileListState = fileListState,
                    onSelectFolder = onSelectFolder,
                    onTrackSelected = { viewModel.playTrack(it) },
                    onSeek = { viewModel.seekTo(it) },
                    onPlayPause = { viewModel.togglePlayPause() },
                    onPrevious = { viewModel.playPrevious() },
                    onNext = { viewModel.playNext() },
                    onShuffleToggle = { viewModel.toggleShuffle() },
                    onVolumeUp = { viewModel.volumeUp() },
                    onVolumeDown = { viewModel.volumeDown() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }

    if (showStorageDialog) {
        StorageSelectionDialog(
            storageOptions = storageOptions,
            onStorageSelected = { option ->
                showStorageDialog = false
                launchFolderPicker(option.initialUri)
            },
            onDismiss = { showStorageDialog = false }
        )
    }
}

@Composable
private fun StorageSelectionDialog(
    storageOptions: List<StorageOption>,
    onStorageSelected: (StorageOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Storage") },
        text = {
            Column {
                storageOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStorageSelected(option) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (option.isPrimary) Icons.Filled.PhoneAndroid else Icons.Filled.SdCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = option.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PortraitLayout(
    uiState: MusicPlayerUiState,
    playbackState: PlaybackState,
    currentPosition: Int,
    duration: Int,
    fileListState: LazyListState,
    onSelectFolder: () -> Unit,
    onTrackSelected: (Int) -> Unit,
    onSeek: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffleToggle: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = uiState.selectedFolderName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (!uiState.hasFiles) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Select a folder to browse audio files",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onSelectFolder) {
                        Text("Select Folder")
                    }
                }
            }
        } else {
            FileListView(
                audioFiles = uiState.audioFiles,
                currentTrackIndex = uiState.currentTrackIndex,
                onTrackSelected = onTrackSelected,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                listState = fileListState
            )

            HorizontalDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                NowPlayingView(
                    currentTrack = uiState.currentTrack,
                    currentPosition = currentPosition,
                    duration = duration,
                    onSeek = onSeek
                )

                Spacer(modifier = Modifier.height(16.dp))

                PlayerControls(
                    playbackState = playbackState,
                    shuffleEnabled = uiState.shuffleEnabled,
                    volume = uiState.volume,
                    onPlayPause = onPlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onShuffleToggle = onShuffleToggle,
                    onVolumeUp = onVolumeUp,
                    onVolumeDown = onVolumeDown
                )
            }
        }
    }
}

@Composable
private fun LandscapeLayout(
    uiState: MusicPlayerUiState,
    playbackState: PlaybackState,
    currentPosition: Int,
    duration: Int,
    fileListState: LazyListState,
    onSelectFolder: () -> Unit,
    onTrackSelected: (Int) -> Unit,
    onSeek: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffleToggle: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!uiState.hasFiles) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Select a folder to browse audio files",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onSelectFolder) {
                    Text("Select Folder")
                }
            }
        }
    } else {
        Row(modifier = modifier) {
            // Left side - File list
            Column(modifier = Modifier.weight(0.5f)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = uiState.selectedFolderName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                FileListView(
                    audioFiles = uiState.audioFiles,
                    currentTrackIndex = uiState.currentTrackIndex,
                    onTrackSelected = onTrackSelected,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    listState = fileListState
                )
            }

            // Vertical divider between sections
            VerticalDivider(modifier = Modifier.fillMaxHeight())

            // Right side - Player controls
            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                NowPlayingView(
                    currentTrack = uiState.currentTrack,
                    currentPosition = currentPosition,
                    duration = duration,
                    onSeek = onSeek,
                    isCompact = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                PlayerControls(
                    playbackState = playbackState,
                    shuffleEnabled = uiState.shuffleEnabled,
                    volume = uiState.volume,
                    onPlayPause = onPlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onShuffleToggle = onShuffleToggle,
                    onVolumeUp = onVolumeUp,
                    onVolumeDown = onVolumeDown,
                    isCompact = true
                )
            }
        }
    }
}
