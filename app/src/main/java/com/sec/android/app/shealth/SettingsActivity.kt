package com.sec.android.app.shealth

import android.app.KeyguardManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Button
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget.StepCoverAppWidget


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

        findViewById<Button>(R.id.testLauncher).setOnClickListener {
            displaySecondaryLaunchers(this)
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
    }

    /**
     * @return true if pass or pin or pattern locks screen
     */
    private fun isDeviceLocked(): Boolean {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return keyguardManager.isDeviceSecure
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (event.keyCode == KeyEvent.KEYCODE_POWER) {
            finishAndRemoveTask()
            true
        } else super.onKeyDown(keyCode, event)
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
        finishAndRemoveTask()
    }

    private fun displaySecondaryLaunchers(context: Context) {
        val packageManager: PackageManager = context.packageManager
        val componentName = ComponentName(context, SecondHomePicker::class.java)
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        val selector = Intent(Intent.ACTION_MAIN)
        selector.addCategory(Intent.CATEGORY_SECONDARY_HOME)
        selector.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(selector)
    }
}