package com.mcis.memoir

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.mcis.memoir.camera.CameraCapturer
import com.mcis.memoir.camera.CaptureResult
import com.mcis.memoir.camera.ImageCaptureSink
import com.mcis.memoir.ui.components.UntitledIcon
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.inter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Screen that shows a REAL live camera viewfinder for taking a photo.
 * Based on Figma node 176:326.
 */
@Composable
fun CameraPreviewScreen(
    onBackClick: () -> Unit = {},
    onCaptureClick: (Uri) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var captureInFlight by remember { mutableStateOf(false) }
    var flashAlpha by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (!granted) {
                onBackClick()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // 1. REAL LIVE Camera Preview using CameraX
            CameraPreviewView(
                modifier = Modifier.fillMaxSize(),
                context = context,
                lifecycleOwner = lifecycleOwner,
                onImageCaptureReady = { imageCapture = it }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha))
            )

            errorMessage?.let { message ->
                LaunchedEffect(message) {
                    delay(4000)
                    if (errorMessage == message) {
                        errorMessage = null
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp)
                        .background(Color.Red.copy(alpha = 0.7f))
                        .clickable { errorMessage = null }
                ) {
                    Text(
                        text = message,
                        color = Color.White,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // 2. Close/Back Button (Top Left)
            Box(
                modifier = Modifier
                    .padding(top = 23.dp, start = 24.dp)
                    .size(50.dp)
                    .clip(CircleShape)
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                UntitledIcon(
                    imageVector = UntitledIcons.CloseIcon,
                    contentDescription = stringResource(R.string.camera_close_content_description),
                    tint = Color.White,
                    size = 32.dp
                )
            }

            // 4. Capture Button (Bottom Center)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 33.dp)
                    .size(80.dp)
                    .alpha(if (imageCapture == null || captureInFlight) 0.5f else 1f)
                    .background(DesignTokens.colorMaroon, CircleShape)
                    .clip(CircleShape)
                    .clickable {
                        val readyImageCapture = imageCapture ?: return@clickable
                        if (captureInFlight) return@clickable
                        captureInFlight = true
                        errorMessage = null
                        coroutineScope.launch {
                            launch {
                                animate(0f, 1f, animationSpec = tween(50)) { value, _ ->
                                    flashAlpha = value
                                }
                                animate(1f, 0f, animationSpec = tween(150)) { value, _ ->
                                    flashAlpha = value
                                }
                            }
                            val executor = ContextCompat.getMainExecutor(context)
                            val capturer = CameraCapturer(ImageCaptureSink(readyImageCapture, executor))
                            when (val result = capturer.capture(context.contentResolver)) {
                                is CaptureResult.Success -> onCaptureClick(result.uri)
                                is CaptureResult.Failure -> {
                                    errorMessage = result.cause.localizedMessage
                                        ?: context.getString(R.string.camera_capture_failed)
                                    captureInFlight = false
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                UntitledIcon(
                    imageVector = UntitledIcons.CameraIcon,
                    contentDescription = stringResource(R.string.camera_capture_content_description),
                    tint = Color.White,
                    size = 24.dp
                )
            }
        } else {
            // Loading/Permission state
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.camera_permission_required),
                    color = Color.White,
                    fontFamily = inter
                )
            }
        }
    }
}

@Composable
fun CameraPreviewView(
    modifier: Modifier,
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onImageCaptureReady: (ImageCapture) -> Unit
) {
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    
    LaunchedEffect(Unit) {
        val cameraProvider = context.getCameraProvider()
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        
        preview.setSurfaceProvider(previewView.surfaceProvider)
        
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            onImageCaptureReady(imageCapture)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { future ->
        future.addListener({
            continuation.resume(future.get())
        }, ContextCompat.getMainExecutor(this))
    }
}

@ComposePreview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
fun CameraPreviewScreenPreview() {
    AppTheme {
        CameraPreviewScreen()
    }
}
