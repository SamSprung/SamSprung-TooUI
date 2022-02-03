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

import android.app.Application
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.StrictMode
import android.view.ContextThemeWrapper
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import com.eightbit.content.ScaledContext
import java.lang.ref.SoftReference
import kotlin.system.exitProcess

class SamSprung : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        if (BuildConfig.FLAVOR == "google"
            && BuildConfig.DEBUG) StrictMode.enableDefaults()

        mContext = SoftReference<Context>(buildDisplayContext(1))

        Thread.setDefaultUncaughtExceptionHandler { _: Thread?, error: Throwable ->
            error.printStackTrace()
            startService(
                Intent(this, OnBroadcastService::class.java).setAction(updating)
            )
            // Unrecoverable error encountered
            exitProcess(1)
        }
        val prefs = getSharedPreferences(prefsValue, MODE_PRIVATE)
        if (prefs.contains("gridview")) {
            with(prefs.edit()) {
                putBoolean(prefLayout, prefs.getBoolean("gridview", true))
                remove("gridview")
                apply()
            }
        }
        if (prefs.contains("hidden_packages")) {
            with(prefs.edit()) {
                putStringSet(prefHidden, prefs.getStringSet("hidden_packages", setOf<String>()))
                remove("hidden_packages")
                apply()
            }
        }
        if (prefs.contains(autoRotate)) {
            with(prefs.edit()) {
                remove(autoRotate)
                apply()
            }
        }
    }

    private fun buildDisplayContext(display: Int): Context {
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displayContext = createDisplayContext(displayManager.getDisplay(display))
        val wm = displayContext.getSystemService(WINDOW_SERVICE) as WindowManager
        return object : ContextThemeWrapper(displayContext, R.style.Theme_SecondScreen) {
            override fun getSystemService(name: String): Any? {
                return if (WINDOW_SERVICE == name) wm else super.getSystemService(name)
            }
        }
    }

    companion object {
        const val provider: String = "com.eightbit.samsprung.provider"
        const val updating: String = "com.eightbit.samsprung.UPDATING"
        const val services: String = "com.eightbit.samsprung.SERVICES"
        const val request_code = 8675309
        var isKeyguardLocked: Boolean = true
        const val prefsValue: String = "samsprung.preferences"
        const val prefLayout: String = "prefLayout"
        const val prefHidden: String = "prefHidden"
        const val autoRotate: String = "autoRotate"
        const val prefSecure: String = "prefSecure"
        const val prefColors: String = "prefColors"
        const val prefAlphas: String = "prefAlphas"
        const val prefWarned: String = "prefWarned"
        const val prefTester: String = "prefTester"

        var mContext: SoftReference<Context>? = null
        fun getCoverContext(): Context? {
            if (null != mContext && null != mContext!!.get()) {
                return ScaledContext.screen(mContext!!.get())
            }
            return null
        }
    }
}