package com.sec.android.app.shealth

import android.app.ActivityOptions
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener

class StepBroadcastReceiver : BroadcastReceiver {
    private var mDisplayListener : DisplayListener? = null
    private var mKeyguardLock : KeyguardManager.KeyguardLock? = null
    private var componentName : ComponentName? = null

    constructor() {}
    constructor(
        mKeyguardLock : KeyguardManager.KeyguardLock?,
        mDisplayListener: DisplayListener?,
        componentName: ComponentName
    ) {
        this.mDisplayListener = mDisplayListener
        this.mKeyguardLock = mKeyguardLock
        this.componentName = componentName
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            mKeyguardLock?.reenableKeyguard()

            val manager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            manager.unregisterDisplayListener(mDisplayListener)

            val screenIntent = Intent(Intent.ACTION_MAIN)
            screenIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            screenIntent.component = componentName
            val options = ActivityOptions.makeBasic().setLaunchDisplayId(0)
            screenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(screenIntent, options.toBundle())
        }
    }
}