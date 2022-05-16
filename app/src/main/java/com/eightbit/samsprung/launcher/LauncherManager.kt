package com.eightbit.samsprung.launcher

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.ResolveInfo
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.appcompat.app.AppCompatActivity
import com.eightbit.app.CoverOptions
import com.eightbit.content.ScaledContext
import com.eightbit.samsprung.SamSprung
import com.eightbit.samsprung.SamSprungOverlay

class LauncherManager(private val overlay: SamSprungOverlay) {

    val displayContext = ScaledContext.cover(ScaledContext.internal(overlay, 1.5f))
    val launcher = displayContext.getSystemService(
        AppCompatActivity.LAUNCHER_APPS_SERVICE
    ) as LauncherApps

    private fun postOrientationHandler(extras: Bundle) {
        val orientationLock = OrientationManager(overlay)
        orientationLock.addOrientationLayout(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        Handler(Looper.getMainLooper()).postDelayed({
            orientationLock.removeOrientationLayout()
            overlay.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            overlay.onStopOverlay()
            val context = ScaledContext.cover(ScaledContext.internal(overlay, 1.5f))
            context.startForegroundService(
                Intent(context, AppDisplayListener::class.java).putExtras(extras)
            )
        }, 20)
    }

    private fun getLaunchBounds() : Rect {
        return overlay.windowManager.currentWindowMetrics.bounds
    }

    fun launchResolveInfo(resolveInfo: ResolveInfo) {
        overlay.setKeyguardListener(object: SamSprungOverlay.KeyguardListener {
            override fun onKeyguardCheck(unlocked: Boolean) {
                if (!unlocked) return
                overlay.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                overlay.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND

                val intent = overlay.packageManager
                    .getLaunchIntentForPackage(resolveInfo.activityInfo.packageName)
                launcher.startMainActivity(
                    ComponentName(
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name
                    ),
                    Process.myUserHandle(),
                    getLaunchBounds(),
                    CoverOptions(null).getAnimatedOptions(
                        1, overlay.getCoordinator(), intent
                    ).toBundle()
                )

                val extras = Bundle()
                extras.putString("launchPackage", resolveInfo.activityInfo.packageName)
                extras.putString("launchActivity", resolveInfo.activityInfo.name)
                postOrientationHandler(extras)
            }
        })
    }

    fun launchApplicationInfo(appInfo: ApplicationInfo) {
        overlay.setKeyguardListener(object: SamSprungOverlay.KeyguardListener {
            override fun onKeyguardCheck(unlocked: Boolean) {
                if (!unlocked) return
                overlay.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                overlay.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND

                val intent = overlay.packageManager.getLaunchIntentForPackage(appInfo.packageName)
                launcher.startMainActivity(
                    intent?.component,
                    Process.myUserHandle(),
                    getLaunchBounds(),
                    CoverOptions(null).getAnimatedOptions(
                        1, overlay.getCoordinator(), intent
                    ).toBundle()
                )

                val extras = Bundle()
                extras.putString("launchPackage", appInfo.packageName)
                postOrientationHandler(extras)
            }
        })
    }

    fun launchPendingIntent(pendingIntent: PendingIntent) {
        overlay.setKeyguardListener(object: SamSprungOverlay.KeyguardListener {
            override fun onKeyguardCheck(unlocked: Boolean) {
                if (!unlocked) return
                overlay.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                overlay.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND

                val extras = Bundle()
//                val parcel = Parcel.obtain()
//                PendingIntent.writePendingIntentOrNullToParcel(pendingIntent, parcel)
//                extras.putByteArray("pendingIntent", parcel.marshall())
//                parcel.recycle()

                pendingIntent.send(
                    displayContext, SamSprung.request_code,
                    Intent().setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_TASK_ON_HOME or
                            Intent.FLAG_ACTIVITY_FORWARD_RESULT
                    ), null, null, null,
                    CoverOptions(null).getActivityOptions(1).toBundle()
                )

                if (pendingIntent.intentSender.creatorPackage == "android") {
                    extras.putString("launchPackage", "com.android.settings")
                } else {
                    extras.putString("launchPackage", pendingIntent.intentSender.creatorPackage)
                }
                postOrientationHandler(extras)
            }
        })
    }
}