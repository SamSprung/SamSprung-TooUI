package com.eightbit.samsprung.update

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
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.*
import java.util.concurrent.Executors
import android.content.ContentResolver
import android.media.AudioAttributes
import com.eightbit.samsprung.*


class CheckUpdatesTask(private var activity: AppCompatActivity) {

    private val repository = "https://api.github.com/repos/SamSprung/SamSprung-TooUI/releases/tags/"

    var listener: CheckUpdateListener? = null

    init {
        if (BuildConfig.FLAVOR != "google") {
            try {
                (activity.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE)
                        as NotificationManager).cancel(SamSprung.request_code)
            } catch (ignored: Exception) { }
            if (activity is UpdateShimActivity) {
                val installer = activity.applicationContext.packageManager.packageInstaller
                for (session: PackageInstaller.SessionInfo in installer.mySessions) {
                    installer.abandonSession(session.sessionId)
                }
            } else {
                Executors.newSingleThreadExecutor().execute {
                    val files: Array<File>? = activity.externalCacheDir?.listFiles { _, name ->
                        name.lowercase(Locale.getDefault()).endsWith(".apk")
                    }
                    if (null != files) {
                        for (file in files) {
                            if (!file.isDirectory) file.delete()
                        }
                    }
                }
                if (activity.packageManager.canRequestPackageInstalls()) {
                    retrieveUpdate()
                } else if (activity is CoverPreferences) {
                    (activity as CoverPreferences).updateLauncher.launch(
                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(
                            Uri.parse(String.format("package:%s", activity.packageName))))
                }
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun installUpdate(apkUri: Uri) = withContext(Dispatchers.IO) {
        val installer = activity.applicationContext.packageManager.packageInstaller
        val resolver = activity.applicationContext.contentResolver
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

    fun downloadUpdate(link: String) {
        val download: String = link.substring(
            link.lastIndexOf(File.separator) + 1)
        val apk = File(activity.externalCacheDir, download)
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            URL(link).openStream().use { input ->
                FileOutputStream(apk).use { output ->
                    input.copyTo(output)
                    CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main) {
                        installUpdate(FileProvider.getUriForFile(
                            activity.applicationContext, SamSprung.provider, apk
                        ))
                    }
                }
            }
        }
    }

    private fun showUpdateNotification(downloadUrl: String) {
        var mNotificationManager: NotificationManager? = null

        val pendingIntent = PendingIntent.getActivity(activity, 0,
            Intent(activity, UpdateShimActivity::class.java).setAction(downloadUrl),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE
            else PendingIntent.FLAG_ONE_SHOT)
        val iconNotification = BitmapFactory.decodeResource(
            activity.resources, R.drawable.ic_baseline_samsprung_24
        )
        if (null == mNotificationManager) {
            mNotificationManager = activity.getSystemService(
                Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        mNotificationManager.createNotificationChannelGroup(
            NotificationChannelGroup("services_group", "Services")
        )
        val notificationChannel = NotificationChannel("update_channel",
            "Update Notification", NotificationManager.IMPORTANCE_DEFAULT)
        notificationChannel.enableLights(false)
        val soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + activity.packageName + "/" + R.raw.oblige_entry
        )
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
        notificationChannel.setSound(soundUri, audioAttributes)
        notificationChannel.vibrationPattern = longArrayOf(1000L,1000L,1000L)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        mNotificationManager.createNotificationChannel(notificationChannel)
        val builder = NotificationCompat.Builder(
            activity, "update_channel")

        val notificationText = activity.getString(
            R.string.update_service, activity.getString(R.string.samsprung))
        builder.setContentTitle(notificationText).setTicker(notificationText)
            .setContentText(activity.getString(R.string.click_update_app))
            .setSmallIcon(R.drawable.ic_baseline_samsprung_24)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(soundUri).setVibrate(longArrayOf(1000L,1000L,1000L))
            .setWhen(0).setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent).setOngoing(false)
        if (null != iconNotification) {
            builder.setLargeIcon(
                Bitmap.createScaledBitmap(
                    iconNotification, 128, 128, false))
        }
        builder.color = ContextCompat.getColor(activity, R.color.secondary_dark)

        val notification: Notification = builder.build()
        notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
        mNotificationManager.notify(SamSprung.request_code, notification)
    }

    private fun parseUpdateJSON(result: String, isPreview: Boolean) {
        val offset = activity.getString(R.string.samsprung).length + 1
        var lastCommit: String? = null
        var downloadUrl: String? = null
        try {
            val jsonObject = JSONTokener(result).nextValue() as JSONObject
            lastCommit = (jsonObject["name"] as String).substring(offset)
            val assets = jsonObject["assets"] as JSONArray
            val asset = assets[0] as JSONObject
            downloadUrl = asset["browser_download_url"] as String
            if (isPreview && BuildConfig.COMMIT != lastCommit) {
                if (null != listener) listener?.onUpdateFound(downloadUrl)
                showUpdateNotification(downloadUrl)
            }
        } catch (ignored: JSONException) { }
        if (!isPreview && null != lastCommit && null != downloadUrl) {
            RequestGitHubAPI(repository + "preview").setResultListener(
                object : RequestGitHubAPI.ResultListener {
                override fun onResults(result: String) {
                    try {
                        val jsonObject = JSONTokener(result).nextValue() as JSONObject
                        val extraCommit = (jsonObject["name"] as String).substring(offset)
                        if (BuildConfig.COMMIT != extraCommit && BuildConfig.COMMIT != lastCommit) {
                            if (null != listener) listener?.onUpdateFound(downloadUrl)
                            showUpdateNotification(downloadUrl)
                        }
                    } catch (ignored: JSONException) { }
                }
            })
        }
    }

    fun retrieveUpdate() {
        val prefs = activity.getSharedPreferences(
            SamSprung.prefsValue, AppCompatActivity.MODE_PRIVATE)
        val isPreview = prefs.getBoolean(SamSprung.prefTester, false)
        RequestGitHubAPI(repository + if (isPreview) "preview" else "sideload")
            .setResultListener(object : RequestGitHubAPI.ResultListener {
            override fun onResults(result: String) {
                parseUpdateJSON(result, isPreview)
            }
        })
    }

    fun setUpdateListener(listener: CheckUpdateListener) {
        this.listener = listener
    }

    interface CheckUpdateListener {
        fun onUpdateFound(downloadUrl: String)
    }
}