package app.swiftsave;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

/** Only X authentication/CSRF cookies are exported; values must never be logged. */
public final class XCookieData {
    private static final int MAX_SIZE = 65536;
    private XCookieData() {}

    public static String toNetscape(String header) {
        if (header == null || header.isEmpty() || header.length() > MAX_SIZE || hasControls(header)) {
            throw new IllegalArgumentException("Finish signing in to X, then use this account.");
        }
        Map<String, String> cookies = new LinkedHashMap<>();
        for (String part : header.split(";")) {
            int equals = part.indexOf('=');
            if (equals < 0) continue;
            String name = part.substring(0, equals).trim();
            if (!isAuthenticationName(name)) continue;
            String value = part.substring(equals + 1).trim();
            if (!validValue(value)) throw new IllegalArgumentException("The X sign-in is incomplete. Sign in again.");
            String previous = cookies.put(name, value);
            if (previous != null && !previous.equals(value)) {
                throw new IllegalArgumentException("More than one X session was found. Sign out and sign in again.");
            }
        }
        if (!cookies.containsKey("auth_token") || !cookies.containsKey("ct0")) {
            throw new IllegalArgumentException("Finish signing in to X, then use this account.");
        }
        StringBuilder output = new StringBuilder("# Netscape HTTP Cookie File\n# SwiftSave private X session\n");
        for (String name : new String[]{"auth_token", "ct0"}) {
            // An empty expiry represents a session cookie in MozillaCookieJar.
            output.append(".x.com\tTRUE\t/\tTRUE\t\t").append(name).append('\t')
                    .append(cookies.get(name)).append('\n');
        }
        return output.toString();
    }

    public static boolean hasAuthenticationCookies(String netscape) {
        if (netscape == null || netscape.length() > MAX_SIZE) return false;
        boolean auth = false;
        boolean csrf = false;
        long now = System.currentTimeMillis() / 1000L;
        for (String line : netscape.split("\n")) {
            if (line.startsWith("#HttpOnly_")) line = line.substring("#HttpOnly_".length());
            else if (line.startsWith("#")) continue;
            String[] fields = line.split("\t", -1);
            if (fields.length != 7) continue;
            String domain = fields[0].toLowerCase(Locale.ROOT);
            if (!(domain.equals(".x.com") || domain.equals("x.com"))
                    || !fields[2].equals("/") || !fields[3].equals("TRUE") || !validValue(fields[6])) continue;
            if (!fields[4].isEmpty() && !fields[4].equals("0")) {
                try { if (Long.parseLong(fields[4]) <= now) continue; }
                catch (NumberFormatException invalid) { continue; }
            }
            if (fields[5].equals("auth_token")) auth = true;
            if (fields[5].equals("ct0")) csrf = true;
        }
        return auth && csrf;
    }

    private static boolean isAuthenticationName(String name) {
        return name.equals("auth_token") || name.equals("ct0");
    }

    private static boolean validValue(String value) {
        if (value.isEmpty() || value.length() > 8192) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= 0x20 || c >= 0x7f || c == ';') return false;
        }
        return true;
    }

    private static boolean hasControls(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) < 0x20 || text.charAt(i) == 0x7f) return true;
        }
        return false;
    }
}
