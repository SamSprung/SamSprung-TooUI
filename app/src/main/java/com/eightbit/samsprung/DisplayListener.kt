package com.eightbit.samsprung

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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.*
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.eightbit.content.ScaledContext
import com.eightbit.view.OnSwipeTouchListener
import com.eightbit.widget.VerticalStrokeTextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.io.File


class DisplayListener : Service() {

    private var mDisplayListener: DisplayManager.DisplayListener? = null
    private lateinit var floatView: View

    @Suppress("DEPRECATION")
    private lateinit var mKeyguardLock: KeyguardManager.KeyguardLock
    private lateinit var displayManager: DisplayManager
    
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val launchPackage = intent?.getStringExtra("launchPackage")
        val launchActivity = intent?.getStringExtra("launchActivity")

        @Suppress("DEPRECATION")
        mKeyguardLock = (getSystemService(Context.KEYGUARD_SERVICE)
                as KeyguardManager).newKeyguardLock("cover_lock")
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

        if (null == launchPackage)
            return dismissDisplayService(displayManager, mKeyguardLock)

        showForegroundNotification(startId)

        val displayContext = ScaledContext.wrap(buildDisplayContext(1))
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

        mDisplayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(display: Int) {}
            override fun onDisplayChanged(display: Int) {
                if (display == 0) {
                    dismissDisplayListener(displayManager, mKeyguardLock)
                    restoreActivityDisplay(launchPackage, launchActivity, display)
                } else {
                    if (SamSprung.isKeyguardLocked)
                        @Suppress("DEPRECATION") mKeyguardLock.disableKeyguard()
                    if (!floatView.isShown)
                        (displayContext.getSystemService(WINDOW_SERVICE)
                                as WindowManager).addView(floatView, params)
                }
            }

            override fun onDisplayRemoved(display: Int) {}
        }
        displayManager.registerDisplayListener(
            mDisplayListener, Handler(Looper.getMainLooper())
        )

        val coordinator = floatView.findViewById<CoordinatorLayout>(R.id.coordinator)
        val mKeyboardView = if (hasAccessibility())
            getKeyboard(coordinator, this) else null

        val menu = floatView.findViewById<LinearLayout>(R.id.button_layout)
        val menuKeys = menu.findViewById<ImageView>(R.id.button_input)
        val menuLogo = menu.findViewById<VerticalStrokeTextView>(R.id.samsprung_logo)
        val menuRecent = menu.findViewById<ImageView>(R.id.button_recent)
        val menuHome = menu.findViewById<ImageView>(R.id.button_home)
        val menuBack = menu.findViewById<ImageView>(R.id.button_back)
        bottomSheetBehavior = BottomSheetBehavior.from(floatView.findViewById(R.id.bottom_sheet)!!)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    val color = SamSprung.prefs.getInt(SamSprung.prefColors,
                        Color.rgb(255, 255, 255))
                    if (!menu.isVisible) menu.visibility = View.VISIBLE
                    val icons = menu.findViewById<LinearLayout>(R.id.icons_layout)
                    for (i in 0 until icons.childCount) {
                        (icons.getChildAt(i) as AppCompatImageView).setColorFilter(color)
                    }
                    menuLogo.setTextColor(color)
                    if (hasAccessibility()) {
                        menuKeys.visibility = View.VISIBLE
                        menuKeys.setOnClickListener {
                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                            if (!mKeyboardView!!.isShown)
                                coordinator.addView(mKeyboardView, 0)
                        }
                    } else {
                        menuKeys.visibility = View.GONE
                    }
                    menuLogo.setOnClickListener {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                    menuRecent.setOnClickListener {
                        resetRecentActivities(launchPackage, launchActivity)
                        dismissDisplayListener(displayManager, mKeyguardLock)
                        startActivity(
                            Intent(buildDisplayContext(1), SamSprungDrawer::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle()
                        )
                    }
                    menuHome.setOnClickListener {
                        resetRecentActivities(launchPackage, launchActivity)
                        dismissDisplayListener(displayManager, mKeyguardLock)
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                        startActivity(
                            Intent(buildDisplayContext(1), SamSprungOverlay::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle()
                        )
                    }
                    menuBack.setOnClickListener {
                        if (hasAccessibility()) {
                            AccessibilityObserver.executeButtonBack()
                        } else {
                            restoreActivityDisplay(launchPackage, launchActivity, 1)
                        }
                    }
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    if (menu.isVisible) menu.visibility = View.GONE
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) { }
        })

        bottomSheetBehavior.isHideable = false

        floatView.findViewById<View>(R.id.bottom_sheet)!!.setOnTouchListener(
            object: OnSwipeTouchListener(this@DisplayListener) {
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
        return START_STICKY
    }

    private fun restoreActivityDisplay(launchPackage: String, launchActivity: String?, display: Int) {
        if (null != launchActivity) {
            val coverIntent = Intent(Intent.ACTION_MAIN)
            coverIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            coverIntent.component = ComponentName(launchPackage, launchActivity)
            coverIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_FORWARD_RESULT or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(coverIntent, ActivityOptions.makeBasic()
                .setLaunchDisplayId(display).toBundle())
        }
    }

    private fun resetRecentActivities(launchPackage: String, launchActivity: String?) {
        restoreActivityDisplay(launchPackage, launchActivity, 0)

        val homeLauncher = Intent(Intent.ACTION_MAIN)
        homeLauncher.addCategory(Intent.CATEGORY_HOME)
        homeLauncher.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_FORWARD_RESULT or
                Intent.FLAG_ACTIVITY_NO_ANIMATION
        startActivity(homeLauncher, ActivityOptions.makeBasic().setLaunchDisplayId(0).toBundle())
    }

    @SuppressLint("InflateParams")
    @Suppress("DEPRECATION")
    private fun getKeyboard (parent: CoordinatorLayout, displayContext: Context) : KeyboardView {
        val mKeyboard = Keyboard(parent.context, R.xml.keyboard_qwerty)
        val mKeyboardView = LayoutInflater.from(displayContext)
            .inflate(R.layout.keyboard_view, null) as KeyboardView
        mKeyboardView.isPreviewEnabled = false
        mKeyboardView.keyboard = mKeyboard
        SamSprungInput.setInputMethod(mKeyboard, mKeyboardView, parent)
        AccessibilityObserver.enableKeyboard(applicationContext)
        return mKeyboardView
    }

    @SuppressLint("LaunchActivityFromNotification")
    private fun showForegroundNotification(startId: Int) {
        var mNotificationManager: NotificationManager? = null
        val pendingIntent = PendingIntent.getService(this, 0,
            Intent(this, DisplayListener::class.java),
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
        val notificationChannel = NotificationChannel("service_channel",
            "Service Notification", NotificationManager.IMPORTANCE_LOW)
        notificationChannel.enableLights(false)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
        mNotificationManager.createNotificationChannel(notificationChannel)
        val builder = NotificationCompat.Builder(this, "service_channel")

        val notificationText = getString(R.string.display_service, getString(R.string.samsprung))
        builder.setContentTitle(notificationText).setTicker(notificationText)
            .setSmallIcon(R.drawable.ic_baseline_samsprung_24)
            .setWhen(0).setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent).setOngoing(true)
        if (null != iconNotification) {
            builder.setLargeIcon(Bitmap.createScaledBitmap(
                iconNotification, 128, 128, false))
        }
        builder.color = ContextCompat.getColor(this, R.color.primary_light)
        startForeground(startId, builder.build())
    }

    private fun dismissDisplayListener(
        displayManager: DisplayManager,
        @Suppress("DEPRECATION")
        mKeyguardLock: KeyguardManager.KeyguardLock
    ) {
        if (hasAccessibility()) AccessibilityObserver.disableKeyboard(applicationContext)
        if (this::floatView.isInitialized && floatView.isAttachedToWindow)
            (buildDisplayContext(1).getSystemService(WINDOW_SERVICE)
                    as WindowManager).removeView(floatView)
        if (null != mDisplayListener) {
            displayManager.unregisterDisplayListener(mDisplayListener)
        }
        if (SamSprung.prefs.getInt(SamSprung.autoRotate, 1) == 0
            && Settings.System.canWrite(applicationContext)) {
            try {
                Settings.System.putInt(applicationContext.contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION,
                    SamSprung.prefs.getInt(SamSprung.autoRotate, 0)
                )
            } catch (ignored: Settings.SettingNotFoundException) { }
        }
        if (SamSprung.isKeyguardLocked)
            @Suppress("DEPRECATION") mKeyguardLock.reenableKeyguard()
        try {
            stopForeground(true)
            stopSelf()
        } catch (ignored: Exception) { }
    }

    private fun dismissDisplayService(
        displayManager: DisplayManager,
        @Suppress("DEPRECATION")
        mKeyguardLock: KeyguardManager.KeyguardLock
    ): Int {
        dismissDisplayListener(displayManager, mKeyguardLock)
        return START_NOT_STICKY
    }

    private fun buildDisplayContext(display: Int): Context {
        // val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displayContext = createDisplayContext(displayManager.getDisplay(display))
        val wm = displayContext.getSystemService(WINDOW_SERVICE) as WindowManager
        return object : ContextThemeWrapper(displayContext, R.style.Theme_SecondScreen) {
            override fun getSystemService(name: String): Any? {
                return if (WINDOW_SERVICE == name) wm else super.getSystemService(name)
            }
        }
    }

    private fun hasAccessibility(): Boolean {
        val serviceString = Settings.Secure.getString(contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return serviceString != null && serviceString.contains(packageName
                + File.separator + AccessibilityObserver::class.java.name)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (this::floatView.isInitialized && floatView.isAttachedToWindow)
            floatView.invalidate()
    }
}