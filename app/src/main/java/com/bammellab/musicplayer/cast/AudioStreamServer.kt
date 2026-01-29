package com.bammellab.musicplayer.cast

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.InputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.UUID

/**
 * Embedded HTTP server that streams audio files to Chromecast.
 * Chromecast requires HTTP URLs, so this server bridges SAF content:// URIs
 * to HTTP endpoints accessible on the local network.
 */
class AudioStreamServer(
    private val context: Context,
    port: Int = 0  // 0 = auto-assign available port
) : NanoHTTPD(port) {

    companion object {
        private const val TAG = "AudioStreamServer"
        private const val PATH_AUDIO = "/audio/"
        private const val PATH_ARTWORK = "/artwork/"
    }

    // Session token to prevent unauthorized access
    private val sessionToken = UUID.randomUUID().toString()

    // Currently registered files for streaming
    private val registeredFiles = mutableMapOf<String, Uri>()

    /**
     * Register an audio file for streaming and get its HTTP URL.
     */
    fun registerFile(uri: Uri): String {
        val fileId = UUID.randomUUID().toString()
        registeredFiles[fileId] = uri
        val serverUrl = getServerUrl()
        return "$serverUrl$PATH_AUDIO$sessionToken/$fileId"
    }

    /**
     * Register album art for streaming and get its HTTP URL.
     */
    fun registerArtwork(uri: Uri): String {
        val fileId = UUID.randomUUID().toString()
        registeredFiles[fileId] = uri
        val serverUrl = getServerUrl()
        return "$serverUrl$PATH_ARTWORK$sessionToken/$fileId"
    }

    /**
     * Clear all registered files.
     */
    fun clearRegisteredFiles() {
        registeredFiles.clear()
    }

    /**
     * Get the server's base URL (e.g., http://192.168.1.100:8080)
     */
    fun getServerUrl(): String {
        val ip = getLocalIpAddress()
        return "http://$ip:$listeningPort"
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        Log.d(TAG, "Request: $uri")

        // Parse path: /audio/{token}/{fileId} or /artwork/{token}/{fileId}
        val parts = uri.split("/").filter { it.isNotEmpty() }
        if (parts.size < 3) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }

        val pathType = parts[0]  // "audio" or "artwork"
        val token = parts[1]
        val fileId = parts[2]

        // Validate session token
        if (token != sessionToken) {
            Log.w(TAG, "Invalid session token")
            return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
        }

        // Get registered file
        val fileUri = registeredFiles[fileId]
        if (fileUri == null) {
            Log.w(TAG, "File not found: $fileId")
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }

        return try {
            serveFile(session, fileUri, pathType == "artwork")
        } catch (e: Exception) {
            Log.e(TAG, "Error serving file", e)
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: ${e.message}")
        }
    }

    private fun serveFile(session: IHTTPSession, uri: Uri, isArtwork: Boolean): Response {
        val contentResolver = context.contentResolver

        // Get file size
        val fileSize = contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L

        // Determine MIME type
        val mimeType = if (isArtwork) {
            "image/jpeg"  // Album art is typically JPEG
        } else {
            contentResolver.getType(uri) ?: "audio/mpeg"
        }

        // Check for range request (for seeking)
        val rangeHeader = session.headers["range"]
        if (rangeHeader != null && fileSize > 0) {
            return servePartialContent(uri, rangeHeader, fileSize, mimeType)
        }

        // Full file response
        val inputStream = contentResolver.openInputStream(uri)
            ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Cannot open file")

        return if (fileSize > 0) {
            newFixedLengthResponse(Response.Status.OK, mimeType, inputStream, fileSize)
        } else {
            newChunkedResponse(Response.Status.OK, mimeType, inputStream)
        }
    }

    private fun servePartialContent(
        uri: Uri,
        rangeHeader: String,
        fileSize: Long,
        mimeType: String
    ): Response {
        // Parse range header: "bytes=0-1023" or "bytes=0-"
        val range = rangeHeader.replace("bytes=", "")
        val rangeParts = range.split("-")
        val start = rangeParts[0].toLongOrNull() ?: 0L
        val end = if (rangeParts.size > 1 && rangeParts[1].isNotEmpty()) {
            rangeParts[1].toLongOrNull() ?: (fileSize - 1)
        } else {
            fileSize - 1
        }

        val contentLength = end - start + 1

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Cannot open file")

        // Skip to start position
        inputStream.skip(start)

        // Create partial content response
        val response = newFixedLengthResponse(
            Response.Status.PARTIAL_CONTENT,
            mimeType,
            LimitedInputStream(inputStream, contentLength),
            contentLength
        )
        response.addHeader("Content-Range", "bytes $start-$end/$fileSize")
        response.addHeader("Accept-Ranges", "bytes")

        return response
    }

    private fun getLocalIpAddress(): String {
        try {
            // Try ConnectivityManager first (modern API)
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            connectivityManager?.activeNetwork?.let { network ->
                connectivityManager.getLinkProperties(network)?.linkAddresses?.forEach { linkAddress ->
                    val address = linkAddress.address
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        return address.hostAddress ?: "127.0.0.1"
                    }
                }
            }

            // Fallback to network interfaces
            NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { networkInterface ->
                networkInterface.inetAddresses?.toList()?.forEach { address ->
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val hostAddress = address.hostAddress
                        if (hostAddress != null) {
                            return hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IP address", e)
        }
        return "127.0.0.1"
    }

    /**
     * InputStream wrapper that limits bytes read.
     * Used for range requests to return only the requested portion.
     */
    private class LimitedInputStream(
        private val wrapped: InputStream,
        private var remaining: Long
    ) : InputStream() {

        override fun read(): Int {
            if (remaining <= 0) return -1
            val result = wrapped.read()
            if (result >= 0) remaining--
            return result
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (remaining <= 0) return -1
            val toRead = minOf(len.toLong(), remaining).toInt()
            val result = wrapped.read(b, off, toRead)
            if (result > 0) remaining -= result
            return result
        }

        override fun close() {
            wrapped.close()
        }
    }
}
