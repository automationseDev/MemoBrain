package net.automationse.memobrainshare;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** Four-button launcher widget for native Knowledge tools. */
public final class KnowledgeWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_knowledge);
            bind(context, views, R.id.widget_search, KnowledgeActivity.MODE_SEARCH, 10);
            bind(context, views, R.id.widget_list, KnowledgeActivity.MODE_LIST, 11);
            bind(context, views, R.id.widget_todo, KnowledgeActivity.MODE_TODO, 12);
            bind(context, views, R.id.widget_read_later, KnowledgeActivity.MODE_READ_LATER, 13);
            manager.updateAppWidget(id, views);
        }
    }

    private static void bind(Context context, RemoteViews views, int viewId, String mode, int requestCode) {
        Intent intent = new Intent(context, KnowledgeActivity.class).putExtra(KnowledgeActivity.EXTRA_MODE, mode);
        PendingIntent pending = PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(viewId, pending);
    }
}
