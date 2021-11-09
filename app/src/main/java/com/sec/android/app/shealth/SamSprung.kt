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

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Process
import java.lang.ref.WeakReference
import kotlin.system.exitProcess

class SamSprung : Application() {
    override fun onCreate() {
        super.onCreate()
        mContext = WeakReference(this)
        mPrefs = WeakReference(
            getSharedPreferences("samsprung.launcher.PREFS", MODE_PRIVATE)
        )
        Thread.setDefaultUncaughtExceptionHandler { _: Thread?, error: Throwable ->
            error.printStackTrace()
            // Unrecoverable error encountered
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }
        if (prefs.contains("screenoff")) {
            with(prefs.edit()) {
                putBoolean(prefScreen, prefs.getBoolean("screenoff", true))
                remove("screenoff")
                apply()
            }
        }
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
    }

    companion object {
        const val provider: String = "com.sec.android.samsprung.provider";
        private lateinit var mContext: WeakReference<Context>
        var isKeyguardLocked: Boolean = true
        private lateinit var mPrefs: WeakReference<SharedPreferences>
        val context: Context get() = mContext.get()!!
        val prefs: SharedPreferences get() = mPrefs.get()!!
        var notices: HashSet<String> = hashSetOf()
        const val prefScreen: String = "prefScreen"
        const val prefLayout: String = "prefLayout"
        const val prefHidden: String = "prefHidden"
        const val autoRotate: String = "autoRotate"
        const val useAppLauncherActivity: Boolean = false
    }
}