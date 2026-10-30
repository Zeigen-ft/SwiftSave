package app.swiftsave;

import java.util.Locale;

/** Converts engine logs into useful messages without exposing private URLs or headers. */
public final class EngineErrors {
    private EngineErrors() {}

    public static String friendly(String detail) {
        String message = detail == null ? "" : detail.toLowerCase(Locale.ROOT);
        if (message.contains("no space left") || message.contains("enospc")) {
            return "Your phone is out of storage. Free some space and try again.";
        }
        if (message.contains("429") || message.contains("too many requests") || message.contains("rate limit")) {
            return "This site is limiting downloads. Wait a while, then try again.";
        }
        if (message.contains("private") || message.contains("login") || message.contains("log in")
                || message.contains("sign in") || message.contains("sign-in") || message.contains("cookies")
                || message.contains("age-restricted") || message.contains("captcha") || message.contains("not a bot")) {
            return "This video needs a login or site verification. Try a public video; account cookies aren't supported.";
        }
        if (message.contains("requested format") || message.contains("no formats")) {
            return "No downloadable video format was found. Update the download engine and try again.";
        }
        if (message.contains("404") || message.contains("not available") || message.contains("removed")
                || message.contains("unavailable") || message.contains("not found")) {
            return "This video is unavailable. Check that the link opens in your browser.";
        }
        if (message.contains("timed out") || message.contains("unable to resolve") || message.contains("network")
                || message.contains("connection") || message.contains("name resolution")) {
            return "Couldn't reach the site. Check your connection and try again.";
        }
        if (message.contains("403") || message.contains("forbidden")) {
            return "The site refused this download. Update the download engine, then try again.";
        }
        if (message.contains("javascript") || message.contains("js runtime") || message.contains("challenge")) {
            return "This site needs a newer extractor or JavaScript runtime. Update the engine; some videos may still be unsupported.";
        }
        if (message.contains("unsupported url") || message.contains("no video") || message.contains("no formats")
                || message.contains("requested format")) {
            return "No downloadable video was found. Check the link or update the download engine.";
        }
        return "Couldn't download this video. Check the link, or update the download engine and try again.";
    }
}
