package com.eightbit.samsprung.launcher

import android.app.ActivityOptions
import android.app.PendingIntent
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
import androidx.appcompat.app.AppCompatActivity
import com.eightbit.content.ScaledContext
import com.eightbit.samsprung.SamSprung
import com.eightbit.samsprung.SamSprungOverlay

class LauncherManager(private val overlay: SamSprungOverlay) {

    private fun getOrientationManager(extras: Bundle) {
        val context = ScaledContext.cover(overlay.applicationContext)
        val orientationChanger = LinearLayout(context)
        val orientationLayout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSPARENT
        )
        orientationLayout.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val windowManager = context.getSystemService(
            Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(orientationChanger, orientationLayout)
        orientationChanger.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            windowManager.removeViewImmediate(orientationChanger)
            overlay.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            overlay.onStopOverlay()
            overlay.startForegroundService(Intent(overlay.applicationContext,
                AppDisplayListener::class.java).putExtras(extras))
        }, 20)
    }

    fun launchApplicationComponent(resolveInfo: ResolveInfo) {
        overlay.setKeyguardListener(object: SamSprungOverlay.KeyguardListener {
            override fun onKeyguardCheck(unlocked: Boolean) {
                if (!unlocked) return
                overlay.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                overlay.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND
                (overlay.getSystemService(AppCompatActivity
                    .LAUNCHER_APPS_SERVICE) as LauncherApps).startMainActivity(
                    ComponentName(
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name
                    ),
                    Process.myUserHandle(),
                    overlay.windowManager.currentWindowMetrics.bounds,
                    ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle()
                )

                val extras = Bundle()
                extras.putString("launchPackage", resolveInfo.activityInfo.packageName)
                extras.putString("launchActivity", resolveInfo.activityInfo.name)

                getOrientationManager(extras)
            }
        }, true)
    }

    fun launchDefaultActivity(appInfo: ApplicationInfo) {
        overlay.setKeyguardListener(object: SamSprungOverlay.KeyguardListener {
            override fun onKeyguardCheck(unlocked: Boolean) {
                if (!unlocked) return
                overlay.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                overlay.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND
                (overlay.getSystemService(AppCompatActivity.LAUNCHER_APPS_SERVICE) as LauncherApps)
                    .startMainActivity(overlay.packageManager
                        .getLaunchIntentForPackage(appInfo.packageName)?.component,
                        Process.myUserHandle(),
                        overlay.windowManager.currentWindowMetrics.bounds,
                        ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle()
                    )

                val extras = Bundle()
                extras.putString("launchPackage", appInfo.packageName)

                getOrientationManager(extras)
            }
        }, true)
    }

    fun launchPendingActivity(pendingIntent: PendingIntent) {
        overlay.setKeyguardListener(object: SamSprungOverlay.KeyguardListener {
            override fun onKeyguardCheck(unlocked: Boolean) {
                if (!unlocked) return
                overlay.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                overlay.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND

                pendingIntent.send(
                    ScaledContext.cover(overlay), SamSprung.request_code, Intent()
                )

                val extras = Bundle()
                extras.putString("launchPackage", pendingIntent.intentSender.creatorPackage)

                getOrientationManager(extras)
            }
        }, true)
    }
}