package app.swiftsave;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Device-private session storage, excluded from Android backup. */
public final class XSessionStore {
    private XSessionStore() {}

    private static File sessionFile(Context context) {
        return new File(new File(context.getNoBackupFilesDir(), "x-session"), "cookies.txt");
    }

    public static synchronized boolean isConnected(Context context) {
        File file = sessionFile(context);
        if (!file.isFile() || file.length() > 65536) return false;
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            StringBuilder text = new StringBuilder();
            char[] buffer = new char[2048];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                text.append(buffer, 0, count);
                if (text.length() > 65536) return false;
            }
            return XCookieData.hasAuthenticationCookies(text.toString());
        } catch (IOException ignored) {
            return false;
        }
    }

    public static synchronized File getCookieFile(Context context) {
        return isConnected(context) ? sessionFile(context) : null;
    }

    public static synchronized void saveFromCookieHeader(Context context, String header) throws IOException {
        final String contents;
        try { contents = XCookieData.toNetscape(header); }
        catch (IllegalArgumentException incomplete) { throw new IOException(incomplete.getMessage()); }
        File destination = sessionFile(context);
        File parent = destination.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Couldn't save the X sign-in on this phone.");
        File temporary = File.createTempFile("session-", ".tmp", parent);
        try {
            temporary.setReadable(false, false);
            temporary.setWritable(false, false);
            temporary.setReadable(true, true);
            temporary.setWritable(true, true);
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                output.write(contents.getBytes(StandardCharsets.UTF_8));
                output.getFD().sync();
            }
            if (!temporary.renameTo(destination)) throw new IOException("Couldn't finish saving the X sign-in.");
        } finally {
            temporary.delete();
        }
    }

    public static synchronized void clear(Context context) throws IOException {
        File file = sessionFile(context);
        if (file.exists() && !file.delete()) throw new IOException("Couldn't remove the saved X sign-in. Try again.");
    }
}
