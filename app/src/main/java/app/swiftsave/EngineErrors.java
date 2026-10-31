package app.swiftsave;

import java.util.Locale;

/** Converts engine logs into useful messages without exposing private URLs or headers. */
public final class EngineErrors {
    private EngineErrors() {}

    public static String friendly(String detail) {
        return friendly(detail, null);
    }

    public static String friendly(String detail, String sourceUrl) {
        String message = detail == null ? "" : detail.toLowerCase(Locale.ROOT);
        boolean x = UrlPolicy.isXUrl(sourceUrl) || message.contains("[twitter]");
        if (message.contains("no space left") || message.contains("enospc")) {
            return "Your phone is out of storage. Free some space and try again.";
        }
        if (hasStatus(message, "429") || message.contains("too many requests") || message.contains("rate limit")) {
            return x ? "X is limiting downloads. Wait a while, then try again."
                    : "This site is limiting downloads. Wait a while, then try again.";
        }
        if (x && (message.contains("guest token") || message.contains("guest_token") || message.contains("guest api"))) {
            return "X refused anonymous access to its video API. Use Sign in to X, or update the engine and retry later; some posts need account access.";
        }
        if (message.contains("private") || message.contains("login") || message.contains("log in")
                || message.contains("sign in") || message.contains("sign-in") || message.contains("cookies")
                || message.contains("protected") || message.contains("authentication") || message.contains("authenticate")
                || message.contains("unauthorized") || message.contains("not authorized")
                || message.contains("age-restricted") || message.contains("age restricted") || message.contains("nsfw")
                || message.contains("sensitive content") || message.contains("sensitive media")
                || message.contains("captcha") || message.contains("not a bot")) {
            if (x) {
                return "X requires sign-in or site verification for this post. Use Sign in to X, then retry. If already connected, refresh your X sign-in.";
            }
            return "This video needs a login or site verification. Try a public video; account cookies aren't supported.";
        }
        if (message.contains("requested format") || message.contains("no formats") || message.contains("no video formats")) {
            if (x) {
                return "X returned no usable video format. Update the engine or refresh your X sign-in, then retry; the post may still be unavailable.";
            }
            return "No downloadable video format was found. Update the download engine and try again.";
        }
        if (message.contains("timed out") || message.contains("unable to resolve") || message.contains("network")
                || message.contains("connection") || message.contains("name resolution")) {
            return "Couldn't reach the site. Check your connection and try again.";
        }
        if (hasStatus(message, "404") || message.contains("not available") || message.contains("removed")
                || message.contains("unavailable") || message.contains("not found")) {
            return x ? "This X post is unavailable. Check that the link opens in your browser."
                    : "This video is unavailable. Check that the link opens in your browser.";
        }
        if (hasStatus(message, "403") || message.contains("forbidden")) {
            return x ? "X refused this download. Update the engine or refresh your X sign-in, then retry; the post may still be unavailable."
                    : "The site refused this download. Update the download engine, then try again.";
        }
        if (message.contains("javascript") || message.contains("js runtime") || message.contains("challenge")) {
            return "This site needs a newer extractor or JavaScript runtime. Update the engine; some videos may still be unsupported.";
        }
        if (message.contains("unsupported url")) {
            return x ? "This X link wasn't recognized. Share the post's /status/ link, or update the download engine."
                    : "No downloadable video was found. Check the link or update the download engine.";
        }
        if (message.contains("no video") || message.contains("no media")) {
            if (x) {
                return "X returned no accessible video. The post may contain only photos or text, be unavailable, or require sign-in. Try Sign in to X or update the engine.";
            }
            return "No downloadable video was found. Check the link or update the download engine.";
        }
        return "Couldn't download this video. Check the link, or update the download engine and try again.";
    }

    private static boolean hasStatus(String message, String code) {
        return message.trim().equals(code)
                || message.matches("(?s).*\\b(?:http(?: error)?|status(?: code)?|error)[:\\s]+" + code + "\\b.*");
    }

}
