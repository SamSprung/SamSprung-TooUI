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

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.*
import java.util.concurrent.Executors

class CheckUpdatesTask {

    private var activity: Activity

    constructor(activity: CoverPreferences) {
        this.activity = activity
        Executors.newSingleThreadExecutor().execute {
            val files: Array<File>? = activity.filesDir.listFiles { _, name ->
                name.lowercase(Locale.getDefault()).endsWith(".apk")
            }
            if (null != files) {
                for (file in files) {
                    if (!file.isDirectory) file.delete()
                }
            }
        }
        if (BuildConfig.FLAVOR != "google") {
            clearPastUpdates()
            if (activity.packageManager.canRequestPackageInstalls()) {
                retrieveUpdate()
            } else {
                activity.updateLauncher.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse(String.format("package:%s", activity.packageName))))
            }
        }
    }

    constructor(activity: SamSprungDrawer) {
        this.activity = activity
        if (BuildConfig.FLAVOR != "google") {
            clearPastUpdates()
            if (activity.packageManager.canRequestPackageInstalls())
                retrieveUpdate()
        }
    }

    private fun clearPastUpdates() {
        try {
            (activity.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE)
                    as NotificationManager).cancel(SamSprung.request_code)
        } catch (ignored: Exception) { }
        Executors.newSingleThreadExecutor().execute {
            val files: Array<File>? = activity.filesDir.listFiles { _, name ->
                name.lowercase(Locale.getDefault()).endsWith(".apk")
            }
            if (null != files) {
                for (file in files) {
                    if (!file.isDirectory) file.delete()
                }
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun installUpdate(apkUri: Uri) = withContext(Dispatchers.IO) {
        val installer = activity.applicationContext.packageManager.packageInstaller
        val resolver = activity.applicationContext.contentResolver
        for (session: PackageInstaller.SessionInfo in installer.mySessions) {
            installer.abandonSession(session.sessionId)
        }
        resolver.openInputStream(apkUri)?.use { apkStream ->
            val length = DocumentFile.fromSingleUri(
                activity.applicationContext, apkUri)?.length() ?: -1
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val sessionId = installer.createSession(params)
            val session = installer.openSession(sessionId)
            session.openWrite("NAME", 0, length).use { sessionStream ->
                apkStream.copyTo(sessionStream)
                session.fsync(sessionStream)
            }
            val pi = PendingIntent.getBroadcast(
                activity.applicationContext, SamSprung.request_code,
                Intent(activity.applicationContext, GitBroadcastReceiver::class.java)
                    .setAction(SamSprung.updating),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                else PendingIntent.FLAG_UPDATE_CURRENT
            )
            session.commit(pi.intentSender)
            session.close()
        }
    }

    private fun downloadUpdate(link: String) {
        val download: String = link.substring(
            link.lastIndexOf(File.separator) + 1)
        val apk = File(activity.filesDir, download)
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            URL(link).openStream().use { input ->
                FileOutputStream(apk).use { output ->
                    input.copyTo(output)
                    CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main) {
                        installUpdate(
                            FileProvider.getUriForFile(
                                activity.applicationContext, SamSprung.provider, apk
                            ))
                    }
                }
            }
        }
    }

    private fun showUpdateNotification() {
        var mNotificationManager: NotificationManager? = null

        val pendingIntent = PendingIntent.getActivity(activity, 0,
            Intent(activity, CoverPreferences::class.java),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE
            else PendingIntent.FLAG_ONE_SHOT)
        val iconNotification = BitmapFactory.decodeResource(
            activity.resources, R.drawable.ic_baseline_samsprung_24)
        if (null == mNotificationManager) {
            mNotificationManager = activity.getSystemService(
                Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        mNotificationManager.createNotificationChannelGroup(
            NotificationChannelGroup("services_group", "Services")
        )
        val notificationChannel = NotificationChannel("update_channel",
            "Update Notification", NotificationManager.IMPORTANCE_LOW)
        notificationChannel.enableLights(false)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        mNotificationManager.createNotificationChannel(notificationChannel)
        val builder = NotificationCompat.Builder(
            activity, "update_channel")

        val notificationText = activity.getString(
            R.string.update_service, activity.getString(R.string.app_name))
        builder.setContentTitle(notificationText).setTicker(notificationText)
            .setContentText(activity.getString(R.string.click_update_app))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setWhen(0).setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent).setOngoing(false)
        if (null != iconNotification) {
            builder.setLargeIcon(
                Bitmap.createScaledBitmap(
                    iconNotification, 128, 128, false))
        }
        builder.color = ContextCompat.getColor(activity, R.color.secondary_light)

        val notification: Notification = builder.build()
        notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
        mNotificationManager.notify(SamSprung.request_code, notification)
    }

    fun retrieveUpdate() {
        RequestGitHubAPI(activity.getString(R.string.latest_url)).setResultListener(
            object : RequestGitHubAPI.ResultListener {
            override fun onResults(result: String) {
                try {
                    val jsonObject = JSONTokener(result).nextValue() as JSONObject
                    val lastCommit = (jsonObject["name"] as String).substring(10)
                    if (BuildConfig.COMMIT != lastCommit) {
                        if (activity is CoverPreferences) {
                            val assets = (jsonObject["assets"] as JSONArray)[0] as JSONObject
                            downloadUpdate(assets["browser_download_url"] as String)
                        } else if (activity is SamSprungDrawer) {
                            showUpdateNotification()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }
}