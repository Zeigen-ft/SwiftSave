package app.swiftsave;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Unit;

/** One foreground job at a time; no server, analytics, or account credentials. */
public final class DownloadService extends Service {
    public static final String ACTION_STATUS = "app.swiftsave.DOWNLOAD_STATUS";
    public static final String ACTION_UPDATE = "app.swiftsave.UPDATE_ENGINE";
    public static final String ACTION_CANCEL = "app.swiftsave.CANCEL";
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_AUDIO_ONLY = "audio_only";
    private static final String CHANNEL = "downloads";
    private static final int NOTIFICATION_ID = 11;
    private static volatile boolean running;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean cancelled;
    private volatile boolean saving;
    private volatile String processId;
    private long lastProgressAt;
    private int latestStartId;
    private PowerManager.WakeLock wakeLock;

    public static boolean isRunning() { return running; }

    @Override public void onCreate() {
        super.onCreate();
        NotificationChannel channel = new NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Download progress and results");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        latestStartId = startId;
        if (intent == null) { stopSelf(startId); return START_NOT_STICKY; }
        if (ACTION_CANCEL.equals(intent.getAction())) {
            if (running) {
                cancelled = true;
                publish("Cancelling…", -1, true, false);
                // Engine destruction may block; never do it on the service's main thread.
                new Thread(() -> {
                    String id = processId;
                    if (id != null) YoutubeDL.getInstance().destroyProcessById(id);
                }, "cancel-download").start();
            } else stopSelf(startId);
            return START_NOT_STICKY;
        }
        if (running) return START_NOT_STICKY;
        final boolean updating = ACTION_UPDATE.equals(intent.getAction());
        final String url;
        try {
            url = updating ? null : UrlPolicy.extractSupportedUrl(intent.getStringExtra(EXTRA_URL));
        } catch (IllegalArgumentException error) {
            // A foreground start must be promoted promptly, even if its input is invalid.
            startForegroundNow("Checking link…");
            publish(error.getMessage(), -1, false, false);
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        running = true;
        cancelled = false;
        saving = false;
        boolean audioOnly = intent.getBooleanExtra(EXTRA_AUDIO_ONLY, false);
        startForegroundNow(updating ? "Updating download engine…" : "Preparing download…");
        publish(updating ? "Updating download engine…" : "Preparing download…", -1, true, false);
        worker.execute(() -> runJob(url, audioOnly, updating));
        return START_NOT_STICKY;
    }

    private void startForegroundNow(String title) {
        Notification notification = notification(title, -1, true);
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else startForeground(NOTIFICATION_ID, notification);
    }

    private void runJob(String url, boolean audioOnly, boolean updating) {
        File workDirectory = new File(getCacheDir(), "job-" + UUID.randomUUID());
        String finalStatus;
        boolean success = false;
        try {
            PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SwiftSave:download");
            wakeLock.acquire(2 * 60 * 60 * 1000L);
            // A previous process kill can leave temporary fragments, never published as completed files.
            clearOldJobs(workDirectory);
            YoutubeDL.getInstance().init(getApplicationContext());
            FFmpeg.getInstance().init(getApplicationContext());
            checkCancelled();
            if (updating) {
                YoutubeDL.getInstance().updateYoutubeDL(getApplicationContext(), YoutubeDL.UpdateChannel._STABLE);
                markEngineUpdated();
                checkCancelled();
                finalStatus = "Download engine is up to date. You're ready to try a link.";
            } else {
                long lastUpdate = getSharedPreferences("engine", MODE_PRIVATE).getLong("updated_at", 0L);
                if (System.currentTimeMillis() - lastUpdate > 24 * 60 * 60 * 1000L) {
                    publish("Checking for download engine updates…", -1, true, false);
                    try {
                        YoutubeDL.getInstance().updateYoutubeDL(getApplicationContext(), YoutubeDL.UpdateChannel._STABLE);
                        markEngineUpdated();
                    } catch (Exception ignored) {
                        // The bundled engine is still useful when its update host is unavailable.
                        publish("Couldn't check for updates. Trying the installed engine…", -1, true, false);
                    }
                    checkCancelled();
                }
                if (!workDirectory.mkdirs()) throw new IOException("Unable to create temporary download folder");
                YoutubeDLRequest request = new YoutubeDLRequest(url);
                request.addOption("--no-playlist");
                request.addOption("--no-mtime");
                request.addOption("--restrict-filenames");
                request.addOption("--socket-timeout", "25");
                request.addOption("--retries", "3");
                request.addOption("--fragment-retries", "3");
                request.addOption("--max-filesize", "2G");
                request.addOption("-o", new File(workDirectory, "%(title).100B_%(id)s.%(ext)s").getAbsolutePath());
                if (audioOnly) {
                    request.addOption("-f", "bestaudio/best");
                    request.addOption("-x");
                    request.addOption("--audio-format", "mp3");
                    request.addOption("--audio-quality", "192K");
                } else {
                    request.addOption("-f", "bv*+ba/b");
                    request.addOption("-S", "vcodec:h264,acodec:aac,res:1080,ext:mp4:m4a");
                    request.addOption("--merge-output-format", "mp4/mkv");
                }
                processId = UUID.randomUUID().toString();
                checkCancelled();
                YoutubeDL.getInstance().execute(request, processId, (progress, eta, line) -> {
                    long now = android.os.SystemClock.elapsedRealtime();
                    if (now - lastProgressAt > 650 && !cancelled) {
                        lastProgressAt = now;
                        int percent = Math.max(0, Math.min(100, Math.round(progress)));
                        String text = percent >= 100 ? "Processing media…"
                                : "Downloading · " + percent + "%" + (eta > 0 ? " · " + eta + "s left" : "");
                        publish(text, percent, true, false);
                    }
                    return Unit.INSTANCE;
                });
                checkCancelled();
                saving = true;
                publish("Saving to Downloads/SwiftSave…", -1, true, false);
                File[] entries = workDirectory.listFiles();
                List<File> completed = new ArrayList<>();
                if (entries != null) for (File file : entries) {
                    if (file.isFile() && file.length() > 0 && isMediaFile(file.getName())) completed.add(file);
                }
                if (completed.isEmpty()) throw new IOException("No video was downloaded");
                List<Uri> published = new ArrayList<>();
                try {
                    for (File file : completed) {
                        checkCancelled();
                        published.add(publishFile(file));
                    }
                    checkCancelled();
                } catch (Exception error) {
                    for (Uri uri : published) deletePublished(uri);
                    throw error;
                }
                finalStatus = completed.size() == 1 ? "Saved to Downloads/SwiftSave. Open downloads to watch or share."
                        : "Saved " + completed.size() + " files to Downloads/SwiftSave.";
            }
            success = true;
        } catch (Exception error) {
            finalStatus = cancelled ? "Cancelled. No incomplete files were saved."
                    : EngineErrors.friendly(error.getMessage());
        } finally {
            processId = null;
            deleteTree(workDirectory);
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
            wakeLock = null;
        }
        final String result = finalStatus;
        final boolean completed = success;
        // Service starts, foreground cleanup and idle state transitions all run on the main thread.
        main.post(() -> {
            stopForeground(STOP_FOREGROUND_REMOVE);
            try { getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, notification(result, -1, false)); }
            catch (SecurityException ignored) { /* Download still succeeds when notifications are declined. */ }
            stopSelf(latestStartId);
            running = false;
            publish(result, completed ? 100 : -1, false, completed);
        });
    }

    private Uri publishFile(File source) throws IOException {
        String extension = source.getName().substring(source.getName().lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (mime == null) mime = "application/octet-stream";
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, source.getName());
            values.put(MediaStore.Downloads.MIME_TYPE, mime);
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SwiftSave");
            values.put(MediaStore.Downloads.IS_PENDING, 1);
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IOException("Couldn't create a Downloads entry");
            try {
                try (OutputStream output = getContentResolver().openOutputStream(uri);
                     InputStream input = new FileInputStream(source)) {
                    if (output == null) throw new IOException("Couldn't open Downloads entry");
                    copy(input, output);
                }
                values.clear();
                values.put(MediaStore.Downloads.IS_PENDING, 0);
                if (getContentResolver().update(uri, values, null, null) != 1) throw new IOException("Couldn't finish saving media");
                return uri;
            } catch (Exception error) {
                getContentResolver().delete(uri, null, null);
                if (error instanceof IOException) throw (IOException) error;
                throw new IOException("Couldn't save to Downloads", error);
            }
        }
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SwiftSave");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Couldn't create Downloads/SwiftSave");
        // createNewFile reserves a unique name and never overwrites an existing download.
        File destination = new File(directory, source.getName());
        int suffix = 1;
        while (!destination.createNewFile()) {
            destination = new File(directory, suffix++ + "_" + source.getName());
        }
        try (InputStream input = new FileInputStream(source); OutputStream output = new FileOutputStream(destination)) {
            copy(input, output);
        } catch (IOException error) {
            destination.delete();
            throw error;
        }
        MediaScannerConnection.scanFile(this, new String[]{destination.getAbsolutePath()}, new String[]{mime}, null);
        return Uri.fromFile(destination);
    }

    private void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        int count;
        while ((count = input.read(buffer)) != -1) {
            checkCancelled();
            output.write(buffer, 0, count);
        }
    }

    private void checkCancelled() throws IOException {
        if (cancelled || Thread.currentThread().isInterrupted()) throw new IOException("Cancelled");
    }

    private void markEngineUpdated() {
        getSharedPreferences("engine", MODE_PRIVATE).edit()
                .putLong("updated_at", System.currentTimeMillis()).apply();
    }

    private void deletePublished(Uri uri) {
        try {
            if ("file".equals(uri.getScheme())) new File(uri.getPath()).delete();
            else getContentResolver().delete(uri, null, null);
        } catch (Exception ignored) { }
    }

    private static boolean isMediaFile(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        for (String extension : new String[]{".mp4", ".mkv", ".webm", ".mov", ".mp3", ".m4a", ".opus", ".ogg", ".aac", ".flac", ".wav"}) {
            if (lower.endsWith(extension) && !lower.matches(".*\\.f\\d+\\.[^.]+$")) return true;
        }
        return false;
    }

    private void clearOldJobs(File current) {
        File[] jobs = getCacheDir().listFiles();
        if (jobs != null) for (File job : jobs) {
            if (job.getName().startsWith("job-") && !job.equals(current)) deleteTree(job);
        }
    }

    private static void deleteTree(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        file.delete();
    }

    private synchronized void publish(String status, int progress, boolean busy, boolean success) {
        getSharedPreferences("download_status", MODE_PRIVATE).edit().putString("status", status)
                .putInt("progress", progress).putBoolean("busy", busy).putBoolean("success", success).apply();
        Intent broadcast = new Intent(ACTION_STATUS).setPackage(getPackageName());
        broadcast.putExtra("status", status).putExtra("progress", progress)
                .putExtra("busy", busy).putExtra("success", success);
        sendBroadcast(broadcast);
        if (busy) getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, notification(status, progress, true));
    }

    private Notification notification(String title, int progress, boolean busy) {
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("SwiftSave")
                .setContentText(title).setStyle(new Notification.BigTextStyle().bigText(title))
                .setContentIntent(open).setOngoing(busy).setOnlyAlertOnce(true).setAutoCancel(!busy);
        if (busy) {
            builder.setProgress(100, Math.max(progress, 0), progress < 0);
            // Updating the shared Python installation cannot safely be interrupted halfway.
            if (processId != null || saving) {
                PendingIntent cancel = PendingIntent.getService(this, 1,
                        new Intent(this, DownloadService.class).setAction(ACTION_CANCEL),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                builder.addAction(new Notification.Action.Builder(null, "Cancel", cancel).build());
            }
        }
        return builder.build();
    }

    @Override public void onTimeout(int startId, int foregroundServiceType) {
        cancelled = true;
        String id = processId;
        if (id != null) new Thread(() -> YoutubeDL.getInstance().destroyProcessById(id), "timeout-download").start();
        publish("Download timed out. Try again later.", -1, false, false);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override public void onDestroy() {
        cancelled = true;
        running = false;
        String id = processId;
        if (id != null) new Thread(() -> YoutubeDL.getInstance().destroyProcessById(id), "stop-download").start();
        worker.shutdownNow();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
