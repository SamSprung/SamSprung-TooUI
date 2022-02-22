package com.eightbit.samsprung.launcher

import android.app.ActivityOptions
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.eightbit.content.ScaledContext
import com.eightbit.samsprung.R
import com.eightbit.samsprung.SamSprung
import com.eightbit.samsprung.SamSprungOverlay

class LaunchManager(private val overlay: SamSprungOverlay) {

    private fun prepareConfiguration() : Boolean {
        overlay.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        overlay.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND

        val keyguardManager = (overlay.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
        if (keyguardManager.isDeviceLocked) {
            overlay.setTurnScreenOn(true)
            Toast.makeText(overlay, R.string.lock_enabled, Toast.LENGTH_LONG).show()
            return false
        }

        @Suppress("DEPRECATION")
        (overlay.application as SamSprung).isKeyguardLocked =
            keyguardManager.inKeyguardRestrictedInputMode()

        keyguardManager.requestDismissKeyguard(overlay,
            object : KeyguardManager.KeyguardDismissCallback() { })

        return true
    }

    private fun getOrientationManager(extras: Bundle) {
        val context = ScaledContext.cover(overlay.applicationContext)
        val orientationChanger = LinearLayout(context)
        val orientationLayout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSPARENT
        )
        orientationLayout.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val windowManager = context.getSystemService(
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
        if (!prepareConfiguration()) return

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
        if (!prepareConfiguration()) return

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
}