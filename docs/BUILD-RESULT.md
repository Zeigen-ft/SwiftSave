# APK build result

- Build: <https://github.com/Zeigen-ft/SwiftSave/actions/runs/41113795641>
- Result: success, 2026-10-30.
- Source commit: `0e917415810e24a4e805dcefd9ffd57b7d3650f8`.
- Package: `app.swiftsave.debug`; version `0.1.0-debug`; Android 8+.

The build passed Java compilation, all 83 URL/error regression assertions,
APK signature verification, and checks that the ARM builds include Python,
FFmpeg, and QuickJS. The app uses download engine 0.18.1.

## Downloads

Sign in to GitHub to access this private repository's artifacts. Extract the
ZIP, then open the `.apk` to install.

| Build | ZIP size | Link |
| --- | ---: | --- |
| ARM64 — most recent phones | 53.9 MiB | [Download](https://github.com/Zeigen-ft/SwiftSave/actions/runs/41113795641/artifacts/13004278838) |
| ARMv7 — older 32-bit phones | 47.5 MiB | [Download](https://github.com/Zeigen-ft/SwiftSave/actions/runs/41113795641/artifacts/13005051785) |
| Universal — larger multi-ABI build | 192.7 MiB | [Download](https://github.com/Zeigen-ft/SwiftSave/actions/runs/41113795641/artifacts/13005850724) |

ARM64 contains `app-arm64-v8a-debug.apk`; ARMv7 contains
`app-armeabi-v7a-debug.apk`.

Phone installation and live site downloads were not tested in this build.
Follow `TESTING.md` for those checks. No claim is made that every public video
from every supported site is downloadable.
