package app.swiftsave;

/** Dummy credentials only, for checking the export with the real yt-dlp cookie jar. */
public final class CookieExportFixture {
    private CookieExportFixture() {}

    public static void main(String[] args) {
        System.out.print(XCookieData.toNetscape("auth_token=fixture-auth; ct0=fixture-csrf; other=discarded"));
    }
}
