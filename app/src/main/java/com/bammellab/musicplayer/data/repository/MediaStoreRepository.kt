package com.bammellab.musicplayer.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.bammellab.musicplayer.data.model.AudioFile
import com.bammellab.musicplayer.data.model.MusicFolder
import java.io.File

class MediaStoreRepository(private val context: Context) {

    data class MediaStoreResult(
        val folders: List<MusicFolder>,
        val allFiles: Map<String, List<AudioFile>>
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

        return MediaStoreResult(folders, filesMap)
    }

    fun getFilesForFolder(folderPath: String, allFiles: Map<String, List<AudioFile>>): List<AudioFile> {
        return allFiles[folderPath]?.sortedBy { it.displayName.lowercase() } ?: emptyList()
    }
}
