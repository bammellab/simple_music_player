package com.bammellab.musicplayer.ui.screens

import android.content.res.Configuration
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bammellab.musicplayer.data.model.FolderNode
import com.bammellab.musicplayer.player.PlaybackState
import com.bammellab.musicplayer.ui.components.CastButton
import com.bammellab.musicplayer.ui.components.FileListView
import com.bammellab.musicplayer.ui.components.FolderListView
import com.bammellab.musicplayer.ui.components.MusicLoadingIndicator
import com.bammellab.musicplayer.ui.components.NowPlayingView
import com.bammellab.musicplayer.ui.components.PlayerControls
import com.bammellab.musicplayer.viewmodel.MusicPlayerViewModel
import com.bammellab.musicplayer.viewmodel.MusicPlayerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    viewModel: MusicPlayerViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val castState by viewModel.castState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val fileListState = rememberLazyListState()
    val folderListState = rememberLazyListState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val smallestWidth = minOf(configuration.screenWidthDp, configuration.screenHeightDp)
    val isTablet = smallestWidth >= 600

    // Load media when permission is granted
    LaunchedEffect(hasPermission) {
        if (hasPermission && !uiState.hasPermission) {
            viewModel.onPermissionResult(true)
        }
    }

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
                navigationIcon = {
                    // Show back button when:
                    // 1. Viewing tracks (not in folder browser) - go back to folder browser
                    // 2. In folder browser and can navigate up - go to parent folder
                    val showBackButton = hasPermission && (
                        (!uiState.showFolderBrowser && uiState.hasFiles) ||
                        (uiState.showFolderBrowser && uiState.canNavigateUp)
                    )
                    if (showBackButton) {
                        IconButton(onClick = { viewModel.handleBackNavigation() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    // Cast button - shows when Cast devices are available
                    CastButton(modifier = Modifier.size(48.dp))
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        when {
            // No permission - show permission request UI
            !hasPermission -> {
                PermissionRequestContent(
                    onRequestPermission = onRequestPermission,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            // Loading
            uiState.isLoading -> {
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
            }

            // Show folder browser
            uiState.showFolderBrowser -> {
                FolderBrowserContent(
                    folders = uiState.currentFolderChildren,
                    displayPath = uiState.currentBrowseDisplayPath,
                    folderListState = folderListState,
                    onFolderSelected = { viewModel.navigateToFolder(it.path) },
                    onPlayFolder = { viewModel.selectFolderForPlayback(it) },
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refreshCurrentView() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            // Show track list and player
            else -> {
                if (isLandscape) {
                    LandscapeLayout(
                        uiState = uiState,
                        playbackState = playbackState,
                        currentPosition = currentPosition,
                        duration = duration,
                        fileListState = fileListState,
                        isTablet = isTablet,
                        onTrackSelected = { viewModel.playTrack(it) },
                        onSeek = { viewModel.seekTo(it) },
                        onPlayPause = { viewModel.togglePlayPause() },
                        onPrevious = { viewModel.playPrevious() },
                        onNext = { viewModel.playNext() },
                        onShuffleToggle = { viewModel.toggleShuffle() },
                        onVolumeUp = { viewModel.volumeUp() },
                        onVolumeDown = { viewModel.volumeDown() },
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refreshCurrentView() },
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
                        onTrackSelected = { viewModel.playTrack(it) },
                        onSeek = { viewModel.seekTo(it) },
                        onPlayPause = { viewModel.togglePlayPause() },
                        onPrevious = { viewModel.playPrevious() },
                        onNext = { viewModel.playNext() },
                        onShuffleToggle = { viewModel.toggleShuffle() },
                        onVolumeUp = { viewModel.volumeUp() },
                        onVolumeDown = { viewModel.volumeDown() },
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refreshCurrentView() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Permission Required",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Grant access to play your music files",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderBrowserContent(
    folders: List<FolderNode>,
    displayPath: String,
    folderListState: LazyListState,
    onFolderSelected: (FolderNode) -> Unit,
    onPlayFolder: (FolderNode) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = displayPath,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (folders.isEmpty()) {
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                            text = "No music folders found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pull down to refresh",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            FolderListView(
                folders = folders,
                onFolderSelected = { node ->
                    // If folder has only direct music (no children), play it directly
                    // Otherwise, navigate into it
                    if (node.hasDirectMusic && !node.hasChildren) {
                        onPlayFolder(node)
                    } else {
                        onFolderSelected(node)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                listState = folderListState,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            )
        }
    }
}

@Composable
private fun PortraitLayout(
    uiState: MusicPlayerUiState,
    playbackState: PlaybackState,
    currentPosition: Int,
    duration: Int,
    fileListState: LazyListState,
    onTrackSelected: (Int) -> Unit,
    onSeek: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffleToggle: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
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

        FileListView(
            audioFiles = uiState.audioFiles,
            currentTrackIndex = uiState.currentTrackIndex,
            onTrackSelected = onTrackSelected,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            listState = fileListState,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh
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
                onVolumeDown = onVolumeDown,
                isCurrentTrackPlayable = uiState.currentTrack?.isPlayable ?: true
            )
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
    isTablet: Boolean,
    onTrackSelected: (Int) -> Unit,
    onSeek: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffleToggle: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
                listState = fileListState,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
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
                isCompact = !isTablet
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
                isCompact = !isTablet,
                isCurrentTrackPlayable = uiState.currentTrack?.isPlayable ?: true
            )
        }
    }
}
