package com.sec.android.app.shealth

import android.Manifest
import android.app.KeyguardManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Button
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget.StepCoverAppWidget
import java.io.*


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.step_widget_edit)

        if (isDeviceLocked()) {
            Toast.makeText(
                applicationContext,
                R.string.caveats_warning,
                Toast.LENGTH_LONG
            ).show()
        }

        findViewById<Button>(R.id.openSettings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }

        val sharedPref = getSharedPreferences(
            "samsprung.launcher.PREFS", Context.MODE_PRIVATE
        )

        findViewById<ToggleButton>(R.id.swapViewType).isChecked =
            sharedPref.getBoolean("gridview", true)
        findViewById<ToggleButton>(R.id.swapViewType).setOnCheckedChangeListener { _, isChecked ->
            with (sharedPref.edit()) {
                putBoolean("gridview", isChecked)
                apply()
            }
            updateWidgets(isChecked)
        }

        findViewById<Button>(R.id.printLogcat).setOnClickListener {
            val permission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 9001)
            } else {
                dumpLogcat()
            }
        }
    }

    /**
     * @return true if pass or pin or pattern locks screen
     */
    private fun isDeviceLocked(): Boolean {
        return (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure
    }

    private fun updateWidgets(isChecked: Boolean) {
        val widgetIntent = Intent(applicationContext, StepCoverAppWidget::class.java)
        widgetIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val widgetManager = AppWidgetManager.getInstance(applicationContext)
        val ids = widgetManager.getAppWidgetIds(
            ComponentName(applicationContext, StepCoverAppWidget::class.java)
        )
        if (isChecked) {
            widgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widgetGridView)
        } else {
            widgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widgetListView)
        }
        widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        sendBroadcast(widgetIntent)
    }

    @Throws(Exception::class)
    fun dumpLogcat() {
        val file = File(externalCacheDir, "samsprung_logcat.txt")
        var mLogcatProc: Process
        var reader: BufferedReader
        val log = StringBuilder()
        val separator = System.getProperty("line.separator")
        log.append(Build.MANUFACTURER)
        log.append(" ")
        log.append(Build.MODEL)
        log.append(separator)
        log.append("Android SDK ")
        log.append(Build.VERSION.SDK_INT)
        log.append(" (")
        log.append(Build.VERSION.RELEASE)
        log.append(")")
        try {
            var line: String?
            mLogcatProc = Runtime.getRuntime().exec(arrayOf("logcat", "-ds", "AndroidRuntime:E"))
            reader = BufferedReader(
                InputStreamReader(
                    mLogcatProc.inputStream
                )
            )
            log.append(separator)
            log.append(separator)
            log.append("AndroidRuntime Logs")
            log.append(separator)
            log.append(separator)
            while (reader.readLine().also { line = it } != null) {
                log.append(line)
                log.append(separator)
            }
            reader.close()
            mLogcatProc =
                Runtime.getRuntime().exec(arrayOf("logcat", "-d", BuildConfig.APPLICATION_ID))
            reader = BufferedReader(
                InputStreamReader(
                    mLogcatProc.inputStream
                )
            )
            log.append(separator)
            log.append(separator)
            log.append("SamSprung Default Logs")
            log.append(separator)
            log.append(separator)
            while (reader.readLine().also { line = it } != null) {
                log.append(line)
                log.append(separator)
            }
            reader.close()
            mLogcatProc =
                Runtime.getRuntime().exec(arrayOf("logcat", "-d",
                    "com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget"))
            reader = BufferedReader(
                InputStreamReader(
                    mLogcatProc.inputStream
                )
            )
            log.append(separator)
            log.append(separator)
            log.append("SamSprung Widget Logs")
            log.append(separator)
            log.append(separator)
            while (reader.readLine().also { line = it } != null) {
                log.append(line)
                log.append(separator)
            }
            reader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        FileOutputStream(file).use { fos -> fos.write(log.toString().toByteArray()) }
        try {
            MediaScannerConnection.scanFile(this,
                arrayOf(file.absolutePath), null, null)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
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
            dumpLogcat()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (event.keyCode == KeyEvent.KEYCODE_POWER) {
            finishAndRemoveTask()
            true
        } else super.onKeyDown(keyCode, event)
    }
}