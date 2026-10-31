package app.swiftsave;

import java.net.URI;

/** Run with tests/run.sh. Exercises behavior at untrusted share-text boundaries. */
public final class RegressionTests {
    private static int assertions;

    public static void main(String[] args) throws Exception {
        supportedLinks();
        xRequestSelection();
        unsafeLinks();
        inputLimits();
        friendlyErrors();
        xErrors();
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

    private static void xRequestSelection() {
        for (String url : new String[]{"https://x.com/user/status/123", "https://mobile.twitter.com/user/status/123/video/2", "https://t.co/short"}) {
            truth(UrlPolicy.isXUrl(url), "X post and share links use one-video selection");
        }
        for (String url : new String[]{null, "https://www.youtube.com/watch?v=123", "https://x.com.evil.test/post", "https://notx.com/post", "file://x.com/post", "https://user@x.com/post"}) {
            truth(!UrlPolicy.isXUrl(url), "other sources do not receive X-specific options");
        }
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

    private static void xErrors() {
        String x = "https://x.com/example/status/2116122944376275285";
        String genericNoVideo = EngineErrors.friendly("No video could be found in this tweet");
        String xNoVideo = EngineErrors.friendly("No video could be found in this tweet", x);
        truth(!genericNoVideo.equals(xNoVideo), "X empty-media errors explain platform access ambiguity");
        truth(xNoVideo.contains("photos or text") && xNoVideo.contains("or require sign-in"), "no-video response does not assert the post has no media or requires login");

        String noFormats = EngineErrors.friendly("ERROR: [twitter] 123: No video formats found!", x);
        truth(noFormats.contains("no usable video format"), "exact yt-dlp no-video-formats error has format recovery guidance");
        truth(!noFormats.equals(xNoVideo), "missing formats and empty post response remain distinct");
        truth(EngineErrors.friendly("ERROR: No video formats found!").contains("video format"), "exact no-video-formats error also works without platform context");
        truth(EngineErrors.friendly("Unsupported URL", x).contains("/status/"), "unsupported X URL asks for a post link without asserting an API refusal");

        String guest = EngineErrors.friendly("ERROR: Unable to extract guest token", x);
        truth(guest.contains("anonymous access"), "guest-token refusal explains anonymous API access");
        truth(guest.contains("some posts") && !guest.contains("X requires sign-in"), "guest refusal does not assert every X post requires login");
        same(guest, EngineErrors.friendly("ERROR: [twitter] 123: Unable to extract guest token"), "extractor marker supplies X context without a URL");

        String[] authenticationErrors = {
            "Login required", "This tweet is protected", "This content is age-restricted",
            "NSFW tweet requires authentication", "Sensitive content is not accessible",
            "You are not authorized to view this post", "Authentication required",
            "Sign in to continue", "Account cookies are required"
        };
        for (String failure : authenticationErrors) {
            String result = EngineErrors.friendly(failure, x);
            truth(result.contains("X requires sign-in or site verification"), "explicit X authentication failure gets accurate recovery guidance");
            truth(result.contains("Use Sign in to X") && result.contains("refresh your X sign-in"), "auth guidance points to the on-device sign-in and refresh flow");
        }
        truth(EngineErrors.friendly("This tweet is not available", x).contains("X post is unavailable"), "unavailable X post has a separate message");
        truth(!EngineErrors.friendly("This tweet is not available", x).equals(guest), "unavailable post and guest failure stay distinct");
        truth(EngineErrors.friendly("HTTP Error 429: Too Many Requests", x).startsWith("X is limiting"), "X rate limit has a wait-and-retry path");
        truth(EngineErrors.friendly("HTTP Error 403: Forbidden", x).startsWith("X refused"), "X access refusal is not falsely described as confirmed account authentication");

        String[] privateDetails = {
            "No video formats found!", "No video could be found in this tweet", "Unable to extract guest token",
            "This tweet is not available", "NSFW authentication required", "HTTP Error 403: Forbidden"
        };
        for (String failure : privateDetails) {
            String result = EngineErrors.friendly(failure + " https://cdn.example.test/v?token=secret-value&signature=secret-signature", x);
            truth(!result.contains("secret-value") && !result.contains("secret-signature") && !result.contains("https://"), "X messages never expose engine URLs or signed tokens");
            truth(result.length() <= 500, "X message remains concise");
        }

        same(genericNoVideo, EngineErrors.friendly("No video could be found in this tweet", "https://x.com.evil.test/video"), "lookalike host does not select X-specific guidance");
        same(genericNoVideo, EngineErrors.friendly("No video could be found in this tweet", "not a URL"), "malformed source URL does not break error handling");
        same(genericNoVideo, EngineErrors.friendly("No video could be found in this tweet", "file://x.com/video"), "non-web source does not select X guidance");
        same(xNoVideo, EngineErrors.friendly("No video could be found in this tweet", "https://MOBILE.TWITTER.COM/example/status/123"), "Twitter subdomains and uppercase host select X guidance");
        truth(!EngineErrors.friendly("No video could be found https://x.com/user/status/1429403404", x).contains("limiting downloads"), "tweet ID digits are not interpreted as an HTTP status");
        truth(!EngineErrors.friendly("Unexpected extractor error https://cdn.example.test/v?token=404403429", x).contains("unavailable"), "signed URL digits alone are not an HTTP404");
        same(EngineErrors.friendly(null), EngineErrors.friendly(null, null), "legacy overload behavior remains available");
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
