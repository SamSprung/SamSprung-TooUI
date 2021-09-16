package com.sec.android.app.shealth

import android.Manifest
import android.app.KeyguardManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.media.MediaScannerConnection
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import android.widget.AdapterView.OnItemLongClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget.StepCoverAppWidget
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.HashSet


class SettingsActivity : AppCompatActivity() {
    private val hidden = "hidden_packages"

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
        var isGridView = sharedPref.getBoolean("gridview", true)

        findViewById<ToggleButton>(R.id.swapViewType).isChecked = isGridView
        findViewById<ToggleButton>(R.id.swapViewType).setOnCheckedChangeListener { _, isChecked ->
            with (sharedPref.edit()) {
                putBoolean("gridview", isChecked)
                apply()
            }
            isGridView = updateWidgets(isChecked)
        }

        findViewById<Button>(R.id.cacheLogcat).setOnClickListener {
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

        val hidenew: HashSet<String> = HashSet<String>().plus(packageName) as HashSet<String>
        val hide: Set<String> = sharedPref.getStringSet(hidden, setOf<String>()) as Set<String>
        hidenew.addAll(hide)

        val listView: ListView = findViewById(R.id.selectionListView)
        listView.adapter = AppSelectionAdapter(this, packages, hidenew, packageManager)

        listView.onItemLongClickListener = OnItemLongClickListener { _, view, index, _ ->
            val packageName = packages[index].activityInfo.packageName
            val appName = packages[index].loadLabel(packageManager).toString()
            if (hide.contains(packageName)) {
                hidenew.remove(packageName)
                with(sharedPref.edit()) {
                    putStringSet(hidden, hidenew)
                    apply()
                }
                view.findViewById<SwitchCompat>(R.id.hiddenItemSwitch).isChecked = true
                Toast.makeText(this, getString(
                    R.string.show_package, appName), Toast.LENGTH_SHORT).show()
            } else {
                hidenew.add(packageName)
                with(sharedPref.edit()) {
                    putStringSet(hidden, hidenew)
                    apply()
                }
                view.findViewById<SwitchCompat>(R.id.hiddenItemSwitch).isChecked = false
                Toast.makeText(this, getString(
                    R.string.hide_package, appName), Toast.LENGTH_SHORT).show()
            }
            updateWidgets(isGridView)
            true
        }
    }

    /**
     * @return true if pass or pin or pattern locks screen
     */
    private fun isDeviceLocked(): Boolean {
        return (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure
    }

    private fun updateWidgets(isChecked: Boolean) : Boolean {
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
        return isChecked
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
            arrayOf(file.absolutePath), null, null
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
            cacheLogcat()
        }
    }
}