/*
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.content.Context
import android.content.ContentResolver
import android.content.ContentValues
import android.location.Location
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.video.MediaStoreOutputOptions
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale

object StorageUtils {
    private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    private var IMAGES_MEDIA_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    private var VIDEO_MEDIA_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    /**
     * Returns media URI for removable SD media card
     */
    private fun getSDMediaUri(
        context: Context,
        mediaType: String? = "Images"): Uri {

        if (mediaType !in listOf("Images", "Video")) throw Exception("Wrong MediaStore type \"${mediaType}\"")

        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val volumes = storageManager.getStorageVolumes()

        for (volume in volumes) {
            if (!volume.isPrimary() && volume.getState() == Environment.MEDIA_MOUNTED) {
                when (mediaType) {
                    "Images" -> return MediaStore.Images.Media.getContentUri(volume.getMediaStoreVolumeName())
                    "Video" -> return MediaStore.Video.Media.getContentUri(volume.getMediaStoreVolumeName())
                }
            }
        }
        return if(mediaType == "Images") IMAGES_MEDIA_URI else VIDEO_MEDIA_URI
    }

    /**
     * Returns a new ImageCapture.OutputFileOptions to use to store a JPEG photo
     */
    fun getPhotoMediaStoreOutputOptions(
        contentResolver: ContentResolver,
        metadata: ImageCapture.Metadata,
        outputStream: OutputStream? = null,
        context: Context
    ): ImageCapture.OutputFileOptions {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, getCurrentTimeString())
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/Photo")
            }
        }

        if (IMAGES_MEDIA_URI === MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {
            IMAGES_MEDIA_URI = getSDMediaUri(context, "Images")
        }

        val outputFileOptions = if (outputStream != null) {
            ImageCapture.OutputFileOptions.Builder(outputStream)
        } else {
            ImageCapture.OutputFileOptions.Builder(
                contentResolver, IMAGES_MEDIA_URI,
                contentValues
            )
        }
        return outputFileOptions
            .setMetadata(metadata)
            .build()
    }

    /**
     * Returns a new OutputFileOptions to use to store a MP4 video
     */
    fun getVideoMediaStoreOutputOptions(
        contentResolver: ContentResolver,
        location: Location?,
        context: Context
    ): MediaStoreOutputOptions {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, getCurrentTimeString())
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/Video")
            }
        }

        if (VIDEO_MEDIA_URI === MediaStore.Video.Media.EXTERNAL_CONTENT_URI) {
            VIDEO_MEDIA_URI = getSDMediaUri(context, "Video")
        }

        return MediaStoreOutputOptions
            .Builder(contentResolver, VIDEO_MEDIA_URI)
            .setContentValues(contentValues)
            .setLocation(location)
            .build()
    }

    private fun getCurrentTimeString(): String {
        return SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
    }
}
