HearThisAndroid
===============

Simple Scripture recording on Android Devices

Synchronizes (over local WiFi) with HearThis for Windows. Can display, record and play back Scripture.

# Building
The project targets Android SDK 36 (Android 16) to meet Google Play's current requirements. The simplest way to get started is to install the latest stable version of Android Studio and open this repository as the project — Android Studio will handle the rest automatically.

> **Important:** use the latest stable version of Android Studio. As of development, this is **Android Studio Panda 2 | 2025.3.2**. Older versions may not recognise or be able to download the required Gradle version, causing the build to fail.

Build requirements:

- **Android Studio** — latest stable release (Panda 2 | 2025.3.2 or later); open the repository root as the project and let Android Studio sync and configure everything automatically
- **Android SDK Platform 36** — install via the SDK Manager in Android Studio (*SDK Platforms* tab, API level 36)
- **Android SDK Build-Tools** — any version compatible with API 36 (install the latest available via the SDK Manager)
- **Android SDK Platform-Tools** — the latest stable version
- **Minimum SDK**: API 23 (Android 6.0 Marshmallow) — required by the CameraX and ML Kit libraries
- **Java 17** — the project is compiled with Java 17 source and target compatibility; make sure the JDK in Android Studio is set to 17 or later
- **Gradle 9.3.1** and **Android Gradle Plugin 9.1.0** — these are managed automatically by the Gradle wrapper; no manual installation is needed

# Testing
The automated test suite has been significantly expanded and all tests are expected to pass with the current configuration.

**Unit tests** (no device or emulator required) live in `app/src/test/java/org/sil/hearthis/` and use JUnit 4 with Robolectric for any tests that need Android context. To run them in Android Studio, right-click the `org.sil.hearthis` package under that directory and choose *Run tests in 'org.sil.hearthis'*.

- `BookButtonTest.java` — tests BookButton state and progress logic
- `RecordActivityUnitTest.java` — tests the scroll-position calculation in RecordActivity (plain JUnit, no Android context needed)
- `AcceptFileHandlerTest.java` — tests the HTTP file upload handler, including path traversal protection
- `HearThisPreferencesTest.java` — tests that application preferences are correctly persisted and retrieved
- `LevelMeterViewTest.java` — tests the level update throttle logic in the audio level meter view
- `RealScriptProviderTest.java` — tests the scripture data parsing logic

**Instrumentation tests** (require an emulator or connected Android device) live in `app/src/androidTest/java/org/sil/hearthis/`. To run them, right-click the package under that directory and choose *Run tests in 'org.sil.hearthis'*.

- `MainActivityTest.java` — tests that the main activity launches and resolves to the correct next screen
- `BookSelectionTest.java` — tests the navigation flow from the book chooser to the chapter chooser
- `ProjectSelectionTest.java` — tests project listing and selection in `ChooseProjectActivity`
- `RecordActivityTest.java` — covers loading, navigation, the recording workflow, and state persistence in `RecordActivity`
- `SyncActivityTest.java` — tests initial UI state, CameraX initialisation, and `SyncService` integration in `SyncActivity`

Shared test utilities (`TestFileSystem`, `TestScriptProvider`) live in `app/src/sharedTest/java/` and are automatically included in both test source sets via `sourceSets` configuration in `app/build.gradle`.

Test library notes:

- Robolectric 4.14.1 and Java dynamic proxies are used in place of Mockito for cleaner, warning-free unit testing
- All `androidx.test` libraries are updated to versions compatible with API 36 (`espresso-core` 3.7.0, `junit-ext` 1.3.0, `uiautomator` 2.3.0)
- `espresso-intents` is included for testing intent-based navigation flows between activities

### Http Server

The application was using a deprecated http server library. We updated the server with a new library called NanoHTTPD. This allows the server to be future-proof.

Along with updating the server library we ensured that the server ran more efficiently. Tasks we looked into and updated are the following:

- Guard Against Double Start and Stop in SyncServer
- Fix Path Traversal Vulnerability in File Handlers
- Remove Static Listener Memory Leaks
- Make Notification Listener List Thread Safe
- Fix UI Thread Violations in SyncActivity
- Improve File Upload Error Handling
- Improve Resource and Stream Management
- Remove Hardcoded Device Name
- Improve Server Lifecycle Management

### Internationalization

Updated application to support various languages. The application currently supports the following:

- English
- Spanish
- French
- German
- Chinese (simplified)

### Edge-to-edge

Making the application compliant with Android visual constraints, including dynamically changing the status bar to fit within the screen layout.

### Warnings

Walked through all the files cleaning up the warnings, mainly being newer code standards, lambda functions instead of function definitions, and updating deprecated libraries and functions.

### Camera and Scanning

The application was using a deprecated `play-services-vision` (GMS Vision) library for QR code scanning. This was replaced with two modern Jetpack and ML Kit libraries.

- **ML Kit Barcode Scanning** (`com.google.mlkit:barcode-scanning`): An on-device barcode scanning library that does not require Google Play Services to be installed on the device. It replaced `play-services-vision`, which Google has deprecated in favour of the standalone ML Kit suite.
- **CameraX** (`androidx.camera:camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`): A Jetpack camera library that correctly manages the camera lifecycle and simplifies camera integration. It replaced the legacy `Camera` and `CameraSource` APIs that were coupled to the old GMS Vision workflow.

These changes required raising the minimum SDK from 21 to 23, as both CameraX and ML Kit require at least API 23.

### Build System

The build system was significantly updated to target API 36. A series of incremental changes were made across several commits.

- Upgraded Android Gradle Plugin from 7.3.1 to 9.1.0
- Upgraded Gradle wrapper from 9.2.1 to 9.3.1
- Updated `compileSdk` and `targetSdk` from 33 to 36
- Updated `minSdk` from 18 to 21 in the initial Android 15 update, then further to 23 when the camera and scanning libraries were added (see Camera and Scanning section above)
- Replaced the deprecated `jcenter()` Maven repository with `mavenCentral()`
- Removed Jetifier, which is no longer needed now that all dependencies use AndroidX-native artifacts
- Added Java 17 source and target compatibility via `compileOptions`
- Added an explicit `namespace` declaration to `app/build.gradle`, which is required by newer AGP versions
- Removed `useLibrary 'org.apache.http.legacy'`, which was only needed by the old Apache HTTP server and became unnecessary after switching to NanoHTTPD
- Cleaned up stale and deprecated entries in `gradle.properties`

### Audio suggestion

The audio functionality could be updated. On one specific device the chapter view would lag and then have audio errors. We believe that the efficiency of the audio functionality should be improved, though it does work. If this is needed for Google Play we are not sure, but it is the next big thing to update.

# License

HearThisAndroid is open source, using the [MIT License](http://sil.mit-license.org). It is Copyright SIL International.
