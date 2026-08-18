package net.automationse.memobrainshare;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private TextView status;
    private TextView preview;
    private EditText note;
    private Button sendButton;
    private LinearLayout detailPanel;
    private EditText categoryInput, tagsInput;
    private Spinner prioritySpinner, profileSpinner;
    private CheckBox readLaterCheck, todoCheck;

    private final ArrayList<Uri> uris = new ArrayList<>();
    private String sharedText = "";
    private String mime = "";
    private SecurePrefs prefs;
    private ConnectionProfileStore profileStore;
    private final ArrayList<ConnectionProfileStore.Profile> profileChoices = new ArrayList<>();
    private final ExecutorService stagingExecutor = Executors.newSingleThreadExecutor();
    private final Handler responseHandler = new Handler(Looper.getMainLooper());
    private Runnable responseWatcher;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        prefs = new SecurePrefs(this);
        profileStore = new ConnectionProfileStore(this);
        profileStore.migrate(prefs);
        NotificationHelper.ensureChannel(this);
        buildUi();
        requestNotificationPermissionIfNeeded();
        handle(getIntent());
        stagingExecutor.execute(() -> PendingJobStore.cleanupExpired(getApplicationContext()));
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        handle(i);
    }

    @Override
    protected void onDestroy() {
        if (responseWatcher != null) responseHandler.removeCallbacks(responseWatcher);
        stagingExecutor.shutdown();
        super.onDestroy();
    }

    private void buildUi() {
        final int background = darkMode() ? Color.rgb(18, 18, 24) : Color.rgb(246, 247, 252);
        final int surface = darkMode() ? Color.rgb(31, 31, 41) : Color.WHITE;
        final int surfaceAlt = darkMode() ? Color.rgb(42, 41, 55) : Color.rgb(239, 238, 250);
        final int primary = darkMode() ? Color.rgb(190, 172, 255) : Color.rgb(92, 67, 190);
        final int onPrimary = darkMode() ? Color.rgb(40, 25, 90) : Color.WHITE;
        final int text = darkMode() ? Color.rgb(242, 239, 248) : Color.rgb(31, 30, 38);
        final int secondaryText = darkMode() ? Color.rgb(198, 194, 208) : Color.rgb(91, 88, 104);

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackgroundColor(background);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        applySystemBarInsets(outer, root);

        LinearLayout hero = card(surface, 22);
        TextView title = label("🧠  MemoBrain", 28, text, Typeface.BOLD);
        hero.addView(title);
        TextView subtitle = label("共有した情報を、あなたの第二の脳へ。", 15, secondaryText, Typeface.NORMAL);
        subtitle.setPadding(0, dp(7), 0, 0);
        hero.addView(subtitle);
        TextView version = label("Version " + BuildConfig.VERSION_NAME + "  •  build " + BuildConfig.VERSION_CODE,
                12, secondaryText, Typeface.NORMAL);
        version.setPadding(0, dp(8), 0, 0);
        hero.addView(version);
        TextView secure = label("🔒  暗号化キュー  •  HTTPS  •  最大24時間保持", 12, primary, Typeface.BOLD);
        secure.setPadding(0, dp(14), 0, 0);
        hero.addView(secure);
        root.addView(hero, cardParams(0, 0, 0, 14));

        TextView section = label("共有内容", 13, secondaryText, Typeface.BOLD);
        section.setPadding(dp(4), 0, 0, dp(7));
        root.addView(section);
        LinearLayout contentCard = card(surface, 18);
        preview = new TextView(this);
        preview.setTextColor(text);
        preview.setTextSize(14);
        preview.setPadding(0, 0, 0, dp(12));
        contentCard.addView(preview);

        note = new EditText(this);
        note.setHint("補足メモを追加（任意）");
        note.setHintTextColor(secondaryText);
        note.setTextColor(text);
        note.setMinLines(2);
        note.setPadding(dp(14), dp(12), dp(14), dp(12));
        note.setBackground(roundRect(surfaceAlt, 14, 0, Color.TRANSPARENT));
        note.setImeOptions(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        contentCard.addView(note, new LinearLayout.LayoutParams(-1, -2));
        root.addView(contentCard, cardParams(0, 0, 0, 12));

        Button detailToggle = new Button(this);
        detailToggle.setText("⚙  詳細を指定して保存   ▾");
        styleButton(detailToggle, surfaceAlt, primary, false);
        root.addView(detailToggle, cardParams(0, 0, 0, 10));

        detailPanel = card(surface, 18);
        detailPanel.setOrientation(LinearLayout.VERTICAL);
        detailPanel.setVisibility(View.GONE);

        TextView destinationLabel = new TextView(this);
        destinationLabel.setText("保存先プロファイル / Knowledge");
        destinationLabel.setTextColor(secondaryText);
        detailPanel.addView(destinationLabel);
        profileSpinner = new Spinner(this);
        detailPanel.addView(profileSpinner);

        categoryInput = new EditText(this);
        categoryInput.setHint("カテゴリ（空欄ならAIに任せる）");
        modernField(categoryInput, text, secondaryText, surfaceAlt);
        detailPanel.addView(categoryInput);
        tagsInput = new EditText(this);
        tagsInput.setHint("タグ（カンマ区切り・空欄ならAIに任せる）");
        modernField(tagsInput, text, secondaryText, surfaceAlt);
        detailPanel.addView(tagsInput);

        TextView priorityLabel = new TextView(this);
        priorityLabel.setText("重要度");
        priorityLabel.setTextColor(secondaryText);
        detailPanel.addView(priorityLabel);
        prioritySpinner = new Spinner(this);
        prioritySpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"AIに任せる", "低", "中", "高"}));
        detailPanel.addView(prioritySpinner);
        readLaterCheck = new CheckBox(this);
        readLaterCheck.setText("あとで読む");
        readLaterCheck.setTextColor(text);
        detailPanel.addView(readLaterCheck);
        todoCheck = new CheckBox(this);
        todoCheck.setText("TODOとして登録");
        todoCheck.setTextColor(text);
        detailPanel.addView(todoCheck);

        Button manageProfiles = new Button(this);
        manageProfiles.setText("接続プロファイルを追加・編集");
        styleButton(manageProfiles, surfaceAlt, primary, false);
        manageProfiles.setOnClickListener(v -> connectionSettings());
        detailPanel.addView(manageProfiles);
        root.addView(detailPanel);
        detailToggle.setOnClickListener(v -> {
            boolean show = detailPanel.getVisibility() != View.VISIBLE;
            detailPanel.setVisibility(show ? View.VISIBLE : View.GONE);
            detailToggle.setText(show ? "⚙  詳細を閉じる   ▴" : "⚙  詳細を指定して保存   ▾");
        });
        refreshProfileSpinner();

        sendButton = new Button(this);
        sendButton.setText("保存する  →");
        sendButton.setTextSize(17);
        styleButton(sendButton, primary, onPrimary, true);
        sendButton.setOnClickListener(v -> enqueueSave());
        root.addView(sendButton, new LinearLayout.LayoutParams(-1, dp(58)));

        LinearLayout shortcuts = new LinearLayout(this);
        shortcuts.setOrientation(LinearLayout.HORIZONTAL);
        shortcuts.setPadding(0, dp(12), 0, 0);
        Button history = new Button(this);
        history.setText("↻  履歴・再送");
        styleButton(history, surface, text, false);
        history.setOnClickListener(v -> showHistory());
        shortcuts.addView(history, new LinearLayout.LayoutParams(0, dp(52), 1f));

        Button chat = new Button(this);
        chat.setText("✦  AIチャット");
        styleButton(chat, surface, text, false);
        chat.setOnClickListener(v -> openChat());
        LinearLayout.LayoutParams chatParams = new LinearLayout.LayoutParams(0, dp(52), 1f);
        chatParams.setMargins(dp(8), 0, 0, 0);
        shortcuts.addView(chat, chatParams);
        root.addView(shortcuts);

        addKnowledgeShortcuts(root, surface, text, "⌕  検索", KnowledgeActivity.MODE_SEARCH,
                "☰  ナレッジ一覧", KnowledgeActivity.MODE_LIST);
        addKnowledgeShortcuts(root, surface, text, "✓  TODO", KnowledgeActivity.MODE_TODO,
                "◷  あとで読む", KnowledgeActivity.MODE_READ_LATER);

        Button about = new Button(this);
        about.setText("アプリ情報  •  v" + BuildConfig.VERSION_NAME);
        styleButton(about, Color.TRANSPARENT, primary, false);
        about.setOnClickListener(v -> showAbout());
        root.addView(about, cardParams(0, 6, 0, 0));

        Button connection = new Button(this);
        connection.setText("接続プロファイルを管理");
        styleButton(connection, Color.TRANSPARENT, primary, false);
        connection.setOnClickListener(v -> connectionSettings());
        root.addView(connection, cardParams(0, 8, 0, 0));

        status = new TextView(this);
        status.setTextColor(secondaryText);
        status.setTextSize(13);
        status.setPadding(0, dp(16), 0, 0);
        root.addView(status);

        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        outer.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(outer);
    }

    private void addKnowledgeShortcuts(LinearLayout parent, int surfaceColor, int textColor,
                                       String firstLabel, String firstMode, String secondLabel, String secondMode) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, 0);
        Button first = new Button(this);
        first.setText(firstLabel);
        styleButton(first, surfaceColor, textColor, false);
        first.setOnClickListener(v -> openKnowledge(firstMode));
        row.addView(first, new LinearLayout.LayoutParams(0, dp(50), 1f));
        Button second = new Button(this);
        second.setText(secondLabel);
        styleButton(second, surfaceColor, textColor, false);
        second.setOnClickListener(v -> openKnowledge(secondMode));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(50), 1f);
        params.setMargins(dp(8), 0, 0, 0);
        row.addView(second, params);
        parent.addView(row);
    }

    private void openKnowledge(String mode) {
        ConnectionProfileStore.Profile selected = profileStore.selected();
        if (selected == null || !selected.isConfigured()) {
            connectionSettings();
            return;
        }
        Intent intent = new Intent(this, KnowledgeActivity.class);
        intent.putExtra(KnowledgeActivity.EXTRA_MODE, mode);
        startActivity(intent);
    }

    private void showAbout() {
        String type = BuildConfig.DEBUG ? "Develop版" : "Release版";
        new AlertDialog.Builder(this)
                .setTitle("MemoBrainについて")
                .setMessage("バージョン: " + BuildConfig.VERSION_NAME
                        + "\nバージョンコード: " + BuildConfig.VERSION_CODE
                        + "\nビルド種別: " + type
                        + "\nアプリID: " + getPackageName())
                .setPositiveButton("閉じる", null)
                .show();
    }

    private boolean darkMode() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private LinearLayout card(int color, int padding) {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(padding), dp(padding), dp(padding), dp(padding));
        view.setBackground(roundRect(color, 20, 1, darkMode() ? Color.rgb(57, 56, 70) : Color.rgb(229, 228, 237)));
        view.setElevation(dp(2));
        return view;
    }

    private LinearLayout.LayoutParams cardParams(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return p;
    }

    private TextView label(String value, float size, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", style));
        return view;
    }

    private GradientDrawable roundRect(int color, int radius, int strokeWidth, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color); drawable.setCornerRadius(dp(radius));
        if (strokeWidth > 0) drawable.setStroke(dp(strokeWidth), strokeColor);
        return drawable;
    }

    private void styleButton(Button button, int background, int foreground, boolean bold) {
        button.setAllCaps(false);
        button.setTextColor(foreground);
        button.setTextSize(14);
        button.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        button.setBackground(roundRect(background, 16, background == Color.TRANSPARENT ? 1 : 0, foreground));
        button.setStateListAnimator(null);
        button.setPadding(dp(12), 0, dp(12), 0);
    }

    private void modernField(EditText field, int text, int hint, int background) {
        field.setTextColor(text); field.setHintTextColor(hint);
        field.setPadding(dp(14), dp(12), dp(14), dp(12));
        field.setBackground(roundRect(background, 12, 0, Color.TRANSPARENT));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(8), 0, 0); field.setLayoutParams(p);
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
        ConnectionProfileStore.Profile selected = profileStore.selected();
        status.setText(selected != null && selected.isConfigured() ? "" : "Dify接続プロファイルの設定が必要です。");
        sendButton.setEnabled(true);
    }

    private void renderPreview() {
        String visible = sharedText == null ? "" : sharedText;
        if (visible.length() > 500) visible = visible.substring(0, 500) + "…";
        preview.setText("種類: " + (mime.isEmpty() ? "テキスト" : mime)
                + "\nファイル: " + uris.size() + "件\n\n" + visible);
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
        ConnectionProfileStore.Profile selected = profileStore.selected();
        String chatUrl = selected == null ? "" : selected.chatUrl;
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

        int profileIndex = profileSpinner == null ? -1 : profileSpinner.getSelectedItemPosition();
        ConnectionProfileStore.Profile profile = profileIndex >= 0 && profileIndex < profileChoices.size()
                ? profileChoices.get(profileIndex) : profileStore.selected();
        if (profile == null || !profile.isConfigured()) {
            connectionSettings();
            return;
        }
        if (!profile.base.toLowerCase(Locale.ROOT).startsWith("https://")) {
            status.setText("Dify API Base はHTTPSで設定してください。");
            connectionSettings();
            return;
        }

        sendButton.setEnabled(false);
        status.setText(uris.isEmpty() ? "暗号化して保存キューに登録しています…" : "共有ファイルを暗号化して一時保存しています…");
        profileStore.select(profile.id);
        final String q = marker() + "\n" + metadata(profile, noteValue) + "\n" + sharedText;
        final String profileId = profile.id;
        final ArrayList<Uri> snapshotUris = new ArrayList<>(uris);
        final String snapshotMime = mime;

        stagingExecutor.execute(() -> {
            try {
                String jobId = PendingJobStore.create(this, q, snapshotUris, snapshotMime, profileId);
                MemoWorkScheduler.enqueue(getApplicationContext(), jobId, ExistingWorkPolicy.KEEP);

                runOnUiThread(() -> {
                    status.setText("保存キューに登録しました。別アプリへ移動してOKです。");
                    Toast.makeText(this, "MemoBrainでバックグラウンド保存を開始しました", Toast.LENGTH_SHORT).show();
                    sendButton.setEnabled(true);
                    note.setText("");
                    categoryInput.setText("");
                    tagsInput.setText("");
                    prioritySpinner.setSelection(0);
                    readLaterCheck.setChecked(false);
                    todoCheck.setChecked(false);
                    watchSaveResult(jobId);
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

    private void watchSaveResult(String jobId) {
        if (responseWatcher != null) responseHandler.removeCallbacks(responseWatcher);
        responseWatcher = new Runnable() {
            @Override
            public void run() {
                JSONArray items = HistoryStore.list(MainActivity.this);
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.optJSONObject(i);
                    if (item == null || !jobId.equals(item.optString("job_id"))) continue;
                    String itemStatus = item.optString("status");
                    if (HistoryStore.SUCCESS.equals(itemStatus)) {
                        String answer = item.optString("answer", "").trim();
                        status.setText(answer.isEmpty() ? "MemoBrainへの保存が完了しました。" : answer);
                        responseWatcher = null;
                        return;
                    }
                    if (HistoryStore.FAILED.equals(itemStatus) || HistoryStore.EXPIRED.equals(itemStatus)) {
                        status.setText("保存に失敗しました。送信履歴から状態を確認してください。");
                        responseWatcher = null;
                        return;
                    }
                    break;
                }
                responseHandler.postDelayed(this, 750);
            }
        };
        responseHandler.post(responseWatcher);
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

                String answer = item.optString("answer", "").trim();
                if (!answer.isEmpty()) {
                    TextView reply = new TextView(this);
                    reply.setText(answer);
                    reply.setTextIsSelectable(true);
                    reply.setPadding(dp(8), dp(4), 0, dp(12));
                    list.addView(reply);
                }

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

    private String metadata(ConnectionProfileStore.Profile profile, String noteValue) {
        String[] priorityValues = {"auto", "low", "medium", "high"};
        int priority = prioritySpinner == null ? 0 : prioritySpinner.getSelectedItemPosition();
        if (priority < 0 || priority >= priorityValues.length) priority = 0;
        try {
            JSONObject meta = new JSONObject();
            meta.put("knowledge", profile.knowledgeName);
            meta.put("category", categoryInput.getText().toString().trim());
            meta.put("tags", tagsInput.getText().toString().trim());
            meta.put("note", noteValue);
            meta.put("priority", priorityValues[priority]);
            meta.put("read_later", readLaterCheck.isChecked());
            meta.put("todo", todoCheck.isChecked());
            return "[MB:META]\n" + meta + "\n[/MB:META]";
        } catch (Exception ignored) { return "[MB:META]\n{}\n[/MB:META]"; }
    }

    private void refreshProfileSpinner() {
        if (profileSpinner == null) return;
        profileChoices.clear();
        profileChoices.addAll(profileStore.list());
        ArrayList<String> labels = new ArrayList<>();
        ConnectionProfileStore.Profile selected = profileStore.selected();
        int selection = 0;
        for (int i = 0; i < profileChoices.size(); i++) {
            ConnectionProfileStore.Profile p = profileChoices.get(i);
            labels.add((p.name.isEmpty() ? "名称未設定" : p.name) + (p.knowledgeName.isEmpty() ? "（Dify既定Knowledge）" : " / " + p.knowledgeName));
            if (selected != null && selected.id.equals(p.id)) selection = i;
        }
        if (labels.isEmpty()) labels.add("プロファイル未設定");
        profileSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        if (!profileChoices.isEmpty()) profileSpinner.setSelection(selection);
    }

    private void connectionSettings() {
        List<ConnectionProfileStore.Profile> items = profileStore.list();
        ArrayList<String> names = new ArrayList<>();
        ConnectionProfileStore.Profile selected = profileStore.selected();
        int checked = -1;
        for (int i = 0; i < items.size(); i++) {
            names.add(items.get(i).name.isEmpty() ? "名称未設定" : items.get(i).name);
            if (selected != null && selected.id.equals(items.get(i).id)) checked = i;
        }
        final int[] choice = {checked};
        new AlertDialog.Builder(this)
                .setTitle("接続プロファイル")
                .setSingleChoiceItems(names.toArray(new String[0]), checked, (d, which) -> choice[0] = which)
                .setPositiveButton("選択", (d, w) -> {
                    if (choice[0] >= 0 && choice[0] < items.size()) profileStore.select(items.get(choice[0]).id);
                    refreshProfileSpinner();
                })
                .setNeutralButton("追加", (d, w) -> editProfile(null))
                .setNegativeButton(items.isEmpty() ? "閉じる" : "編集", (d, w) -> {
                    if (choice[0] >= 0 && choice[0] < items.size()) editProfile(items.get(choice[0]));
                }).show();
    }

    private void editProfile(ConnectionProfileStore.Profile existing) {
        EditText name = field("プロファイル名（例: 仕事）", existing == null ? "" : existing.name, false);
        EditText knowledge = field("Knowledge名（例: 技術情報）", existing == null ? "" : existing.knowledgeName, false);
        EditText base = field("Dify API Base（HTTPS）", existing == null ? "" : existing.base, false);
        EditText key = field("Dify App API Key", existing == null ? "" : existing.key, true);
        EditText chat = field("Dify Web App URL（任意・HTTPS）", existing == null ? "" : existing.chatUrl, false);
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(24), 0, dp(24), 0);
        form.addView(name); form.addView(knowledge); form.addView(base); form.addView(key); form.addView(chat);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(existing == null ? "プロファイル追加" : "プロファイル編集")
                .setView(form).setPositiveButton("保存", null).setNeutralButton(existing == null ? "" : "削除", null).setNegativeButton("キャンセル", null).create();
        dialog.setOnShowListener(x -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String b = base.getText().toString().trim(), k = key.getText().toString().trim(), c = chat.getText().toString().trim();
                if (name.getText().toString().trim().isEmpty()) { name.setError("名前を入力してください"); return; }
                if (!b.toLowerCase(Locale.ROOT).startsWith("https://")) { base.setError("HTTPS URLを入力してください"); return; }
                if (k.isEmpty()) { key.setError("API Keyを入力してください"); return; }
                if (!c.isEmpty() && !c.toLowerCase(Locale.ROOT).startsWith("https://")) { chat.setError("HTTPS URLを入力してください"); return; }
                ConnectionProfileStore.Profile p = new ConnectionProfileStore.Profile(existing == null ? null : existing.id,
                        name.getText().toString(), knowledge.getText().toString(), b, k, c);
                profileStore.save(p, true); refreshProfileSpinner(); status.setText(""); dialog.dismiss();
            });
            if (existing == null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.GONE);
            else dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                profileStore.delete(existing.id); refreshProfileSpinner(); dialog.dismiss();
            });
        });
        dialog.show();
    }

    private EditText field(String hint, String value, boolean password) {
        EditText field = new EditText(this); field.setHint(hint); field.setText(value); field.setSingleLine(true);
        field.setImeOptions(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        if (password) field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return field;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 5001);
        }
    }
}
