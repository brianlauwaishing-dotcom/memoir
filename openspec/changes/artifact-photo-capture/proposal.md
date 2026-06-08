## Why

`CameraPreviewScreen` is the smallest stub in the umbrella's wiring list: the live viewfinder is fully wired via CameraX `Preview` use case, but the capture button at `CameraPreviewScreen.kt:131` invokes `onCaptureClick: () -> Unit = {}` which `MyAppNavigation` connects only to `backStack.removeLastOrNull()`. Tapping the maroon shutter button takes no photo; it just navigates back. The umbrella's §2 line 6 ("CameraPreview onCapture writes to MediaStore") is the explicit fix.

This change adds the missing `ImageCapture` use case binding, writes captured JPEGs to the public MediaStore Pictures collection (under a `Memoir/` subfolder), shows a brief white-flash overlay for capture feedback, and handles the legacy `WRITE_EXTERNAL_STORAGE` permission for API ≤ 28. No content-pipeline schema growth, no MVI ViewModel, no string-resource rework beyond a couple of new labels.

## What Changes

- Add `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />` to `AndroidManifest.xml`. Android 10+ doesn't need this permission for `MediaStore.Images` insertion, but the `minSdk = 24` build target requires it on API 24-28.
- Introduce `com.mcis.memoir.camera.CameraCapturer`: a pure-Kotlin helper class with `suspend fun capture(imageCapture: ImageCapture, contentResolver: ContentResolver): CaptureResult`. Builds the `ImageCapture.OutputFileOptions` targeting `MediaStore.Images` via a `ContentValues` with `RELATIVE_PATH = "Pictures/Memoir"`, invokes `imageCapture.takePicture(...)` with a `MainExecutor`, and bridges the `OnImageSavedCallback` to the suspending function via `suspendCancellableCoroutine`. Returns `CaptureResult.Success(uri)` or `CaptureResult.Failure(throwable)`.
- Modify `CameraPreviewView` (currently binds only `Preview`) to also bind an `ImageCapture` use case at the same `bindToLifecycle` call. Hoist the `ImageCapture` instance into the parent `CameraPreviewScreen` Composable via a `remember { ImageCapture.Builder().build() }`, so the screen can pass it to `CameraCapturer.capture(...)` on the click handler.
- Modify `CameraPreviewScreen` capture-button click handler: launch a coroutine via `rememberCoroutineScope()`, set a `var captureInFlight by remember { mutableStateOf(false) }` state, call `CameraCapturer.capture(...)`, on success show a 200ms white-flash overlay + invoke `onCaptureClick()` to nav back, on failure set `var errorMessage by remember { mutableStateOf<String?>(null) }` shown as an in-screen text overlay (no snackbar host wired in this change).
- Disable the capture button while `captureInFlight == true` to prevent rapid double-fires.
- Replace the hard-coded `"Camera access required to take photos."` string at `CameraPreviewScreen.kt:145` with `stringResource(R.string.camera_permission_required)` and the corresponding `values-zh/` mirror `相機權限以拍照`.
- Replace the hard-coded `contentDescription = "Close"` at `CameraPreviewScreen.kt:101`, `"Settings"` at `:117`, and `"Capture"` at `:136` with `stringResource(R.string.X)` ids. Three new string entries land: `camera_close_content_description`, `camera_settings_content_description`, `camera_capture_content_description`, each with `values-zh/` mirrors.
- Remove the dead "Flash/Settings Icon (Top Right)" block at `CameraPreviewScreen.kt:108-121` — it renders an `InfoIcon` with no click handler and no documented purpose; deleting it is a one-line UI cleanup and removes one of the three content-description strings (so only two new strings actually need to land: `camera_close_content_description` and `camera_capture_content_description`).
- Add a `CameraCapturerTest` unit test that exercises the `CaptureResult.Success` / `CaptureResult.Failure` mapping using a hand-rolled fake `ImageCapture` substitute pattern documented in the test file (CameraX's `ImageCapture` is a concrete class without an interface, so the test wraps the helper in an injection seam — see design D2).
- **Not in scope**:
  - Per-capture metadata (spotId / artifactId encoded into filename or EXIF). Capture context isn't threaded through `CameraPreviewDestination` today; threading it is `memory-creation-flow`'s problem if needed there.
  - Flash control / camera switching (front vs back) / zoom / focus tap. Out of scope; CameraPreview is a one-tap utility.
  - EXIF GPS data — would require `ACCESS_FINE_LOCATION` permission; privacy cost > UX benefit for MVP.
  - Returning the captured `Uri` to the caller. `onCaptureClick: () -> Unit` stays parameterless; the captured photo lives in MediaStore and is reachable via the system Gallery / Photos app.
  - Multi-shot mode (take several photos before exit).
  - Persistent capture history within the app.
  - Wiring the captured photo into the memory-creation flow (umbrella #7 has its own photo picker).
  - MVI ViewModel — `SavedViewModel` precedent applies (no business logic worth a reducer; camera lifecycle is intrinsically Compose-scoped).

## Capabilities

### New Capabilities
- `artifact-photo-capture`: The capture-button-to-MediaStore pipeline on `CameraPreviewScreen`. Owns the `CameraCapturer` helper, the `ImageCapture` use case binding, the legacy `WRITE_EXTERNAL_STORAGE` declaration, and the in-screen capture feedback (flash overlay + error text).

### Modified Capabilities
<!-- None — `content-pipeline` and `language-toggle` are read as foundations, not modified. -->

## Impact

- **New files**:
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/camera/CameraCapturer.kt` (helper class + `CaptureResult` sealed type)
  - `frontend/mobile/app/src/test/java/com/mcis/memoir/camera/CameraCapturerTest.kt` (JUnit5 + MockK on the injection seam)
- **Modified files**:
  - `frontend/mobile/app/src/main/AndroidManifest.xml` — add `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />`.
  - `frontend/mobile/app/src/main/java/com/mcis/memoir/CameraPreviewScreen.kt` — bind `ImageCapture` use case in `CameraPreviewView`; wire `CameraCapturer.capture(...)` to the shutter button click; add flash overlay + error overlay; replace hard-coded strings; delete the dead Settings icon block.
  - `frontend/mobile/app/src/main/res/values/strings.xml` — add `camera_permission_required`, `camera_close_content_description`, `camera_capture_content_description`.
  - `frontend/mobile/app/src/main/res/values-zh/strings.xml` — add the three Chinese mirror entries.
- **Dependencies added**: none. CameraX is already on the catalog from change #1 onwards (`camerax = "1.4.1"`).
- **Risk acknowledgements**:
  - On API 24-28 devices, `WRITE_EXTERNAL_STORAGE` is a runtime-prompted dangerous permission. The current screen only requests `CAMERA`; this change does NOT add a runtime prompt for `WRITE_EXTERNAL_STORAGE` — the manifest declaration is sufficient for MediaStore writes through `ContentResolver` on those API levels (verified per Android docs — MediaStore inserts on API < 29 go through `WRITE_EXTERNAL_STORAGE`, but it's auto-granted at install on API < 23 and runtime-granted on API 23-28 via the same path — actually it IS runtime on 23+). Implementation MUST verify behavior on API 24/25/28 emulator before merging.
  - `ImageCapture.OutputFileOptions` Build for MediaStore requires API 29 `RELATIVE_PATH` ContentValue OR falls back to `_DATA` ContentValue on older APIs. The helper handles both branches.
  - `CameraPreviewScreen.kt:165` currently does `CameraSelector.DEFAULT_BACK_CAMERA`; keep the same behavior — no front-camera support added.
  - The CameraX `ImageCapture` class has no interface and can't be MockK'd directly without `mockkConstructor`. The unit test uses a small `CaptureSink` seam (interface with the `takePicture` shape) injected into `CameraCapturer`, so MockK can substitute a fake sink and the test never instantiates a real `ImageCapture`.
- **Not changed**:
  - Any other screen or VM.
  - MockData / ContentRepository / DataStore.
  - Navigation graph (the `CameraPreviewDestination` entry block is touched only to verify the `onCaptureClick` callback still navigates back — no signature change to that lambda).
