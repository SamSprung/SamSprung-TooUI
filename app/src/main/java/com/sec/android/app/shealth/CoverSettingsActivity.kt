package com.sec.android.app.shealth

/* ====================================================================
 * Copyright (c) 2012-2021 AbandonedCart.  All rights reserved.
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
import android.app.KeyguardManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget.StepCoverAppWidget
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.HashSet


class CoverSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.step_widget_edit)

        if (isDeviceSecure()) {
            Toast.makeText(
                applicationContext,
                R.string.caveats_warning,
                Toast.LENGTH_LONG
            ).show()
        }

        val settingsLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            // End of permission approval process
        }

        val overlayLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (!Settings.System.canWrite(applicationContext)) {
                settingsLauncher.launch(Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:$packageName")
                ))
            }
        }

        val noticeLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (!Settings.canDrawOverlays(applicationContext)) {
                overlayLauncher.launch(Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ))
            } else if (!Settings.System.canWrite(applicationContext)) {
                settingsLauncher.launch(Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:$packageName")
                ))
            }
        }

        findViewById<Button>(R.id.openSettings).setOnClickListener {
            if (isNotificationListenerEnabled()) {
                if (Settings.canDrawOverlays(applicationContext)) {
                    if (Settings.System.canWrite(applicationContext)) {
                        Toast.makeText(
                            applicationContext,
                            R.string.settings_notice,
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        settingsLauncher.launch(Intent(
                            Settings.ACTION_MANAGE_WRITE_SETTINGS,
                            Uri.parse("package:$packageName")
                        ))
                    }
                } else {
                    overlayLauncher.launch(Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    ))
                }
            } else {
                noticeLauncher.launch(Intent(
                    Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                ))
            }
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
            sendAppWidgetUpdateBroadcast(isChecked)
        }

        findViewById<Button>(R.id.cacheLogcat).setOnClickListener {
            findViewById<ScrollView>(R.id.logWrapper).visibility = View.VISIBLE
            findViewById<TextView>(R.id.printLogcat).text = printLogcat()

            val permission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 9001)
            } else {
                cacheLogcat()
            }
        }

        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        mainIntent.removeCategory(Intent.CATEGORY_HOME)
        val packages = packageManager.queryIntentActivities(mainIntent, 0)
        packages.removeIf { item -> item.activityInfo.packageName == packageName }
        Collections.sort(packages, ResolveInfo.DisplayNameComparator(packageManager))

        val unlisted: HashSet<String> = HashSet<String>().plus(packageName) as HashSet<String>
        val hide: Set<String> = SamSprung.prefs.getStringSet(
            SamSprung.prefHidden, setOf<String>()) as Set<String>
        unlisted.addAll(hide)

        val listView: ListView = findViewById(R.id.selectionListView)
        listView.adapter = AppSelectionAdapter(this, packages, unlisted)
    }

    /**
     * @return true if pass or pin or pattern locks screen
     */
    private fun isDeviceSecure(): Boolean {
        return (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure
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
                if (cn != null && TextUtils.equals(packageName, cn.packageName)) return true
            }
        }
        return false
    }

    private fun sendAppWidgetUpdateBroadcast(isGridView: Boolean) {
        val widgetManager = AppWidgetManager.getInstance(applicationContext)
        val ids = widgetManager.getAppWidgetIds(
            ComponentName(applicationContext, StepCoverAppWidget::class.java))
        widgetManager.notifyAppWidgetViewDataChanged(ids,
            if (isGridView) R.id.widgetGridView else R.id.widgetListView
        )

        val updateIntent = Intent(applicationContext, StepCoverAppWidget::class.java)
        updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
            AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(
                ComponentName(applicationContext, StepCoverAppWidget::class.java))
        )
        applicationContext.sendBroadcast(updateIntent)
    }

    private fun printLogcat(): String {
        val log = StringBuilder()
        val separator = System.getProperty("line.separator")
        try {
            var line: String?
            val mLogcatProc: Process = Runtime.getRuntime().exec(arrayOf(
                "logcat", "-d",
                "com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget",
                "-t", "2048"
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

    private fun cacheLogcat() {
        val logcat = findViewById<TextView>(R.id.printLogcat).text.toString()
        val file = File(externalCacheDir, "samsprung_logcat.txt")
        FileOutputStream(file).use { fos ->
            fos.write(logcat.toByteArray())
        }
        MediaScannerConnection.scanFile(
            applicationContext,
            arrayOf(file.absolutePath),
            arrayOf("text/plain"), null
        )
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
        if (permission == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == 9001)
                cacheLogcat()
        }
    }
}