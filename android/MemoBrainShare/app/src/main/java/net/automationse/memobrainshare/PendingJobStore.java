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
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PendingJobStore {
    public static final long MAX_RETENTION_MILLIS = 24L * 60L * 60L * 1000L;
    private static final String JOB_FILE = "job.json.enc";
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<>\\\"]+", Pattern.CASE_INSENSITIVE);

    private PendingJobStore() {}

    public static String create(Context context, String query, List<Uri> uris, String fallbackMime, String profileId) throws Exception {
        String jobId = UUID.randomUUID().toString();
        File jobDir = jobDir(context, jobId);
        if (!jobDir.mkdirs() && !jobDir.isDirectory()) throw new IllegalStateException("保存用フォルダを作成できませんでした");

        try {
            JSONArray files = new JSONArray();
            JSONArray fingerprints = urlFingerprints(query);
            ContentResolver resolver = context.getContentResolver();
            for (int i = 0; i < uris.size(); i++) {
                Uri uri = uris.get(i);
                String displayName = displayName(context, uri);
                String mime = resolver.getType(uri);
                if (mime == null || mime.trim().isEmpty()) mime = fallbackMime;
                if (mime == null || mime.trim().isEmpty()) mime = "application/octet-stream";

                String encryptedName = "payload_" + (i + 1) + ".enc";
                File target = new File(jobDir, encryptedName);
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                try (InputStream raw = resolver.openInputStream(uri); FileOutputStream out = new FileOutputStream(target)) {
                    if (raw == null) throw new IllegalStateException("共有ファイルを開けません");
                    try (DigestInputStream in = new DigestInputStream(raw, digest)) {
                        CryptoStore.encrypt(in, out);
                    }
                }
                fingerprints.put("file:" + hex(digest.digest()));

                JSONObject f = new JSONObject();
                f.put("encrypted_name", encryptedName);
                f.put("name", displayName);
                f.put("mime", mime);
                files.put(f);
            }

            if (HistoryStore.hasDuplicate(context, fingerprints)) throw new DuplicateException();

            JSONObject root = new JSONObject();
            root.put("job_id", jobId);
            root.put("query", query);
            root.put("profile_id", profileId == null ? "" : profileId);
            root.put("files", files);
            root.put("fingerprints", fingerprints);
            root.put("created_at", System.currentTimeMillis());

            byte[] jsonBytes = root.toString().getBytes(StandardCharsets.UTF_8);
            try (ByteArrayInputStream in = new ByteArrayInputStream(jsonBytes); FileOutputStream out = new FileOutputStream(new File(jobDir, JOB_FILE))) {
                CryptoStore.encrypt(in, out);
            }
            HistoryStore.addQueued(context, jobId, kind(query), fingerprints);
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

    public static boolean exists(Context context, String jobId) {
        return new File(jobDir(context, jobId), JOB_FILE).isFile();
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
                if (isExpired(job)) {
                    HistoryStore.updateStatus(context, dir.getName(), HistoryStore.EXPIRED);
                    deleteRecursively(dir);
                }
            } catch (Exception e) {
                if (now - dir.lastModified() >= MAX_RETENTION_MILLIS) {
                    HistoryStore.updateStatus(context, dir.getName(), HistoryStore.EXPIRED);
                    deleteRecursively(dir);
                }
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

    private static JSONArray urlFingerprints(String query) throws Exception {
        JSONArray out = new JSONArray();
        Matcher matcher = URL_PATTERN.matcher(query == null ? "" : query);
        while (matcher.find()) {
            String value = matcher.group();
            while (value.endsWith(".") || value.endsWith(",") || value.endsWith(")") || value.endsWith("]")) {
                value = value.substring(0, value.length() - 1);
            }
            String canonical = canonicalUrl(value);
            out.put("url:" + sha256(canonical));
        }
        return out;
    }

    private static String canonicalUrl(String value) {
        try {
            Uri uri = Uri.parse(value);
            Uri.Builder builder = uri.buildUpon().scheme(uri.getScheme().toLowerCase(Locale.ROOT))
                    .authority(uri.getAuthority() == null ? null : uri.getAuthority().toLowerCase(Locale.ROOT))
                    .fragment(null).clearQuery();
            Set<String> names = uri.getQueryParameterNames();
            List<String> sorted = new ArrayList<>(names);
            Collections.sort(sorted);
            for (String name : sorted) {
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.startsWith("utm_") || lower.equals("fbclid") || lower.equals("gclid")) continue;
                List<String> values = new ArrayList<>(uri.getQueryParameters(name));
                Collections.sort(values);
                for (String parameter : values) builder.appendQueryParameter(name, parameter);
            }
            String canonical = builder.build().toString();
            int queryStart = canonical.indexOf('?');
            String pathPart = queryStart >= 0 ? canonical.substring(0, queryStart) : canonical;
            String queryPart = queryStart >= 0 ? canonical.substring(queryStart) : "";
            if (pathPart.endsWith("/") && pathPart.indexOf('/', pathPart.indexOf("//") + 2) >= 0) {
                pathPart = pathPart.substring(0, pathPart.length() - 1);
            }
            return pathPart + queryPart;
        } catch (Exception ignored) {
            return value.trim();
        }
    }

    private static String kind(String query) {
        if (query == null || !query.startsWith("[MB:")) return "TEXT";
        int end = query.indexOf(']');
        return end > 4 ? query.substring(4, end) : "TEXT";
    }

    private static String sha256(String value) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) out.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        return out.toString();
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

    public static final class DuplicateException extends Exception {
        public DuplicateException() { super("同じURLまたはファイルは既に送信履歴にあります"); }
    }
}
