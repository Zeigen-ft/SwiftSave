package app.swiftsave;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** A compact, dependency-free interface for sharing and downloading a media link. */
public final class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(12, 18, 24);
    private static final int SURFACE = Color.rgb(22, 31, 39);
    private static final int INPUT = Color.rgb(12, 22, 29);
    private static final int TEAL = Color.rgb(91, 226, 194);
    private static final int TEXT = Color.rgb(239, 246, 247);
    private static final int MUTED = Color.rgb(154, 174, 184);
    private static final int STORAGE_REQUEST = 100;
    private static final int NOTIFICATION_REQUEST = 101;
    private static final String STATUS_ACTION = "app.swiftsave.DOWNLOAD_STATUS";

    private EditText linkInput;
    private Button downloadButton;
    private Button videoButton;
    private Button audioButton;
    private Button updateButton;
    private Button cancelButton;
    private TextView statusText;
    private TextView statusLabel;
    private ProgressBar progressBar;
    private boolean audioOnly;
    private boolean busy;
    private boolean receiverRegistered;
    private String pendingUrl;
    private boolean pendingAudio;

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            renderStatus(intent.getStringExtra("status"), intent.getIntExtra("progress", -1),
                    intent.getBooleanExtra("busy", false), intent.getBooleanExtra("success", false));
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        window.setStatusBarColor(BACKGROUND);
        window.setNavigationBarColor(BACKGROUND);
        window.getDecorView().setSystemUiVisibility(0);
        if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false);
        if (Build.VERSION.SDK_INT >= 29) window.setNavigationBarContrastEnforced(false);
        if (state != null) {
            audioOnly = state.getBoolean("audio_only", false);
            pendingUrl = state.getString("pending_url");
            pendingAudio = state.getBoolean("pending_audio", false);
        }
        buildInterface();
        if (state != null) linkInput.setText(state.getString("input", ""));
        else acceptSharedLink(getIntent());
        restoreStatus();
    }

    private void buildInterface() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);
        scroll.setClipToPadding(false);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(24), dp(30), dp(24), dp(28));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);
        scroll.requestApplyInsets();

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = text("↓", 32, BACKGROUND);
        logo.setGravity(Gravity.CENTER);
        logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        logo.setBackground(shape(TEAL, 18, 0));
        header.addView(logo, new LinearLayout.LayoutParams(dp(54), dp(54)));
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(14), 0, 0, 0);
        TextView title = text("SwiftSave", 23, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.addView(title);
        TextView subtitle = text("A little app for the links you love.", 12, MUTED);
        brand.addView(subtitle, margin(-1, -2, 0, 4, 0, 0));
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        page.addView(header);

        TextView eyebrow = text("SAVE IT FOR LATER", 11, TEAL);
        eyebrow.setLetterSpacing(0.13f);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        page.addView(eyebrow, margin(-1, -2, 0, 36, 0, 10));
        TextView hero = text("One link.\nYours to keep.", 35, TEXT);
        hero.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        hero.setLineSpacing(dp(2), 1f);
        page.addView(hero);
        TextView description = text("Paste a video link or share it directly to SwiftSave.", 14, MUTED);
        description.setLineSpacing(dp(4), 1f);
        page.addView(description, margin(-1, -2, 0, 12, 0, 20));

        LinearLayout platforms = new LinearLayout(this);
        platforms.setGravity(Gravity.CENTER_VERTICAL);
        String[] names = {"YouTube", "TikTok", "Facebook", "X"};
        for (int i = 0; i < names.length; i++) {
            TextView chip = text(names[i], 11, MUTED);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(9), dp(8), dp(9), dp(8));
            chip.setBackground(shape(SURFACE, 9, Color.rgb(42, 57, 67)));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(i == 3 ? dp(36) : 0, -2, i == 3 ? 0 : 1);
            if (i > 0) p.leftMargin = dp(6);
            platforms.addView(chip, p);
        }
        page.addView(platforms, margin(-1, -2, 0, 0, 0, 24));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(19), dp(18), dp(18));
        card.setBackground(shape(SURFACE, 22, Color.rgb(39, 53, 63)));
        TextView linkLabel = text("VIDEO LINK", 11, MUTED);
        linkLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        linkLabel.setLetterSpacing(0.08f);
        card.addView(linkLabel);

        linkInput = new EditText(this);
        linkInput.setTextColor(TEXT);
        linkInput.setHintTextColor(Color.rgb(102, 126, 140));
        linkInput.setHint("https://…");
        linkInput.setTextSize(15);
        linkInput.setPadding(dp(13), dp(12), dp(13), dp(12));
        linkInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        linkInput.setSingleLine(true);
        linkInput.setSelectAllOnFocus(false);
        linkInput.setContentDescription("Video link");
        linkInput.setBackground(shape(INPUT, 12, Color.rgb(43, 65, 77)));
        card.addView(linkInput, margin(-1, dp(54), 0, 11, 0, 0));

        Button pasteButton = button("Paste from clipboard", false);
        pasteButton.setTextColor(TEAL);
        pasteButton.setTextSize(12);
        pasteButton.setPadding(0, 0, 0, 0);
        pasteButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        pasteButton.setBackgroundColor(Color.TRANSPARENT);
        pasteButton.setOnClickListener(v -> pasteLink());
        card.addView(pasteButton, new LinearLayout.LayoutParams(-1, dp(45)));

        LinearLayout mode = new LinearLayout(this);
        mode.setPadding(dp(4), dp(4), dp(4), dp(4));
        mode.setBackground(shape(INPUT, 12, 0));
        videoButton = button("Video", false);
        audioButton = button("Audio only", false);
        videoButton.setOnClickListener(v -> selectMode(false));
        audioButton.setOnClickListener(v -> selectMode(true));
        mode.addView(videoButton, new LinearLayout.LayoutParams(0, dp(44), 1));
        mode.addView(audioButton, new LinearLayout.LayoutParams(0, dp(44), 1));
        card.addView(mode, margin(-1, -2, 0, 4, 0, 17));
        selectMode(audioOnly);

        downloadButton = button("Download video  ↓", true);
        downloadButton.setOnClickListener(v -> beginDownload());
        card.addView(downloadButton, new LinearLayout.LayoutParams(-1, dp(54)));
        page.addView(card);
        selectMode(audioOnly);

        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(dp(17), dp(17), dp(17), dp(17));
        statusCard.setBackground(shape(Color.rgb(16, 27, 34), 16, Color.rgb(34, 50, 60)));
        statusLabel = text("READY WHEN YOU ARE", 10, TEAL);
        statusLabel.setLetterSpacing(0.1f);
        statusLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statusCard.addView(statusLabel);
        statusText = text("Your downloads will appear here.", 13, MUTED);
        statusText.setLineSpacing(dp(4), 1f);
        statusText.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        statusCard.addView(statusText, margin(-1, -2, 0, 9, 0, 0));
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(TEAL));
        progressBar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(TEAL));
        progressBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(39, 58, 67)));
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);
        statusCard.addView(progressBar, margin(-1, dp(5), 0, 15, 0, 0));
        cancelButton = button("Cancel", false);
        cancelButton.setTextColor(MUTED);
        cancelButton.setBackgroundColor(Color.TRANSPARENT);
        cancelButton.setVisibility(View.GONE);
        cancelButton.setOnClickListener(v -> {
            try {
                startService(new Intent(this, DownloadService.class).setAction("app.swiftsave.CANCEL"));
                cancelButton.setEnabled(false);
                statusText.setText("Cancelling…");
            } catch (RuntimeException unavailable) {
                Toast.makeText(this, "Couldn't cancel. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams cancelParams = margin(-2, dp(44), 0, 6, 0, 0);
        cancelParams.gravity = Gravity.END;
        statusCard.addView(cancelButton, cancelParams);
        page.addView(statusCard, margin(-1, -2, 0, 16, 0, 4));

        LinearLayout utilityRow = new LinearLayout(this);
        Button filesButton = button("Open downloads", false);
        filesButton.setBackgroundColor(Color.TRANSPARENT);
        filesButton.setTextColor(TEXT);
        filesButton.setTextSize(12);
        filesButton.setOnClickListener(v -> openDownloads());
        updateButton = button("Update engine", false);
        updateButton.setBackgroundColor(Color.TRANSPARENT);
        updateButton.setTextColor(MUTED);
        updateButton.setTextSize(12);
        updateButton.setOnClickListener(v -> updateEngine());
        utilityRow.addView(filesButton, new LinearLayout.LayoutParams(0, dp(48), 1));
        utilityRow.addView(updateButton, new LinearLayout.LayoutParams(0, dp(48), 1));
        page.addView(utilityRow);

        TextView footer = text("Public, accessible links work best. Some posts require sign-in or may be unavailable. Save only media you have permission to download.", 11, Color.rgb(115, 141, 155));
        footer.setGravity(Gravity.CENTER);
        footer.setLineSpacing(dp(4), 1f);
        page.addView(footer, margin(-1, -2, 7, 19, 7, 0));
    }

    private void selectMode(boolean audio) {
        audioOnly = audio;
        if (videoButton != null) {
            videoButton.setBackground(shape(audio ? INPUT : Color.rgb(47, 69, 77), 9, 0));
            videoButton.setTextColor(audio ? MUTED : TEXT);
            videoButton.setSelected(!audio);
            audioButton.setBackground(shape(audio ? Color.rgb(47, 69, 77) : INPUT, 9, 0));
            audioButton.setTextColor(audio ? TEXT : MUTED);
            audioButton.setSelected(audio);
        }
        if (downloadButton != null && !busy) downloadButton.setText(audio ? "Download audio  ↓" : "Download video  ↓");
    }

    private void pasteLink() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip() || clipboard.getPrimaryClip().getItemCount() == 0) {
            Toast.makeText(this, "Copy a video link first.", Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence clip = clipboard.getPrimaryClip().getItemAt(0).coerceToText(this);
        if (clip == null) return;
        String url = extractLink(clip.toString());
        linkInput.setText(url);
        linkInput.setSelection(linkInput.length());
        linkInput.setError(null);
    }

    private void beginDownload() {
        if (busy) return;
        String url;
        try {
            url = UrlPolicy.extractSupportedUrl(linkInput.getText().toString());
        } catch (IllegalArgumentException invalidLink) {
            linkInput.setError(invalidLink.getMessage());
            linkInput.requestFocus();
            return;
        }
        linkInput.setText(url);
        pendingUrl = url;
        pendingAudio = audioOnly;
        if (Build.VERSION.SDK_INT <= 28 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST);
            return;
        }
        launchPendingDownload();
    }

    private void launchPendingDownload() {
        if (pendingUrl == null) return;
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST);
        }
        Intent service = new Intent(this, DownloadService.class);
        service.putExtra("url", pendingUrl);
        service.putExtra("audio_only", pendingAudio);
        if (startWork(service, "Starting your download…")) {
            pendingUrl = null;
            View focused = getCurrentFocus();
            if (focused != null) ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }

    private void updateEngine() {
        if (busy) return;
        Intent service = new Intent(this, DownloadService.class).setAction("app.swiftsave.UPDATE_ENGINE");
        startWork(service, "Checking for engine updates…");
    }

    private boolean startWork(Intent service, String message) {
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(service);
            else startService(service);
            renderStatus(message, -1, true, false);
            return true;
        } catch (RuntimeException e) {
            renderStatus("Couldn't start. Return to the app and try again.", -1, false, false);
            return false;
        }
    }

    private static String extractLink(String text) {
        try { return UrlPolicy.extractSupportedUrl(text); }
        catch (IllegalArgumentException invalid) { return text.trim(); }
    }

    private void acceptSharedLink(Intent intent) {
        if (intent == null) return;
        String shared = null;
        if (Intent.ACTION_SEND.equals(intent.getAction())) shared = intent.getStringExtra(Intent.EXTRA_TEXT);
        else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) shared = intent.getData().toString();
        if (shared != null) {
            linkInput.setText(extractLink(shared));
            linkInput.setSelection(linkInput.length());
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        acceptSharedLink(intent);
    }

    @Override protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(statusReceiver, new IntentFilter(STATUS_ACTION), Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(statusReceiver, new IntentFilter(STATUS_ACTION));
        receiverRegistered = true;
        restoreStatus();
    }

    @Override protected void onStop() {
        if (receiverRegistered) {
            unregisterReceiver(statusReceiver);
            receiverRegistered = false;
        }
        super.onStop();
    }

    private void restoreStatus() {
        SharedPreferences prefs = getSharedPreferences("download_status", MODE_PRIVATE);
        if (prefs.getBoolean("busy", false) && !DownloadService.isRunning()) {
            prefs.edit().putString("status", "Previous task was interrupted. You can try again.")
                    .putInt("progress", -1).putBoolean("busy", false).putBoolean("success", false).apply();
        }
        renderStatus(prefs.getString("status", "Your downloads will appear here."), prefs.getInt("progress", -1),
                prefs.getBoolean("busy", false), prefs.getBoolean("success", false));
    }

    private void renderStatus(String message, int progress, boolean working, boolean success) {
        busy = working;
        statusLabel.setText(working ? "WORKING ON IT" : success ? "ALL DONE" : "READY WHEN YOU ARE");
        statusText.setText(message == null || message.trim().isEmpty() ? "Your downloads will appear here." : message);
        progressBar.setVisibility(working ? View.VISIBLE : View.GONE);
        cancelButton.setVisibility(working ? View.VISIBLE : View.GONE);
        cancelButton.setEnabled(working);
        progressBar.setIndeterminate(progress < 0);
        if (progress >= 0) progressBar.setProgress(Math.min(100, Math.max(0, progress)));
        downloadButton.setEnabled(!working);
        updateButton.setEnabled(!working);
        linkInput.setEnabled(!working);
        videoButton.setEnabled(!working);
        audioButton.setEnabled(!working);
        downloadButton.setAlpha(working ? 0.6f : 1f);
        downloadButton.setText(working ? "Please wait…" : audioOnly ? "Download audio  ↓" : "Download video  ↓");
    }

    private void openDownloads() {
        try { startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)); }
        catch (RuntimeException unavailable) {
            try { startActivity(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE)); }
            catch (RuntimeException noFilesApp) { Toast.makeText(this, "Open your Files app and choose Downloads.", Toast.LENGTH_LONG).show(); }
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) launchPendingDownload();
            else renderStatus("Allow storage access to save files to Downloads.", -1, false, false);
        }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("input", linkInput.getText().toString());
        state.putBoolean("audio_only", audioOnly);
        state.putString("pending_url", pendingUrl);
        state.putBoolean("pending_audio", pendingAudio);
        super.onSaveInstanceState(state);
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setFontFeatureSettings("kern");
        return view;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(primary ? BACKGROUND : TEXT);
        button.setBackground(shape(primary ? TEAL : INPUT, 12, 0));
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setStateListAnimator(null);
        button.setPadding(dp(8), 0, dp(8), 0);
        return button;
    }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        if (stroke != 0) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private LinearLayout.LayoutParams margin(int width, int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
}
