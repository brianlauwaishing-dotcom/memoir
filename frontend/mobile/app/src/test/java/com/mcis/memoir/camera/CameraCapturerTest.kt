package com.mcis.memoir.camera

import android.content.ContentResolver
import android.net.Uri
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CameraCapturerTest {
    private val contentResolver = mockk<ContentResolver>(relaxed = true)

    @Test
    fun captureReturnsSuccessWhenSinkReturnsUri() = runTest {
        val uri = mockk<Uri>()
        val sink = mockk<CaptureSink>()
        coEvery { sink.savePhoto(any(), contentResolver) } returns uri

        val result = CameraCapturer(sink).capture(contentResolver, now = { 0L })

        assertTrue(result is CaptureResult.Success)
        assertSame(uri, (result as CaptureResult.Success).uri)
        coVerify(exactly = 1) { sink.savePhoto(any(), contentResolver) }
    }

    @Test
    fun captureReturnsFailureWithOriginalCauseWhenSinkThrows() = runTest {
        val cause = IOException("disk full")
        val sink = mockk<CaptureSink>()
        coEvery { sink.savePhoto(any(), contentResolver) } throws cause

        val result = CameraCapturer(sink).capture(contentResolver, now = { 0L })

        assertTrue(result is CaptureResult.Failure)
        assertSame(cause, (result as CaptureResult.Failure).cause)
        coVerify(exactly = 1) { sink.savePhoto(any(), contentResolver) }
    }

    @Test
    fun captureUsesTimestampFilenamePatternFromInjectedClock() = runTest {
        val filename = slot<String>()
        val sink = mockk<CaptureSink>()
        coEvery { sink.savePhoto(capture(filename), contentResolver) } returns mockk()

        CameraCapturer(sink).capture(contentResolver, now = { 1718848320000L })

        assertTrue(Regex("^memoir_\\d{8}_\\d{6}\\.jpg$").matches(filename.captured))
        coVerify(exactly = 1) { sink.savePhoto(any(), contentResolver) }
    }

    @Test
    fun captureFilenamePatternIsStableWhenDefaultLocaleIsJapanese() = runTest {
        val originalLocale = Locale.getDefault()
        val filename = slot<String>()
        val sink = mockk<CaptureSink>()
        coEvery { sink.savePhoto(capture(filename), contentResolver) } returns mockk()

        try {
            Locale.setDefault(Locale.JAPAN)

            CameraCapturer(sink).capture(contentResolver, now = { 1718848320000L })

            assertTrue(Regex("^memoir_\\d{8}_\\d{6}\\.jpg$").matches(filename.captured))
        } finally {
            Locale.setDefault(originalLocale)
        }
        coVerify(exactly = 1) { sink.savePhoto(any(), contentResolver) }
    }
}
