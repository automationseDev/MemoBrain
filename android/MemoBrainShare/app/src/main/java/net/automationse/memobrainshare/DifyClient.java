package net.automationse.memobrainshare;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DifyClient {
    private final String base;
    private final String key;
    private final String user = "memobrain-android";

    public DifyClient(String base, String key) {
        String normalized = base == null ? "" : base.trim().replaceAll("/+$", "");
        if (!normalized.toLowerCase().startsWith("https://")) {
            throw new IllegalArgumentException("Dify API Base はHTTPSが必須です");
        }
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Dify API Key が設定されていません");
        }
        this.base = normalized;
        this.key = key.trim();
    }

    private String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) > 0) out.write(b, 0, n);
        return out.toString(StandardCharsets.UTF_8.name());
    }

    private HttpURLConnection open(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(30_000);
        c.setReadTimeout(300_000);
        c.setInstanceFollowRedirects(false);
        return c;
    }

    public String uploadFile(File file, String fileName, String mime) throws Exception {
        if (!file.exists()) throw new IOException("共有ファイルが見つかりません");

        String boundary = "----MemoBrain" + System.currentTimeMillis();
        HttpURLConnection c = open(base + "/files/upload");
        c.setDoOutput(true);
        c.setRequestMethod("POST");
        c.setRequestProperty("Authorization", "Bearer " + key);
        c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        String cr = "\r\n";
        try (OutputStream out = c.getOutputStream()) {
            out.write(("--" + boundary + cr
                    + "Content-Disposition: form-data; name=\"user\"" + cr + cr
                    + user + cr).getBytes(StandardCharsets.UTF_8));

            out.write(("--" + boundary + cr
                    + "Content-Disposition: form-data; name=\"file\"; filename=\""
                    + safeHeaderFileName(fileName) + "\"" + cr
                    + "Content-Type: " + mime + cr + cr).getBytes(StandardCharsets.UTF_8));

            try (InputStream in = new FileInputStream(file)) {
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }

            out.write(cr.getBytes(StandardCharsets.UTF_8));
            out.write(("--" + boundary + "--" + cr).getBytes(StandardCharsets.UTF_8));
        }

        int code = c.getResponseCode();
        String body = readAll(code < 400 ? c.getInputStream() : c.getErrorStream());
        if (code >= 300) throw new IOException("Dify Upload HTTP " + code);
        return new JSONObject(body).getString("id");
    }

    public void chat(String query, List<FileRef> files) throws Exception {
        JSONObject root = new JSONObject();
        root.put("inputs", new JSONObject());
        root.put("query", query);
        root.put("response_mode", "blocking");
        root.put("conversation_id", "");
        root.put("user", user);

        JSONArray arr = new JSONArray();
        for (FileRef f : files) {
            JSONObject x = new JSONObject();
            x.put("type", f.type);
            x.put("transfer_method", "local_file");
            x.put("upload_file_id", f.id);
            arr.put(x);
        }
        root.put("files", arr);

        byte[] data = root.toString().getBytes(StandardCharsets.UTF_8);
        HttpURLConnection c = open(base + "/chat-messages");
        c.setDoOutput(true);
        c.setRequestMethod("POST");
        c.setRequestProperty("Authorization", "Bearer " + key);
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        try (OutputStream out = c.getOutputStream()) {
            out.write(data);
        }

        int code = c.getResponseCode();
        readAll(code < 400 ? c.getInputStream() : c.getErrorStream());
        if (code >= 300) throw new IOException("Dify Chat HTTP " + code);
    }

    private String safeHeaderFileName(String value) {
        String s = value == null || value.trim().isEmpty() ? "shared-file" : value.trim();
        return s.replace("\"", "").replace("\r", "").replace("\n", "");
    }

    public static class FileRef {
        public final String id;
        public final String type;

        public FileRef(String id, String type) {
            this.id = id;
            this.type = type;
        }
    }
}
