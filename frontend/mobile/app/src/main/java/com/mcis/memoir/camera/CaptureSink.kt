package com.mcis.memoir.camera

import android.content.ContentResolver
import android.net.Uri

fun interface CaptureSink {
    suspend fun savePhoto(filename: String, contentResolver: ContentResolver): Uri
}
