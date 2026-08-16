package net.automationse.memobrainshare;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public final class PendingJobStore {
    public static final long MAX_RETENTION_MILLIS = 24L * 60L * 60L * 1000L;
    private static final String JOB_FILE = "job.json.enc";

    private PendingJobStore() {}

    public static String create(Context context, String query, List<Uri> uris, String fallbackMime) throws Exception {
        String jobId = UUID.randomUUID().toString();
        File jobDir = jobDir(context, jobId);
        if (!jobDir.mkdirs() && !jobDir.isDirectory()) throw new IllegalStateException("保存用フォルダを作成できませんでした");

        try {
            JSONArray files = new JSONArray();
            ContentResolver resolver = context.getContentResolver();
            for (int i = 0; i < uris.size(); i++) {
                Uri uri = uris.get(i);
                String displayName = displayName(context, uri);
                String mime = resolver.getType(uri);
                if (mime == null || mime.trim().isEmpty()) mime = fallbackMime;
                if (mime == null || mime.trim().isEmpty()) mime = "application/octet-stream";

                String encryptedName = "payload_" + (i + 1) + ".enc";
                File target = new File(jobDir, encryptedName);
                try (InputStream in = resolver.openInputStream(uri); FileOutputStream out = new FileOutputStream(target)) {
                    if (in == null) throw new IllegalStateException("共有ファイルを開けません");
                    CryptoStore.encrypt(in, out);
                }

                JSONObject f = new JSONObject();
                f.put("encrypted_name", encryptedName);
                f.put("name", displayName);
                f.put("mime", mime);
                files.put(f);
            }

            JSONObject root = new JSONObject();
            root.put("job_id", jobId);
            root.put("query", query);
            root.put("files", files);
            root.put("created_at", System.currentTimeMillis());

            byte[] jsonBytes = root.toString().getBytes(StandardCharsets.UTF_8);
            try (ByteArrayInputStream in = new ByteArrayInputStream(jsonBytes); FileOutputStream out = new FileOutputStream(new File(jobDir, JOB_FILE))) {
                CryptoStore.encrypt(in, out);
            }
            return jobId;
        } catch (Exception e) {
            delete(context, jobId);
            throw e;
        }
    }

    public static JSONObject read(Context context, String jobId) throws Exception {
        File json = new File(jobDir(context, jobId), JOB_FILE);
        if (!json.exists()) throw new IllegalStateException("保存ジョブが見つかりません");
        try (FileInputStream in = new FileInputStream(json); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CryptoStore.decrypt(in, out);
            return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
        }
    }

    public static void decryptPayload(Context context, String jobId, String encryptedName, File output) throws Exception {
        File input = new File(jobDir(context, jobId), encryptedName);
        if (!input.exists()) throw new IllegalStateException("共有ファイルが見つかりません");
        File parent = output.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory()) throw new IllegalStateException("一時作業フォルダを作成できませんでした");
        try (FileInputStream in = new FileInputStream(input); FileOutputStream out = new FileOutputStream(output)) {
            CryptoStore.decrypt(in, out);
        }
    }

    public static File jobDir(Context context, String jobId) {
        return new File(new File(context.getFilesDir(), "pending_jobs"), jobId);
    }

    public static boolean isExpired(JSONObject job) {
        long createdAt = job.optLong("created_at", 0L);
        return createdAt <= 0L || System.currentTimeMillis() - createdAt >= MAX_RETENTION_MILLIS;
    }

    public static void cleanupExpired(Context context) {
        File root = new File(context.getFilesDir(), "pending_jobs");
        File[] jobs = root.listFiles();
        if (jobs == null) return;
        long now = System.currentTimeMillis();
        for (File dir : jobs) {
            if (!dir.isDirectory()) continue;
            try {
                JSONObject job = read(context, dir.getName());
                if (isExpired(job)) deleteRecursively(dir);
            } catch (Exception e) {
                if (now - dir.lastModified() >= MAX_RETENTION_MILLIS) deleteRecursively(dir);
            }
        }
    }

    public static void delete(Context context, String jobId) { deleteRecursively(jobDir(context, jobId)); }
    public static void deleteAll(Context context) { deleteRecursively(new File(context.getFilesDir(), "pending_jobs")); }

    public static void deleteRecursively(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File child : children) deleteRecursively(child);
        }
        f.delete();
    }

    private static String displayName(Context context, Uri uri) {
        try (Cursor c = context.getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int ix = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (ix >= 0) {
                    String v = c.getString(ix);
                    if (v != null && !v.trim().isEmpty()) return v;
                }
            }
        } catch (Exception ignored) {}
        String last = uri.getLastPathSegment();
        return last == null || last.trim().isEmpty() ? "shared-file" : last;
    }
}
