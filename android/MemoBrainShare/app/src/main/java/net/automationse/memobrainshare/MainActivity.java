package net.automationse.memobrainshare;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    private TextView status;
    private TextView preview;
    private EditText note;
    private Button sendButton;
    private Button privacyOptionsButton;
    private FrameLayout adContainer;
    private AdView adView;

    private final ArrayList<Uri> uris = new ArrayList<>();
    private String sharedText = "";
    private String mime = "";
    private SecurePrefs prefs;
    private final ExecutorService stagingExecutor = Executors.newSingleThreadExecutor();

    private ConsentInformation consentInformation;
    private final AtomicBoolean mobileAdsInitialized = new AtomicBoolean(false);

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        prefs = new SecurePrefs(this);
        NotificationHelper.ensureChannel(this);
        buildUi();
        requestNotificationPermissionIfNeeded();
        handle(getIntent());
        stagingExecutor.execute(() -> PendingJobStore.cleanupExpired(getApplicationContext()));
        initConsentAndAds();
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        handle(i);
    }

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
        stagingExecutor.shutdown();
        super.onDestroy();
    }

    private void buildUi() {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 64, 32, 32);

        TextView title = new TextView(this);
        title.setText("🧠 MemoBrain Share");
        title.setTextSize(25);
        root.addView(title);

        TextView required = new TextView(this);
        required.setText("【Dify必須】このアプリ単体では保存できません。利用者自身のDify環境とApp API Keyが必要です。共有内容は、利用者が設定したDifyへHTTPSで送信されます。");
        required.setPadding(0, 12, 0, 0);
        root.addView(required);

        TextView privacy = new TextView(this);
        privacy.setText("端末内の送信待ちデータはアプリ専用領域で暗号化し、送信完了・最終失敗時に削除します。未送信でも最大24時間で削除します。バックアップも無効です。");
        privacy.setPadding(0, 10, 0, 0);
        root.addView(privacy);

        preview = new TextView(this);
        preview.setPadding(0, 24, 0, 12);
        root.addView(preview);

        note = new EditText(this);
        note.setHint("補足メモ（任意）");
        note.setMinLines(3);
        note.setImeOptions(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        root.addView(note, new LinearLayout.LayoutParams(-1, -2));

        sendButton = new Button(this);
        sendButton.setText("MemoBrainに保存（バックグラウンド）");
        sendButton.setOnClickListener(v -> enqueueSave());
        root.addView(sendButton);

        Button connection = new Button(this);
        connection.setText("Dify接続設定（必須）");
        connection.setOnClickListener(v -> connectionSettings());
        root.addView(connection);

        privacyOptionsButton = new Button(this);
        privacyOptionsButton.setText("広告のプライバシー設定");
        privacyOptionsButton.setVisibility(View.GONE);
        privacyOptionsButton.setOnClickListener(v -> showPrivacyOptions());
        root.addView(privacyOptionsButton);

        status = new TextView(this);
        status.setPadding(0, 16, 0, 0);
        root.addView(status);

        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        outer.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        adContainer = new FrameLayout(this);
        adContainer.setVisibility(View.GONE);
        outer.addView(adContainer, new LinearLayout.LayoutParams(-1, -2));

        setContentView(outer);
    }

    @SuppressWarnings("deprecation")
    private void handle(Intent i) {
        uris.clear();
        sharedText = i == null ? "" : i.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText == null) sharedText = "";
        mime = i == null || i.getType() == null ? "" : i.getType();

        if (i != null && Intent.ACTION_SEND.equals(i.getAction())) {
            Uri u = i.getParcelableExtra(Intent.EXTRA_STREAM);
            if (u != null) uris.add(u);
        } else if (i != null && Intent.ACTION_SEND_MULTIPLE.equals(i.getAction())) {
            ArrayList<Uri> a = i.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (a != null) uris.addAll(a);
        }

        String visible = sharedText;
        if (visible.length() > 500) visible = visible.substring(0, 500) + "…";

        preview.setText("種類: " + (mime.isEmpty() ? "テキスト" : mime)
                + "\nファイル: " + uris.size() + "件\n\n" + visible);
        status.setText(prefs.isConfigured() ? "" : "Dify接続設定が必要です。");
        sendButton.setEnabled(true);
    }

    private String marker() {
        String s = sharedText.toLowerCase(Locale.ROOT);
        if (s.contains("youtube.com") || s.contains("youtu.be")) return "[MB:YOUTUBE]";
        if (mime.startsWith("image/")) return "[MB:IMAGE]";
        if (mime.startsWith("video/")) return "[MB:VIDEO]";
        if (mime.equals("application/pdf") || mime.contains("word") || mime.contains("document")) return "[MB:DOC]";
        if (s.contains("http://") || s.contains("https://")) return "[MB:URL]";
        return "[MB:TEXT]";
    }

    private boolean isShareInvocation() {
        String action = getIntent() == null ? null : getIntent().getAction();
        return Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action);
    }

    private void enqueueSave() {
        String key = prefs.getKey();
        String base = prefs.getBase();
        if (key.isEmpty() || base.isEmpty()) {
            connectionSettings();
            return;
        }
        if (!base.toLowerCase(Locale.ROOT).startsWith("https://")) {
            status.setText("Dify API Base はHTTPSで設定してください。");
            connectionSettings();
            return;
        }

        sendButton.setEnabled(false);
        status.setText(uris.isEmpty() ? "暗号化して保存キューに登録しています…" : "共有ファイルを暗号化して一時保存しています…");
        final String q = marker() + "\n" + sharedText + "\n補足: " + note.getText().toString();
        final ArrayList<Uri> snapshotUris = new ArrayList<>(uris);
        final String snapshotMime = mime;

        stagingExecutor.execute(() -> {
            try {
                String jobId = PendingJobStore.create(this, q, snapshotUris, snapshotMime);
                Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
                Data input = new Data.Builder().putString(MemoSaveWorker.KEY_JOB_ID, jobId).build();
                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MemoSaveWorker.class)
                        .setInputData(input)
                        .setConstraints(constraints)
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                        .addTag("memobrain-save")
                        .build();

                WorkManager wm = WorkManager.getInstance(getApplicationContext());
                wm.enqueueUniqueWork("memobrain-save-" + jobId, ExistingWorkPolicy.KEEP, request);

                OneTimeWorkRequest cleanup = new OneTimeWorkRequest.Builder(MemoCleanupWorker.class)
                        .setInputData(input)
                        .setInitialDelay(PendingJobStore.MAX_RETENTION_MILLIS, TimeUnit.MILLISECONDS)
                        .addTag("memobrain-cleanup")
                        .build();
                wm.enqueueUniqueWork("memobrain-cleanup-" + jobId, ExistingWorkPolicy.REPLACE, cleanup);

                runOnUiThread(() -> {
                    status.setText("保存キューに登録しました。別アプリへ移動してOKです。");
                    Toast.makeText(this, "MemoBrainでバックグラウンド保存を開始しました", Toast.LENGTH_SHORT).show();
                    sendButton.setEnabled(true);
                    note.setText("");
                    if (isShareInvocation()) new Handler(Looper.getMainLooper()).postDelayed(this::finish, 700);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    sendButton.setEnabled(true);
                    status.setText("保存準備に失敗しました。共有元またはDify接続設定を確認してください。");
                });
            }
        });
    }

    private void connectionSettings() {
        final TextView explain = new TextView(this);
        explain.setText("本アプリの利用にはDifyが必須です。共有内容はここで指定したDifyへ送信されます。API BaseとAPI Keyは端末内で暗号化して保存し、APKには含めません。");
        explain.setPadding(0, 0, 0, 16);

        final EditText base = new EditText(this);
        base.setHint("Dify API Base 例: https://example.com/v1");
        base.setText(prefs.getBase());
        base.setSingleLine(true);
        base.setImeOptions(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);

        final EditText key = new EditText(this);
        key.setHint("Dify App API Key (app-...)");
        key.setText(prefs.getKey());
        key.setSingleLine(true);
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setImeOptions(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        if (Build.VERSION.SDK_INT >= 26) key.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);

        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(32, 0, 32, 0);
        l.addView(explain);
        l.addView(base);
        l.addView(key);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Dify接続設定")
                .setView(l)
                .setPositiveButton("保存", null)
                .setNeutralButton("接続情報を削除", null)
                .setNegativeButton("キャンセル", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String baseValue = base.getText().toString().trim();
                String keyValue = key.getText().toString().trim();
                if (!baseValue.toLowerCase(Locale.ROOT).startsWith("https://")) {
                    base.setError("HTTPSのDify API Baseを入力してください");
                    return;
                }
                if (keyValue.isEmpty()) {
                    key.setError("Dify App API Keyを入力してください");
                    return;
                }
                try {
                    prefs.putBase(baseValue);
                    prefs.putKey(keyValue);
                    status.setText("");
                    Toast.makeText(this, "Dify接続設定を暗号化して保存しました", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } catch (Exception e) {
                    Toast.makeText(this, "接続設定を安全に保存できませんでした", Toast.LENGTH_LONG).show();
                }
            });

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v ->
                    new AlertDialog.Builder(this)
                            .setTitle("接続情報を削除")
                            .setMessage("端末に保存されているDify API BaseとAPI Keyを削除します。")
                            .setPositiveButton("削除", (d, w) -> {
                                prefs.clearConnection();
                                base.setText("");
                                key.setText("");
                                status.setText("Dify接続設定が必要です。");
                                Toast.makeText(this, "接続情報を削除しました", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            })
                            .setNegativeButton("キャンセル", null)
                            .show());
        });
        dialog.show();
    }

    private void initConsentAndAds() {
        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consentInformation.requestConsentInfoUpdate(
                this,
                params,
                () -> {
                    updatePrivacyOptionsButton();
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(this, formError -> {
                        updatePrivacyOptionsButton();
                        if (consentInformation.canRequestAds()) initializeMobileAdsSdk();
                    });
                },
                requestConsentError -> {
                    updatePrivacyOptionsButton();
                    if (consentInformation.canRequestAds()) initializeMobileAdsSdk();
                });
        if (consentInformation.canRequestAds()) initializeMobileAdsSdk();
    }

    private void initializeMobileAdsSdk() {
        if (!mobileAdsInitialized.compareAndSet(false, true)) {
            runOnUiThread(this::loadBanner);
            return;
        }
        new Thread(() -> MobileAds.initialize(getApplicationContext(), initializationStatus -> runOnUiThread(this::loadBanner))).start();
    }

    private void loadBanner() {
        if (consentInformation != null && !consentInformation.canRequestAds()) {
            hideBanner();
            return;
        }
        String unitId = BuildConfig.ADMOB_BANNER_ID;
        if (unitId == null || unitId.trim().isEmpty()) {
            hideBanner();
            return;
        }
        if (adView != null) {
            adView.destroy();
            adContainer.removeAllViews();
        }
        adView = new AdView(this);
        adView.setAdUnitId(unitId.trim());
        adView.setAdSize(AdSize.BANNER);
        adContainer.removeAllViews();
        adContainer.addView(adView, new FrameLayout.LayoutParams(-2, -2));
        adContainer.setVisibility(View.VISIBLE);
        adView.loadAd(new AdRequest.Builder().build());
    }

    private void hideBanner() {
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
        adContainer.removeAllViews();
        adContainer.setVisibility(View.GONE);
    }

    private void updatePrivacyOptionsButton() {
        runOnUiThread(() -> {
            boolean required = consentInformation != null
                    && consentInformation.getPrivacyOptionsRequirementStatus() == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
            privacyOptionsButton.setVisibility(required ? View.VISIBLE : View.GONE);
        });
    }

    private void showPrivacyOptions() {
        if (consentInformation == null) return;
        UserMessagingPlatform.showPrivacyOptionsForm(this, formError -> {
            updatePrivacyOptionsButton();
            if (consentInformation.canRequestAds()) initializeMobileAdsSdk();
            else hideBanner();
        });
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 5001);
        }
    }
}
