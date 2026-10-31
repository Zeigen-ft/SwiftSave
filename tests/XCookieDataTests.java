package app.swiftsave;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tests cookie transfer boundaries using dummy values; no real accounts or network calls. */
public final class XCookieDataTests {
    private static int assertions;
    private static final String AUTH = "fixture-auth";
    private static final String CSRF = "fixture-csrf";

    public static void main(String[] args) {
        exportRequiredCookies();
        rejectIncompleteOrInjectedHeaders();
        validateCookieFile();
        System.out.println("PASS: " + assertions + " assertions (private X session export)");
    }

    private static void exportRequiredCookies() {
        String exported = XCookieData.toNetscape("other=discarded-private; auth_token=" + AUTH + "; ct0=" + CSRF);
        truth(exported.startsWith("# Netscape HTTP Cookie File"), "file has a standard Netscape cookie header");
        truth(!exported.contains("discarded-private") && !exported.contains("other"), "unrelated account/tracking cookies are not exported");
        List<String[]> rows = rows(exported);
        same(2, rows.size(), "exactly the two required cookies are exported");
        Map<String, String> values = new HashMap<>();
        for (String[] row : rows) {
            same(7, row.length, "each cookie has seven Netscape columns");
            same(".x.com", row[0], "cookies are scoped to X");
            same("TRUE", row[1], "domain cookies include X subdomains");
            same("/", row[2], "cookies apply to the X request paths");
            same("TRUE", row[3], "cookies are HTTPS-only");
            same("", row[4], "session expiry is empty for Python/yt-dlp compatibility");
            values.put(row[5], row[6]);
        }
        same(AUTH, values.get("auth_token"), "authentication value is preserved");
        same(CSRF, values.get("ct0"), "CSRF value is preserved");
        truth(XCookieData.hasAuthenticationCookies(exported), "an export can be recognized as a complete session");

        String equals = XCookieData.toNetscape("auth_token=fixture=a==; ct0=csrf=b==");
        Map<String, String> equalsValues = new HashMap<>();
        for (String[] row : rows(equals)) equalsValues.put(row[5], row[6]);
        same("fixture=a==", equalsValues.get("auth_token"), "equals signs inside auth values are preserved");
        same("csrf=b==", equalsValues.get("ct0"), "equals signs inside CSRF values are preserved");

        String repeated = XCookieData.toNetscape("auth_token=" + AUTH + "; ct0=" + CSRF + "; auth_token=" + AUTH);
        same(2, rows(repeated).size(), "identical cookie duplicates do not create duplicate rows");
        truth(XCookieData.hasAuthenticationCookies(repeated), "identical duplicates remain usable");
    }

    private static void rejectIncompleteOrInjectedHeaders() {
        String[] invalid = {
            null, "", "   ",
            "auth_token=" + AUTH,
            "ct0=" + CSRF,
            "auth_token=; ct0=" + CSRF,
            "auth_token=" + AUTH + "; ct0=",
            "AUTH_TOKEN=" + AUTH + "; ct0=" + CSRF,
            "auth_token=" + AUTH + "; CT0=" + CSRF,
            "not_auth_token=" + AUTH + "; ct0=" + CSRF,
            "auth_token=" + AUTH + "; not_ct0=" + CSRF,
            "auth_token=" + AUTH + "; ct0=" + CSRF + "; auth_token=conflicting-auth",
            "auth_token=" + AUTH + "; ct0=" + CSRF + "; ct0=conflicting-csrf",
            "auth_token=" + AUTH + "\r\n.evil.test\tTRUE\t/\tTRUE\t\tauth_token\tinjected; ct0=" + CSRF,
            "auth_token=" + AUTH + "; ct0=" + CSRF + "\nother=injected",
            "auth_token=" + AUTH + "; ct0=" + CSRF + "\r",
            "auth_token=" + AUTH + "\tvalue; ct0=" + CSRF,
            "auth_token=" + AUTH + "\u0000; ct0=" + CSRF,
            "auth_token=" + AUTH + "; ct0=" + CSRF + "\u007f",
            "auth_token=" + AUTH + "; ct0=" + CSRF + "; other=" + "a".repeat(1024 * 1024)
        };
        for (String header : invalid) rejected(header);
    }

    private static void validateCookieFile() {
        String session = file(row(".x.com", "", "auth_token", AUTH), row(".x.com", "", "ct0", CSRF));
        truth(XCookieData.hasAuthenticationCookies(session), "empty expiration represents session cookies");
        truth(XCookieData.hasAuthenticationCookies(file(row("x.com", "0", "auth_token", AUTH), row("x.com", "0", "ct0", CSRF))), "zero expiry is also a session cookie");
        truth(XCookieData.hasAuthenticationCookies(file(row(".X.COM", "0", "auth_token", AUTH), row("x.com", "0", "ct0", CSRF))), "DNS domain comparison is case-insensitive");
        long now = System.currentTimeMillis() / 1000L;
        String future = Long.toString(now + 3600L);
        String past = Long.toString(now - 3600L);
        truth(XCookieData.hasAuthenticationCookies(file(row(".x.com", future, "auth_token", AUTH), row(".x.com", future, "ct0", CSRF))), "unexpired persistent cookies are usable");
        truth(!XCookieData.hasAuthenticationCookies(file(row(".x.com", past, "auth_token", AUTH), row(".x.com", future, "ct0", CSRF))), "expired authentication cookie is not usable");
        truth(!XCookieData.hasAuthenticationCookies(file(row(".x.com", future, "auth_token", AUTH), row(".x.com", past, "ct0", CSRF))), "expired CSRF cookie is not usable");
        truth(!XCookieData.hasAuthenticationCookies(file(row(".x.com", "-1", "auth_token", AUTH), row(".x.com", "0", "ct0", CSRF))), "negative Unix expiration is expired");
        truth(!XCookieData.hasAuthenticationCookies(file(row(".x.com", "never", "auth_token", AUTH), row(".x.com", "0", "ct0", CSRF))), "invalid expiration is not treated as a session cookie");

        for (String domain : new String[]{".x.com.evil.test", "evilx.com", ".twitter.com", ".example.test"}) {
            truth(!XCookieData.hasAuthenticationCookies(file(row(domain, "0", "auth_token", AUTH), row(domain, "0", "ct0", CSRF))), "unrelated cookie domains cannot authenticate X");
        }
        truth(!XCookieData.hasAuthenticationCookies(file(row(".evil.test", "0", "auth_token", AUTH), row(".x.com", "0", "ct0", CSRF))), "cookies from unrelated domains cannot be combined into a session");
        truth(!XCookieData.hasAuthenticationCookies(file(row(".x.com", "0", "auth_token", ""), row(".x.com", "0", "ct0", CSRF))), "empty authentication value is not connected");
        truth(!XCookieData.hasAuthenticationCookies(file(row(".x.com", "0", "auth_token", AUTH), row(".x.com", "0", "ct0", ""))), "empty CSRF value is not connected");
        truth(!XCookieData.hasAuthenticationCookies(file(row(".x.com", "0", "AUTH_TOKEN", AUTH), row(".x.com", "0", "ct0", CSRF))), "cookie names remain case-sensitive");
        truth(!XCookieData.hasAuthenticationCookies(file(row(".x.com", "0", "auth_token", AUTH))), "authentication cookie alone is insufficient");
        truth(!XCookieData.hasAuthenticationCookies(file(row(".x.com", "0", "ct0", CSRF))), "CSRF cookie alone is insufficient");
        truth(!XCookieData.hasAuthenticationCookies("# auth_token=" + AUTH + "; ct0=" + CSRF + "\n"), "comment text cannot supply authentication");
        truth(!XCookieData.hasAuthenticationCookies("auth_token=" + AUTH + "; ct0=" + CSRF), "a raw header is not mistaken for a Netscape file");
        truth(!XCookieData.hasAuthenticationCookies(null), "missing cookie file is disconnected");
        truth(!XCookieData.hasAuthenticationCookies(""), "empty cookie file is disconnected");
        truth(!XCookieData.hasAuthenticationCookies(".x.com\tTRUE\t/\tTRUE\t0\tauth_token\n" + row(".x.com", "0", "ct0", CSRF)), "malformed Netscape columns are rejected");
    }

    private static List<String[]> rows(String exported) {
        List<String[]> result = new ArrayList<>();
        for (String line : exported.split("\\r?\\n")) {
            if (!line.isEmpty() && !line.startsWith("#")) result.add(line.split("\\t", -1));
        }
        return result;
    }

    private static String row(String domain, String expires, String name, String value) {
        return domain + "\tTRUE\t/\tTRUE\t" + expires + "\t" + name + "\t" + value + "\n";
    }

    private static String file(String... rows) {
        return "# Netscape HTTP Cookie File\n" + String.join("", rows);
    }

    private static void rejected(String header) {
        try {
            XCookieData.toNetscape(header);
            throw new AssertionError("Accepted an incomplete or unsafe cookie header");
        } catch (IllegalArgumentException expected) {
            assertions++;
            String message = expected.getMessage();
            truth(message != null && !message.trim().isEmpty(), "invalid session has a useful error");
            truth(!message.contains(AUTH) && !message.contains(CSRF)
                    && !message.contains("conflicting-auth") && !message.contains("conflicting-csrf"), "validation errors never echo cookie values");
        }
    }

    private static void same(Object expected, Object actual, String explanation) {
        truth(expected.equals(actual), explanation);
    }

    private static void truth(boolean condition, String explanation) {
        if (!condition) throw new AssertionError(explanation);
        assertions++;
    }
}
