package com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget

import android.app.ActivityOptions
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.sec.android.app.shealth.R


class StepCoverAppWidget: AppWidgetProvider() {

    private val onClickTag = "OnClickTag"

    override fun onReceive(context: Context?, intent: Intent) {
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            // refresh all your widgets
            val mgr = AppWidgetManager.getInstance(context)
            val cn = context?.let { ComponentName(it, StepCoverAppWidget::class.java) }
            mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.widgetListView)
        }
        if (intent.action.equals(onClickTag)) {
            val launchPackage = intent.getStringExtra("launchPackage")
            val launchActivity = intent.getStringExtra("launchActivity")
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            intent.component = ComponentName(launchPackage!!, launchActivity!!)
            val options = ActivityOptions.makeBasic().setLaunchDisplayId(1)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context?.startActivity(intent, options.toBundle())
        }
        super.onReceive(context, intent);
    }

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        appWidgetIds?.forEach { appWidgetId ->
            val views = RemoteViews(
                context?.packageName,
                R.layout.step_widget_view
            )
            val intent = Intent(context, StepWidgetService::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME));
            views.setRemoteAdapter(R.id.widgetListView, intent)

            val itemIntent = Intent(context, StepCoverAppWidget::class.java)
            itemIntent.action = onClickTag
            itemIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            val toastPendingIntent = PendingIntent.getBroadcast(
                context, 0, itemIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setPendingIntentTemplate(R.id.widgetListView, toastPendingIntent)

            appWidgetManager?.updateAppWidget(appWidgetId, views)

        }
    }
}