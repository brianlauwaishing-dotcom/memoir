## Context

Smallest screen change in the umbrella. Consumes:
- `tainan-route-content-pipeline` change #1: nothing directly (camera screen has no content reads).
- `language-toggle` change #2: `values-zh/strings.xml` is the canonical Chinese chrome-text channel.
- `home-discovery` change #3: JUnit5 + MockK + Turbine test stack (already activated).
- `route-bookmarking` change #4: `SavedViewModel`'s "no VM if no business logic" precedent — applied here.
- `artifact-discovery-flow` change #5: indirect — ArtifactDiscovery / ArtifactDetail still call `onCameraClick = { backStack.add(CameraPreviewDestination) }`; this change makes the camera button do something useful.

**Current state**:
- `CameraPreviewScreen.kt:84` calls `CameraPreviewView(modifier, context, lifecycleOwner)`.
- `CameraPreviewView.kt:162-179` binds only `Preview` via `cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)`. No `ImageCapture` use case is bound.
- `CameraPreviewScreen.kt:131` capture-button `onClick` invokes the `onCaptureClick: () -> Unit = {}` parameter directly — no `takePicture` call.
- `MyAppNavigation.kt:393-402` (the `CameraPreviewDestination` entry) wires `onCaptureClick = { backStack.removeLastOrNull() }`, so tapping the shutter just navigates back.
- `CameraPreviewScreen.kt:108-121` renders a useless `InfoIcon` labeled "Settings/Flash" with no click handler.
- Manifest has `<uses-permission android:name="android.permission.CAMERA" />` but NO `WRITE_EXTERNAL_STORAGE`.
- Three contentDescription literals (`"Close"`, `"Settings"`, `"Capture"`) and the permission-denied message (`"Camera access required to take photos."`) are hard-coded Kotlin strings.
- Permission flow at `CameraPreviewScreen.kt:52-75` works correctly: checks at compose time, prompts if not granted, navigates back on denial.

**Constraints**:
- `minSdk = 24` per `app/build.gradle.kts:16` — `WRITE_EXTERNAL_STORAGE` matters for API 24-28.
- CameraX `ImageCapture` is a concrete class without an interface — can't be MockK'd via subclassing without `mockkConstructor`. Use an injection seam.
- The Composable already runs inside a CameraX-bound lifecycle; the `ImageCapture` use case must be bound at the same time as `Preview` (single `bindToLifecycle` call rebinds both).
- `MediaStore.Images.Media.RELATIVE_PATH` is API 29+. On API < 29, fall back to `MediaStore.Images.Media.DATA` with an absolute path. The `ImageCapture.OutputFileOptions.Builder(contentResolver, contentUri, contentValues)` constructor handles both branches via the `ContentValues` keys.

## Goals / Non-Goals

**Goals:**
1. Tapping the shutter button writes a JPEG to MediaStore (Pictures/Memoir/) and then navigates back.
2. Capture failure is visible to the user as an inline error (no silent failure).
3. Capture button is debounced — a single in-flight capture cannot be fired twice.
4. White-flash overlay gives perceptible feedback that the shot was taken.
5. Hard-coded Kotlin strings on this screen move into resources with `values-zh/` mirrors.
6. The dead "Settings" icon is removed.

**Non-Goals:**
- Returning the captured `Uri` to the caller. `onCaptureClick: () -> Unit` stays parameterless; future memory-creation flow uses its own photo picker.
- Per-spot / per-artifact context in the filename or EXIF.
- Flash control, camera switching, zoom, tap-to-focus.
- Multi-shot mode.
- A capture-history viewer inside the app.
- An MVI ViewModel. Per `SavedViewModel` precedent, no business logic worth a reducer; camera lifecycle is intrinsically Compose-scoped.
- Capturing video.

## Decisions

### D1. `ImageCapture` is bound alongside `Preview` at the same `bindToLifecycle` call

```kotlin
@Composable
fun CameraPreviewView(
    modifier: Modifier,
    context: Context,
    lifecycleOwner: LifecycleOwner,
    onImageCaptureReady: (ImageCapture) -> Unit   // NEW — hoists the bound ImageCapture up to the parent
) {
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    LaunchedEffect(Unit) {
        val cameraProvider = context.getCameraProvider()
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture     // bound alongside preview
            )
            onImageCaptureReady(imageCapture)
        } catch (e: Exception) {
            e.printStackTrace()
            // capture button remains disabled because imageCapture is never readied
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
```

The parent `CameraPreviewScreen` holds `var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }`; the capture button's click is enabled only when `imageCapture != null && !captureInFlight`.

**Why hoist via callback vs `remember` at parent scope:**
- The `ImageCapture` instance must be created in the same recomposition where `bindToLifecycle` is called — moving the `remember` to the parent risks lifecycle desync if recomposition order changes.
- Callback hoisting is the standard Compose CameraX integration pattern (per Android docs).

### D2. `CameraCapturer` with an injection seam

CameraX's `ImageCapture` is a concrete final class — can't be mocked by subclassing. To keep the unit test trivial, introduce a thin seam:

```kotlin
fun interface CaptureSink {
    suspend fun savePhoto(filename: String, contentResolver: ContentResolver): Uri
}

// Production binding wraps ImageCapture.takePicture
class ImageCaptureSink(private val imageCapture: ImageCapture) : CaptureSink {
    override suspend fun savePhoto(filename: String, contentResolver: ContentResolver): Uri =
        suspendCancellableCoroutine { cont ->
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Memoir")
                }
            }
            val options = ImageCapture.OutputFileOptions.Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ).build()

            imageCapture.takePicture(
                options,
                Executors.newSingleThreadExecutor(),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val uri = output.savedUri ?: return cont.resumeWithException(
                            IllegalStateException("savedUri was null")
                        )
                        cont.resume(uri)
                    }
                    override fun onError(exception: ImageCaptureException) {
                        cont.resumeWithException(exception)
                    }
                }
            )
        }
}

sealed interface CaptureResult {
    data class Success(val uri: Uri) : CaptureResult
    data class Failure(val cause: Throwable) : CaptureResult
}

class CameraCapturer(private val sink: CaptureSink) {
    suspend fun capture(contentResolver: ContentResolver, now: () -> Long = System::currentTimeMillis): CaptureResult {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(now()))
        val filename = "memoir_$timestamp.jpg"
        return runCatching { sink.savePhoto(filename, contentResolver) }
            .fold(
                onSuccess = { CaptureResult.Success(it) },
                onFailure = { CaptureResult.Failure(it) }
            )
    }
}
```

The Composable constructs `CameraCapturer(ImageCaptureSink(imageCapture))` once `imageCapture` becomes non-null.

**Why the seam:**
- `CameraCapturer.capture` is pure Kotlin testable: pass a fake `CaptureSink`, assert success/failure mapping, assert filename format, assert `now()` clock injection works.
- Avoids `mockkConstructor(ImageCapture::class)` which is intrusive (modifies the bytecode for every test in the JVM and can leak across tests).
- Production code retains the standard CameraX pattern via `ImageCaptureSink`.

### D3. Filename format

`memoir_yyyyMMdd_HHmmss.jpg` — sortable by name, no leading zero ambiguity, no locale-dependent date parsing. `Locale.US` lock prevents `SimpleDateFormat` quirks under unusual locales.

The format does NOT encode spotId / artifactId because the navigator does not currently thread that context into `CameraPreviewDestination`. If a future change needs context-aware filenames, the navigator threads `CameraPreviewDestination(spotId: String?, artifactId: Int?)` then.

### D4. UI flow on capture

```
[user taps shutter]
  ↓
captureInFlight = true
  ↓
shutter button disabled (alpha 0.5)
  ↓
white-flash overlay alpha 0 → 1 (50ms) → 0 (150ms)
  ↓
[CameraCapturer.capture suspends]
  ↓
  ├── Success → onCaptureClick()   (navigates back via MyAppNavigation)
  └── Failure → captureInFlight = false; errorMessage = e.message; shutter re-enabled
```

The white flash starts before the capture call returns — perceptually it gives the user "I tapped, something happened" feedback even on slow devices. If the suspending capture takes >300ms, the flash has already faded and the screen sits with the shutter disabled until success/failure resolves.

Error UI: a small `Text` rendered at the top of the screen below the close button, white text on a translucent red `Box`, dismissed when the user taps it OR after 4 seconds via `LaunchedEffect(errorMessage)`. Text content is the exception's `localizedMessage ?: stringResource(R.string.camera_capture_failed)`.

**Why white-flash vs camera shutter sound:**
- Sound requires audio focus management and is jarring without volume controls.
- Some devices have legal requirements for shutter sound (Japan, Korea); MediaStore's API doesn't help. Skip — the umbrella's MVP scope.
- White flash is visually unambiguous and universally understood.

### D5. Permission flow stays as-is for CAMERA; WRITE_EXTERNAL_STORAGE is manifest-only

CAMERA flow at `CameraPreviewScreen.kt:52-75` already prompts at composition time and navigates back on denial. No change.

WRITE_EXTERNAL_STORAGE is added to the manifest with `android:maxSdkVersion="28"`. On API 23-28 the permission is dangerous and requires a runtime prompt — but MediaStore writes through `ContentResolver` do NOT require a runtime grant when targeting `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` on those API levels (per Android docs since API 19). The manifest declaration is sufficient because the OS grants the permission at install time on those legacy APIs.

**Verify at implementation time:** the implementer MUST run the capture flow on an API 24 emulator AND an API 28 emulator to confirm no `SecurityException` from MediaStore. If a runtime prompt IS required, the implementer adds a multi-permission launcher in this change's implementing PR.

### D6. String hygiene

Five existing hard-coded literals on the screen:
- `"Close"` → `R.string.camera_close_content_description`
- `"Settings"` → DELETED with the dead icon block (see D7)
- `"Capture"` → `R.string.camera_capture_content_description`
- `"Camera access required to take photos."` → `R.string.camera_permission_required`
- (new) `R.string.camera_capture_failed` for the fallback error message when the exception carries no localized message.

`values-zh/` mirrors land in the same change.

### D7. Delete the dead Settings icon block

`CameraPreviewScreen.kt:108-121` renders an `InfoIcon` labeled "Settings" with no click handler. It has no documented purpose, no Figma reference visible, and no design intent in the change set. Delete it — one-line UI cleanup, removes one of the three planned new content-description strings.

If the designer later wants a settings affordance, they re-add it with an actual handler. No fallback for currently-non-existent functionality.

### D8. Test approach

`CameraCapturerTest.kt` (JUnit5, no MockK on Android types — see D2):
1. `capture()` returns `Success` when `CaptureSink.savePhoto` returns a Uri.
2. `capture()` returns `Failure(cause)` when `CaptureSink.savePhoto` throws.
3. Filename format matches `memoir_yyyyMMdd_HHmmss.jpg` for an injected `now = { 1718848320000L }` value (e.g. `memoir_20240619_233200.jpg` for that timestamp).
4. `Locale.US` is respected (a test with `Locale.JAPAN` default still produces the same format).

Integration test for the `ImageCaptureSink → real ImageCapture → MediaStore` path is intentionally NOT written — requires camera hardware and is brittle in CI. Manual emulator smoke (task 8) covers it.

## Risks / Trade-offs

- **WRITE_EXTERNAL_STORAGE quirks on API 24-28**: documented as manifest-only-sufficient per Android docs, but implementation MUST verify on emulator. If wrong, the fix is a runtime permission prompt in `CameraPreviewScreen` (additive, not architectural).
- **No `Uri` returned to caller**: the captured photo is "lost" from the app's perspective — only reachable via the system Gallery. Acceptable per umbrella scope; `memory-creation-flow` has its own photo picker that reads MediaStore.
- **`Executors.newSingleThreadExecutor()` leaks per capture**: the executor is created fresh on every `savePhoto` call and never explicitly shut down. CameraX's `takePicture` does not document executor lifecycle requirements. Implementation MAY use `ContextCompat.getMainExecutor(context)` instead to avoid the leak — both are correct; the choice has zero functional impact. Pick `ContextCompat.getMainExecutor(context)` to avoid the leak; only reach for a background executor if main-thread JPEG encoding becomes a profiled problem.
- **White-flash overlay timing on slow devices**: if capture takes >300ms, the user sees flash-then-empty-pause-then-back-nav. Acceptable.
- **`CameraSelector.DEFAULT_BACK_CAMERA` hardcoded**: same as today's code. Front-camera support is a separate change.
- **Concurrent recomposition + capture**: if the user backgrounds the app mid-capture, the suspending `savePhoto` may resume on a destroyed Composable. `viewModelScope` would help here, but per D-no-VM we use `rememberCoroutineScope()`. The risk is small (MediaStore insert is fast); on app backgrounding the coroutine cancels and the file may end up partially written. MediaStore's transaction model handles partial inserts safely.

## Migration Plan

This change is additive at the Manifest layer, surgical at the screen layer, and adds one new helper + one test.

1. Add `WRITE_EXTERNAL_STORAGE` line to `AndroidManifest.xml`.
2. Add string entries (`camera_close_content_description`, `camera_capture_content_description`, `camera_permission_required`, `camera_capture_failed`) to `values/strings.xml` and `values-zh/strings.xml`.
3. Create `camera/CameraCapturer.kt` with `CaptureSink`, `ImageCaptureSink`, `CaptureResult`, `CameraCapturer`.
4. Modify `CameraPreviewView` Composable to bind `ImageCapture` use case and hoist via `onImageCaptureReady` callback.
5. Modify `CameraPreviewScreen` Composable: track `imageCapture` + `captureInFlight` + `errorMessage` + `flashAlpha` state; wire shutter click to `CameraCapturer.capture(...)`; add overlay rendering for flash and error.
6. Delete the dead Settings icon block (`CameraPreviewScreen.kt:108-121`).
7. Replace hard-coded strings with `stringResource` calls.
8. Write `CameraCapturerTest.kt`.
9. Manual emulator smoke (api 24, api 28, api 33+): grant permission → tap shutter → see flash → see app return to ArtifactDiscovery → open system Photos / Gallery → confirm new file in Pictures/Memoir/.

**Rollback**: revert the change commit. `CameraPreviewScreen` returns to its capture-button-does-nothing form. Manifest entry stays harmless on rollback (no consumer requires its presence).

## Open Questions

- **Should capture failure be retry-able from the same screen?** Currently the user sees the error message, taps to dismiss, and has to tap shutter again. Acceptable UX.
- **Should the screen close after success or show a thumbnail confirmation?** Per the existing nav wire (`onCaptureClick = { backStack.removeLastOrNull() }`), it closes. Changing this requires the navigator to render a confirmation overlay — out of scope.
- **Future `memory-creation-flow` integration**: when memory creation arrives, it probably wants to read the captured photo back. Options: (a) thread the `Uri` through navigation back to a memory-edit pending state; (b) rely on the system picker to find the MediaStore image. Defer; this change locks the MediaStore write side only.
- **Should we add a small `Toast.makeText(...)` confirmation?** Toast doesn't require a snackbar host, but it's UX-noisy on top of the flash. Skip unless real users report missing the confirmation.
