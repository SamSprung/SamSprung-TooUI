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

package com.eightbit.samsprung

import android.app.Application
import android.app.KeyguardManager
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
        const val provider: String = "com.eightbit.samsprung.provider"
        const val updating: String = "com.eightbit.samsprung.UPDATING"
        const val services: String = "com.eightbit.samsprung.SERVICES"
        const val launcher: String = "com.eightbit.samsprung.LAUNCHER"

        const val request_code = 8675309
        const val notification = request_code.toString()

        const val prefsValue: String = "samsprung.preferences"
        const val prefLayout: String = "prefLayout"
        const val prefHidden: String = "prefHidden"
        const val prefColors: String = "prefColors"
        const val prefAlphas: String = "prefAlphas"
        const val prefWarned: String = "prefWarned"
        const val prefViewer: String = "prefViewer"
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
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        Thread.setDefaultUncaughtExceptionHandler { _: Thread?, error: Throwable ->
            val exception = StringWriter()
            error.printStackTrace(PrintWriter(exception))
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
