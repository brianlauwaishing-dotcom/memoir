package com.mcis.memoir.camera

import android.content.ContentResolver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraCapturer(private val sink: CaptureSink) {
    suspend fun capture(
        contentResolver: ContentResolver,
        now: () -> Long = System::currentTimeMillis
    ): CaptureResult {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(now()))
        val filename = "memoir_$timestamp.jpg"
        return runCatching { sink.savePhoto(filename, contentResolver) }
            .fold(
                onSuccess = { CaptureResult.Success(it) },
                onFailure = { CaptureResult.Failure(it) }
            )
    }
}
