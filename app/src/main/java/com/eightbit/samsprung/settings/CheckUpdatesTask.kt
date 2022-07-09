package com.eightbit.samsprung.settings

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

import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.eightbit.samsprung.BuildConfig
import com.eightbit.samsprung.GitBroadcastReceiver
import com.eightbit.samsprung.R
import com.eightbit.samsprung.SamSprung
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
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

class CheckUpdatesTask(private var activity: Activity) {

    private val repo = "https://api.github.com/repos/SamSprung/SamSprung-TooUI/releases/tags/"
    var listener: CheckUpdateListener? = null
    var listenerPlay: CheckPlayUpdateListener? = null
    private var appUpdateManager: AppUpdateManager? = null
    private var isUpdateAvailable = false

    init {
        if (SamSprung.isGooglePlay()) {
            if (null == appUpdateManager)
                appUpdateManager = AppUpdateManagerFactory.create(activity)
            val appUpdateInfoTask = appUpdateManager?.appUpdateInfo
            // Checks that the platform will allow the specified type of update.
            appUpdateInfoTask?.addOnSuccessListener { appUpdateInfo ->
                isUpdateAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability
                    .UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                if (isUpdateAvailable && null != listenerPlay)
                    listenerPlay?.onPlayUpdateFound(appUpdateInfo)
            }
        } else {
            configureUpdates()
        }
    }

    private fun configureUpdates() {
        try {
            (activity.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE)
                    as NotificationManager).cancel(SamSprung.request_code)
        } catch (ignored: Exception) { }
        if (activity is UpdateShimActivity) {
            val installer = activity.applicationContext.packageManager.packageInstaller
            for (session: PackageInstaller.SessionInfo in installer.mySessions) {
                try {
                    installer.abandonSession(session.sessionId)
                } catch (ignored: Exception) { }
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
        val download: String = link.substring(link.lastIndexOf(File.separator) + 1)
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

    fun downloadPlayUpdate(appUpdateInfo: AppUpdateInfo) {
        appUpdateManager?.startUpdateFlowForResult(
            // Pass the intent that is returned by 'getAppUpdateInfo()'.
            appUpdateInfo,
            // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
            AppUpdateType.IMMEDIATE,
            // The current activity making the update request.
            activity,
            // Include a request code to later monitor this update request.
            SamSprung.request_code)

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
            isUpdateAvailable = isPreview && BuildConfig.COMMIT != lastCommit
            if (isUpdateAvailable && null != listener)
                listener?.onUpdateFound(downloadUrl)
        } catch (ignored: JSONException) { }
        if (!isPreview && null != lastCommit && null != downloadUrl) {
            RequestGitHubAPI(repo + "preview").setResultListener(
                object : RequestGitHubAPI.ResultListener {
                override fun onResults(result: String) {
                    try {
                        val jsonObject = JSONTokener(result).nextValue() as JSONObject
                        val extraCommit = (jsonObject["name"] as String).substring(offset)
                        isUpdateAvailable = BuildConfig.COMMIT != extraCommit
                                && BuildConfig.COMMIT != lastCommit
                        if (isUpdateAvailable && null != listener)
                            listener?.onUpdateFound(downloadUrl)
                    } catch (ignored: JSONException) { }
                }
            })
        }
    }

    fun retrieveUpdate() {
        val prefs = activity.getSharedPreferences(
            SamSprung.prefsValue, AppCompatActivity.MODE_PRIVATE)
        val isPreview = prefs.getBoolean(SamSprung.prefTester, false)
        RequestGitHubAPI(repo + if (isPreview) "preview" else "sideload")
            .setResultListener(object : RequestGitHubAPI.ResultListener {
            override fun onResults(result: String) {
                parseUpdateJSON(result, isPreview)
            }
        })
    }

    fun hasPendingUpdate(): Boolean {
        return isUpdateAvailable
    }

    fun setUpdateListener(listener: CheckUpdateListener) {
        this.listener = listener
    }

    fun setPlayUpdateListener(listenerPlay: CheckPlayUpdateListener) {
        this.listenerPlay = listenerPlay
    }

    interface CheckUpdateListener {
        fun onUpdateFound(downloadUrl: String)
    }

    interface CheckPlayUpdateListener {
        fun onPlayUpdateFound(appUpdateInfo: AppUpdateInfo)
    }
}