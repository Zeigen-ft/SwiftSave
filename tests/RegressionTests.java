package app.swiftsave;

import java.net.URI;

/** Run with tests/run.sh. Exercises behavior at untrusted share-text boundaries. */
public final class RegressionTests {
    private static int assertions;

    public static void main(String[] args) throws Exception {
        supportedLinks();
        unsafeLinks();
        inputLimits();
        friendlyErrors();
        System.out.println("PASS: " + assertions + " assertions (URL policy and error messages)");
    }

    private static void supportedLinks() throws Exception {
        String[] links = {
            "https://www.youtube.com/watch?v=demo&feature=share",
            "https://youtu.be/demo?t=12",
            "https://m.youtube.com/shorts/demo",
            "https://facebook.com/user/videos/123/",
            "https://www.facebook.com/reel/123",
            "https://fb.com/watch/123",
            "https://fb.watch/example/",
            "https://x.com/user/status/123?s=20",
            "https://mobile.twitter.com/user/status/123",
            "https://t.co/example",
            "https://www.tiktok.com/@user/video/123",
            "https://vm.tiktok.com/example/",
            "https://vt.tiktok.com/example/"
        };
        for (String link : links) {
            same(link, UrlPolicy.extractSupportedUrl(link), "supported platform URL");
        }
        same("https://x.com/user/status/123", UrlPolicy.extractSupportedUrl("http://x.com/user/status/123"), "upgrade HTTP");
        same("https://youtu.be/demo", UrlPolicy.extractSupportedUrl("Watch this video: https://youtu.be/demo then share it"), "extract from share prose");
        same("https://x.com/user/status/123", UrlPolicy.extractSupportedUrl("https://example.com/unrelated https://x.com/user/status/123"), "skip unsupported URL in share prose");
        same("https", new URI(UrlPolicy.extractSupportedUrl("HTTPS://YOUTU.BE/demo")).getScheme().toLowerCase(), "case-insensitive URL scheme");
        truth(new URI(UrlPolicy.extractSupportedUrl("https://YouTube.com/watch?v=demo")).getHost().equalsIgnoreCase("youtube.com"), "case-insensitive hostname");
    }

    private static void unsafeLinks() {
        String[] links = {
            null,
            "",
            "  \n  ",
            "no URL here",
            "youtube.com/watch?v=demo",
            "javascript:https://not-a-platform.example/",
            "file:///etc/passwd",
            "content://media/external/video/media/1",
            "https://example.com/video.mp4",
            "https://youtube.com.evil.test/watch?v=demo",
            "https://notyoutube.com/watch?v=demo",
            "https://youtube-com.example/watch?v=demo",
            "https://facebook.com.evil.test/reel/123",
            "https://evil-twitter.com/user/status/123",
            "https://tiktok.com.evil.test/@user/video/123",
            "https://youtube.com@evil.test/video",
            "https://user:password@youtube.com/watch?v=demo",
            "https://127.0.0.1/video.mp4",
            "https://localhost/video",
            "https://[::1]/video",
            "https://youtube.com:8443/watch?v=demo",
            "http://youtube.com:443/watch?v=demo",
            "https://youtube.com:80/watch?v=demo"
        };
        for (String link : links) rejected(link);
    }

    private static void inputLimits() {
        rejected("a".repeat(16385));
        rejected("https://x.com/" + "a".repeat(4096));
        // Large clipboard content should fail predictably, even when it starts
        // with a valid link: callers must not scan unbounded shared input.
        rejected("https://x.com/user/status/123 " + "a".repeat(16384));
        truth(UrlPolicy.extractSupportedUrl("prefix " + "a".repeat(15000) + " https://x.com/user/status/123").contains("x.com"), "bounded long share text remains usable");
    }

    private static void friendlyErrors() {
        String secret = "https://cdn.example.test/video?token=opaque-session-value&signature=do-not-display";
        String[] failures = {
            null, "", "ERROR: HTTP Error 429: Too Many Requests " + secret,
            "ERROR: HTTP Error 403: Forbidden " + secret,
            "ERROR: Sign in to confirm your age. " + secret,
            "ERROR: Login required " + secret,
            "ERROR: Unable to download webpage: network is unreachable " + secret,
            "ERROR: Connection timed out " + secret,
            "ERROR: CAPTCHA challenge " + secret,
            "ERROR: Requested format is not available " + secret,
            "ERROR: No supported JavaScript runtime could be found " + secret,
            "ERROR: Something unexpected happened " + secret
        };
        for (String failure : failures) {
            String message = EngineErrors.friendly(failure);
            truth(message != null && !message.trim().isEmpty(), "error has useful display text");
            truth(message.length() <= 500, "error stays concise");
            truth(!message.contains(secret) && !message.contains("opaque-session-value") && !message.contains("do-not-display"), "error does not expose raw signed URLs");
        }
        truth(!EngineErrors.friendly("HTTP Error 429: Too Many Requests").equals(EngineErrors.friendly("Login required")), "rate limit and login errors have distinct recovery guidance");
        truth(!EngineErrors.friendly("Requested format is not available").equals(EngineErrors.friendly("Video is not available")), "format failure and unavailable video need different recovery guidance");
    }

    private static void rejected(String input) {
        try {
            UrlPolicy.extractSupportedUrl(input);
            throw new AssertionError("Accepted unsafe/invalid input: " + input);
        } catch (IllegalArgumentException expected) {
            assertions++;
        }
    }

    private static void same(Object expected, Object actual, String explanation) {
        truth(expected.equals(actual), explanation + ": expected " + expected + ", received " + actual);
    }

    private static void truth(boolean condition, String explanation) {
        if (!condition) throw new AssertionError(explanation);
        assertions++;
    }
}
