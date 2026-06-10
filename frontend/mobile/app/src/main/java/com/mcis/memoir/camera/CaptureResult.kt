package com.mcis.memoir.camera

import android.net.Uri

sealed interface CaptureResult {
    data class Success(val uri: Uri) : CaptureResult
    data class Failure(val cause: Throwable) : CaptureResult
}
