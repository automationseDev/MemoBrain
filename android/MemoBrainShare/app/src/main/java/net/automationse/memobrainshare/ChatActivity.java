package net.automationse.memobrainshare;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
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

    private WebView webView;
    private ProgressBar progress;
    private ValueCallback<Uri[]> fileUploadCallback;
    private String allowedHost = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        SecurePrefs prefs = new SecurePrefs(this);
        String url = prefs.getChatWebUrl();
        if (!isHttpsUrl(url)) {
            Toast.makeText(this, "AIチャット Web URL をDify接続設定から登録してください", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Uri initialUri = Uri.parse(url);
        allowedHost = initialUri.getHost() == null ? "" : initialUri.getHost().toLowerCase(Locale.ROOT);
        buildUi();
        configureWebView();
        webView.loadUrl(url);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(16, 16, 16, 8);

        Button close = new Button(this);
        close.setText("戻る");
        close.setOnClickListener(v -> finish());
        header.addView(close, new LinearLayout.LayoutParams(-2, -2));

        TextView title = new TextView(this);
        title.setText("  MemoBrain AIチャット");
        title.setTextSize(20);
        title.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, -1, 1f));

        Button reload = new Button(this);
        reload.setText("再読込");
        reload.setOnClickListener(v -> {
            if (webView != null) webView.reload();
        });
        header.addView(reload, new LinearLayout.LayoutParams(-2, -2));

        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(-1, -2));

        webView = new WebView(this);
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

        // Google recommends registering a WebView that can contain AdSense / GPT tags.
        // If the configured URL is the Dify Web App directly this has no visible effect.
        MobileAds.registerWebView(webView);

        webView.setWebViewClient(new WebViewClient() {
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
                if (isAllowedMainFrame(uri)) return false;
                openExternal(uri);
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
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
                if (fileUploadCallback != null) {
                    fileUploadCallback.onReceiveValue(null);
                }
                fileUploadCallback = filePathCallback;
                try {
                    Intent chooser = fileChooserParams.createIntent();
                    startActivityForResult(chooser, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException e) {
                    fileUploadCallback = null;
                    Toast.makeText(ChatActivity.this, "ファイル選択アプリを開けませんでした", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
    }

    private boolean isAllowedMainFrame(Uri uri) {
        if (uri == null) return false;
        String scheme = uri.getScheme();
        if (scheme == null || !"https".equalsIgnoreCase(scheme)) return false;
        String host = uri.getHost();
        if (host == null) return false;
        return allowedHost.isEmpty() || allowedHost.equals(host.toLowerCase(Locale.ROOT));
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
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (fileUploadCallback != null) {
            fileUploadCallback.onReceiveValue(null);
            fileUploadCallback = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
