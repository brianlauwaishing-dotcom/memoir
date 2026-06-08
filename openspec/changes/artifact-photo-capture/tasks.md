## 1. Manifest + permission

- [ ] 1.1 In `frontend/mobile/app/src/main/AndroidManifest.xml`, after the existing `<uses-permission android:name="android.permission.CAMERA" />` line, add `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />` — needed for MediaStore inserts on API 24-28; auto-granted by manifest on those API levels
- [ ] 1.2 No runtime permission prompt added for `WRITE_EXTERNAL_STORAGE` (per design D5); verify behavior on emulator (task 9.2/9.3); if runtime prompt is actually required on API 24-28 emulator, add a multi-permission launcher in this PR

## 2. String resources

- [ ] 2.1 Add to `frontend/mobile/app/src/main/res/values/strings.xml`:
  ```xml
  <string name="camera_close_content_description">Close</string>
  <string name="camera_capture_content_description">Capture</string>
  <string name="camera_permission_required">Camera access required to take photos.</string>
  <string name="camera_capture_failed">Could not save photo. Try again.</string>
  ```
- [ ] 2.2 Add to `frontend/mobile/app/src/main/res/values-zh/strings.xml`:
  ```xml
  <string name="camera_close_content_description">關閉</string>
  <string name="camera_capture_content_description">拍照</string>
  <string name="camera_permission_required">需要相機權限才能拍照</string>
  <string name="camera_capture_failed">無法儲存照片，請重試</string>
  ```
- [ ] 2.3 Do NOT add any `_zh`-suffixed siblings — this change uses the canonical-id + `values-zh/` pattern from the start

## 3. CameraCapturer helper

- [ ] 3.1 Create `frontend/mobile/app/src/main/java/com/mcis/memoir/camera/CaptureResult.kt`:
  ```kotlin
  sealed interface CaptureResult {
      data class Success(val uri: Uri) : CaptureResult
      data class Failure(val cause: Throwable) : CaptureResult
  }
  ```
- [ ] 3.2 Create `frontend/mobile/app/src/main/java/com/mcis/memoir/camera/CaptureSink.kt`:
  ```kotlin
  fun interface CaptureSink {
      suspend fun savePhoto(filename: String, contentResolver: ContentResolver): Uri
  }
  ```
- [ ] 3.3 Create `frontend/mobile/app/src/main/java/com/mcis/memoir/camera/ImageCaptureSink.kt`:
  ```kotlin
  class ImageCaptureSink(private val imageCapture: ImageCapture, private val executor: Executor) : CaptureSink {
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
              imageCapture.takePicture(options, executor, object : ImageCapture.OnImageSavedCallback {
                  override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                      val uri = output.savedUri
                      if (uri != null) cont.resume(uri)
                      else cont.resumeWithException(IllegalStateException("savedUri was null"))
                  }
                  override fun onError(exception: ImageCaptureException) {
                      cont.resumeWithException(exception)
                  }
              })
          }
  }
  ```
- [ ] 3.4 Create `frontend/mobile/app/src/main/java/com/mcis/memoir/camera/CameraCapturer.kt`:
  ```kotlin
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
  ```

## 4. CameraPreviewView — bind ImageCapture

- [ ] 4.1 Modify the `CameraPreviewView` Composable at `CameraPreviewScreen.kt:154-185`: add parameter `onImageCaptureReady: (ImageCapture) -> Unit`
- [ ] 4.2 Inside the Composable, add `val imageCapture = remember { ImageCapture.Builder().build() }`
- [ ] 4.3 In the existing `LaunchedEffect(Unit) { ... cameraProvider.bindToLifecycle(...) }`, append `imageCapture` as the fourth `bindToLifecycle(...)` argument: `cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)`
- [ ] 4.4 Immediately after the successful `bindToLifecycle` call, invoke `onImageCaptureReady(imageCapture)`. If `bindToLifecycle` throws (catch block), do NOT call the callback — the parent will see `imageCapture` state stay null and keep the shutter disabled

## 5. CameraPreviewScreen — wire capture flow

- [ ] 5.1 Drop the dead Settings icon block at `CameraPreviewScreen.kt:108-121` (the `UntitledIcon(imageVector = UntitledIcons.InfoIcon, contentDescription = "Settings", ...)` Box) — no click handler, no Figma intent
- [ ] 5.2 At the top of `CameraPreviewScreen` Composable, add state holders:
  ```kotlin
  val coroutineScope = rememberCoroutineScope()
  var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
  var captureInFlight by remember { mutableStateOf(false) }
  var flashAlpha by remember { mutableFloatStateOf(0f) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  ```
- [ ] 5.3 Pass `onImageCaptureReady = { imageCapture = it }` to `CameraPreviewView(...)`
- [ ] 5.4 Change the shutter button's `onClick` to a non-empty handler that fires only when `imageCapture != null && !captureInFlight`:
  ```kotlin
  onClick = {
      val ic = imageCapture ?: return@onClick
      if (captureInFlight) return@onClick
      captureInFlight = true
      errorMessage = null
      coroutineScope.launch {
          // animate flash
          launch {
              animate(0f, 1f, animationSpec = tween(50)) { v, _ -> flashAlpha = v }
              animate(1f, 0f, animationSpec = tween(150)) { v, _ -> flashAlpha = v }
          }
          val executor = ContextCompat.getMainExecutor(context)
          val capturer = CameraCapturer(ImageCaptureSink(ic, executor))
          when (val result = capturer.capture(context.contentResolver)) {
              is CaptureResult.Success -> onCaptureClick()
              is CaptureResult.Failure -> {
                  errorMessage = result.cause.localizedMessage
                      ?: context.getString(R.string.camera_capture_failed)
                  captureInFlight = false
              }
          }
      }
  }
  ```
  Note: `onCaptureClick()` causes navigation back, after which the Composable disposes — no need to reset `captureInFlight` on success
- [ ] 5.5 Disable the shutter button visually while disabled: wrap the existing `Box(...).clip(CircleShape).clickable {...}` modifier chain with `.alpha(if (imageCapture == null || captureInFlight) 0.5f else 1f)`
- [ ] 5.6 Render the white flash overlay just above the shutter button (NOT above the close button — flash should not occlude the back affordance): `Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = flashAlpha)))`. Place this overlay AFTER the `CameraPreviewView` in the Z-order but BEFORE the close/shutter buttons so the buttons remain visible during the flash
- [ ] 5.7 Render the error overlay near the top of the screen below the close button: when `errorMessage != null`, show a `Box(Modifier.fillMaxWidth().padding(top = 100.dp).background(Color.Red.copy(alpha = 0.7f))) { Text(errorMessage!!, color = Color.White, modifier = Modifier.padding(16.dp)) }` with a `LaunchedEffect(errorMessage) { delay(4000); errorMessage = null }` auto-dismiss
- [ ] 5.8 Replace `contentDescription = "Close"` at the close-button `UntitledIcon` with `stringResource(R.string.camera_close_content_description)`
- [ ] 5.9 Replace `contentDescription = "Capture"` at the shutter `UntitledIcon` with `stringResource(R.string.camera_capture_content_description)`
- [ ] 5.10 Replace the hard-coded `"Camera access required to take photos."` at the permission-denied state with `stringResource(R.string.camera_permission_required)`

## 6. MyAppNavigation — no signature change

- [ ] 6.1 Verify the `CameraPreviewDestination` entry block at `MyAppNavigation.kt:393-402` continues to call `CameraPreviewScreen(onBackClick = { backStack.removeLastOrNull() }, onCaptureClick = { backStack.removeLastOrNull() })`. This change does NOT widen the `onCaptureClick` signature — the captured Uri is intentionally not threaded back to the caller

## 7. Tests

- [ ] 7.1 Create `frontend/mobile/app/src/test/java/com/mcis/memoir/camera/CameraCapturerTest.kt` (JUnit5, MockK on the `CaptureSink` interface — pure-Kotlin interface, safe to mock):
  - Test 1: `capture()` returns `CaptureResult.Success(uri)` when fake `CaptureSink.savePhoto(...)` returns a `Uri.parse("content://media/external/images/media/42")`
  - Test 2: `capture()` returns `CaptureResult.Failure(cause)` with the original `IOException` when fake `CaptureSink.savePhoto(...)` throws
  - Test 3: filename pattern `^memoir_\d{8}_\d{6}\.jpg$` matches with an injected `now = { 1718848320000L }` (decode date locally as the test asserts the regex, not the exact wall clock)
  - Test 4: `Locale.US` lock — set `Locale.setDefault(Locale.JAPAN)`, run capture, assert the filename still matches the ASCII-digit regex (no Japanese numeral leakage)
  - Test 5: `MockK.coVerify { sink.savePhoto(any(), any()) }` called exactly once per `capture(...)` invocation
- [ ] 7.2 Run `cd frontend/mobile && ./gradlew :app:testDebugUnitTest`; confirm the new tests pass alongside all prior changes' tests
- [ ] 7.3 Do NOT write an integration test that fires the real `ImageCapture.takePicture` — requires camera hardware, brittle in CI

## 8. Verification gate

- [ ] 8.1 `cd frontend/mobile && ./gradlew :app:assembleDebug` succeeds
- [ ] 8.2 `cd frontend/mobile && ./gradlew :app:testDebugUnitTest` passes (full suite)
- [ ] 8.3 `openspec validate artifact-photo-capture --strict` reports zero issues

## 9. Manual emulator smoke

- [ ] 9.1 API 34+ emulator: home → tap a route → tap a spot → ArtifactDiscovery → Camera → grant CAMERA permission → tap shutter → see white flash → screen returns to ArtifactDiscovery → open system Photos app → confirm new file under Pictures/Memoir/ named `memoir_<timestamp>.jpg`
- [ ] 9.2 API 28 emulator: repeat 9.1; if MediaStore insert throws `SecurityException`, add a runtime `WRITE_EXTERNAL_STORAGE` prompt in this PR (per design D5 fallback note)
- [ ] 9.3 API 24 emulator (if available): repeat 9.1
- [ ] 9.4 Test capture failure path manually by toggling airplane mode + disabling storage — verify error overlay appears, dismisses after 4s, shutter re-enables
- [ ] 9.5 Test double-tap: rapid two-finger tap on shutter — verify only one file appears in MediaStore, not two
- [ ] 9.6 Toggle locale to zh → relaunch → repeat capture flow → verify chrome text (permission message, error message if triggered) renders in Chinese
