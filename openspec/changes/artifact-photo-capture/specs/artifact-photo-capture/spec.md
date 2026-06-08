## ADDED Requirements

### Requirement: Capture button writes a JPEG to MediaStore Pictures/Memoir

Tapping the shutter button on `CameraPreviewScreen` SHALL invoke `CameraCapturer.capture(...)`, which uses the bound CameraX `ImageCapture` use case to write a JPEG file to `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` with `MIME_TYPE = "image/jpeg"`. On API 29+ the file SHALL be placed under `RELATIVE_PATH = "Pictures/Memoir"`; on API < 29 the equivalent absolute path under the device's Pictures directory SHALL be used.

#### Scenario: Successful capture writes the file and resolves a Uri
- **WHEN** the user taps the shutter button, permission is granted, and `ImageCapture` is bound
- **THEN** a new file appears under `Pictures/Memoir/memoir_<yyyyMMdd_HHmmss>.jpg` in the device's MediaStore index, AND `CameraCapturer.capture(...)` returns `CaptureResult.Success(uri)` where `uri` is the MediaStore Uri of the new file

#### Scenario: Capture failure surfaces a CaptureResult.Failure
- **WHEN** the underlying `ImageCapture.takePicture` invokes its `OnImageSavedCallback.onError(...)` (e.g. simulated I/O failure in the test seam)
- **THEN** `CameraCapturer.capture(...)` returns `CaptureResult.Failure(cause)` carrying the original exception

### Requirement: Capture filename uses a sortable timestamp format under Locale.US

The captured file name SHALL be formatted as `memoir_yyyyMMdd_HHmmss.jpg` using `SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)`. The clock source SHALL be injectable for testability (default `System::currentTimeMillis`).

#### Scenario: Filename uses the injected clock value
- **WHEN** `CameraCapturer.capture(contentResolver, now = { 1718848320000L })` is invoked
- **THEN** the filename passed into `CaptureSink.savePhoto` equals `"memoir_20240619_233200.jpg"` (UTC interpretation depends on test JVM timezone; the spec asserts the format pattern, not the exact wall-clock string)

#### Scenario: Filename format is locale-stable
- **WHEN** the test JVM default locale is `Locale.JAPAN` and `CameraCapturer.capture(...)` is invoked
- **THEN** the filename still matches the regex `^memoir_\d{8}_\d{6}\.jpg$` (no Japanese digits, no locale-specific separators) — the `Locale.US` lock on the `SimpleDateFormat` is enforced

### Requirement: ImageCapture use case is bound alongside Preview

`CameraPreviewView` SHALL bind both a `Preview` use case AND an `ImageCapture` use case in a single `ProcessCameraProvider.bindToLifecycle(...)` call. The bound `ImageCapture` instance MUST be hoisted to the parent `CameraPreviewScreen` Composable via an `onImageCaptureReady: (ImageCapture) -> Unit` callback so the parent can pass it to `CameraCapturer`.

#### Scenario: ImageCapture is bound on first composition
- **WHEN** `CameraPreviewScreen` first composes after the user grants the CAMERA permission
- **THEN** within one Compose frame after the camera is bound, the parent Composable's `imageCapture` state holds a non-null `ImageCapture` instance

#### Scenario: Shutter button is disabled until ImageCapture is ready
- **WHEN** the camera has not yet finished binding (e.g. immediately after permission grant before the `bindToLifecycle` call returns)
- **THEN** the shutter button click handler is a no-op AND the button renders at 0.5 alpha to communicate its disabled state

### Requirement: Capture flow shows a white-flash overlay and disables re-fire

While a capture is in flight (the suspending `CameraCapturer.capture(...)` has not yet resolved), `CameraPreviewScreen` SHALL render a white overlay whose alpha animates from 0 → 1 over 50ms then 1 → 0 over 150ms, AND SHALL disable the shutter button so a second tap is a no-op.

#### Scenario: Single tap fires exactly one capture
- **WHEN** the user double-taps the shutter button within 200ms
- **THEN** `CameraCapturer.capture(...)` is invoked exactly once, AND the second tap is suppressed by the `captureInFlight` guard

#### Scenario: Capture success navigates back
- **WHEN** `CameraCapturer.capture(...)` returns `CaptureResult.Success(uri)`
- **THEN** the `onCaptureClick: () -> Unit` callback parameter is invoked exactly once, AND `MyAppNavigation`'s wired handler pops the back stack

#### Scenario: Capture failure shows an inline error and re-enables the button
- **WHEN** `CameraCapturer.capture(...)` returns `CaptureResult.Failure(cause)`
- **THEN** the screen renders a translucent-red Text overlay carrying `cause.localizedMessage` (or `stringResource(R.string.camera_capture_failed)` if the message is null), AND `captureInFlight` flips back to false so the user can retry

### Requirement: WRITE_EXTERNAL_STORAGE manifest entry covers legacy APIs

`AndroidManifest.xml` SHALL declare `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />` so MediaStore inserts work on API 24-28 without a runtime prompt. No new permissions are required on API 29+.

#### Scenario: Manifest declares the legacy permission with maxSdkVersion cap
- **WHEN** the repo is inspected after this change lands
- **THEN** `AndroidManifest.xml` contains exactly one `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />` entry, AND no other new permission is declared

### Requirement: Camera screen uses stringResource for chrome text, with the dead Settings icon removed

`CameraPreviewScreen` SHALL NOT carry any hard-coded English Kotlin string literal in the UI tree. The `"Camera access required to take photos."` permission-denied message and the contentDescription literals for the close and capture buttons MUST be replaced with `stringResource(R.string.X)` lookups against the canonical (non-`_zh`-suffixed) ids. The dead Settings icon block at `CameraPreviewScreen.kt:108-121` SHALL be deleted entirely.

#### Scenario: No hard-coded English strings remain on the screen
- **WHEN** the source of `CameraPreviewScreen.kt` is inspected after this change lands
- **THEN** `grep -nE '"Camera access required|"Close"|"Settings"|"Capture"' CameraPreviewScreen.kt` returns zero matches, AND the file references the three new string ids (`camera_permission_required`, `camera_close_content_description`, `camera_capture_content_description`) via `stringResource(...)`

#### Scenario: Settings icon block is gone
- **WHEN** the source of `CameraPreviewScreen.kt` is inspected after this change lands
- **THEN** the file contains no `UntitledIcon(imageVector = UntitledIcons.InfoIcon, contentDescription = "Settings", ...)` block, AND no Composable renders a non-clickable icon at the top-right of the camera screen

### Requirement: CameraCapturer is unit-testable via the CaptureSink seam

`CameraCapturer` SHALL depend on a `fun interface CaptureSink { suspend fun savePhoto(filename: String, contentResolver: ContentResolver): Uri }` rather than directly on CameraX's `ImageCapture`. Production code SHALL wrap `ImageCapture.takePicture` in `ImageCaptureSink(imageCapture)`. Tests SHALL substitute a hand-rolled fake `CaptureSink` and avoid `mockkConstructor` on CameraX types.

#### Scenario: Unit test substitutes a fake sink and asserts Success mapping
- **WHEN** `CameraCapturer(sink = FakeSink(returns = mockUri)).capture(contentResolver, now = { 0L })` is invoked
- **THEN** the call returns `CaptureResult.Success(mockUri)` and `FakeSink.savePhotoCallCount == 1`

#### Scenario: Unit test substitutes a throwing sink and asserts Failure mapping
- **WHEN** `CameraCapturer(sink = ThrowingSink(throws = IOException("disk full"))).capture(...)` is invoked
- **THEN** the call returns `CaptureResult.Failure(cause)` where `cause is IOException` AND `cause.message == "disk full"`

### Requirement: onCaptureClick callback signature stays parameterless

The `CameraPreviewScreen.onCaptureClick: () -> Unit` callback parameter SHALL NOT be widened to carry the captured `Uri` in this change. The captured photo is reachable via the system MediaStore index; downstream consumers (future memory-creation flow) read MediaStore directly rather than receiving the Uri through navigation state.

#### Scenario: Composable signature does not expose the Uri
- **WHEN** the source of `CameraPreviewScreen.kt` is inspected after this change lands
- **THEN** the public `CameraPreviewScreen` function declares `onCaptureClick: () -> Unit`, NOT `onCaptureClick: (Uri) -> Unit`

#### Scenario: MyAppNavigation's CameraPreviewDestination wiring is unchanged
- **WHEN** the source of `MyAppNavigation.kt` is inspected after this change lands
- **THEN** the `CameraPreviewDestination` entry block still wires `onCaptureClick = { backStack.removeLastOrNull() }` — no other navigation behavior is added by this change
