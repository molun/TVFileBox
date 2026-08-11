# TVFileBox

<a href="README.md">English <img src="https://flagcdn.com/gb.svg" height="14" alt="United Kingdom flag"></a> | <a href="README.zh-CN.md">简体中文 <img src="https://flagcdn.com/cn.svg" height="14" alt="China flag"></a>

TVFileBox is a lightweight file manager and local Wi-Fi upload tool designed for Android TV boxes, including legacy devices running Android 4.0.

The interface follows the Android system language. Chinese system locales use Simplified Chinese; all other locales use English. This also applies to the phone upload webpage.

## Features

- Browse the device's default shared-storage directory with a TV remote or touchscreen.
- Create and name folders in the directory currently displayed by the file browser.
- Press Home to return to the initial shared-storage directory, such as `/storage/emulated/0`.
- Press the remote Back key to move to the parent directory.
- Press OK/Enter to open a file or enter a folder.
- Long-press OK/Enter, or long-press a row on a touchscreen, to delete a file or folder after a confirmation dialog.
- Press the remote Settings/Menu key to open the focused row's Rename/Delete menu.
- Open APK files with Android's system package installer.
- Open Remote Transfer to display a QR code and receive files from a phone browser on the same local network.
- Show upload progress, exact storage path, uploaded-file history, and Open/Delete actions.
- Stop the embedded HTTP server as soon as the Remote Transfer screen closes.

## Remote controls

| Action | Remote input |
| --- | --- |
| Move through files | D-pad Up/Down |
| Open file / enter folder | OK, D-pad Center, or Enter |
| Delete selected item | Long-press OK, D-pad Center, or Enter |
| Rename/Delete menu | Settings or Menu |
| Parent directory | Back |
| Initial directory | Home button in the app toolbar |

All delete operations display a second confirmation dialog and are permanent.

## APK selection

- `TVFileBox-v1.2.4-armeabi-v7a.apk`: 32-bit ARM TV boxes.
- `TVFileBox-v1.2.4-arm64-v8a.apk`: 64-bit ARM devices.
- `TVFileBox-v1.2.4-universal.apk`: universal build for general installation.

TVFileBox currently contains no native `.so` libraries, so the three requested ABI-labeled APKs are byte-identical and compatible with both ARMv7 and ARM64. The universal file is recommended for most users.

Minimum Android version: Android 4.0 (API 14).

The release APKs are debug-signed for testing and can update earlier TVFileBox builds signed with the same debug key.

## Open and build

1. Open this repository in Android Studio.
2. Let Gradle sync the project.
3. Connect an Android TV box or emulator.
4. Run the `app` configuration, or build from PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug
```

## Storage and compatibility notes

- The default upload directory is obtained from `getExternalFilesDir("uploads")`; Android removes it when the app is uninstalled.
- A regular, non-root application cannot browse protected directories such as `/data`.
- Writing to removable SD cards on Android 4.4 may be restricted by the device firmware.
- APK installation is never silent. The user must confirm installation in Android's system installer.
- Android 8 and later may require enabling “Install unknown apps” for TVFileBox.
- The current `targetSdk` is 28 to preserve legacy direct-path behavior on old TV boxes. This configuration is intended for sideloading, not direct Google Play submission.
- Some OEM remotes reserve the Settings key. TVFileBox also accepts the Menu key as a fallback.

## Security

- The upload URL contains a random session token.
- The embedded HTTP server only runs while the Remote Transfer screen is visible.
- Uploaded filenames are sanitized to prevent path traversal.
- Uploads are written to temporary `.part` files before being finalized.
- The phone and TV must be reachable on the same local network; router AP isolation can block access.

## Icon and third-party software

The application icon uses Google's `folder_open` Material Icon under the Apache License 2.0. NanoHTTPD and ZXing are also used. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for sources and licenses.

## Project license

No license has been assigned to the TVFileBox source code yet. Third-party components remain under their respective licenses.
