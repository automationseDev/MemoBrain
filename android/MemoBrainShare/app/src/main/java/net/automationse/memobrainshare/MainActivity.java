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
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private static final int REQUEST_WIDGET_FILE = 6201;

    private TextView status;
    private TextView preview;
    private EditText note;
    private Button sendButton;

    private final ArrayList<Uri> uris = new ArrayList<>();
    private String sharedText = "";
    private String mime = "";
    private SecurePrefs prefs;
    private final ExecutorService stagingExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        prefs = new SecurePrefs(this);
        NotificationHelper.ensureChannel(this);
        buildUi();
        requestNotificationPermissionIfNeeded();
        handle(getIntent());
        handleWidgetAction(getIntent());
        stagingExecutor.execute(() -> PendingJobStore.cleanupExpired(getApplicationContext()));
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        handle(i);
        handleWidgetAction(i);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_WIDGET_FILE || resultCode != RESULT_OK || data == null) return;
        Uri selected = data.getData();
        if (selected == null) return;
        try {
            getContentResolver().takePersistableUriPermission(selected,
                    data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}
        uris.clear();
        uris.add(selected);
        sharedText = "";
        String selectedMime = getContentResolver().getType(selected);
        mime = selectedMime == null ? "application/octet-stream" : selectedMime;
        renderPreview();
        status.setText("ファイルを選択しました。必要なら補足メモを入力して保存してください。");
    }

    @Override
    protected void onDestroy() {
        stagingExecutor.shutdown();
        super.onDestroy();
    }

    private void buildUi() {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        applySystemBarInsets(outer, root);

        TextView title = new TextView(this);
        title.setText("🧠 MemoBrain Share");
        title.setTextSize(25);
        root.addView(title);

        TextView required = new TextView(this);
        required.setText("【Dify必須】このアプリ単体では保存できません。利用者自身のDify環境とApp API Keyが必要です。共有内容は、利用者が設定したDifyへHTTPSで送信されます。");
        required.setPadding(0, dp(12), 0, 0);
        root.addView(required);

        TextView privacy = new TextView(this);
        privacy.setText("端末内の送信待ちデータは暗号化します。成功時に削除し、失敗時は再送用として最大24時間だけ保持します。履歴に本文やファイル名は保存しません。");
        privacy.setPadding(0, dp(10), 0, 0);
        root.addView(privacy);

        TextView webAds = new TextView(this);
        webAds.setText("AIチャット画面では、開発者管理の案内・AdSenseページと、利用者が設定したDifyを別々のWebViewで表示します。利用者のDify URLは広告ページへ送信しません。");
        webAds.setPadding(0, dp(10), 0, 0);
        root.addView(webAds);

        preview = new TextView(this);
        preview.setPadding(0, dp(24), 0, dp(12));
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

        Button history = new Button(this);
        history.setText("送信履歴・失敗した送信の再送");
        history.setOnClickListener(v -> showHistory());
        root.addView(history);

        Button chat = new Button(this);
        chat.setText("Dify AIチャットを開く");
        chat.setOnClickListener(v -> openChat());
        root.addView(chat);

        Button connection = new Button(this);
        connection.setText("Dify接続設定（必須）");
        connection.setOnClickListener(v -> connectionSettings());
        root.addView(connection);

        status = new TextView(this);
        status.setPadding(0, dp(16), 0, 0);
        root.addView(status);

        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        outer.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(outer);
    }

    private void applySystemBarInsets(View outer, View content) {
        final int horizontal = dp(20);
        final int topBase = dp(24);
        final int bottomBase = dp(24);
        content.setPadding(horizontal, topBase, horizontal, bottomBase);

        outer.setOnApplyWindowInsetsListener((v, insets) -> {
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
            content.setPadding(horizontal, topBase + top, horizontal, bottomBase);
            v.setPadding(0, 0, 0, bottom);
            return insets;
        });
        outer.requestApplyInsets();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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

        renderPreview();
        status.setText(prefs.isConfigured() ? "" : "Dify接続設定が必要です。");
        sendButton.setEnabled(true);
    }

    private void renderPreview() {
        String visible = sharedText == null ? "" : sharedText;
        if (visible.length() > 500) visible = visible.substring(0, 500) + "…";
        preview.setText("種類: " + (mime.isEmpty() ? "テキスト" : mime)
                + "\nファイル: " + uris.size() + "件\n\n" + visible);
    }

    private void handleWidgetAction(Intent intent) {
        if (intent == null) return;
        String action = intent.getStringExtra(MemoBrainWidget.EXTRA_ACTION);
        if (action == null || action.isEmpty()) return;
        intent.removeExtra(MemoBrainWidget.EXTRA_ACTION);

        if (MemoBrainWidget.ACTION_MEMO.equals(action)) {
            note.requestFocus();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        } else if (MemoBrainWidget.ACTION_FILE.equals(action)) {
            Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            picker.addCategory(Intent.CATEGORY_OPENABLE);
            picker.setType("*/*");
            picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(picker, REQUEST_WIDGET_FILE);
        } else if (MemoBrainWidget.ACTION_CHAT.equals(action)) {
            openChat();
        } else if (MemoBrainWidget.ACTION_HISTORY.equals(action)) {
            showHistory();
        }
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

    private void openChat() {
        String chatUrl = prefs.getChatWebUrl();
        if (chatUrl.isEmpty() || !chatUrl.toLowerCase(Locale.ROOT).startsWith("https://")) {
            Toast.makeText(this, "Dify接続設定でDify Web App URLを登録してください", Toast.LENGTH_LONG).show();
            connectionSettings();
            return;
        }
        startActivity(new Intent(this, ChatActivity.class));
    }

    private void enqueueSave() {
        String noteValue = note.getText() == null ? "" : note.getText().toString().trim();
        boolean hasSharedText = sharedText != null && !sharedText.trim().isEmpty();
        boolean hasAttachments = !uris.isEmpty();
        if (!hasSharedText && !hasAttachments && noteValue.isEmpty()) {
            status.setText("保存する内容がありません。テキストまたは補足メモを入力するか、ファイルを共有してください。");
            Toast.makeText(this, "保存する内容を入力してください", Toast.LENGTH_LONG).show();
            note.requestFocus();
            return;
        }

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
        final String q = marker() + "\n" + sharedText + "\n補足: " + noteValue;
        final ArrayList<Uri> snapshotUris = new ArrayList<>(uris);
        final String snapshotMime = mime;

        stagingExecutor.execute(() -> {
            try {
                String jobId = PendingJobStore.create(this, q, snapshotUris, snapshotMime);
                MemoWorkScheduler.enqueue(getApplicationContext(), jobId, ExistingWorkPolicy.KEEP);

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
                    if (e instanceof PendingJobStore.DuplicateException) {
                        status.setText("同じURLまたはファイルが送信履歴にあるため、重複登録を止めました。");
                        Toast.makeText(this, "重複するURLまたはファイルです", Toast.LENGTH_LONG).show();
                    } else {
                        status.setText("保存準備に失敗しました。共有元またはDify接続設定を確認してください。");
                    }
                });
            }
        });
    }

    private void showHistory() {
        JSONArray items = HistoryStore.list(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(20), dp(8), dp(20), dp(8));

        if (items.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("送信履歴はありません。");
            list.addView(empty);
        } else {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd HH:mm", Locale.JAPAN);
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                String jobId = item.optString("job_id");
                String itemStatus = item.optString("status");
                String kind = item.optString("kind", "TEXT");
                long createdAt = item.optLong("created_at");

                TextView row = new TextView(this);
                row.setText(format.format(new Date(createdAt)) + "  " + kind + "  " + historyStatus(itemStatus));
                row.setPadding(0, dp(10), 0, dp(4));
                list.addView(row);

                if (HistoryStore.FAILED.equals(itemStatus)) {
                    Button retry = new Button(this);
                    retry.setText("この送信を再試行");
                    retry.setEnabled(PendingJobStore.exists(this, jobId));
                    list.addView(retry);
                    retry.setOnClickListener(v -> {
                        if (!PendingJobStore.exists(this, jobId)) {
                            HistoryStore.updateStatus(this, jobId, HistoryStore.EXPIRED);
                            Toast.makeText(this, "再送期限が切れています", Toast.LENGTH_LONG).show();
                            return;
                        }
                        HistoryStore.updateStatus(this, jobId, HistoryStore.QUEUED);
                        MemoWorkScheduler.enqueue(this, jobId, ExistingWorkPolicy.REPLACE);
                        Toast.makeText(this, "再送キューへ登録しました", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(list);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("送信履歴")
                .setView(scroll)
                .setPositiveButton("閉じる", null)
                .setNeutralButton("履歴を消去", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("送信履歴を消去")
                        .setMessage("重複判定に使う履歴も消去します。送信待ちデータは消去されず、24時間で自動削除されます。")
                        .setPositiveButton("消去", (d, w) -> {
                            HistoryStore.clear(this);
                            dialog.dismiss();
                            Toast.makeText(this, "送信履歴を消去しました", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("キャンセル", null)
                        .show()));
        dialog.show();
    }

    private String historyStatus(String value) {
        if (HistoryStore.QUEUED.equals(value)) return "送信待ち";
        if (HistoryStore.SENDING.equals(value)) return "送信中";
        if (HistoryStore.SUCCESS.equals(value)) return "成功";
        if (HistoryStore.FAILED.equals(value)) return "失敗（再送可）";
        if (HistoryStore.EXPIRED.equals(value)) return "期限切れ";
        return value;
    }

    private void connectionSettings() {
        final TextView explain = new TextView(this);
        explain.setText("本アプリの利用にはDifyが必須です。共有内容はここで指定したDifyへ送信されます。API Base、API Key、Dify Web App URLは端末内で暗号化して保存し、広告ページへは送信しません。");
        explain.setPadding(0, 0, 0, dp(16));

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

        final EditText chatWebUrl = new EditText(this);
        chatWebUrl.setHint("Dify Web App URL（任意・HTTPS）");
        chatWebUrl.setText(prefs.getChatWebUrl());
        chatWebUrl.setSingleLine(true);
        chatWebUrl.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        chatWebUrl.setImeOptions(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);

        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(32), 0, dp(32), 0);
        l.addView(explain);
        l.addView(base);
        l.addView(key);
        l.addView(chatWebUrl);

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
                String chatWebValue = chatWebUrl.getText().toString().trim();
                if (!baseValue.toLowerCase(Locale.ROOT).startsWith("https://")) {
                    base.setError("HTTPSのDify API Baseを入力してください");
                    return;
                }
                if (keyValue.isEmpty()) {
                    key.setError("Dify App API Keyを入力してください");
                    return;
                }
                if (!chatWebValue.isEmpty() && !chatWebValue.toLowerCase(Locale.ROOT).startsWith("https://")) {
                    chatWebUrl.setError("HTTPSのDify Web App URLを入力してください");
                    return;
                }
                try {
                    prefs.putBase(baseValue);
                    prefs.putKey(keyValue);
                    prefs.putChatWebUrl(chatWebValue);
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
                            .setMessage("端末に保存されているDify API Base、API Key、Dify Web App URLを削除します。")
                            .setPositiveButton("削除", (d, w) -> {
                                prefs.clearConnection();
                                base.setText("");
                                key.setText("");
                                chatWebUrl.setText("");
                                status.setText("Dify接続設定が必要です。");
                                Toast.makeText(this, "接続情報を削除しました", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            })
                            .setNegativeButton("キャンセル", null)
                            .show());
        });
        dialog.show();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 5001);
        }
    }
}
