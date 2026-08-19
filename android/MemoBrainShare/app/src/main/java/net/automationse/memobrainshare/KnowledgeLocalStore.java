package net.automationse.memobrainshare;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Encrypted Knowledge response cache and search history. */
public final class KnowledgeLocalStore {
    private static final Object LOCK = new Object();
    private static final String FILE_NAME = "knowledge_cache.json.enc";
    private static final int MAX_ENTRIES = 80;
    private static final int MAX_HISTORY = 20;

    public static final class Cached {
        public final String answer;
        public final long savedAt;
        Cached(String answer, long savedAt) { this.answer = answer; this.savedAt = savedAt; }
    }

    private final Context context;
    public KnowledgeLocalStore(Context context) { this.context = context.getApplicationContext(); }

    public Cached get(String key) {
        synchronized (LOCK) {
            JSONObject entry = readSafe().optJSONObject("entries");
            entry = entry == null ? null : entry.optJSONObject(key);
            return entry == null ? null : new Cached(entry.optString("answer"), entry.optLong("saved_at"));
        }
    }

    public void put(String key, String answer) {
        if (key == null || key.isEmpty() || answer == null || answer.isEmpty()) return;
        synchronized (LOCK) {
            try {
                JSONObject root = readSafe();
                JSONObject entries = root.optJSONObject("entries");
                if (entries == null) entries = new JSONObject();
                JSONObject value = new JSONObject();
                value.put("answer", answer);
                value.put("saved_at", System.currentTimeMillis());
                entries.put(key, value);
                while (entries.length() > MAX_ENTRIES) removeOldest(entries);
                root.put("entries", entries);
                write(root);
            } catch (Exception ignored) { }
        }
    }

    public void addHistory(String query) {
        String clean = query == null ? "" : query.trim();
        if (clean.isEmpty()) return;
        synchronized (LOCK) {
            try {
                JSONObject root = readSafe();
                JSONArray old = root.optJSONArray("history");
                JSONArray next = new JSONArray();
                next.put(clean);
                if (old != null) for (int i = 0; i < old.length() && next.length() < MAX_HISTORY; i++) {
                    String item = old.optString(i).trim();
                    if (!item.isEmpty() && !clean.equalsIgnoreCase(item)) next.put(item);
                }
                root.put("history", next);
                write(root);
            } catch (Exception ignored) { }
        }
    }

    public List<String> history() {
        synchronized (LOCK) {
            ArrayList<String> out = new ArrayList<>();
            JSONArray values = readSafe().optJSONArray("history");
            if (values != null) for (int i = 0; i < values.length(); i++) {
                String value = values.optString(i).trim();
                if (!value.isEmpty()) out.add(value);
            }
            return out;
        }
    }

    private void removeOldest(JSONObject entries) {
        String oldestKey = null; long oldest = Long.MAX_VALUE;
        JSONArray names = entries.names();
        if (names == null) return;
        for (int i = 0; i < names.length(); i++) {
            String key = names.optString(i);
            JSONObject value = entries.optJSONObject(key);
            long saved = value == null ? 0 : value.optLong("saved_at");
            if (saved < oldest) { oldest = saved; oldestKey = key; }
        }
        if (oldestKey != null) entries.remove(oldestKey);
    }

    private JSONObject empty() throws Exception {
        JSONObject root = new JSONObject();
        root.put("version", 1);
        root.put("entries", new JSONObject());
        root.put("history", new JSONArray());
        return root;
    }

    private File file() { return new File(context.getFilesDir(), FILE_NAME); }
    private JSONObject readSafe() {
        if (!file().isFile()) try { return empty(); } catch (Exception e) { return new JSONObject(); }
        try (FileInputStream in = new FileInputStream(file()); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CryptoStore.decrypt(in, out);
            return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
        } catch (Exception e) {
            return new JSONObject();
        }
    }
    private void write(JSONObject root) throws Exception {
        File tmp = new File(context.getFilesDir(), FILE_NAME + ".tmp");
        try (ByteArrayInputStream in = new ByteArrayInputStream(root.toString().getBytes(StandardCharsets.UTF_8));
             FileOutputStream out = new FileOutputStream(tmp)) { CryptoStore.encrypt(in, out); }
        if (file().exists() && !file().delete()) throw new IllegalStateException("キャッシュを更新できません");
        if (!tmp.renameTo(file())) throw new IllegalStateException("キャッシュを確定できません");
    }
}
