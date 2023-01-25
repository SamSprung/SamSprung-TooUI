/*
 * ====================================================================
 * Copyright (c) 2021-2022 AbandonedCart.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * For the purpose of this license, the phrase "SamSprung labels" shall
 * be used to refer to the labels "8-bit Dream", "TwistedUmbrella",
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

package com.eightbit.samsprung.settings

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
import com.eightbit.net.RequestGitHubAPI
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

class UpdateManager(private var activity: Activity) {

    private val repo = "https://api.github.com/repos/SamSprung/SamSprung-TooUI/releases/tags/"
    var listener: CheckUpdateListener? = null
    var listenerPlay: CheckPlayUpdateListener? = null
    private var appUpdateManager: AppUpdateManager? = null
    private var isUpdateAvailable = false

    private val scopeIO = CoroutineScope(Dispatchers.IO)

    init {
        if (BuildConfig.GOOGLE_PLAY) configureManager() else configureUpdates()
    }

    private fun configureManager() {
        if (null == appUpdateManager) appUpdateManager = AppUpdateManagerFactory.create(activity)
        val appUpdateInfoTask = appUpdateManager?.appUpdateInfo
        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask?.addOnSuccessListener { appUpdateInfo ->
            isUpdateAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability
                .UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            if (isUpdateAvailable && null != listenerPlay)
                listenerPlay?.onPlayUpdateFound(appUpdateInfo)
        }
    }

    private fun configureUpdates() {
        try {
            (activity.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE)
                    as NotificationManager).cancel(SamSprung.request_code)
        } catch (ignored: Exception) { }
        if (activity is UpdateShimActivity) {
            val installer = activity.applicationContext.packageManager.packageInstaller
            installer.mySessions.forEach {
                try {
                    installer.abandonSession(it.sessionId)
                } catch (ignored: Exception) { }
            }
        } else {
            scopeIO.launch {
                val files: Array<File>? = activity.externalCacheDir?.listFiles { _, name ->
                    name.lowercase(Locale.getDefault()).endsWith(".apk")
                }
                files?.forEach { if (!it.isDirectory) it.delete() }
            }
            retrieveUpdate()
        }
    }

    private fun installUpdate(apkUri: Uri) {
        scopeIO.launch {
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
    }

    fun downloadUpdate(link: String) {
        if (activity.packageManager.canRequestPackageInstalls()) {
            val download: String = link.substring(link.lastIndexOf(File.separator) + 1)
            val apk = File(activity.externalCacheDir, download)
            scopeIO.launch(Dispatchers.IO) {
                URL(link).openStream().use { input ->
                    FileOutputStream(apk).use { output ->
                        input.copyTo(output)
                        installUpdate(FileProvider.getUriForFile(
                            activity.applicationContext, SamSprung.provider, apk
                        ))
                    }
                }
            }
        } else if (activity is CoverPreferences) {
            (activity as CoverPreferences).updateLauncher.launch(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(
                    Uri.parse(String.format("package:%s", activity.packageName))))
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

    private fun parseUpdateJSON(result: String) {
        val offset = activity.getString(R.string.samsprung).length + 1
        try {
            val jsonObject = JSONTokener(result).nextValue() as JSONObject
            val lastCommit = (jsonObject["name"] as String).substring(offset)
            val assets = jsonObject["assets"] as JSONArray
            val asset = assets[0] as JSONObject
            val downloadUrl = asset["browser_download_url"] as String
            isUpdateAvailable = BuildConfig.COMMIT != lastCommit
            if (isUpdateAvailable && null != listener)
                listener?.onUpdateFound(downloadUrl)
        } catch (ignored: JSONException) { }
    }

    fun retrieveUpdate() {
        RequestGitHubAPI(repo + "sideload")
            .setResultListener(object : RequestGitHubAPI.ResultListener {
            override fun onResults(result: String) {
                parseUpdateJSON(result)
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
