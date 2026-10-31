# X downloads in SwiftSave 0.2.0

X can return "No video could be found" when an anonymous request cannot access
a post. This response can also describe an unavailable post, so the app should
not assert that it lacks video or that every X post requires login.

The original app had no X account support. Version 0.2.0 adds optional sign-in
and clearer errors. It also selects one X video per request to avoid processing
an extra preview card that can fail after a real video succeeds.

## Sign in on your phone

1. Tap **Sign in to X** in SwiftSave.
2. Sign in on X's HTTPS page using your X username and password. Complete any
   verification that X requests. External browser sign-ins do not connect the
   app's WebView session.
3. Tap **Use X account** after completing sign-in.
4. When SwiftSave shows **X connected**, retry the video link.
5. To remove the session, tap **X connected → Sign out**.

The session stays in the app's private storage, excluded from Android backup.
Only the X authentication and CSRF cookies are exported for the download engine,
and they are scoped to HTTPS X domains. They are not logged, sent to a SwiftSave
server, copied to the clipboard, or attached to downloads from other sites.

## What was checked

- Java regression checks cover X-specific error guidance and safe URL handling.
- Cookie export checks use synthetic values to cover incomplete sessions,
  injection attempts, expiry, and domain restrictions.
- A real yt-dlp cookie-jar check loads the actual Java export, verifies X HTTPS
  requests receive it, verifies unrelated domains do not, then saves and reloads
  the session successfully.
- An offline yt-dlp fixture reproduces the preview-card failure and verifies
  first-video selection avoids it without any network or media download.
- Anonymous metadata probes returned no video for the reported post and an old
  public control. They did not fetch images, media segments, or descriptions and
  do not prove that signed-in access succeeds.

Live account sign-in and authenticated downloads need testing on the phone.
The app does not bypass X verification or give an account access it lacks.

## Upstream evidence

- [Preview-card failure, yt-dlp issue 17939](https://github.com/yt-dlp/yt-dlp/issues/17939)
- [Maintainer explanation of ambiguous unavailable/restricted responses](https://github.com/yt-dlp/yt-dlp/issues/14742#issuecomment-3443694079)
- [Maintainer explanation of valid authenticated X cookies](https://github.com/yt-dlp/yt-dlp/issues/17563#issuecomment-5464743152)
