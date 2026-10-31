"""Check the app's real Java export using yt-dlp's cookie jar and fake tokens."""
import sys
from yt_dlp.cookies import YoutubeDLCookieJar

jar = YoutubeDLCookieJar(sys.argv[1])
jar.load(ignore_discard=True, ignore_expires=True)
header = jar.get_cookie_header("https://api.x.com/1.1/") or ""
assert "auth_token=fixture-auth" in header
assert "ct0=fixture-csrf" in header
assert "other=" not in header
assert not jar.get_cookie_header("https://www.youtube.com/")
assert not jar.get_cookie_header("https://x.com.evil.test/")
jar.save(ignore_discard=True, ignore_expires=True)
reloaded = YoutubeDLCookieJar(sys.argv[1])
reloaded.load(ignore_discard=True, ignore_expires=True)
assert "auth_token=fixture-auth" in (reloaded.get_cookie_header("https://x.com/") or "")
print("PASS: exported X session is recognized by yt-dlp and stays scoped to X")
