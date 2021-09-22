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

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.window.java.layout.WindowInfoRepositoryCallbackAdapter
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoRepository.Companion.windowInfoRepository
import androidx.window.layout.WindowLayoutInfo
import java.util.concurrent.Executor


class AppLauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val launchPackage = intent.getStringExtra("launchPackage")
        val launchActivity = intent.getStringExtra("launchActivity")

        if (launchPackage == null || launchActivity == null) finish()

        WindowInfoRepositoryCallbackAdapter(
            windowInfoRepository()
        ).addWindowLayoutInfoListener(runOnUiThreadExecutor(),
            { windowLayoutInfo: WindowLayoutInfo ->
            for (displayFeature in windowLayoutInfo.displayFeatures) {
                if (displayFeature is FoldingFeature) {
                    if (displayFeature.state == FoldingFeature.State.HALF_OPENED ||
                        displayFeature.state == FoldingFeature.State.FLAT) {
                        val displayIntent = Intent(Intent.ACTION_MAIN)
                        displayIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                        displayIntent.component = ComponentName(launchPackage!!, launchActivity!!)
                        val launchDisplay = ActivityOptions.makeBasic().setLaunchDisplayId(0)
                        displayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        displayIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        displayIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                        displayIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        startActivity(displayIntent, launchDisplay.toBundle())
                    }
                }
            }
        })

        val launchIntent = Intent(Intent.ACTION_MAIN)
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        launchIntent.component = ComponentName(launchPackage!!, launchActivity!!)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(1)
        try {
            val applicationInfo: ApplicationInfo = packageManager.getApplicationInfo (
                launchPackage, PackageManager.GET_META_DATA
            )
            applicationInfo.metaData.putString(
                "com.samsung.android.activity.showWhenLocked", "true"
            )
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
        startActivity(launchIntent, options.toBundle())
        finish()
    }

    private fun runOnUiThreadExecutor(): Executor {
        val handler = Handler(Looper.getMainLooper())
        return Executor() {
            handler.post(it)
        }
    }
}