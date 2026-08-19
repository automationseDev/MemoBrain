package net.automationse.memobrainshare;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Configuration;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Native Knowledge tools backed exclusively by the configured Dify App API. */
public final class KnowledgeActivity extends Activity {
    public static final String EXTRA_MODE = "knowledge_mode";
    public static final String MODE_SEARCH = "search";
    public static final String MODE_LIST = "list";
    public static final String MODE_TODO = "todo";
    public static final String MODE_READ_LATER = "read_later";

    private static final Pattern LIST_ENTRY = Pattern.compile("^\\s*\\d+[.．、]\\s*(.+?)\\s*(?:\\([0-9]{2}/[0-9]{2}\\s+[0-9]{2}:[0-9]{2}\\))?\\s*$");
    private static final Pattern ITEM_DATE = Pattern.compile("(?i)(?:updated_at:\\s*)?([0-9]{4}[-/][0-9]{2}[-/][0-9]{2}(?:[ T][0-9]{2}:[0-9]{2}(?::[0-9]{2})?)?)");

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ArrayList<ConnectionProfileStore.Profile> profiles = new ArrayList<>();
    private ConnectionProfileStore profileStore;
    private KnowledgeLocalStore localStore;
    private Spinner profileSpinner;
    private EditText searchField;
    private EditText categoryField;
    private EditText tagField;
    private LinearLayout results;
    private TextView heading;
    private ProgressBar progress;
    private TextView syncStatus;
    private Spinner sortSpinner;
    private JSONArray visibleItems = new JSONArray();
    private String visibleMessage = "";
    private String visibleAction = "";
    private String mode = MODE_SEARCH;
    private String lastSearch = "";
    private int surface;
    private int foreground;
    private int muted;
    private int accent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        profileStore = new ConnectionProfileStore(this);
        profileStore.migrate(new SecurePrefs(this));
        localStore = new KnowledgeLocalStore(this);
        String requested = getIntent().getStringExtra(EXTRA_MODE);
        if (requested != null && !requested.trim().isEmpty()) mode = requested;
        buildUi();
        selectMode(mode);
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void buildUi() {
        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        int background = dark ? Color.rgb(18, 18, 24) : Color.rgb(246, 247, 252);
        surface = dark ? Color.rgb(31, 31, 41) : Color.WHITE;
        foreground = dark ? Color.rgb(242, 239, 248) : Color.rgb(31, 30, 38);
        muted = dark ? Color.rgb(198, 194, 208) : Color.rgb(91, 88, 104);
        accent = dark ? Color.rgb(190, 172, 255) : Color.rgb(92, 67, 190);

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(background);
        shell.setPadding(dp(18), dp(16), dp(18), dp(12));
        shell.setOnApplyWindowInsetsListener((view, insets) -> {
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
            view.setPadding(dp(18), dp(16) + top, dp(18), dp(12) + bottom);
            return insets;
        });

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("←", false);
        back.setOnClickListener(view -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(54), dp(46)));
        TextView title = text("MemoBrain Knowledge", 19, true);
        title.setPadding(dp(12), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        shell.addView(header);

        profileSpinner = new Spinner(this);
        refreshProfiles();
        LinearLayout.LayoutParams profileParams = new LinearLayout.LayoutParams(-1, dp(48));
        profileParams.setMargins(0, dp(12), 0, dp(8));
        shell.addView(profileSpinner, profileParams);

        addNavigationRow(shell, "⌕ 検索", MODE_SEARCH, "☰ 一覧", MODE_LIST);
        addNavigationRow(shell, "✓ TODO", MODE_TODO, "◷ あとで読む", MODE_READ_LATER);

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchField = new EditText(this);
        searchField.setHint("Knowledgeから検索する");
        searchField.setHintTextColor(muted);
        searchField.setTextColor(foreground);
        searchField.setSingleLine(true);
        searchField.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        searchField.setBackground(background(surface));
        searchField.setPadding(dp(12), 0, dp(12), 0);
        searchField.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search();
                return true;
            }
            return false;
        });
        searchRow.addView(searchField, new LinearLayout.LayoutParams(0, dp(48), 1f));
        Button search = button("検索", true);
        search.setOnClickListener(view -> search());
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(dp(78), dp(48));
        searchParams.setMargins(dp(8), 0, 0, 0);
        searchRow.addView(search, searchParams);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(48));
        rowParams.setMargins(0, dp(14), 0, dp(12));
        shell.addView(searchRow, rowParams);

        LinearLayout filterRow = new LinearLayout(this);
        categoryField = filterField("カテゴリ（任意）");
        filterRow.addView(categoryField, new LinearLayout.LayoutParams(0, dp(44), 1f));
        tagField = filterField("タグ（任意）");
        LinearLayout.LayoutParams tagParams = new LinearLayout.LayoutParams(0, dp(44), 1f);
        tagParams.setMargins(dp(8), 0, 0, 0);
        filterRow.addView(tagField, tagParams);
        LinearLayout.LayoutParams filterParams = new LinearLayout.LayoutParams(-1, dp(44));
        filterParams.setMargins(0, 0, 0, dp(12));
        shell.addView(filterRow, filterParams);

        LinearLayout toolsRow = new LinearLayout(this);
        sortSpinner = new Spinner(this);
        sortSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"新しい順", "タイトル順", "タイトル逆順"}));
        toolsRow.addView(sortSpinner, new LinearLayout.LayoutParams(0, dp(44), 1f));
        Button apply = button("絞り込み", false);
        apply.setOnClickListener(view -> {
            if (MODE_SEARCH.equals(mode) && searchField.getText() != null && !searchField.getText().toString().trim().isEmpty()) search();
            else selectMode(mode);
        });
        LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(dp(100), dp(44));
        applyParams.setMargins(dp(8), 0, 0, 0);
        toolsRow.addView(apply, applyParams);
        Button history = button("履歴", false);
        history.setOnClickListener(view -> showSearchHistory());
        LinearLayout.LayoutParams historyParams = new LinearLayout.LayoutParams(dp(76), dp(44));
        historyParams.setMargins(dp(8), 0, 0, 0);
        toolsRow.addView(history, historyParams);
        shell.addView(toolsRow, new LinearLayout.LayoutParams(-1, dp(44)));

        sortSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (visibleItems.length() > 0) renderStructuredItems();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        syncStatus = text("未同期", 11, false);
        syncStatus.setTextColor(muted);
        LinearLayout.LayoutParams syncParams = new LinearLayout.LayoutParams(-1, -2);
        syncParams.setMargins(0, dp(8), 0, dp(4));
        shell.addView(syncStatus, syncParams);

        heading = text("", 16, true);
        shell.addView(heading);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(4));
        progressParams.setMargins(0, dp(8), 0, dp(8));
        shell.addView(progress, progressParams);

        ScrollView scroll = new ScrollView(this);
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(results, new ScrollView.LayoutParams(-1, -2));
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(shell);
        shell.requestApplyInsets();
    }

    private void addNavigationRow(LinearLayout parent, String firstLabel, String firstMode, String secondLabel, String secondMode) {
        LinearLayout row = new LinearLayout(this);
        Button first = button(firstLabel, false);
        first.setOnClickListener(view -> selectMode(firstMode));
        row.addView(first, new LinearLayout.LayoutParams(0, dp(46), 1f));
        Button second = button(secondLabel, false);
        second.setOnClickListener(view -> selectMode(secondMode));
        LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        secondParams.setMargins(dp(8), 0, 0, 0);
        row.addView(second, secondParams);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(46));
        params.setMargins(0, dp(6), 0, 0);
        parent.addView(row, params);
    }

    private void selectMode(String selectedMode) {
        mode = selectedMode;
        results.removeAllViews();
        if (MODE_LIST.equals(mode)) {
            heading.setText("保存したナレッジ");
            executeAction("knowledge_list", "");
        } else if (MODE_TODO.equals(mode)) {
            heading.setText("未完了のTODO");
            executeAction("todo_list", "");
        } else if (MODE_READ_LATER.equals(mode)) {
            heading.setText("あとで読む");
            executeAction("read_later_list", "");
        } else {
            mode = MODE_SEARCH;
            heading.setText("Knowledge検索");
            addMessage("質問やキーワードを入力すると、DifyのKnowledgeから直接検索できます。");
        }
    }

    private void search() {
        String query = searchField.getText() == null ? "" : searchField.getText().toString().trim();
        if (query.isEmpty()) {
            searchField.setError("検索内容を入力してください");
            return;
        }
        mode = MODE_SEARCH;
        lastSearch = query;
        localStore.addHistory(query);
        heading.setText("検索結果");
        executeAction("knowledge_search", query);
    }

    private void executeAction(String action, String query) {
        JSONObject inputs = new JSONObject();
        try {
            inputs.put("action", action);
            inputs.put("query", query == null ? "" : query.trim());
            inputs.put("category", categoryField == null ? "" : categoryField.getText().toString().trim());
            inputs.put("tag", tagField == null ? "" : tagField.getText().toString().trim());
        } catch (Exception ignored) {
        }
        executeRequest(query == null || query.trim().isEmpty() ? action : query, inputs, false);
    }

    private void execute(String query, boolean canResearch) {
        executeRequest(query, new JSONObject(), canResearch);
    }

    private void executeRequest(String query, JSONObject inputs, boolean canResearch) {
        ConnectionProfileStore.Profile profile = selectedProfile();
        if (profile == null || !profile.isConfigured()) {
            results.removeAllViews();
            addMessage("先にホーム画面からDify接続プロファイルを設定してください。");
            return;
        }
        profileStore.select(profile.id);
        final String cacheKey = cacheKey(profile.id, inputs, query);
        KnowledgeLocalStore.Cached cached = localStore.get(cacheKey);
        if (cached != null) {
            showAnswer(cached.answer, canResearch, true, cached.savedAt);
        }
        if (!isOnline()) {
            progress.setVisibility(View.GONE);
            if (cached == null) {
                results.removeAllViews();
                addMessage("オフラインです。この条件のキャッシュはまだありません。");
                syncStatus.setText("オフライン・未取得");
            }
            return;
        }
        progress.setVisibility(View.VISIBLE);
        if (cached == null) {
            results.removeAllViews();
            addMessage("Difyへ問い合わせています…");
        }
        syncStatus.setText(cached == null ? "同期中…" : "キャッシュ表示中・同期中…");
        executor.execute(() -> {
            try {
                String answer = new DifyClient(profile.base, profile.key).chat(query, Collections.emptyList(), inputs);
                localStore.put(cacheKey, answer);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    progress.setVisibility(View.GONE);
                    showAnswer(answer, canResearch, false, System.currentTimeMillis());
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    progress.setVisibility(View.GONE);
                    if (cached == null) {
                        results.removeAllViews();
                        addMessage("問い合わせに失敗しました。接続設定、通信状態、Dify側の実行状況を確認してください。");
                        syncStatus.setText("同期失敗");
                    } else {
                        syncStatus.setText("同期失敗・キャッシュを表示中");
                    }
                });
            }
        });
    }

    private void showAnswer(String answer, boolean canResearch) {
        showAnswer(answer, canResearch, false, System.currentTimeMillis());
    }

    private void showAnswer(String answer, boolean canResearch, boolean cached, long savedAt) {
        results.removeAllViews();
        syncStatus.setText((cached ? "キャッシュ: " : "同期済み: ")
                + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(savedAt)));
        String value = answer == null ? "" : answer.trim();
        if (value.isEmpty()) {
            addMessage("Difyから表示できる回答が返されませんでした。");
            return;
        }
        if (showStructuredAnswer(value)) return;
        addMessage(value);
        if (canResearch && needsResearch(value) && !lastSearch.isEmpty()) {
            Button research = button("🌐 Webで調査してKnowledgeへ保存", true);
            research.setOnClickListener(view -> confirmResearch());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(54));
            params.setMargins(0, dp(12), 0, dp(8));
            results.addView(research, params);
        }
        if (MODE_LIST.equals(mode) || MODE_TODO.equals(mode) || MODE_READ_LATER.equals(mode)) {
            addManagementActions(value);
        }
    }

    private boolean showStructuredAnswer(String value) {
        try {
            JSONObject payload = new JSONObject(value);
            if (payload.optInt("version", 0) != 1) return false;
            String action = payload.optString("action", "");
            String message = payload.optString("message", "").trim();
            JSONArray items = payload.optJSONArray("items");
            visibleAction = action;
            visibleMessage = message;
            visibleItems = items == null ? new JSONArray() : items;
            renderStructuredItems();
            if ((items == null || items.length() == 0)
                    && ("todo_list".equals(action) || "read_later_list".equals(action))) {
                addManualManagement();
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void renderStructuredItems() {
        results.removeAllViews();
        if (!visibleMessage.isEmpty()) addMessage(visibleMessage);
        ArrayList<JSONObject> items = new ArrayList<>();
        for (int i = 0; i < visibleItems.length(); i++) {
            JSONObject item = visibleItems.optJSONObject(i);
            if (item != null) items.add(item);
        }
        int order = sortSpinner == null ? 0 : sortSpinner.getSelectedItemPosition();
        Comparator<JSONObject> byTitle = Comparator.comparing(
                item -> item.optString("title", ""), String.CASE_INSENSITIVE_ORDER);
        if (order == 0) Collections.sort(items, Comparator.comparing(this::itemDate).reversed());
        else if (order == 1) Collections.sort(items, byTitle);
        else if (order == 2) Collections.sort(items, byTitle.reversed());
        for (JSONObject item : items) {
            addItemActions(item.optString("title", "(無題)"), item.optString("preview", "").trim());
        }
    }

    private String itemDate(JSONObject item) {
        Matcher matcher = ITEM_DATE.matcher(item.optString("preview", ""));
        String latest = "";
        while (matcher.find()) latest = matcher.group(1);
        return latest.replace('/', '-').replace('T', ' ');
    }

    private void showSearchHistory() {
        List<String> history = localStore.history();
        if (history.isEmpty()) {
            Toast.makeText(this, "検索履歴はまだありません", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] values = history.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("検索履歴").setItems(values, (dialog, which) -> {
            searchField.setText(values[which]);
            searchField.setSelection(values[which].length());
            search();
        }).setNegativeButton("閉じる", null).show();
    }

    private String cacheKey(String profileId, JSONObject inputs, String query) {
        return Integer.toHexString((profileId + "|" + inputs.toString() + "|" + query).hashCode());
    }

    private boolean isOnline() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) return false;
        Network network = manager.getActiveNetwork();
        NetworkCapabilities caps = network == null ? null : manager.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private boolean needsResearch(String answer) {
        return answer.contains("Knowledgeに十分な情報がありません")
                || answer.contains("Web調査:") || answer.contains("Web調査：");
    }

    private void confirmResearch() {
        final String query = lastSearch;
        new AlertDialog.Builder(this)
                .setTitle("Web調査を実行")
                .setMessage("GeminiでWebを調査し、結果をKnowledgeへ保存します。\n\n" + query)
                .setPositiveButton("調査して保存", (dialog, which) -> execute("Web調査: " + query, false))
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void addManagementActions(String answer) {
        String[] lines = answer.split("\\r?\\n");
        int count = 0;
        for (String line : lines) {
            Matcher matcher = LIST_ENTRY.matcher(line);
            if (!matcher.matches()) continue;
            String title = matcher.group(1).trim();
            if (title.isEmpty()) continue;
            addItemActions(title);
            if (++count >= 20) break;
        }
        if ((MODE_TODO.equals(mode) || MODE_READ_LATER.equals(mode)) && count == 0) {
            addManualManagement();
        }
    }

    private void addItemActions(String title) {
        addItemActions(title, "");
    }

    private void addItemActions(String title, String preview) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackground(background(surface));
        card.addView(text(title, 14, true));
        if (!preview.isEmpty()) {
            TextView summary = text(preview, 12, false);
            summary.setPadding(0, dp(5), 0, dp(7));
            card.addView(summary);
        }
        LinearLayout actions = new LinearLayout(this);
        Button detail = button("詳細", false);
        detail.setOnClickListener(view -> {
            mode = MODE_SEARCH;
            lastSearch = title;
            heading.setText("ナレッジ詳細");
            executeAction("knowledge_detail", title);
        });
        actions.addView(detail, new LinearLayout.LayoutParams(0, dp(42), 1f));
        if (MODE_TODO.equals(mode) || MODE_READ_LATER.equals(mode)) {
            boolean todo = MODE_TODO.equals(mode);
            Button complete = button(todo ? "完了にする" : "読了にする", true);
            complete.setOnClickListener(view -> executeAction(todo ? "todo_complete" : "read_later_complete", title));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1f);
            params.setMargins(dp(8), 0, 0, 0);
            actions.addView(complete, params);
        }
        card.addView(actions);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(8), 0, 0);
        results.addView(card, params);
    }

    private void addManualManagement() {
        boolean todo = MODE_TODO.equals(mode);
        Button action = button(todo ? "TODOを指定して完了" : "記事を指定して読了", false);
        action.setOnClickListener(view -> {
            EditText input = new EditText(this);
            input.setHint(todo ? "完了するTODOのタイトル" : "読了した記事のタイトル");
            new AlertDialog.Builder(this)
                    .setTitle(todo ? "TODOを完了" : "あとで読むを完了")
                    .setView(input)
                    .setPositiveButton("実行", (dialog, which) -> {
                        String title = input.getText().toString().trim();
                        if (!title.isEmpty()) executeAction(todo ? "todo_complete" : "read_later_complete", title);
                    })
                    .setNegativeButton("キャンセル", null)
                    .show();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(48));
        params.setMargins(0, dp(12), 0, 0);
        results.addView(action, params);
    }

    private void addMessage(String message) {
        TextView body = text(message, 14, false);
        body.setTextIsSelectable(true);
        body.setPadding(dp(14), dp(14), dp(14), dp(14));
        body.setBackground(background(surface));
        results.addView(body, new LinearLayout.LayoutParams(-1, -2));
    }

    private EditText filterField(String hint) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setHintTextColor(muted);
        field.setTextColor(foreground);
        field.setSingleLine(true);
        field.setTextSize(13);
        field.setBackground(background(surface));
        field.setPadding(dp(10), 0, dp(10), 0);
        return field;
    }

    private void refreshProfiles() {
        profiles.clear();
        profiles.addAll(profileStore.list());
        List<String> labels = new ArrayList<>();
        ConnectionProfileStore.Profile selected = profileStore.selected();
        int selection = 0;
        for (int index = 0; index < profiles.size(); index++) {
            ConnectionProfileStore.Profile profile = profiles.get(index);
            labels.add(profile.name + (profile.knowledgeName.isEmpty() ? "" : " / " + profile.knowledgeName));
            if (selected != null && selected.id.equals(profile.id)) selection = index;
        }
        if (labels.isEmpty()) labels.add("Difyプロファイル未設定");
        profileSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        if (!profiles.isEmpty()) profileSpinner.setSelection(selection);
    }

    private ConnectionProfileStore.Profile selectedProfile() {
        int selected = profileSpinner.getSelectedItemPosition();
        return selected >= 0 && selected < profiles.size() ? profiles.get(selected) : null;
    }

    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(foreground);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String value, boolean primary) {
        Button view = new Button(this);
        view.setText(value);
        view.setTextColor(primary ? Color.WHITE : foreground);
        view.setAllCaps(false);
        view.setTextSize(13);
        view.setPadding(dp(8), 0, dp(8), 0);
        view.setBackground(background(primary ? accent : surface));
        return view;
    }

    private GradientDrawable background(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(12));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
