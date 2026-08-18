package net.automationse.memobrainshare;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public final class NotificationHelper {
    private static final String CHANNEL_ID = "memobrain_saves";
    private static final String CHANNEL_NAME = "MemoBrain 保存結果";

    private NotificationHelper() {}

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("MemoBrainのバックグラウンド保存結果を通知します");
                channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                nm.createNotificationChannel(channel);
            }
        }
    }

    public static void success(Context context, boolean hasAnswer) {
        notify(context,
                1001 + (int) (System.currentTimeMillis() % 100000),
                "MemoBrainに保存しました",
                hasAnswer ? "Difyからの返信を送信履歴で確認できます" : "バックグラウンド保存が完了しました");
    }

    public static void failure(Context context, String safeReason) {
        String reason = safeReason == null || safeReason.trim().isEmpty()
                ? "接続設定またはDify側を確認してください"
                : safeReason.trim();
        notify(context,
                2001 + (int) (System.currentTimeMillis() % 100000),
                "MemoBrainの保存に失敗しました",
                reason);
    }

    private static void notify(Context context, int id, String title, String text) {
        ensureChannel(context);

        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent open = new Intent(context, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                context,
                id,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_memobrain)
                .setContentTitle(title)
                .setContentText(text)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(id, n);
    }
}
