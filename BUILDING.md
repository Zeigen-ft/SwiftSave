# Build SwiftSave

This source package has not been compiled into an APK in the current workspace.
The workspace has Java, but no Android SDK or Gradle installation. The included
wrapper was run with the configured proxy and a writable Gradle cache; its
download failed with `java.net.SocketException: Operation not permitted` because
the execution sandbox blocked network sockets. Requests for network access did
not complete. See `docs/build-attempt.log` for the actual bootstrap failure.
Maven dependency resolution and Android device testing remain unverified.

## Requirements

- JDK 17 (JDK 21 can also run this Gradle version)
- Gradle 8.11.1 (downloaded by the included wrapper)
- Android SDK platform 35 and build tools 35.0.0
- Internet access to Google Maven, Maven Central, and Gradle's distribution service

The Android Gradle plugin is pinned to 8.9.2. The download engine and FFmpeg
artifacts are pinned to `io.github.junkfood02.youtubedl-android` version `0.18.1`.
The dependency coordinates and Java API usage were checked against the official
upstream 0.18.1 README and source. This version bundles QuickJS for JavaScript
extraction and Python 3.12.11. The artifacts could not be fetched in this workspace.
The first connected build must resolve and compile these dependencies before
the APK can be considered ready.

## Local build

Install the SDK requirements using Android Studio's SDK Manager. Point
`ANDROID_HOME` to the SDK, or create an untracked
`local.properties` file with `sdk.dir=/absolute/path/to/android-sdk`.

From the repository directory, run:

```sh
bash tests/run.sh
./gradlew --no-daemon :app:assembleDebug
```

The universal debug APK will be at
`app/build/outputs/apk/debug/app-universal-debug.apk`. Smaller ARM64 and ARMv7 APKs
are produced alongside it; the ARM64 build is the smaller choice for most recent
phones. The universal APK can also include other ABIs from native dependencies;
its contents and size should be checked after building. Android 8.0 or newer is required.

The debug package is `app.swiftsave.debug` and is automatically signed by the
Android build tools with a local debug key. It can be installed alongside Seal.
It is intended for testing. A production release needs your own signing key.

The genuine Gradle wrapper scripts and bootstrap JAR came from the user's
uploaded Seal archive. The wrapper downloads Gradle 8.11.1 on the first build;
it still needs network access. Gradle's Apache 2.0 license and provenance are
included in `gradle/wrapper/LICENSE` and `gradle/wrapper/NOTICE`.

## GitHub Actions build

The included `.github/workflows/build-apk.yml` installs the SDK, uses the wrapper, runs
the URL/error regression suite, and builds signed debug APKs. Push the source to
a repository you control, open **Actions → Build SwiftSave APK → Run workflow**,
then download the `SwiftSave-debug-APKs` artifact from a successful run.

The workflow has not been run here. A green workflow verifies compilation, not
downloads from every supported website. Follow `docs/TESTING.md` on an ARM Android
device before distributing the app.
