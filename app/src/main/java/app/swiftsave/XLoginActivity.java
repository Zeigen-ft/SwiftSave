package app.swiftsave;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.Locale;

/** Lets the user sign in on X itself; credentials never cross an app bridge. */
public final class XLoginActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(12, 18, 24);
    private static final int SURFACE = Color.rgb(22, 31, 39);
    private static final int TEAL = Color.rgb(91, 226, 194);
    private static final int TEXT = Color.rgb(239, 246, 247);
    private static final int MUTED = Color.rgb(154, 174, 184);
    private static final String LOGIN_URL = "https://x.com/i/flow/login";

    private WebView browser;
    private TextView hostText;
    private TextView message;
    private ProgressBar progress;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(0);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(BACKGROUND);
        page.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        setContentView(page);
        page.requestApplyInsets();

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(10), dp(12), dp(4));
        TextView title = label("Connect X", 20, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button close = action("Close", false);
        close.setOnClickListener(v -> finish());
        header.addView(close, new LinearLayout.LayoutParams(dp(74), dp(44)));
        page.addView(header);

        TextView instructions = label("Sign in on X, then tap Use X account. Your session stays on this phone.", 13, MUTED);
        instructions.setPadding(dp(18), dp(4), dp(18), dp(8));
        instructions.setLineSpacing(dp(3), 1f);
        page.addView(instructions);

        hostText = label("x.com", 12, TEAL);
        hostText.setPadding(dp(18), dp(7), dp(18), dp(7));
        hostText.setBackgroundColor(SURFACE);
        page.addView(hostText);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(TEAL));
        page.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));

        browser = new WebView(this);
        browser.setBackgroundColor(BACKGROUND);
        WebSettings settings = browser.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setSafeBrowsingEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        // X's own CAPTCHA providers may need third-party cookies inside their HTTPS frames.
        CookieManager.getInstance().setAcceptThirdPartyCookies(browser, true);
        browser.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) {
                    // Keep the main page on X while allowing its HTTPS verification frames.
                    return !"https".equalsIgnoreCase(request.getUrl().getScheme());
                }
                return handleNavigation(request.getUrl(), request.hasGesture());
            }

            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleNavigation(Uri.parse(url), true);
            }

            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                Uri uri = Uri.parse(url);
                if (!isXPage(uri)) {
                    view.stopLoading();
                    hostText.setText("Only X sign-in pages open here");
                    return;
                }
                hostText.setText(uri.getHost());
                progress.setVisibility(View.VISIBLE);
            }

            @Override public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.GONE);
            }
        });
        page.addView(browser, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setPadding(dp(16), dp(9), dp(16), dp(13));
        message = label("Use your X username and password here. External browser sign-ins won't connect this app.", 12, MUTED);
        message.setLineSpacing(dp(3), 1f);
        message.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        footer.addView(message);
        Button useAccount = action("Use X account", true);
        useAccount.setOnClickListener(v -> saveSession());
        LinearLayout.LayoutParams useParams = new LinearLayout.LayoutParams(-1, dp(49));
        useParams.topMargin = dp(9);
        footer.addView(useAccount, useParams);
        page.addView(footer);

        // Rotation is handled in place by the manifest; session data never enters a Bundle.
        browser.loadUrl(LOGIN_URL);
    }

    private boolean handleNavigation(Uri uri, boolean userGesture) {
        if (isXPage(uri)) return false;
        String scheme = uri.getScheme();
        if (userGesture && ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)); }
            catch (RuntimeException unavailable) { message.setText("Couldn't open your browser. Use X sign-in here."); }
        } else if (message != null) {
            message.setText("Use your X username and password to sign in here.");
        }
        return true;
    }

    private static boolean isXPage(Uri uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null) return false;
        if (uri.getPort() != -1 && uri.getPort() != 443) return false;
        String host = uri.getHost();
        if (host == null) return false;
        host = host.toLowerCase(Locale.ROOT);
        return host.equals("x.com") || host.endsWith(".x.com")
                || host.equals("twitter.com") || host.endsWith(".twitter.com");
    }

    private void saveSession() {
        if (DownloadService.isRunning()) {
            message.setText("Wait for the current download to finish, then connect X.");
            return;
        }
        String cookies = CookieManager.getInstance().getCookie("https://x.com/");
        if (!containsCookie(cookies, "auth_token") || !containsCookie(cookies, "ct0")) {
            message.setText("Finish signing in on X, then tap Use X account.");
            return;
        }
        try {
            XSessionStore.saveFromCookieHeader(this, cookies);
            CookieManager.getInstance().flush();
            setResult(RESULT_OK);
            finish();
        } catch (IOException | IllegalArgumentException failure) {
            message.setText("Couldn't save the X session. Finish signing in and try again.");
        }
    }

    private static boolean containsCookie(String header, String wanted) {
        if (header == null) return false;
        for (String part : header.split(";")) {
            int equals = part.indexOf('=');
            if (equals > 0 && wanted.equals(part.substring(0, equals).trim())
                    && !part.substring(equals + 1).trim().isEmpty()) return true;
        }
        return false;
    }

    @Override public void onBackPressed() {
        if (browser != null && browser.canGoBack()) browser.goBack();
        else super.onBackPressed();
    }

    @Override protected void onResume() {
        super.onResume();
        if (browser != null) browser.onResume();
    }

    @Override protected void onPause() {
        if (browser != null) browser.onPause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (browser != null) {
            browser.stopLoading();
            browser.setWebViewClient(new WebViewClient());
            if (browser.getParent() instanceof ViewGroup) {
                ((ViewGroup) browser.getParent()).removeView(browser);
            }
            browser.destroy();
            browser = null;
        }
        super.onDestroy();
    }

    private TextView label(String value, int size, int color) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextSize(size);
        label.setTextColor(color);
        label.setGravity(Gravity.CENTER_VERTICAL);
        return label;
    }

    private Button action(String text, boolean primary) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(primary ? BACKGROUND : TEXT);
        button.setTextSize(14);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        GradientDrawable background = new GradientDrawable();
        background.setColor(primary ? TEAL : SURFACE);
        background.setCornerRadius(dp(10));
        button.setBackground(background);
        return button;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
