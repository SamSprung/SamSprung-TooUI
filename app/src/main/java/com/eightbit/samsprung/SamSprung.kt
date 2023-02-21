/*
 * ====================================================================
 * Copyright (c) 2021-2023 AbandonedCart.  All rights reserved.
 *
 * See https://github.com/SamSprung/.github/blob/main/LICENSE#L5
 * ====================================================================
 *
 * The license and distribution terms for any publicly available version or
 * derivative of this code cannot be changed.  i.e. this code cannot simply be
 * copied and put under another distribution license
 * [including the GNU Public License.] Content not subject to these terms is
 * subject to to the terms and conditions of the Apache License, Version 2.0.
 */

package com.eightbit.samsprung

import android.app.Application
import android.app.KeyguardManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.eightbit.io.Debug
import com.eightbit.samsprung.drawer.OrientationManager
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class SamSprung : Application() {

    fun setThemePreference() {
        when (getSharedPreferences(prefsValue, MODE_PRIVATE).getInt(prefThemes, 0)) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    companion object {
        const val provider: String = "${BuildConfig.APPLICATION_ID}.provider"
        const val updating: String = "${BuildConfig.APPLICATION_ID}.UPDATING"
        const val services: String = "${BuildConfig.APPLICATION_ID}.SERVICES"
        const val launcher: String = "${BuildConfig.APPLICATION_ID}.LAUNCHER"

        const val request_code = 8675309
        const val notification = request_code.toString()

        const val prefsValue: String = "samsprung.preferences"
        const val prefLayout: String = "prefLayout"
        const val prefHidden: String = "prefHidden"
        const val prefColors: String = "prefColors"
        const val prefAlphas: String = "prefAlphas"
        const val prefWarned: String = "prefWarned"
        const val prefViewer: String = "prefViewer"
        const val prefCloser: String = "prefCloser"
        const val prefSlider: String = "prefSlider"
        const val prefShifts: String = "prefShifts"
        const val prefThemes: String = "prefThemes"
        const val prefDelays: String = "prefDelays"
        const val prefSnooze: String = "prefSnooze"
        const val prefReacts: String = "prefReacts"
        const val prefSearch: String = "prefSearch"
        const val prefRadius: String = "prefRadius"
        const val prefLength: String = "prefLength"
        const val prefRotate: String = "prefRotate"
        const val prefCarded: String = "prefCarded"
        const val prefUpdate: String = "prefUpdate"

        var hasSubscription = false
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        Thread.setDefaultUncaughtExceptionHandler { _: Thread?, error: Throwable ->
            val exception = StringWriter()
            error.printStackTrace(PrintWriter(exception))
            Toast.makeText(this, R.string.logcat_crash, Toast.LENGTH_SHORT).show()
            Debug(this).processException(
                (getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure,
                exception.toString()
            )
            // Unrecoverable error encountered
            try {
                OrientationManager(this).removeOrientationLayout()
            } catch (ignored: Exception) { }
            exitProcess(0)
        }

        setThemePreference()
    }
}
