package net.automationse.memobrainshare;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MemoSaveWorker extends Worker {
    public static final String KEY_JOB_ID = "job_id";

    public MemoSaveWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String jobId = getInputData().getString(KEY_JOB_ID);
        if (jobId == null || jobId.trim().isEmpty()) return Result.failure();

        File workDir = new File(new File(getApplicationContext().getCacheDir(), "memobrain_work"), jobId);
        PendingJobStore.deleteRecursively(workDir);
        workDir.mkdirs();

        boolean terminal = false;
        try {
            SecurePrefs prefs = new SecurePrefs(getApplicationContext());
            String key = prefs.getKey();
            String base = prefs.getBase();
            if (key.isEmpty() || base.isEmpty()) throw new IllegalStateException("Dify接続設定がありません");

            JSONObject job = PendingJobStore.read(getApplicationContext(), jobId);
            if (PendingJobStore.isExpired(job)) {
                terminal = true;
                NotificationHelper.failure(getApplicationContext(), "送信待ちデータの保存期限が切れました");
                return Result.failure();
            }

            String query = job.optString("query", "");
            JSONArray files = job.optJSONArray("files");
            DifyClient client = new DifyClient(base, key);
            List<DifyClient.FileRef> refs = new ArrayList<>();

            if (files != null) {
                for (int i = 0; i < files.length(); i++) {
                    if (isStopped()) return Result.failure();
                    JSONObject f = files.getJSONObject(i);
                    String encryptedName = f.getString("encrypted_name");
                    String name = f.optString("name", "shared-file");
                    String mime = f.optString("mime", "application/octet-stream");
                    File local = new File(workDir, "input_" + i + extensionOnly(name));
                    PendingJobStore.decryptPayload(getApplicationContext(), jobId, encryptedName, local);

                    try {
                        if (mime.startsWith("video/")) {
                            List<File> frames = sampleVideo(local, workDir);
                            if (frames.isEmpty()) throw new IOException("動画フレームを抽出できませんでした");
                            try {
                                for (File frame : frames) {
                                    String id = client.uploadFile(frame, frame.getName(), "image/jpeg");
                                    refs.add(new DifyClient.FileRef(id, "image"));
                                }
                            } finally {
                                for (File frame : frames) frame.delete();
                            }
                        } else {
                            String id = client.uploadFile(local, name, mime);
                            refs.add(new DifyClient.FileRef(id, difyType(mime)));
                        }
                    } finally {
                        local.delete();
                    }
                }
            }

            client.chat(query, refs);
            terminal = true;
            NotificationHelper.success(getApplicationContext());
            return Result.success();
        } catch (Exception e) {
            if (getRunAttemptCount() < 2 && isRetryable(e)) return Result.retry();
            terminal = true;
            NotificationHelper.failure(getApplicationContext(), safeReason(e));
            return Result.failure();
        } finally {
            PendingJobStore.deleteRecursively(workDir);
            if (terminal) PendingJobStore.delete(getApplicationContext(), jobId);
        }
    }

    private boolean isRetryable(Exception e) {
        return e instanceof IOException || e.getCause() instanceof IOException;
    }

    private String safeReason(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) return "Difyへの送信に失敗しました";
        if (message.startsWith("Dify Upload HTTP ") || message.startsWith("Dify Chat HTTP ")) return message;
        if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            return message.length() > 80 ? message.substring(0, 80) : message;
        }
        return "Difyへの送信に失敗しました";
    }

    private String difyType(String mime) {
        if (mime.startsWith("image/")) return "image";
        if (mime.startsWith("audio/")) return "audio";
        if (mime.startsWith("video/")) return "video";
        return "document";
    }

    private String extensionOnly(String name) {
        if (name == null) return ".tmp";
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot >= name.length() - 1) return ".tmp";
        String ext = name.substring(dot).toLowerCase(Locale.ROOT);
        if (!ext.matches("\\.[a-z0-9]{1,10}")) return ".tmp";
        return ext;
    }

    private List<File> sampleVideo(File video, File workDir) throws Exception {
        ArrayList<File> out = new ArrayList<>();
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(video.getAbsolutePath());
            long durationMs = 0;
            try {
                String raw = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (raw != null) durationMs = Long.parseLong(raw);
            } catch (Exception ignored) {}

            for (int i = 0; i < 6; i++) {
                long us = durationMs > 0 ? (durationMs * 1000L * i / 5L) : i * 1_000_000L;
                Bitmap b = r.getFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if (b != null) {
                    File frame = new File(workDir, "video_frame_" + i + ".jpg");
                    try (FileOutputStream o = new FileOutputStream(frame)) {
                        b.compress(Bitmap.CompressFormat.JPEG, 85, o);
                    } finally {
                        b.recycle();
                    }
                    out.add(frame);
                }
            }
        } finally {
            r.release();
        }
        return out;
    }
}
