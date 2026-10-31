package app.swiftsave;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts a supported public website URL from a platform's shared text. */
public final class UrlPolicy {
    private static final Pattern LINK = Pattern.compile("https?://[^\\s<>\"\\p{Cntrl}]+", Pattern.CASE_INSENSITIVE);
    private static final String[] DOMAINS = {
            "facebook.com", "fb.com", "fb.watch", "twitter.com", "x.com", "t.co",
            "tiktok.com", "youtube.com", "youtu.be"
    };

    private UrlPolicy() {}

    public static String extractSupportedUrl(String sharedText) {
        if (sharedText == null || sharedText.trim().isEmpty()) {
            throw new IllegalArgumentException("Paste a video link first.");
        }
        if (sharedText.length() > 16384) {
            throw new IllegalArgumentException("That shared text is too long. Paste just the video link.");
        }
        Matcher matcher = LINK.matcher(sharedText);
        while (matcher.find()) {
            String candidate = trimPunctuation(matcher.group());
            if (candidate.length() > 4096) continue;
            try {
                URI uri = new URI(candidate);
                String host = uri.getHost();
                if (host == null || uri.getRawUserInfo() != null) continue;
                String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
                host = host.toLowerCase(Locale.ROOT);
                int port = uri.getPort();
                if (port != -1 && !(scheme.equals("https") && port == 443)
                        && !(scheme.equals("http") && port == 80)) continue;
                if (!supportedHost(host)) continue;
                // URI's multi-part constructor re-escapes signed query parameters. Preserve raw components.
                return "https://" + host + (uri.getRawPath() == null ? "" : uri.getRawPath())
                        + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
            } catch (URISyntaxException ignored) {
                // Try the next URL, if the share contains more than one.
            }
        }
        throw new IllegalArgumentException("Use a Facebook, X/Twitter, TikTok, or YouTube video link.");
    }

    private static boolean supportedHost(String host) {
        for (String domain : DOMAINS) {
            if (host.equals(domain) || host.endsWith("." + domain)) return true;
        }
        return false;
    }

    /** X status links can yield an extra preview-card entry in yt-dlp. */
    public static boolean isXUrl(String url) {
        if (url == null) return false;
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null || uri.getRawUserInfo() != null) return false;
            String scheme = uri.getScheme();
            if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) return false;
            host = host.toLowerCase(Locale.ROOT);
            return host.equals("x.com") || host.endsWith(".x.com")
                    || host.equals("twitter.com") || host.endsWith(".twitter.com")
                    || host.equals("t.co");
        } catch (URISyntaxException ignored) {
            return false;
        }
    }

    private static String trimPunctuation(String value) {
        while (!value.isEmpty()) {
            char last = value.charAt(value.length() - 1);
            if (last == '.' || last == ',' || last == '!' || last == ')' || last == ']' || last == '}') {
                value = value.substring(0, value.length() - 1);
            } else break;
        }
        return value;
    }
}
