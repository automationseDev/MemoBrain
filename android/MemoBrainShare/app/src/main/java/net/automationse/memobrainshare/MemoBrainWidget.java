package net.automationse.memobrainshare;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

public class MemoBrainWidget extends AppWidgetProvider {
    public static final String EXTRA_ACTION = "memobrain_widget_action";
    public static final String ACTION_MEMO = "memo";
    public static final String ACTION_FILE = "file";
    public static final String ACTION_CHAT = "chat";
    public static final String ACTION_HISTORY = "history";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) update(context, manager, id);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, MemoBrainWidget.class);
        int[] ids = manager.getAppWidgetIds(provider);
        for (int id : ids) update(context, manager, id);
    }

    private static void update(Context context, AppWidgetManager manager, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_memobrain);
        views.setTextViewText(R.id.widget_title, BuildConfig.DEBUG ? "MemoBrain  •  DEV" : "MemoBrain");
        views.setTextViewText(R.id.widget_status, statusText(context));
        views.setOnClickPendingIntent(R.id.widget_memo, action(context, ACTION_MEMO, 7101));
        views.setOnClickPendingIntent(R.id.widget_file, action(context, ACTION_FILE, 7102));
        views.setOnClickPendingIntent(R.id.widget_chat, action(context, ACTION_CHAT, 7103));
        views.setOnClickPendingIntent(R.id.widget_history, action(context, ACTION_HISTORY, 7104));
        manager.updateAppWidget(widgetId, views);
    }

    private static PendingIntent action(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXTRA_ACTION, action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static String statusText(Context context) {
        JSONArray history = HistoryStore.list(context);
        int pending = 0;
        int failed = 0;
        String latest = "まだ送信履歴がありません";
        for (int i = 0; i < history.length(); i++) {
            JSONObject item = history.optJSONObject(i);
            if (item == null) continue;
            String status = item.optString("status");
            if (HistoryStore.QUEUED.equals(status) || HistoryStore.SENDING.equals(status)) pending++;
            if (HistoryStore.FAILED.equals(status)) failed++;
            if (i == 0) latest = label(status);
        }
        if (failed > 0) return "要確認 " + failed + "件  •  タップして履歴を表示";
        if (pending > 0) return "送信中 " + pending + "件  •  " + latest;
        return "最新: " + latest;
    }

    private static String label(String status) {
        if (HistoryStore.QUEUED.equals(status)) return "送信待ち";
        if (HistoryStore.SENDING.equals(status)) return "送信中";
        if (HistoryStore.SUCCESS.equals(status)) return "保存完了";
        if (HistoryStore.FAILED.equals(status)) return "送信失敗";
        if (HistoryStore.EXPIRED.equals(status)) return "期限切れ";
        return "状態不明";
    }
}
