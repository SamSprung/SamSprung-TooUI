package com.sec.android.app.shealth

import android.app.ActivityOptions
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget.StepCoverAppWidget


class OffBroadcastReceiver : BroadcastReceiver {

    private var componentName : ComponentName? = null

    constructor()
    constructor(componentName: ComponentName) {
        this.componentName = componentName
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_FULLY_REMOVED) {
            updateAppWidget(context)
        }
        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
            if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                updateAppWidget(context)
            }
        }
        if (intent.action == Intent.ACTION_SCREEN_OFF && componentName != null) {
            val serviceIntent = Intent(context, DisplayListenerService::class.java)
            serviceIntent.action = "samsprung.launcher.STOP"
            context.startService(serviceIntent)

            val screenIntent = Intent(Intent.ACTION_MAIN)
            screenIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            screenIntent.component = componentName
            val options = ActivityOptions.makeBasic().setLaunchDisplayId(0)
            screenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            screenIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            screenIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            screenIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            context.startActivity(screenIntent, options.toBundle())

            context.applicationContext.unregisterReceiver(this)
        }
    }

    private fun updateAppWidget(context: Context) {
        val updateIntent = Intent(context, StepCoverAppWidget::class.java)
        updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
            AppWidgetManager.getInstance(context.applicationContext).getAppWidgetIds(
            ComponentName(context.applicationContext, StepCoverAppWidget::class.java))
        )
        context.sendBroadcast(updateIntent)
    }
}