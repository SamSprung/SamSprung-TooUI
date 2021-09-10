package com.sec.android.app.shealth

import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class ScreenBroadcastReceiver : BroadcastReceiver {

    private var componentName : ComponentName? = null

    @Suppress("UNUSED") constructor()
    constructor(
        componentName: ComponentName
    ) {
        this.componentName = componentName
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            val serviceIntent = Intent(context, DisplayListenerService::class.java)
            serviceIntent.action = "samsprung.launcher.STOP"
            // context.startService(serviceIntent)
            context.stopService(serviceIntent)

            val screenIntent = Intent(Intent.ACTION_MAIN)
            screenIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            screenIntent.component = componentName
            val options = ActivityOptions.makeBasic().setLaunchDisplayId(0)
            screenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(screenIntent, options.toBundle())
        }
    }
}