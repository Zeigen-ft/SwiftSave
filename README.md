# SwiftSave

Installable APKs are available from the successful
[GitHub Actions build](https://github.com/Zeigen-ft/SwiftSave/actions/runs/41151459413) of version 0.2.0.

- [ARM64 APK ZIP — most recent phones](https://github.com/Zeigen-ft/SwiftSave/actions/runs/41151459413/artifacts/13022773303)
- [ARMv7 APK ZIP — older 32-bit phones](https://github.com/Zeigen-ft/SwiftSave/actions/runs/41151459413/artifacts/13022623481)

Extract the ZIP, then open the `.apk` to install. Android 8 or newer is required.
The private repository's downloads require your GitHub sign-in.

A small Android downloader built from scratch. Paste a public video link from
Facebook, X/Twitter, TikTok, or YouTube, or share a link to SwiftSave from another
app. Choose video or MP3 audio, then save to `Downloads/SwiftSave`.

The interface uses native Android views. Extraction and conversion run on the
phone using yt-dlp and FFmpeg through youtubedl-android. There is no hosted
backend, analytics, SwiftSave account, or API key. Optional X sign-in stays on
the device.

## What is included

- One screen: link input, paste, video/audio, download, cancel, and progress.
- Android share-sheet support for text links, including platform share messages.
- Optional X sign-in using X's own HTTPS page, with a private on-device session
  and a sign-out button. See `docs/X-DOWNLOADS.md`.
- One-video selection for X posts, avoiding an upstream preview-card failure.
- Background downloads with a foreground notification.
- MP3 audio conversion and video format selection preferring H.264/AAC, up to
  1080p, and MP4 for broad Android playback compatibility.
- Downloads storage using MediaStore on Android 10+; storage permission only on
  Android 8/9.
- A daily engine update check, plus a manual update button, to refresh yt-dlp
  when website extraction changes. A failed update check falls back to the
  installed engine.
- One job at a time, with incomplete temporary files removed after failure or
  cancellation; completed files use unique names.

## Build an APK

Use Android Studio with JDK 17 or 21 and Android SDK 35. Open this directory,
allow Gradle to sync, then choose **Build > Build App Bundle(s) / APK(s) > Build
APK(s)**. The debug APK is signed automatically with a development key.

For command-line and GitHub Actions instructions, see `BUILDING.md`. The workflow
produces APK artifacts you can download from the Actions run. A successful
build is still followed by the device checks in `docs/TESTING.md`.

## Use

1. Install the built APK on Android 8 or later. Allow installation from the app
   that opens the APK if Android asks.
2. Paste a video link, or use **Share > SwiftSave** in the source app.
3. Select **Video** or **Audio only**, then tap **Download**.
4. Use **Open downloads** to find the completed file in `Downloads/SwiftSave`.

On Android 13+, notification permission lets you see progress outside the app;
downloads can still run if you decline. Android 8/9 needs storage permission.
If a site starts failing, tap **Update engine**, then retry.

For X posts that need an account, tap **Sign in to X**, complete the normal X
login, then tap **Use X account**. Once SwiftSave shows **X connected**, retry
the post. Use **X connected → Sign out** to remove the saved session.

## Limits and current verification

Site support depends on yt-dlp and what each website makes available. X supports
your own authenticated session; other sites' account cookies are not supported.
Signing in does not grant access to deleted posts or content your account cannot
view. Paid and DRM-protected videos are outside this version's scope.
Engine 0.18.1 includes a
QuickJS JavaScript runtime for extraction. Some YouTube videos still require site
verification and will fail with an explanation. Other sites can also require login or block
automated downloads. Use public media you have permission to save.

There is no playlist support, queue, or download history in this
version. Video size is limited to 2 GB by the engine when its size is known;
temporary processing needs additional free storage.

The GitHub Actions build passed Java compilation, 247 regression assertions,
APK signature verification, and checks that Python, FFmpeg, and QuickJS are
included. Real yt-dlp checks verified X first-video selection and the exported
session's HTTPS/domain scoping. See `docs/BUILD-RESULT.md` for the exact commit and artifacts.
**Installation and live site downloads still need testing on a phone.**

## Development

```sh
./tests/run.sh
```

This harness needs only a JDK and covers public-link parsing, deceptive hosts,
shared prose, input limits, and safe error messages. See `docs/TESTING.md` for
the Android/device checks that remain necessary.

## Dependencies and source

The app code and interface were created independently. The uploaded Seal source
was used to verify dependency configuration and download engine usage; its
artwork and interface are not included. The downloader depends on
[youtubedl-android](https://github.com/yausername/youtubedl-android),
[yt-dlp](https://github.com/yt-dlp/yt-dlp), and
[FFmpeg](https://ffmpeg.org/). See `THIRD_PARTY.md` before distributing a binary.

SwiftSave's source is licensed under GPLv3; see `LICENSE`.
