package com.sec.android.app.shealth

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged

class MainActivity : AppCompatActivity() {

    private val launchPackage = "org.thoughtcrime.securesms"
    private val launchActivity = "$launchPackage.RoutingActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val intent = intent
//        if (intent != null) {
//            val launchPackage = intent.getStringExtra("launchPackage")
//            val launchActivity = intent.getStringExtra("launchActivity")
//            if (launchPackage != null && launchActivity != null) {
//                val launchIntent = Intent(Intent.ACTION_MAIN)
//                launchIntent.addCategory(Intent.CATEGORY_LAUNCHER)
//                launchIntent.component = ComponentName(launchPackage, launchActivity)
//                val options = ActivityOptions.makeBasic().setLaunchDisplayId(1)
//                launchIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
//                startActivity(launchIntent, options.toBundle())
//            }
//            finish()
//        }

        val sharedPref = getSharedPreferences("com.zflip.launcher.PREFS", Context.MODE_PRIVATE)

        findViewById<EditText>(R.id.launchPackage).setText(sharedPref.getString("launchPackage", launchPackage) ?: launchPackage)
        findViewById<EditText>(R.id.launchActivity).setText(sharedPref.getString("launchActivity", launchActivity) ?: launchActivity)

        findViewById<EditText>(R.id.launchPackage).doOnTextChanged { text, start, before, count ->
            with (sharedPref.edit()) {
                putString("launchPackage", text.toString())
                apply()
            }
        }
        findViewById<EditText>(R.id.launchActivity).doOnTextChanged { text, start, before, count ->
            with (sharedPref.edit()) {
                putString("launchActivity", text.toString())
                apply()
            }
        }

        findViewById<Button>(R.id.launchIntent).setOnClickListener {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.component = ComponentName(
                findViewById<EditText>(R.id.launchPackage).text.toString(),
                findViewById<EditText>(R.id.launchActivity).text.toString()
            )
            val options = ActivityOptions.makeBasic().setLaunchDisplayId(1)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent, options.toBundle())
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (event.keyCode == KeyEvent.KEYCODE_POWER) {
            // do what you want with the power button
            true
        } else super.onKeyDown(keyCode, event)
    }
}