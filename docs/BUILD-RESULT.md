# APK build result

- Build: <https://github.com/Zeigen-ft/SwiftSave/actions/runs/41151459413>
- Result: success, 2026-10-31.
- Source commit: `8ed356c5da166c65085be80ea060fb53d50a08c8`.
- Package: `app.swiftsave.debug`; version `0.2.0-debug`; Android 8+.

The build passed Java compilation, all 247 URL/error/session regression assertions,
APK signature verification, and checks that the ARM builds include Python,
FFmpeg, and QuickJS. The app uses download engine 0.18.1.
Real yt-dlp checks passed for the offline X preview-card regression and for the
Java-exported session's HTTPS/X-domain scoping through cookie-jar load/save/reload.

Version 0.2.0 adds optional on-device X sign-in and fixes first-video selection
for X posts. See `X-DOWNLOADS.md` for instructions and the limits of verification.

## Downloads

Sign in to GitHub to access this private repository's artifacts. Extract the
ZIP, then open the `.apk` to install.

| Build | ZIP size | Link |
| --- | ---: | --- |
| ARM64 — most recent phones | 53.9 MiB | [Download](https://github.com/Zeigen-ft/SwiftSave/actions/runs/41151459413/artifacts/13022773303) |
| ARMv7 — older 32-bit phones | 47.5 MiB | [Download](https://github.com/Zeigen-ft/SwiftSave/actions/runs/41151459413/artifacts/13022623481) |

ARM64 contains `app-arm64-v8a-debug.apk`; ARMv7 contains
`app-armeabi-v7a-debug.apk`.

Phone installation and live site downloads were not tested in this build.
Follow `TESTING.md` for those checks. No claim is made that every public video
from every supported site is downloadable.
