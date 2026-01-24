package com.bammellab.musicplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bammellab.musicplayer.data.model.FolderNode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderListView(
    folders: List<FolderNode>,
    onFolderSelected: (FolderNode) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(
                items = folders,
                key = { it.path }
            ) { folder ->
                FolderNodeItem(
                    node = folder,
                    onClick = { onFolderSelected(folder) }
                )
            }
        }
    }
}

@Composable
private fun FolderNodeItem(
    node: FolderNode,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Use music note icon if folder has direct music, folder icon otherwise
            val fallbackIcon = if (node.hasDirectMusic) Icons.Filled.MusicNote else Icons.Filled.Folder

            AlbumArtImage(
                uri = null,
                albumArtUri = node.albumArtUri,
                size = 48.dp,
                fallbackIcon = fallbackIcon,
                showBackground = true
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Show track count info based on folder type
                val trackText = when {
                    node.hasDirectMusic && node.hasChildren -> {
                        "${node.directTrackCount} track${if (node.directTrackCount != 1) "s" else ""}, ${node.totalTrackCount - node.directTrackCount} in subfolders"
                    }
                    node.hasDirectMusic -> {
                        "${node.directTrackCount} track${if (node.directTrackCount != 1) "s" else ""}"
                    }
                    else -> {
                        "${node.totalTrackCount} track${if (node.totalTrackCount != 1) "s" else ""} in subfolders"
                    }
                }

                Text(
                    text = trackText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
