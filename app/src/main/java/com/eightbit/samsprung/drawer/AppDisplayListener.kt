/*
 * ====================================================================
 * Copyright (c) 2021-2023 AbandonedCart.  All rights reserved.
 *
 * See https://github.com/SamSprung/.github/blob/main/LICENSE#L5
 * ====================================================================
 *
 * The license and distribution terms for any publicly available version or
 * derivative of this code cannot be changed.  i.e. this code cannot simply be
 * copied and put under another distribution license
 * [including the GNU Public License.] Content not subject to these terms is
 * subject to to the terms and conditions of the Apache License, Version 2.0.
 */

package com.eightbit.samsprung.drawer

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.*
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.eightbit.app.CoverOptions
import com.eightbit.content.ScaledContext
import com.eightbit.samsprung.*
import com.eightbit.view.OnSwipeTouchListener
import com.google.android.material.bottomsheet.BottomSheetBehavior

class AppDisplayListener : Service() {

    private lateinit var prefs: SharedPreferences
    private var offReceiver: BroadcastReceiver? = null
    private var mDisplayListener: DisplayManager.DisplayListener? = null
    private lateinit var floatView: View
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private lateinit var vibrator: Vibrator
    private val effectClick = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        var launchPackage: String? = intent.getStringExtra("launchPackage")
        val launchActivity: String? = if (intent.hasExtra("launchActivity"))
            intent.getStringExtra("launchActivity") else null
        var componentName: ComponentName? = if (null == launchActivity && null != launchPackage)
            packageManager.getLaunchIntentForPackage(launchPackage)?.component else null

//        val pendingIntent: ByteArray? = intent.getByteArrayExtra("pendingIntent")
//        if (null != pendingIntent) {
//            val parcel = Parcel.obtain()
//            parcel.unmarshall(pendingIntent, 0, pendingIntent.size)
//            parcel.setDataPosition(0)
//            parcel.recycle()
//        }

        prefs = getSharedPreferences(SamSprung.prefsValue, MODE_PRIVATE)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") (getSystemService(VIBRATOR_SERVICE) as Vibrator)
        }

        offReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_OFF) {
                    if (null != componentName)
                        resetRecentActivities(componentName, 0)
                    else if (null != launchPackage)
                        resetRecentActivities(launchPackage, launchActivity, 0)
                    componentName = null
                    launchPackage = null
                    onDismissOverlay()
                }
            }
        }
        IntentFilter(Intent.ACTION_SCREEN_OFF).also {
            registerReceiver(offReceiver, it)
        }

        (getSystemService(DISPLAY_SERVICE) as DisplayManager).run {
            mDisplayListener = object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(display: Int) {}
                override fun onDisplayChanged(display: Int) {
                    if (display == 0 && getDisplay(0).state == Display.STATE_ON) {
                        if (null != componentName)
                            restoreActivityDisplay(componentName, display)
                        else if (null != launchPackage)
                            restoreActivityDisplay(launchPackage, launchActivity, display)
                        componentName = null
                        launchPackage = null
                        onDismissOverlay()
                    }
                }

                override fun onDisplayRemoved(display: Int) {}
            }
            registerDisplayListener(mDisplayListener, null)
        }

        showForegroundNotification(startId)

        val displayContext = ScaledContext(ScaledContext(applicationContext).cover()).internal(1.5f)
        floatView = LayoutInflater.from(displayContext).inflate(R.layout.navigation_menu, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM
        floatView.keepScreenOn = true

        val icons = floatView.findViewById<LinearLayout>(R.id.icons_layout)
        val menuClose = icons.findViewById<AppCompatImageView>(R.id.retract_drawer)

        bottomSheetBehavior = BottomSheetBehavior.from(floatView.findViewById(R.id.bottom_sheet_nav)!!)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    val color = prefs.getInt(
                        SamSprung.prefColors,
                        Color.rgb(255, 255, 255))
                    if (!icons.isVisible) icons.visibility = View.VISIBLE
                    for (i in 0 until icons.childCount) {
                        (icons.getChildAt(i) as AppCompatImageView).setColorFilter(color)
                    }
                    menuClose.setColorFilter(color)
                    setClickListeners(icons, componentName, launchPackage, launchActivity)
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    if (icons.isVisible) icons.visibility = View.GONE
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) { }
        })

        val color = prefs.getInt(
            SamSprung.prefColors,
            Color.rgb(255, 255, 255))
        if (!icons.isVisible) icons.visibility = View.VISIBLE
        for (i in 0 until icons.childCount) {
            (icons.getChildAt(i) as AppCompatImageView).setColorFilter(color)
        }
        menuClose.setColorFilter(color)
        setClickListeners(icons, componentName, launchPackage, launchActivity)

        floatView.findViewById<View>(R.id.bottom_sheet_nav)!!.setOnTouchListener(
            object: OnSwipeTouchListener(this@AppDisplayListener) {
            override fun onSwipeTop() : Boolean {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                return true
            }
            override fun onSwipeBottom() : Boolean {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                return true
            }
        })

        (displayContext.getSystemService(WINDOW_SERVICE) as WindowManager).addView(floatView, params)
        return START_NOT_STICKY
    }

    private fun restoreActivityDisplay(componentName: ComponentName?, display: Int) {
        ScaledContext(applicationContext).restore(display).run {
            (getSystemService(AppCompatActivity.LAUNCHER_APPS_SERVICE)
                    as LauncherApps).startMainActivity(
                componentName,
                Process.myUserHandle(),
                (getSystemService(WINDOW_SERVICE) as WindowManager).maximumWindowMetrics.bounds,
                CoverOptions(null).getActivityOptions(display).toBundle()
            )
        }
    }

    private fun restoreActivityDisplay(pkg: String?, cls: String?, display: Int) {
        if (null != pkg && null != cls) {
            restoreActivityDisplay(ComponentName(pkg, cls), display)
        }
    }

    private fun resetRecentActivities(componentName: ComponentName?, display: Int) {
        restoreActivityDisplay(componentName, 0)

        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_FORWARD_RESULT or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
        }, CoverOptions(null).getActivityOptions(display).toBundle())
    }

    private fun resetRecentActivities(pkg: String?, cls: String?, display: Int) {
        if (null != pkg && null != cls) {
            resetRecentActivities(ComponentName(pkg, cls), display)
        }
    }

    private fun setClickListeners(
        menu: LinearLayout, componentName: ComponentName?,
        launchPackage: String?, launchActivity: String?) {
        menu.findViewById<AppCompatImageView>(R.id.retract_drawer).setOnClickListener {
            if (prefs.getBoolean(SamSprung.prefReacts, true))
                vibrator.vibrate(effectClick)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        menu.findViewById<ImageView>(R.id.button_recent).setOnClickListener {
            if (prefs.getBoolean(SamSprung.prefReacts, true))
                vibrator.vibrate(effectClick)
            if (null != componentName)
                resetRecentActivities(componentName, 1)
            else
                resetRecentActivities(launchPackage, launchActivity, 1)
            onDismissOverlay()
            startForegroundService(Intent(
                applicationContext, OnBroadcastService::class.java
            ).setAction(SamSprung.launcher))
        }
        menu.findViewById<ImageView>(R.id.button_home).setOnClickListener {
            if (prefs.getBoolean(SamSprung.prefReacts, true))
                vibrator.vibrate(effectClick)
            if (null != componentName)
                resetRecentActivities(componentName, 1)
            else
                resetRecentActivities(launchPackage, launchActivity, 1)
            onDismissOverlay()
            startForegroundService(
                Intent(
                    applicationContext, OnBroadcastService::class.java
                ).setAction(SamSprung.services)
            )
        }
        menu.findViewById<ImageView>(R.id.button_back).setOnClickListener {
            if (prefs.getBoolean(SamSprung.prefReacts, true))
                vibrator.vibrate(effectClick)
            if (AccessibilityObserver.hasEnabledService(this)) {
                AccessibilityObserver.performBackAction()
            } else {
                if (null != componentName)
                    restoreActivityDisplay(componentName, 1)
                else
                    restoreActivityDisplay(launchPackage, launchActivity, 1)
            }
        }
    }

    @SuppressLint("LaunchActivityFromNotification")
    private fun showForegroundNotification(startId: Int) {
        val mNotificationManager: NotificationManager = getSystemService(
            Context.NOTIFICATION_SERVICE) as NotificationManager
        val pendingIntent = PendingIntent.getService(this, 0,
            Intent(this, AppDisplayListener::class.java),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_IMMUTABLE else 0)
        val iconNotification = BitmapFactory.decodeResource(resources, R.mipmap.sprung_icon)
        var group = mNotificationManager.getNotificationChannelGroup("tooui_services")
        if (null == group) {
            mNotificationManager.createNotificationChannelGroup(
                NotificationChannelGroup("tooui_services", "SamSprung Services")
            )
            group = mNotificationManager.getNotificationChannelGroup("tooui_services")
        }
        val notificationChannel = NotificationChannel("display_channel",
            "Display Notification", NotificationManager.IMPORTANCE_LOW)
        notificationChannel.enableLights(false)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
        mNotificationManager.createNotificationChannel(notificationChannel)
        val builder = NotificationCompat.Builder(this, "display_channel")

        val notificationText = getString(R.string.display_service, getString(R.string.samsprung))
        builder.setContentTitle(notificationText).setTicker(notificationText)
            .setSmallIcon(R.drawable.ic_baseline_samsprung_24dp)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setWhen(0).setOnlyAlertOnce(true).setGroup(group.id)
            .setContentIntent(pendingIntent).setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
        if (null != iconNotification) {
            builder.setLargeIcon(Bitmap.createScaledBitmap(
                iconNotification, 128, 128, false
            ))
        }
        builder.color = ContextCompat.getColor(this, R.color.primary_light)
        startForeground(startId, builder.build())
    }

    fun onDismissOverlay() {
        if (prefs.getBoolean(SamSprung.prefRotate, false)) {
            OrientationManager(this).removeOrientationLayout()
        }
        if (null != mDisplayListener) {
            (getSystemService(DISPLAY_SERVICE) as DisplayManager)
                .unregisterDisplayListener(mDisplayListener)
        }
        try {
            if (null != offReceiver) unregisterReceiver(offReceiver)
        } catch (ignored: Exception) { }
        ScaledContext(
            ScaledContext(applicationContext).cover(R.style.Theme_SecondScreen)
        ).internal(1.5f).run {
            (getSystemService(WINDOW_SERVICE) as WindowManager).run {
                try {
                    removeViewImmediate(floatView)
                } catch (rvi: Exception) {
                    try {
                        removeView(floatView)
                    } catch (ignored: Exception) { }
                }
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        onDismissOverlay()
    }
}
