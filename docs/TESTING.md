# Validation

SwiftSave uses an Android download engine for site extraction. A successful Java
unit test or APK build does not establish that a platform currently permits a
particular download.

GitHub Actions has verified Java compilation, 247 regression assertions, APK
signatures, and bundled Python/FFmpeg/QuickJS. See `BUILD-RESULT.md` for the exact
build. Device installation and live downloads remain untested.

## Local checks

Run the project regression harness with a JDK:

```sh
./tests/run.sh
```

It currently passes 142 link/error assertions and 105 private-session assertions
using a JDK, without an Android SDK. It does not execute Android UI, storage APIs,
notifications, or downloads. CI also runs real yt-dlp offline checks for the X
preview-card failure and session cookie-jar load/save/reload behavior.

With the Android SDK and dependency repositories reachable, run the Gradle build
commands documented in the project README. Keep the full build log and the
resulting APK together for review.

## Device checks required before distribution

Use an Android 8+ physical device. Check these on at least Android 8/9 and a
recent Android release because shared storage and foreground-service behavior
change across Android versions:

- Cold launch; paste a link; receive text through the Android share sheet.
- Download an accessible public video separately from Facebook, X/Twitter,
  TikTok, and YouTube. Confirm both playback and audio in the saved result.
- Repeat in audio mode and verify that the file contains playable audio.
- Background the app and lock the device during a download. Confirm progress
  remains visible and that completion appears only after saving succeeds.
- Open Downloads and confirm the saved filename and MIME type are usable.
- Deny notification permission; grant or deny storage permission on Android 8/9.
  Confirm the app explains required storage access and does not report success
  when saving failed.
- Try an invalid link, unsupported host, private/deleted post, login-required
  post, interrupted network, and insufficient free storage. Confirm useful
  errors and that a later valid download still works.
- Rotate or reopen the activity during a running job. Confirm it restores the
  current progress and cannot start a duplicate job.
- Update the engine; verify that network failure leaves the current engine
  usable and provides a retry path.
- Open **Sign in to X**, complete X's normal login and verification, then tap
  **Use X account**. Confirm **X connected** appears and an accessible account-only
  post can be downloaded. Verify that session management is disabled during a job.
- Sign out of X and confirm the session is removed and the next X request is
  anonymous. A connected session must not be attached to another site's request.

X account support is optional and does not grant access your account lacks.
Other sites' account cookies remain unsupported. Site changes can require
an engine update and can temporarily prevent extraction.

## Current environment limits

The build environment initially had Java 21 but no Android SDK or Gradle. Its
execution sandbox blocked network sockets needed to fetch the tools and
dependencies. The wrapper bootstrap failure is captured in `build-attempt.log`.
The complete Android build succeeded remotely using GitHub Actions. Site support
and device behavior still need the checks listed above.
