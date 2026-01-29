package com.bammellab.musicplayer.data.repository

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserManager
import android.provider.MediaStore
import androidx.annotation.RequiresPermission
import com.bammellab.musicplayer.data.model.AudioFile
import com.bammellab.musicplayer.data.model.FolderNode
import com.bammellab.musicplayer.data.model.MusicFolder
import java.io.File

class MediaStoreRepository(private val context: Context) {

    data class MediaStoreResult(
        val folders: List<MusicFolder>,
        val allFiles: Map<String, List<AudioFile>>,
        val folderTree: FolderNode?
    )

    fun queryAudioFiles(): MediaStoreResult {
        val filesMap = mutableMapOf<String, MutableList<AudioFile>>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(displayNameColumn) ?: "Unknown"
                val mimeType = cursor.getString(mimeTypeColumn) ?: "audio/*"
                val size = cursor.getLong(sizeColumn)
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val artist = cursor.getString(artistColumn) ?: ""
                val album = cursor.getString(albumColumn) ?: ""
                val data = cursor.getString(dataColumn) ?: ""

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val albumArtUri = if (albumId > 0) {
                    ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )
                } else null

                val folderPath = File(data).parent ?: ""

                val audioFile = AudioFile(
                    uri = contentUri,
                    displayName = displayName,
                    mimeType = mimeType,
                    size = size,
                    duration = duration,
                    albumId = albumId,
                    albumArtUri = albumArtUri,
                    artist = artist,
                    album = album,
                    folderPath = folderPath
                )

                filesMap.getOrPut(folderPath) { mutableListOf() }.add(audioFile)
            }
        }

        // Build folder list from the grouped files
        val folders = filesMap.map { (path, files) ->
            val displayName = File(path).name.ifEmpty { path }
            // Use the album art from the first file that has one
            val albumArtUri = files.firstNotNullOfOrNull { it.albumArtUri }

            MusicFolder(
                path = path,
                displayName = displayName,
                trackCount = files.size,
                albumArtUri = albumArtUri
            )
        }.sortedBy { it.displayName.lowercase() }

        // Build folder tree
        val folderTree = buildFolderTree(filesMap)

        return MediaStoreResult(folders, filesMap, folderTree)
    }

    fun getFilesForFolder(folderPath: String, allFiles: Map<String, List<AudioFile>>): List<AudioFile> {
        return allFiles[folderPath]?.sortedBy { it.displayName.lowercase() } ?: emptyList()
    }

    /**
     * Build a hierarchical folder tree from flat folder paths.
     * Finds the common root and creates intermediate nodes as needed.
     */
    private fun buildFolderTree(filesMap: Map<String, List<AudioFile>>): FolderNode? {
        if (filesMap.isEmpty()) return null

        val folderPaths = filesMap.keys.toList()

        // Find common root path
        val commonRoot = findCommonRoot(folderPaths)
        if (commonRoot.isEmpty()) return null

        // Build tree structure recursively
        return buildNodeRecursive(commonRoot, filesMap)
    }

    /**
     * Find the longest common prefix path among all folder paths.
     */
    private fun findCommonRoot(paths: List<String>): String {
        if (paths.isEmpty()) return ""
        if (paths.size == 1) {
            // Single folder - use its parent as root
            return File(paths.first()).parent ?: paths.first()
        }

        // Split paths into segments
        val pathSegments = paths.map { it.split(File.separator) }
        val minLength = pathSegments.minOf { it.size }

        val commonSegments = mutableListOf<String>()
        for (i in 0 until minLength) {
            val segment = pathSegments[0][i]
            if (pathSegments.all { it[i] == segment }) {
                commonSegments.add(segment)
            } else {
                break
            }
        }

        return commonSegments.joinToString(File.separator)
    }

    /**
     * Convert storage folder names to human-friendly display names.
     * - "emulated" -> "Internal Storage"
     * - "0" -> Primary user's name
     * - "10", "11", etc. -> Secondary user's name
     * - "XXXX-XXXX" (SD card volume ID pattern) -> "SD Card"
     */
    private fun getHumanFriendlyName(path: String): String {
        val rawName = File(path).name.ifEmpty { path }

        // Check for internal storage
        if (rawName == "emulated") {
            return "Internal Storage"
        }

        // Check for user ID folder (0 for primary, 10+ for secondary users)
        val userId = rawName.toIntOrNull()
        if (userId != null && (userId == 0 || userId >= 10)) {
            return getUserDisplayName(userId)
        }

        // Check for SD card pattern (4 hex digits, dash, 4 hex digits like "51ED-A7DC")
        val sdCardPattern = Regex("^[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}$")
        if (sdCardPattern.matches(rawName)) {
            return "SD Card"
        }

        return rawName
    }

    /**
     * Get the display name for a user ID.
     * Uses UserManager.userName on API 31+ for the current user,
     * falls back to a default label otherwise.
     */
    @RequiresPermission(anyOf = ["android.permission.QUERY_USERS", Manifest.permission.GET_ACCOUNTS_PRIVILEGED, "android.permission.CREATE_USERS", "android.permission.MANAGE_USERS"])
    private fun getUserDisplayName(userId: Int): String {
        return try {
            val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
            // getUserName() returns the current user's name (API 31+)
            // We can only get the name for the current user without special permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Get current user ID from the process UID (user ID is in the upper bits)
                val currentUserId = Process.myUid() / 100000
                if (userId == currentUserId) {
                    userManager?.userName?.takeIf { it.isNotBlank() } ?: getDefaultUserLabel(userId)
                } else {
                    getDefaultUserLabel(userId)
                }
            } else {
                getDefaultUserLabel(userId)
            }
        } catch (e: Exception) {
            getDefaultUserLabel(userId)
        }
    }

    /**
     * Get a default label for a user ID when the actual name is not available.
     */
    private fun getDefaultUserLabel(userId: Int): String {
        return when (userId) {
            0 -> "Primary User"
            else -> "User ${userId - 9}"  // User 10 → "User 1", User 11 → "User 2"
        }
    }

    /**
     * Recursively build a FolderNode for a given path.
     */
    private fun buildNodeRecursive(
        path: String,
        filesMap: Map<String, List<AudioFile>>
    ): FolderNode {
        val name = getHumanFriendlyName(path)
        val directFiles = filesMap[path] ?: emptyList()
        val directTrackCount = directFiles.size
        val directAlbumArt = directFiles.firstNotNullOfOrNull { it.albumArtUri }

        // Find immediate children folders
        val childPaths = findImmediateChildren(path, filesMap.keys)
        val children = childPaths
            .map { buildNodeRecursive(it, filesMap) }
            .sortedBy { it.name.lowercase() }

        // Calculate total track count (direct + all descendants)
        val totalTrackCount = directTrackCount + children.sumOf { it.totalTrackCount }

        // Album art: use direct art if available, otherwise inherit from first child
        val albumArtUri = directAlbumArt ?: children.firstNotNullOfOrNull { it.albumArtUri }

        return FolderNode(
            path = path,
            name = name,
            directTrackCount = directTrackCount,
            totalTrackCount = totalTrackCount,
            albumArtUri = albumArtUri,
            children = children
        )
    }

    /**
     * Find immediate child folder paths for a given parent path.
     * This includes both folders with direct music and intermediate folders.
     */
    private fun findImmediateChildren(parentPath: String, allFolderPaths: Set<String>): List<String> {
        val immediateChildren = mutableSetOf<String>()
        val parentNormalized = if (parentPath.endsWith(File.separator)) parentPath else parentPath + File.separator

        for (folderPath in allFolderPaths) {
            if (!folderPath.startsWith(parentNormalized)) continue
            if (folderPath == parentPath) continue

            // Get the relative path after the parent
            val relativePath = folderPath.removePrefix(parentNormalized)
            val segments = relativePath.split(File.separator)

            if (segments.isNotEmpty()) {
                // The immediate child is the first segment
                val immediateChildPath = parentPath + File.separator + segments[0]
                immediateChildren.add(immediateChildPath)
            }
        }

        return immediateChildren.toList()
    }

    /**
     * Get children of a folder at a specific path from the tree.
     */
    fun getChildrenAtPath(folderTree: FolderNode?, path: String): List<FolderNode> {
        if (folderTree == null) return emptyList()

        // If at the root
        if (folderTree.path == path) {
            return folderTree.children
        }

        // Search recursively
        return findNodeAtPath(folderTree, path)?.children ?: emptyList()
    }

    /**
     * Find a node at a specific path in the tree.
     */
    fun findNodeAtPath(root: FolderNode?, path: String): FolderNode? {
        if (root == null) return null
        if (root.path == path) return root

        for (child in root.children) {
            val found = findNodeAtPath(child, path)
            if (found != null) return found
        }

        return null
    }

    /**
     * Get the parent path of a given path.
     */
    fun getParentPath(path: String): String? {
        return File(path).parent
    }
}
