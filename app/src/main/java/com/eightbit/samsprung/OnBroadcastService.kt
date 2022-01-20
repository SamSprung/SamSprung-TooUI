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

class OnBroadcastService : Service() {

    inner class OnBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_SCREEN_ON == intent.action) {
                context.startService(Intent(context, DisplayListenerService::class.java))
                val coverIntent = Intent(context.applicationContext, SamSprungDrawer::class.java)
                coverIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(coverIntent,
                    ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle())
            }
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    @SuppressLint("InflateParams")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        showForegroundNotification(startId)

        if (!Settings.canDrawOverlays(applicationContext)
            || SamSprung.removing == intent?.action)
            return dismissOverlayService()

        val onScreenFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
        onScreenFilter.priority = 999
        onScreenFilter.also {
            applicationContext.registerReceiver(OnBroadcastReceiver(), it)
        }

        return START_STICKY
    }

    @SuppressLint("LaunchActivityFromNotification")
    private fun showForegroundNotification(startId: Int) {
        var mNotificationManager: NotificationManager? = null
        val pendingIntent = PendingIntent.getService(this, 0,
            Intent(this, OnBroadcastService::class.java)
                .setAction(SamSprung.removing),
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

        val notificationText = getString(R.string.overlay_service, getString(R.string.app_name))
        builder.setContentTitle(notificationText).setTicker(notificationText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setWhen(0).setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent).setOngoing(true)
        if (null != iconNotification) {
            builder.setLargeIcon(
                Bitmap.createScaledBitmap(
                    iconNotification, 128, 128, false))
        }
        builder.color = ContextCompat.getColor(this, R.color.purple_200)
        startForeground(startId, builder.build())
    }

    private fun dismissOverlayService(): Int {
        try {
            stopForeground(true)
            stopSelf()
        } catch (ignored: Exception) { }
        return START_NOT_STICKY
    }
}