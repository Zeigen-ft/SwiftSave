# Third-party components

SwiftSave's source was created independently for this project. The build
configuration resolves the Android downloader and FFmpeg dependencies from
Maven Central; those dependencies and their bundled Python, yt-dlp, FFmpeg,
native libraries, and other components have their own licenses.

- youtubedl-android: <https://github.com/yausername/youtubedl-android>
- yt-dlp: <https://github.com/yt-dlp/yt-dlp>
- FFmpeg: <https://ffmpeg.org/legal.html>
- Python: <https://docs.python.org/3/license.html>
- Kotlin runtime: <https://github.com/JetBrains/kotlin>

No third-party AARs, executables, or runtime files are included in this source
archive. The official upstream 0.18.1 README confirms the pinned Maven dependency
group `io.github.junkfood02.youtubedl-android` and version `0.18.1`. Its sources
confirm the bundled QuickJS runtime. The uploaded Seal credits identify
youtubedl-android as GPLv3, FFmpeg as GPLv2, yt-dlp as Unlicense, and Kotlin as
Apache 2.0. This environment could not retrieve and inspect the pinned artifacts.

- Pinned engine source and build instructions:
  <https://github.com/yausername/youtubedl-android/tree/0.18.1>
- QuickJS: <https://bellard.org/quickjs/> (MIT license)
Before redistributing an APK, retain the actual dependencies' license notices
and satisfy any applicable corresponding-source obligations, including those
of the FFmpeg build selected by the Android wrapper. Check the licenses in the
resolved artifacts and their matching upstream release sources.
