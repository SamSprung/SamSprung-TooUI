package com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget

import android.app.ActivityOptions
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.content.Intent.ACTION_SCREEN_OFF
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener
import android.net.Uri
import android.widget.RemoteViews
import com.sec.android.app.shealth.R
import com.sec.android.app.shealth.StepBroadcastReceiver
import com.sec.android.app.shealth.StepWidgetService


class StepCoverAppWidget: AppWidgetProvider() {

    private val onClickTag = "OnClickTag"
    private var mDisplayListener : DisplayListener? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            // refresh all your widgets
            val mgr = AppWidgetManager.getInstance(context)
            mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(
                ComponentName(context, StepCoverAppWidget::class.java)), R.id.widgetListView)
        }
        if (intent.action.equals(onClickTag)) {
            val launchPackage = intent.getStringExtra("launchPackage")
            val launchActivity = intent.getStringExtra("launchActivity")

            val coverIntent = Intent(Intent.ACTION_MAIN)
            coverIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            coverIntent.component = ComponentName(launchPackage!!, launchActivity!!)
            val options = ActivityOptions.makeBasic().setLaunchDisplayId(1)
            try {
                val applicationInfo : ApplicationInfo = context.packageManager.getApplicationInfo (
                    launchPackage, PackageManager.GET_META_DATA
                )
                applicationInfo.metaData.putString(
                    "com.samsung.android.activity.showWhenLocked", "true"
                )
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            coverIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(coverIntent, options.toBundle())

            mDisplayListener = object : DisplayListener {
                override fun onDisplayAdded(display: Int) {}
                override fun onDisplayChanged(display: Int) {
                    val displayIntent = Intent(Intent.ACTION_MAIN)
                    displayIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                    displayIntent.component = ComponentName(launchPackage, launchActivity)
                    val launchDisplay = ActivityOptions.makeBasic().setLaunchDisplayId(display)
                    displayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(displayIntent, launchDisplay.toBundle())
                }

                override fun onDisplayRemoved(display: Int) {}
            }

            val manager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            manager.registerDisplayListener(mDisplayListener, null)

            val mReceiver: BroadcastReceiver = StepBroadcastReceiver(
                mDisplayListener, ComponentName(launchPackage, launchActivity)
            )
            context.applicationContext?.registerReceiver(
                mReceiver, IntentFilter(ACTION_SCREEN_OFF))
        }
        super.onReceive(context, intent);
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(
                context.packageName,
                R.layout.step_widget_view
            )
            val intent = Intent(context, StepWidgetService::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME));
            views.setRemoteAdapter(R.id.widgetListView, intent)

            val itemIntent = Intent(context, StepCoverAppWidget::class.java)
            itemIntent.action = onClickTag
            itemIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            val itemPendingIntent = PendingIntent.getBroadcast(
                context, 0, itemIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setPendingIntentTemplate(R.id.widgetListView, itemPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
