package com.mcis.memoir

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.mcis.memoir.ui.components.UntitledIcon
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.inter
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Screen that shows a REAL live camera viewfinder for taking a photo.
 * Based on Figma node 176:326.
 */
@Composable
fun CameraPreviewScreen(
    onBackClick: () -> Unit = {},
    onCaptureClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
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
                lifecycleOwner = lifecycleOwner
            )

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
                    contentDescription = "Close",
                    tint = Color.White,
                    size = 32.dp
                )
            }

            // 3. Flash/Settings Icon (Top Right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 32.dp, end = 30.dp)
                    .size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                UntitledIcon(
                    imageVector = UntitledIcons.InfoIcon,
                    contentDescription = "Settings",
                    tint = Color.White,
                    size = 24.dp
                )
            }

            // 4. Capture Button (Bottom Center)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 33.dp)
                    .size(80.dp)
                    .background(DesignTokens.colorMaroon, CircleShape)
                    .clip(CircleShape)
                    .clickable { onCaptureClick() },
                contentAlignment = Alignment.Center
            ) {
                UntitledIcon(
                    imageVector = UntitledIcons.CameraIcon,
                    contentDescription = "Capture",
                    tint = Color.White,
                    size = 24.dp
                )
            }
        } else {
            // Loading/Permission state
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Camera access required to take photos.",
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
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val previewView = remember { PreviewView(context) }
    
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
                preview
            )
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
