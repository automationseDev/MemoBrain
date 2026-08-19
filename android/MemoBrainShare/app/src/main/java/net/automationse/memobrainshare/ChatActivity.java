package net.automationse.memobrainshare;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.MobileAds;

import java.util.Locale;

public class ChatActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 6001;
    private static final String AD_CONTENT_URL = "https://automationse.net/memobrain-ad/";

    private WebView adWebView;
    private WebView difyWebView;
    private ProgressBar progress;
    private ValueCallback<Uri[]> fileUploadCallback;
    private String difyAllowedHost = "";
    private String adAllowedHost = "";
    private OnBackInvokedCallback backCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        ConnectionProfileStore profiles = new ConnectionProfileStore(this);
        profiles.migrate(new SecurePrefs(this));
        ConnectionProfileStore.Profile selected = profiles.selected();
        String difyUrl = selected == null ? "" : selected.chatUrl;
        if (!isHttpsUrl(difyUrl)) {
            Toast.makeText(this, "Dify Web App URL をDify接続設定から登録してください", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        difyAllowedHost = normalizedHost(Uri.parse(difyUrl));
        adAllowedHost = normalizedHost(Uri.parse(AD_CONTENT_URL));
        buildUi();
        configureAdWebView();
        configureDifyWebView();

        if (Build.VERSION.SDK_INT >= 33) {
            backCallback = this::handleBack;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
        }

        adWebView.loadUrl(AD_CONTENT_URL);
        difyWebView.loadUrl(difyUrl);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(6), dp(8), dp(6));

        Button close = new Button(this);
        close.setText("← 戻る");
        close.setTextSize(14);
        close.setMinWidth(0);
        close.setMinHeight(dp(48));
        close.setPadding(dp(12), 0, dp(12), 0);
        close.setOnClickListener(v -> finish());
        header.addView(close, new LinearLayout.LayoutParams(-2, -2));

        TextView title = new TextView(this);
        title.setText("MemoBrain AIチャット");
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));

        Button reload = new Button(this);
        reload.setText("再読込");
        reload.setTextSize(14);
        reload.setMinWidth(0);
        reload.setMinHeight(dp(48));
        reload.setPadding(dp(12), 0, dp(12), 0);
        reload.setOnClickListener(v -> {
            if (adWebView != null) adWebView.reload();
            if (difyWebView != null) difyWebView.reload();
        });
        header.addView(reload, new LinearLayout.LayoutParams(-2, -2));

        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));

        adWebView = new WebView(this);
        root.addView(adWebView, new LinearLayout.LayoutParams(-1, dp(210)));

        difyWebView = new WebView(this);
        root.addView(difyWebView, new LinearLayout.LayoutParams(-1, 0, 1f));

        applySystemBarInsets(root, header);
        setContentView(root);
    }

    private void applySystemBarInsets(View root, View header) {
        final int left = dp(8);
        final int topBase = dp(6);
        final int right = dp(8);
        final int bottomBase = dp(6);

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                top = bars.top;
                bottom = bars.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            header.setPadding(left, topBase + top, right, bottomBase);
            v.setPadding(0, 0, 0, bottom);
            return insets;
        });
        root.requestApplyInsets();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void applyBaseSettings(WebView view, boolean allowContentAccess) {
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(allowContentAccess);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(view, true);
    }

    private void configureAdWebView() {
        applyBaseSettings(adWebView, false);

        // Only the publisher-owned WebView contains AdSense. The user's Dify
        // URL is loaded by a separate WebView and is never sent to this page.
        MobileAds.registerWebView(adWebView);

        adWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                Uri uri = request.getUrl();
                if (isAllowedHttpsHost(uri, adAllowedHost)) return false;
                openExternal(uri);
                return true;
            }
        });
    }

    private void configureDifyWebView() {
        applyBaseSettings(difyWebView, true);
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

        difyWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                Uri uri = request.getUrl();
                if (isAllowedHttpsHost(uri, difyAllowedHost)) return false;
                openExternal(uri);
                return true;
            }
        });

        difyWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {
                if (fileUploadCallback != null) fileUploadCallback.onReceiveValue(null);
                fileUploadCallback = filePathCallback;
                try {
                    startActivityForResult(fileChooserParams.createIntent(), FILE_CHOOSER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException e) {
                    fileUploadCallback = null;
                    Toast.makeText(ChatActivity.this, "ファイル選択アプリを開けませんでした", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
    }

    private String normalizedHost(Uri uri) {
        if (uri == null || uri.getHost() == null) return "";
        return uri.getHost().toLowerCase(Locale.ROOT);
    }

    private boolean isAllowedHttpsHost(Uri uri, String allowedHost) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme())) return false;
        String host = normalizedHost(uri);
        return !host.isEmpty() && !allowedHost.isEmpty() && allowedHost.equals(host);
    }

    private boolean isHttpsUrl(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        try {
            Uri uri = Uri.parse(value.trim());
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void openExternal(Uri uri) {
        if (uri == null) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "リンクを開けませんでした", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || fileUploadCallback == null) return;
        Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
        fileUploadCallback.onReceiveValue(result);
        fileUploadCallback = null;
    }

    @Override
    @SuppressLint("GestureBackNavigation")
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        handleBack();
    }

    @SuppressWarnings("deprecation")
    private void handleBack() {
        if (difyWebView != null && difyWebView.canGoBack()) difyWebView.goBack();
        else finishAfterTransition();
    }

    @Override
    protected void onDestroy() {
        if (Build.VERSION.SDK_INT >= 33 && backCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
            backCallback = null;
        }
        if (fileUploadCallback != null) {
            fileUploadCallback.onReceiveValue(null);
            fileUploadCallback = null;
        }
        destroyWebView(adWebView);
        destroyWebView(difyWebView);
        adWebView = null;
        difyWebView = null;
        super.onDestroy();
    }

    private void destroyWebView(WebView view) {
        if (view == null) return;
        view.stopLoading();
        view.setWebChromeClient(null);
        view.setWebViewClient(null);
        view.loadUrl("about:blank");
        view.clearHistory();
        view.destroy();
    }
}
