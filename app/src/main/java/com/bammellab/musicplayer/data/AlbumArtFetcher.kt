package com.bammellab.musicplayer.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream

private const val TAG = "AlbumArtFetcher"

/**
 * Wrapper class to identify album art loading requests for Coil.
 */
data class AlbumArtRequest(val uri: Uri)

/**
 * Custom Coil Fetcher that extracts embedded album art from audio files
 * using MediaMetadataRetriever.
 */
class AlbumArtFetcher(
    private val context: Context,
    private val data: AlbumArtRequest,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val uriString = data.uri.toString()

        // Check if this is a MediaStore album art URI
        if (uriString.contains("albumart")) {
            return fetchFromMediaStore()
        }

        // Otherwise, try to extract embedded art from the audio file
        return fetchEmbeddedArt()
    }

    private fun fetchFromMediaStore(): FetchResult? {
        return try {
            Log.d(TAG, "Fetching MediaStore album art for: ${data.uri}")
            val inputStream = context.contentResolver.openInputStream(data.uri)
            if (inputStream == null) {
                Log.d(TAG, "No album art found at: ${data.uri}")
                return null
            }

            Log.d(TAG, "Found MediaStore album art for: ${data.uri}")
            SourceResult(
                source = ImageSource(
                    source = inputStream.source().buffer(),
                    context = context
                ),
                mimeType = null,
                dataSource = DataSource.DISK
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching MediaStore album art for ${data.uri}: ${e.message}", e)
            null
        }
    }

    private fun fetchEmbeddedArt(): FetchResult? {
        val retriever = MediaMetadataRetriever()
        return try {
            Log.d(TAG, "Fetching embedded album art for: ${data.uri}")
            retriever.setDataSource(context, data.uri)
            val artBytes = retriever.embeddedPicture

            if (artBytes == null) {
                Log.d(TAG, "No embedded picture found for: ${data.uri}")
                return null
            }

            Log.d(TAG, "Found album art (${artBytes.size} bytes) for: ${data.uri}")
            SourceResult(
                source = ImageSource(
                    source = ByteArrayInputStream(artBytes).source().buffer(),
                    context = context
                ),
                mimeType = null,
                dataSource = DataSource.DISK
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching embedded album art for ${data.uri}: ${e.message}", e)
            null
        } finally {
            retriever.release()
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<AlbumArtRequest> {
        override fun create(
            data: AlbumArtRequest,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            Log.d(TAG, "Factory creating fetcher for: ${data.uri}")
            return AlbumArtFetcher(context, data, options)
        }
    }
}
