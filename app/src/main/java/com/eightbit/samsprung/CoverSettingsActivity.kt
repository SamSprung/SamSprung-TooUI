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

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
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
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.URL
import java.util.*
import kotlin.collections.HashSet


class CoverSettingsActivity : AppCompatActivity() {
    companion object {
        const val GENERAL = 9000
        const val LOGCAT = 9001
    }

    private lateinit var switch: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.step_widget_edit)

        val files: Array<File>? = filesDir.listFiles { _, name ->
            name.lowercase(Locale.getDefault()).endsWith(".apk") }
        if (null != files) {
            for (file in files) {
                if (!file.isDirectory) file.delete()
            }
        }
        val permission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), GENERAL)
        }
        if (BuildConfig.FLAVOR != "google") {
            if (packageManager.canRequestPackageInstalls()) {
                retrieveUpdate()
            } else {
                registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    if (packageManager.canRequestPackageInstalls())
                        retrieveUpdate()
                }.launch(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(
                        Uri.parse(String.format("package:%s", packageName))
                    )
                )
            }
        }

        if (isDeviceSecure()) {
            Toast.makeText(
                applicationContext,
                R.string.caveats_warning,
                Toast.LENGTH_LONG
            ).show()
        }

        val enableScreenOff = SamSprung.prefs.getBoolean(SamSprung.prefScreen, false)

        findViewById<ToggleButton>(R.id.swapScreenOff).isChecked = enableScreenOff
        findViewById<ToggleButton>(R.id.swapScreenOff).setOnCheckedChangeListener { _, isChecked ->
            with (SamSprung.prefs.edit()) {
                putBoolean(SamSprung.prefScreen, isChecked)
                apply()
            }
        }

        val isGridView = SamSprung.prefs.getBoolean(SamSprung.prefLayout, true)

        findViewById<ToggleButton>(R.id.swapViewType).isChecked = isGridView
        findViewById<ToggleButton>(R.id.swapViewType).setOnCheckedChangeListener { _, isChecked ->
            with (SamSprung.prefs.edit()) {
                putBoolean(SamSprung.prefLayout, isChecked)
                apply()
            }
        }

        findViewById<Button>(R.id.cacheLogcat).setOnClickListener {
            findViewById<ScrollView>(R.id.logWrapper).visibility = View.VISIBLE
            findViewById<TextView>(R.id.printLogcat).text = captureLogcat()

            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), LOGCAT)
            } else {
                printLogcat()
            }
        }

        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        mainIntent.removeCategory(Intent.CATEGORY_HOME)
        val packages = packageManager.queryIntentActivities(mainIntent, 0)
        Collections.sort(packages, ResolveInfo.DisplayNameComparator(packageManager))

        val unlisted: HashSet<String> = HashSet()
        val hide: Set<String> = SamSprung.prefs.getStringSet(
            SamSprung.prefHidden, setOf<String>()) as Set<String>
        unlisted.addAll(hide)

        val listView: ListView = findViewById(R.id.selectionListView)
        listView.adapter = FilteredAppsAdapter(this, packages, unlisted)

        if (Settings.canDrawOverlays(applicationContext) && isAccessibilityEnabled()) {
            IntentFilter(Intent.ACTION_SCREEN_ON).also {
                applicationContext.registerReceiver(OffBroadcastReceiver(), it)
            }
        }
    }

    private val noticeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        // End of permission approval process
    }

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (!isNotificationListenerEnabled()) {
            noticeLauncher.launch(Intent(
                Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            ))
        }
    }

    private val accessibilityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (isAccessibilityEnabled())
            switch.isChecked = true
        if (!Settings.System.canWrite(applicationContext)) {
            settingsLauncher.launch(Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:$packageName")
            ))
        } else if (!isNotificationListenerEnabled()) {
            noticeLauncher.launch(Intent(
                Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            ))
        }
    }

    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (isAccessibilityEnabled()) {
            if (Settings.canDrawOverlays(applicationContext)) {
                switch.isChecked = true
                IntentFilter(Intent.ACTION_SCREEN_ON).also {
                    applicationContext.registerReceiver(OffBroadcastReceiver(), it)
                }
            }
            if (!Settings.System.canWrite(applicationContext)) {
                settingsLauncher.launch(Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:$packageName")
                ))
            } else if (!isNotificationListenerEnabled()) {
                noticeLauncher.launch(Intent(
                    Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                ))
            }
        } else {
            switch.isChecked = false
            accessibilityLauncher.launch(Intent(
                Settings.ACTION_ACCESSIBILITY_SETTINGS,
            ))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_menu, menu)
        val itemswitch: MenuItem = menu.findItem(R.id.switch_action_bar)
        itemswitch.setActionView(R.layout.permission_switch)
        switch = menu.findItem(R.id.switch_action_bar).actionView
            .findViewById(R.id.switch2) as SwitchCompat
        switch.isChecked = Settings.canDrawOverlays(applicationContext) && isAccessibilityEnabled()
        switch.setOnClickListener {
            if (switch.isChecked) {
                if (Settings.canDrawOverlays(applicationContext)) {
                    if (isAccessibilityEnabled()) {
                        switch.isChecked = true
                        if (Settings.System.canWrite(applicationContext)) {
                            if (isNotificationListenerEnabled()) {
                                Toast.makeText(
                                    applicationContext,
                                    R.string.settings_notice,
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                noticeLauncher.launch(Intent(
                                    Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                                ))
                            }
                        } else {
                            settingsLauncher.launch(Intent(
                                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                Uri.parse("package:$packageName")
                            ))
                        }
                    } else {
                        switch.isChecked = false
                        accessibilityLauncher.launch(Intent(
                            Settings.ACTION_ACCESSIBILITY_SETTINGS
                        ))
                    }
                } else {
                    switch.isChecked = false
                    overlayLauncher.launch(Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    ))
                }
            }
        }
        return true
    }

    /**
     * @return true if pass or pin or pattern locks screen
     */
    private fun isDeviceSecure(): Boolean {
        return (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure
    }

    /**
     * https://stackoverflow.com/a/14923144/461982
     */
    private fun isAccessibilityEnabled(): Boolean {
        val enabledServices = (getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager)
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_VISUAL)
        for (enabledService in enabledServices) {
            val enabledServiceInfo: ServiceInfo = enabledService.resolveInfo.serviceInfo
            if (enabledServiceInfo.packageName.equals(packageName)
                && enabledServiceInfo.name.equals(NotificationAccessibility::class.java.name)
            ) return true
        }
        return false
    }

    /**
     * https://github.com/kpbird/NotificationListenerService-Example
     */
    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        )
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (i in names.indices) {
                val cn = ComponentName.unflattenFromString(names[i])
                if (null != cn && TextUtils.equals(packageName, cn.packageName)) return true
            }
        }
        return false
    }

    private fun captureLogcat(): String {
        val log = StringBuilder()
        val separator = System.getProperty("line.separator")
        try {
            var line: String?
            val mLogcatProc: Process = Runtime.getRuntime().exec(arrayOf(
                "logcat", "-d",
                "com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget",
                "-t", "512"
            ))
            val reader = BufferedReader(InputStreamReader(mLogcatProc.inputStream))
            log.append(separator)
            log.append("SamSprung Widget Logs")
            log.append(separator)
            log.append(separator)
            while (reader.readLine().also { line = it } != null) {
                log.append(line)
                log.append(separator)
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return log.toString()
    }

    private fun printLogcat() {
        val logcat = findViewById<TextView>(R.id.printLogcat).text.toString()
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "samsprung_logcat")
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?.let {
            contentResolver.openOutputStream(it).use { fos ->
                fos?.write(logcat.toByteArray())
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun installUpdate(apkUri: Uri) = withContext(Dispatchers.IO) {
        val installer = applicationContext.packageManager.packageInstaller
        val resolver = applicationContext.contentResolver
        resolver.openInputStream(apkUri)?.use { apkStream ->
            val length = DocumentFile.fromSingleUri(applicationContext, apkUri)?.length() ?: -1
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val sessionId = installer.createSession(params)
            val session = installer.openSession(sessionId)
            session.openWrite("NAME", 0, length).use { sessionStream ->
                apkStream.copyTo(sessionStream)
                session.fsync(sessionStream)
            }
            val intent = Intent(applicationContext, GitBroadcastReceiver::class.java)
            intent.identifier = "8675309"
            intent.action = SamSprung.updating
            val pi = PendingIntent.getBroadcast(
                applicationContext, SamSprung.request_code, intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                else PendingIntent.FLAG_UPDATE_CURRENT
            )
            session.commit(pi.intentSender)
            session.close()
        }
    }

    private fun downloadUpdate(link: String) {
        val download: String = link.substring(link.lastIndexOf('/') + 1)
        val apk = File(filesDir, download)
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            URL(link).openStream().use { input ->
                FileOutputStream(apk).use { output ->
                    input.copyTo(output)
                    CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main) {
                        installUpdate(
                            FileProvider.getUriForFile(
                                applicationContext, SamSprung.provider, apk
                        ))
                    }
                }
            }
        }
    }

    private fun retrieveUpdate() {
        RequestLatestCommit(getString(R.string.git_url)).setResultListener(
            object : RequestLatestCommit.ResultListener {
            override fun onResults(result: String) {
                try {
                    val jsonObject = JSONTokener(result).nextValue() as JSONObject
                    val lastCommit = (jsonObject["name"] as String).substring(10)
                    if (BuildConfig.COMMIT != lastCommit) {
                        val assets = (jsonObject["assets"] as JSONArray)[0] as JSONObject
                        downloadUpdate(assets["browser_download_url"] as String)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        )
        if (permission == PackageManager.PERMISSION_GRANTED && LOGCAT == requestCode)
            printLogcat()
    }
}