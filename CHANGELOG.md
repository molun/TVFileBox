# Changelog

## 1.2.4

- Added complete English UI resources while retaining the Simplified Chinese interface.
- The app now follows the Android system language: Chinese locales use Chinese, and every non-Chinese locale falls back to English.
- Localized the file browser, dialogs, status messages, upload history actions, and the phone upload webpage.

## 1.2.3

- The app now initially focuses the Remote Transfer toolbar button instead of Home.
- File rows only show their selected highlight while the file list actually owns focus, removing the duplicate-focus appearance at startup.

## 1.2.2

- The Remote Transfer screen now focuses the first Open/Install button when it opens, including on older TV firmware that delays window focus.
- Upload action buttons are now placed directly in the remote-control focus chain; the first D-pad press also restores focus as a firmware fallback.
- D-pad Up/Down now moves directly between the same action buttons on adjacent upload rows without stopping on a row outline.
- Removed the upload row selection outline because upload rows are controlled through their action buttons.
- Restored one-tap touchscreen Open/Install behavior while keeping automatic focus after uploads.

## 1.2.1

- Removed the “Uploaded files” heading to give the upload list more room on small touchscreen speakers.
- After an upload, the list scrolls to the newest file and focuses its Open/Install button automatically.

## 1.2.0

- Replaced the generic launcher graphic with Google's Apache-2.0 `folder_open` Material Icon.
- Added touchscreen row long-press deletion with a confirmation dialog.
- Added remote OK/Enter long-press deletion with a confirmation dialog.
- Kept Settings/Menu as the focused row's Rename/Delete action menu.
- Added English and Simplified Chinese README files with language switching links.
- Added third-party icon and library notices.
