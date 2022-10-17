/*
 * ====================================================================
 * Copyright (c) 2021-2022 AbandonedCart.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * For the purpose of this license, the phrase "SamSprung labels" shall
 * be used to refer to the labels "8-Bit Dream", "TwistedUmbrella",
 * "SamSprung" and "AbandonedCart" and these labels should be considered
 * the equivalent of any usage of the aforementioned phrase.
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All materials mentioning features or use of this software and
 *    redistributions of any form whatsoever must display the following
 *    acknowledgment unless made available by tagged, public "commits":
 *    "This product includes software developed for SamSprung by AbandonedCart"
 *
 * 4. The SamSprung labels must not be used in any form to endorse or promote
 *    products derived from this software without prior written permission.
 *    For written permission, please contact enderinexiledc@gmail.com
 *
 * 5. Products derived from this software may not be called by the SamSprung
 *    labels nor may these labels appear in their names or product information
 *    without prior written permission of AbandonedCart.
 *
 * THIS SOFTWARE IS PROVIDED BY AbandonedCart AND SamSprung ``AS IS'' AND ANY
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

package com.eightbit.samsprung

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.eightbit.app.CoverOptions
import com.eightbit.content.ScaledContext
import com.eightbit.samsprung.launcher.OrientationManager

class OnBroadcastService : Service() {

    private val onReceiver = object : BroadcastReceiver() {
        @SuppressLint("NotifyDataSetChanged")
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_USER_PRESENT == intent.action
                || Intent.ACTION_SCREEN_ON == intent.action) {
                if (getSharedPreferences(SamSprung.prefsValue, MODE_PRIVATE)
                        .getBoolean(SamSprung.prefRotate, false)) {
                    OrientationManager(context).removeOrientationLayout()
                }
                ScaledContext.cover(context).startActivity(
                    Intent(context.applicationContext, SamSprungOverlay::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    CoverOptions(null).getActivityOptions(1).toBundle()
                )
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("InflateParams")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        showForegroundNotification(startId)

        if (!Settings.canDrawOverlays(applicationContext) || SamSprung.updating == intent?.action) {
            return stopOverlayService()
        } else if (SamSprung.services == intent?.action) {
            startActivity(
                Intent(applicationContext, SamSprungOverlay::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                CoverOptions(null).getActivityOptions(1).toBundle()
            )
        } else if (SamSprung.launcher == intent?.action) {
            startActivity(
                Intent(applicationContext, SamSprungOverlay::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setAction(SamSprung.launcher),
                CoverOptions(null).getActivityOptions(1).toBundle()
            )
        }
        try {
            applicationContext.unregisterReceiver(onReceiver)
        } catch (ignored: Exception) { }
        IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
            priority = 999
        }.also {
            registerReceiver(onReceiver, it)
        }
        return START_STICKY
    }

    @SuppressLint("LaunchActivityFromNotification")
    private fun showForegroundNotification(startId: Int) {
        var mNotificationManager: NotificationManager? = null
        val pendingIntent = PendingIntent.getService(this, 0,
            Intent(this, OnBroadcastService::class.java)
                .setAction(SamSprung.updating),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_IMMUTABLE else 0)
        val iconNotification = BitmapFactory.decodeResource(resources, R.mipmap.sprung_icon)
        if (null == mNotificationManager) {
            mNotificationManager = getSystemService(
                Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        var group = mNotificationManager.getNotificationChannelGroup("samsprung_services")
        if (null == group) {
            mNotificationManager.createNotificationChannelGroup(
                NotificationChannelGroup("samsprung_services", "SamSprung Services")
            )
            group = mNotificationManager.getNotificationChannelGroup("samsprung_services")
        }
        val notificationChannel = NotificationChannel("tooui_overlay_channel",
            "TooUI Overlay Notification", NotificationManager.IMPORTANCE_LOW)
        notificationChannel.enableLights(false)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
        mNotificationManager.createNotificationChannel(notificationChannel)
        val builder = NotificationCompat.Builder(this, "tooui_overlay_channel")

        val notificationText = getString(R.string.overlay_service, getString(R.string.samsprung))
        builder.setContentTitle(notificationText).setTicker(notificationText)
            .setSmallIcon(R.drawable.ic_baseline_samsprung_24dp)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setWhen(0).setOnlyAlertOnce(true).setGroup(group.id)
            .setContentIntent(pendingIntent).setOngoing(true)
        if (null != iconNotification) {
            builder.setLargeIcon(Bitmap.createScaledBitmap(
                iconNotification, 128, 128, false
            ))
        }
        builder.color = ContextCompat.getColor(this, R.color.primary_dark)
        startForeground(startId, builder.build())
    }

    private fun stopOverlayService(): Int {
        try {
            unregisterReceiver(onReceiver)
        } catch (ignored: Exception) { }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (Settings.canDrawOverlays(applicationContext)) {
            startForegroundService(Intent(application, OnBroadcastService::class.java))
        }
    }
}