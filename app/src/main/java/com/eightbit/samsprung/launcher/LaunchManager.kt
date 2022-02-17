package com.eightbit.samsprung.launcher

import android.app.Activity
import android.app.ActivityOptions
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.ResolveInfo
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.eightbit.samsprung.SamSprung
import com.eightbit.samsprung.SamSprungOverlay

class LaunchManager(private val overlay: SamSprungOverlay) {

    private fun prepareConfiguration() {
        overlay.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        overlay.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND

        val mKeyguardManager = (overlay.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
        @Suppress("DEPRECATION")
        (overlay.application as SamSprung).isKeyguardLocked = mKeyguardManager.inKeyguardRestrictedInputMode()

        if ((overlay.application as SamSprung).isKeyguardLocked) {
            @Suppress("DEPRECATION")
            mKeyguardManager.newKeyguardLock("cover_lock").disableKeyguard()
        }

        mKeyguardManager.requestDismissKeyguard(overlay,
            object : KeyguardManager.KeyguardDismissCallback() { })
    }

    private fun getOrientationManager(extras: Bundle) {
        val orientationChanger = LinearLayout((overlay.application as SamSprung).getScaledContext())
        val orientationLayout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSPARENT
        )
        orientationLayout.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val windowManager = (overlay.application as SamSprung).getScaledContext()?.getSystemService(
            Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(orientationChanger, orientationLayout)
        orientationChanger.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            overlay.runOnUiThread {
                windowManager.removeViewImmediate(orientationChanger)
                overlay.onDismiss()
                overlay.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                overlay.startForegroundService(
                    Intent(overlay, AppDisplayListener::class.java).putExtras(extras))
            }
        }, 50)
    }

    fun launchApplicationComponent(resolveInfo: ResolveInfo) {
        prepareConfiguration()

        (overlay.getSystemService(AppCompatActivity
            .LAUNCHER_APPS_SERVICE) as LauncherApps).startMainActivity(
            ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name),
            Process.myUserHandle(),
            overlay.windowManager.currentWindowMetrics.bounds,
            ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle()
        )

        val extras = Bundle()
        extras.putString("launchPackage", resolveInfo.activityInfo.packageName)
        extras.putString("launchActivity", resolveInfo.activityInfo.name)

        getOrientationManager(extras)
    }

    fun launchDefaultActivity(appInfo: ApplicationInfo) {
        prepareConfiguration()

        (overlay.getSystemService(AppCompatActivity.LAUNCHER_APPS_SERVICE) as LauncherApps)
            .startMainActivity(
                overlay.packageManager.getLaunchIntentForPackage(appInfo.packageName)?.component,
                Process.myUserHandle(),
                overlay.windowManager.currentWindowMetrics.bounds,
                ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle()
            )

        val extras = Bundle()
        extras.putString("launchPackage", appInfo.packageName)

        getOrientationManager(extras)
    }

    fun launchIntentSender(intentSender: IntentSender) {
        prepareConfiguration()

        overlay.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        overlay.startIntentSender(intentSender, null, 0, 0, 0,
            ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle())

        Handler(Looper.getMainLooper()).postDelayed({
            overlay.runOnUiThread {
                val extras = Bundle()
                extras.putString("launchPackage", intentSender.creatorPackage)

                overlay.startForegroundService(
                    Intent(overlay,
                        AppDisplayListener::class.java).putExtras(extras))

                overlay.onDismiss()
            }
        }, 100)
    }
}