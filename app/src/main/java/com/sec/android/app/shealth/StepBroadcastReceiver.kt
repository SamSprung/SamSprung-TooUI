package com.sec.android.app.shealth

import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener

class StepBroadcastReceiver : BroadcastReceiver {
    private var mDisplayListener: DisplayListener? = null

    constructor() {}
    constructor(mDisplayListener: DisplayListener?) {
        this.mDisplayListener = mDisplayListener
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            val manager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            manager.unregisterDisplayListener(mDisplayListener)
            val sharedPref = context.getSharedPreferences(
                "com.zflip.launcher.PREFS", Context.MODE_PRIVATE
            )
            val launchPackage = sharedPref.getString("launchPackage", null)
            val launchActivity = sharedPref.getString("launchActivity", null)
            val screenIntent = Intent(Intent.ACTION_MAIN)
            screenIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            screenIntent.component = ComponentName(launchPackage!!, launchActivity!!)
            val options = ActivityOptions.makeBasic().setLaunchDisplayId(0)
            screenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(screenIntent, options.toBundle())
        }
    }
}