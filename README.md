# SwiftSave

A small Android downloader built from scratch. Paste a public video link from
Facebook, X/Twitter, TikTok, or YouTube, or share a link to SwiftSave from another
app. Choose video or MP3 audio, then save to `Downloads/SwiftSave`.

The interface uses native Android views. Extraction and conversion run on the
phone using yt-dlp and FFmpeg through youtubedl-android. There is no hosted
backend, analytics, account system, or API key.

## What is included

- One screen: link input, paste, video/audio, download, cancel, and progress.
- Android share-sheet support for text links, including platform share messages.
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
produces an APK artifact you can download from the Actions run. A successful
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

## Limits and current verification

Site support depends on yt-dlp and what each website makes available. Private,
login-only, paid, DRM-protected, and other restricted videos are outside this
version's scope. Account cookies are not included. Engine 0.18.1 includes a
QuickJS JavaScript runtime for extraction. Some YouTube videos still require site
verification and will fail with an explanation. Other sites can also require login or block
automated downloads. Use public media you have permission to save.

There is no playlist support, queue, login browser, or download history in this
version. Video size is limited to 2 GB by the engine when its size is known;
temporary processing needs additional free storage.

The standalone Java checks for URL validation and error messages pass. At
creation time this environment had no Android SDK/Gradle and its execution
sandbox blocked network sockets needed to download them. The included genuine
Gradle wrapper was tested and failed at the Gradle download step; see
`docs/build-attempt.log`. **No APK build or live site download has been verified here.**
The source is provided for an Android build and device validation, without
claiming guaranteed compatibility with all links.

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
