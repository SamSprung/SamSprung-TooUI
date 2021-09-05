package com.sec.android.app.shealth

import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        finishAndRemoveTask()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (event.keyCode == KeyEvent.KEYCODE_POWER) {
            // do what you want with the power button
            true
        } else super.onKeyDown(keyCode, event)
    }
}