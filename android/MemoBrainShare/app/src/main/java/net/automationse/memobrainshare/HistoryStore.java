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
import java.util.HashSet;
import java.util.Set;

/** Stores non-content delivery history encrypted at rest. */
public final class HistoryStore {
    public static final String QUEUED = "queued";
    public static final String SENDING = "sending";
    public static final String SUCCESS = "success";
    public static final String FAILED = "failed";
    public static final String EXPIRED = "expired";

    private static final String FILE_NAME = "delivery_history.json.enc";
    private static final int MAX_ENTRIES = 100;
    private static final long MAX_AGE_MILLIS = 30L * 24L * 60L * 60L * 1000L;
    private static final Object LOCK = new Object();

    private HistoryStore() {}

    public static void addQueued(Context context, String jobId, String kind, JSONArray fingerprints) throws Exception {
        synchronized (LOCK) {
            JSONArray history = readInternal(context);
            JSONObject item = new JSONObject();
            long now = System.currentTimeMillis();
            item.put("job_id", jobId);
            item.put("kind", kind == null ? "TEXT" : kind);
            item.put("status", QUEUED);
            item.put("created_at", now);
            item.put("updated_at", now);
            item.put("fingerprints", fingerprints == null ? new JSONArray() : fingerprints);
            history.put(item);
            writeInternal(context, prune(history));
        }
    }

    public static void updateStatus(Context context, String jobId, String status) {
        synchronized (LOCK) {
            try {
                JSONArray history = readInternal(context);
                for (int i = 0; i < history.length(); i++) {
                    JSONObject item = history.optJSONObject(i);
                    if (item != null && jobId.equals(item.optString("job_id"))) {
                        item.put("status", status);
                        item.put("updated_at", System.currentTimeMillis());
                        break;
                    }
                }
                writeInternal(context, prune(history));
            } catch (Exception ignored) {
                // History must never make the actual save fail.
            }
        }
    }

    public static JSONArray list(Context context) {
        synchronized (LOCK) {
            try {
                JSONArray source = prune(readInternal(context));
                JSONArray newestFirst = new JSONArray();
                for (int i = source.length() - 1; i >= 0; i--) newestFirst.put(source.getJSONObject(i));
                return newestFirst;
            } catch (Exception ignored) {
                return new JSONArray();
            }
        }
    }

    public static boolean hasDuplicate(Context context, JSONArray fingerprints) {
        if (fingerprints == null || fingerprints.length() == 0) return false;
        synchronized (LOCK) {
            try {
                Set<String> wanted = new HashSet<>();
                for (int i = 0; i < fingerprints.length(); i++) wanted.add(fingerprints.optString(i));
                JSONArray history = prune(readInternal(context));
                for (int i = 0; i < history.length(); i++) {
                    JSONObject item = history.optJSONObject(i);
                    if (item == null || EXPIRED.equals(item.optString("status"))) continue;
                    JSONArray known = item.optJSONArray("fingerprints");
                    if (known == null) continue;
                    for (int j = 0; j < known.length(); j++) {
                        if (wanted.contains(known.optString(j))) return true;
                    }
                }
            } catch (Exception ignored) {}
            return false;
        }
    }

    public static void clear(Context context) {
        synchronized (LOCK) {
            File file = file(context);
            if (file.exists()) file.delete();
        }
    }

    private static JSONArray readInternal(Context context) throws Exception {
        File file = file(context);
        if (!file.exists()) return new JSONArray();
        try (FileInputStream in = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CryptoStore.decrypt(in, out);
            return new JSONArray(out.toString(StandardCharsets.UTF_8.name()));
        }
    }

    private static void writeInternal(Context context, JSONArray history) throws Exception {
        File target = file(context);
        File temporary = new File(target.getParentFile(), FILE_NAME + ".tmp");
        byte[] bytes = history.toString().getBytes(StandardCharsets.UTF_8);
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes); FileOutputStream out = new FileOutputStream(temporary)) {
            CryptoStore.encrypt(in, out);
        }
        if (target.exists() && !target.delete()) throw new IllegalStateException("古い履歴を更新できません");
        if (!temporary.renameTo(target)) {
            temporary.delete();
            throw new IllegalStateException("履歴を保存できません");
        }
    }

    private static JSONArray prune(JSONArray source) {
        JSONArray out = new JSONArray();
        long cutoff = System.currentTimeMillis() - MAX_AGE_MILLIS;
        int start = Math.max(0, source.length() - MAX_ENTRIES);
        for (int i = start; i < source.length(); i++) {
            JSONObject item = source.optJSONObject(i);
            if (item != null && item.optLong("created_at", 0L) >= cutoff) out.put(item);
        }
        return out;
    }

    private static File file(Context context) {
        return new File(context.getFilesDir(), FILE_NAME);
    }
}
