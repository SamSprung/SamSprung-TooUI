package com.eightbit.samsprung.launcher

/* ====================================================================
 * Copyright (c) 2012-2022 AbandonedCart.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All advertising materials mentioning features or use of this
 *    software and redistributions of any form whatsoever
 *    must display the following acknowledgment:
 *    "This product includes software developed by AbandonedCart" unless
 *    otherwise displayed by tagged, public repository entries.
 *
 * 4. The names "8-Bit Dream", "TwistedUmbrella" and "AbandonedCart"
 *    must not be used in any form to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact enderinexiledc@gmail.com
 *
 * 5. Products derived from this software may not be called "8-Bit Dream",
 *    "TwistedUmbrella" or "AbandonedCart" nor may these labels appear
 *    in their names without prior written permission of AbandonedCart.
 *
 * THIS SOFTWARE IS PROVIDED BY AbandonedCart ``AS IS'' AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE OpenSSL PROJECT OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 * The license and distribution terms for any publicly available version or
 * derivative of this code cannot be changed.  i.e. this code cannot simply be
 * copied and put under another distribution license
 * [including the GNU Public License.] Content not subject to these terms is
 * subject to to the terms and conditions of the Apache License, Version 2.0.
 */

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.*
import android.provider.Settings
import android.view.*
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.eightbit.samsprung.AccessibilityObserver
import com.eightbit.samsprung.OnBroadcastService
import com.eightbit.samsprung.R
import com.eightbit.samsprung.SamSprung
import com.eightbit.view.OnSwipeTouchListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.io.File

class AppDisplayListener : Service() {

    private lateinit var prefs: SharedPreferences
    private lateinit var offReceiver: BroadcastReceiver
    private lateinit var mDisplayListener: DisplayManager.DisplayListener
    @Suppress("DEPRECATION")
    private lateinit var mKeyguardLock: KeyguardManager.KeyguardLock
    private lateinit var floatView: View
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val launchPackage = intent.getStringExtra("launchPackage")
        val launchActivity = intent.getStringExtra("launchActivity")
        val intentSender = intent.getBooleanExtra("intentSender", false)
        var componentName: ComponentName? = null

        prefs = getSharedPreferences(SamSprung.prefsValue, MODE_PRIVATE)
        @Suppress("DEPRECATION")
        mKeyguardLock = (getSystemService(Context.KEYGUARD_SERVICE)
                as KeyguardManager).newKeyguardLock("cover_lock")
        if ((application as SamSprung).isKeyguardLocked)
            @Suppress("DEPRECATION") mKeyguardLock.disableKeyguard()

        if (intentSender) {
            componentName = packageManager.getLaunchIntentForPackage(launchPackage!!)?.component
        }

        offReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_OFF) {
                    if (null != componentName)
                        resetRecentActivities(componentName)
                    else
                        resetRecentActivities(launchPackage, launchActivity)
                    onDismiss()
                    stopForeground(true)
                    stopSelf()
                    if (!isServiceRunning(applicationContext, OnBroadcastService::class.java))
                        startForegroundService(Intent(context, OnBroadcastService::class.java))
                }
            }
        }
        IntentFilter(Intent.ACTION_SCREEN_OFF).also {
            registerReceiver(offReceiver, it)
        }

        showForegroundNotification(startId)

        val displayContext = (application as SamSprung).getScaledContext()!!
        floatView = LayoutInflater.from(displayContext).inflate(R.layout.navigation_menu, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM
        floatView.keepScreenOn = true

        mDisplayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(display: Int) {}
            override fun onDisplayChanged(display: Int) {
                if (display == 0) {
                    if (null != componentName)
                        restoreActivityDisplay(componentName, display)
                    else
                        restoreActivityDisplay(launchPackage, launchActivity, display)
                    onDismiss()
                    stopForeground(true)
                    stopSelf()
                }
            }

            override fun onDisplayRemoved(display: Int) {}
        }
        (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).registerDisplayListener(
            mDisplayListener, Handler(Looper.getMainLooper())
        )

        val menu = floatView.findViewById<LinearLayout>(R.id.button_layout)
        val icons = menu.findViewById<LinearLayout>(R.id.icons_layout)
        val menuClose = menu.findViewById<AppCompatImageView>(R.id.retract_drawer)

        bottomSheetBehavior = BottomSheetBehavior.from(floatView.findViewById(R.id.bottom_sheet)!!)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    val color = prefs.getInt(
                        SamSprung.prefColors,
                        Color.rgb(255, 255, 255))
                    if (!menu.isVisible) menu.visibility = View.VISIBLE
                    for (i in 0 until icons.childCount) {
                        (icons.getChildAt(i) as AppCompatImageView).setColorFilter(color)
                    }
                    menuClose.setColorFilter(color)
                    setClickListeners(menu, componentName, launchPackage, launchActivity)
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    if (menu.isVisible) menu.visibility = View.GONE
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) { }
        })

        val color = prefs.getInt(
            SamSprung.prefColors,
            Color.rgb(255, 255, 255))
        if (!menu.isVisible) menu.visibility = View.VISIBLE
        for (i in 0 until icons.childCount) {
            (icons.getChildAt(i) as AppCompatImageView).setColorFilter(color)
        }
        menuClose.setColorFilter(color)
        if (menu.isVisible) menu.visibility = View.GONE

        floatView.findViewById<View>(R.id.bottom_sheet)!!.setOnTouchListener(
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

        (displayContext.getSystemService(WINDOW_SERVICE)
                as WindowManager).addView(floatView, params)
        return START_NOT_STICKY
    }

    private fun restoreActivityDisplay(
        componentName: ComponentName, display: Int) {
            val coverIntent = Intent(Intent.ACTION_MAIN)
            coverIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            coverIntent.component = componentName
            coverIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_FORWARD_RESULT or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(coverIntent, ActivityOptions.makeBasic()
                .setLaunchDisplayId(display).toBundle())
    }

    private fun restoreActivityDisplay(
        pkg: String?, cls: String?, display: Int) {
        if (null != pkg && null != cls) {
            restoreActivityDisplay(ComponentName(pkg, cls), display)
        }
    }

    private fun resetRecentActivities(pkg: String?, cls: String?) {
        restoreActivityDisplay(pkg, cls, 0)

        val homeLauncher = Intent(Intent.ACTION_MAIN)
        homeLauncher.addCategory(Intent.CATEGORY_HOME)
        homeLauncher.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_FORWARD_RESULT or
                Intent.FLAG_ACTIVITY_NO_ANIMATION
        startActivity(homeLauncher, ActivityOptions
            .makeBasic().setLaunchDisplayId(0).toBundle())
    }

    private fun resetRecentActivities(componentName: ComponentName) {
        restoreActivityDisplay(componentName, 0)

        val homeLauncher = Intent(Intent.ACTION_MAIN)
        homeLauncher.addCategory(Intent.CATEGORY_HOME)
        homeLauncher.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_FORWARD_RESULT or
                Intent.FLAG_ACTIVITY_NO_ANIMATION
        startActivity(homeLauncher, ActivityOptions
            .makeBasic().setLaunchDisplayId(0).toBundle())
    }

    private fun tactileFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator.vibrate(VibrationEffect
                .createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(VibrationEffect
                .createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun setClickListeners(menu: LinearLayout, componentName: ComponentName?,
                                  launchPackage: String?, launchActivity: String?) {
        menu.findViewById<AppCompatImageView>(R.id.retract_drawer).setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        menu.findViewById<ImageView>(R.id.button_recent).setOnClickListener {
            tactileFeedback()
            if (null != componentName)
                resetRecentActivities(componentName)
            else
                resetRecentActivities(launchPackage, launchActivity)
            onDismiss()
            stopForeground(true)
            stopSelf()
            startForegroundService(Intent(
                applicationContext, OnBroadcastService::class.java
            ).setAction(SamSprung.launcher))
        }
        menu.findViewById<ImageView>(R.id.button_home).setOnClickListener {
            tactileFeedback()
            if (null != componentName)
                resetRecentActivities(componentName)
            else
                resetRecentActivities(launchPackage, launchActivity)
            onDismiss()
            stopForeground(true)
            stopSelf()
            startForegroundService(
                Intent(
                    applicationContext,
                    OnBroadcastService::class.java
                ).setAction(SamSprung.services)
            )
        }
        menu.findViewById<ImageView>(R.id.button_back).setOnClickListener {
            tactileFeedback()
            if (hasAccessibility()) {
                AccessibilityObserver.executeButtonBack()
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
        var mNotificationManager: NotificationManager? = null
        val pendingIntent = PendingIntent.getService(this, 0,
            Intent(this, AppDisplayListener::class.java),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_IMMUTABLE else 0)
        val iconNotification = BitmapFactory.decodeResource(resources, R.mipmap.sprung_icon)
        if (null == mNotificationManager) {
            mNotificationManager = getSystemService(
                Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        mNotificationManager.createNotificationChannelGroup(
            NotificationChannelGroup("services_group", "Services")
        )
        val notificationChannel = NotificationChannel("display_channel",
            "Display Notification", NotificationManager.IMPORTANCE_LOW)
        notificationChannel.enableLights(false)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
        mNotificationManager.createNotificationChannel(notificationChannel)
        val builder = NotificationCompat.Builder(this, "display_channel")

        val notificationText = getString(R.string.display_service, getString(R.string.samsprung))
        builder.setContentTitle(notificationText).setTicker(notificationText)
            .setSmallIcon(R.drawable.ic_baseline_samsprung_24)
            .setWhen(0).setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent).setOngoing(true)
            .setGroup("services_group")
        if (null != iconNotification) {
            builder.setLargeIcon(Bitmap.createScaledBitmap(
                iconNotification, 128, 128, false))
        }
        builder.color = ContextCompat.getColor(this, R.color.primary_light)
        startForeground(startId, builder.build())
    }

    private fun hasAccessibility(): Boolean {
        val serviceString = Settings.Secure.getString(contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return serviceString != null && serviceString.contains(packageName
                + File.separator + AccessibilityObserver::class.java.name)
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        for (service in (context.getSystemService(ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    fun onDismiss() {
        try {
            if (this::offReceiver.isInitialized)
                unregisterReceiver(offReceiver)
        } catch (ignored: Exception) { }
        if (this::mDisplayListener.isInitialized) {
            (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
                .unregisterDisplayListener(mDisplayListener)
        }

        if (this::floatView.isInitialized) {
            try {
                val displayContext = (application as SamSprung).getScaledContext()!!
                (displayContext.getSystemService(WINDOW_SERVICE)
                        as WindowManager).removeViewImmediate(floatView)
            } catch (ignored: Exception) { }
        }
        if ((application as SamSprung).isKeyguardLocked)
            @Suppress("DEPRECATION") mKeyguardLock.reenableKeyguard()
    }

    override fun onDestroy() {
        super.onDestroy()
        onDismiss()
    }
}