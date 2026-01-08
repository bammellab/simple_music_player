package com.example.musicplayer.util

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import android.net.Uri

data class StorageOption(
    val name: String,
    val uuid: String?,
    val isPrimary: Boolean,
    val initialUri: Uri?
)

object StorageHelper {

    private fun getAttributedContext(context: Context): Context {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.createAttributionContext("attributionTag")
        } else {
            context
        }
    }

    /*
    FOR LATER DEBUGGING:
    4. Explicitly Handle the "null" Tag
In Kotlin, if you are passing this context to a system service or a library that doesn't expect an attribution tag, it might throw a fit even if declared. Ensure your attributionTag string exactly matches (case-sensitive) what is in the XML.

If the error persists specifically when calling a system service, try checking if the context actually carries the tag:

Kotlin

private fun getAttributedContext(context: Context): Context {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Note: Attribution improved in API 31
        val attributed = context.createAttributionContext("attributionTag")
        // Debug check: Verify the tag was applied
        println("Attribution Tag: ${attributed.attributionTag}")
        attributed
    } else {
        context
    }
}
     */

    fun getStorageOptions(context: Context): List<StorageOption> {
        val attributedContext = getAttributedContext(context)
        val storageManager = attributedContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val volumes = storageManager.storageVolumes

        return volumes.filter { it.state == "mounted" }.map { volume ->
            StorageOption(
                name = volume.getDescription(attributedContext) ?: if (volume.isPrimary) "Internal Storage" else "SD Card",
                uuid = volume.uuid,
                isPrimary = volume.isPrimary,
                initialUri = buildInitialUri(volume)
            )
        }
    }

    private fun buildInitialUri(volume: StorageVolume): Uri? {
        // For primary storage, use the primary external storage URI
        // For SD cards, use the volume's UUID to build a URI
        return if (volume.isPrimary) {
            DocumentsContract.buildRootUri(
                "com.android.externalstorage.documents",
                "primary"
            )
        } else {
            volume.uuid?.let { uuid ->
                DocumentsContract.buildRootUri(
                    "com.android.externalstorage.documents",
                    uuid
                )
            }
        }
    }

    fun hasMultipleStorageOptions(context: Context): Boolean {
        return getStorageOptions(context).size > 1
    }
}
