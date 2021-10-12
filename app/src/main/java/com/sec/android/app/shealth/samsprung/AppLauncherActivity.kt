package com.sec.android.app.shealth.samsprung

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
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Consumer
import androidx.window.java.layout.WindowInfoRepositoryCallbackAdapter
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoRepository.Companion.windowInfoRepository
import androidx.window.layout.WindowLayoutInfo
import com.sec.android.app.shealth.SamSprung
import java.util.concurrent.Executor


class AppLauncherActivity : AppCompatActivity() {
    private lateinit var windowWasher : Consumer<WindowLayoutInfo>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val launchPackage = intent.getStringExtra("launchPackage")
        val launchActivity = intent.getStringExtra("launchActivity")

        if (launchPackage == null || launchActivity == null) finish()

        if (SamSprung.prefs.getBoolean(SamSprung.autoRotate, true)) {
            Settings.System.putInt(
                SamSprung.context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION, 0
            )
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND

        val wIRCA = WindowInfoRepositoryCallbackAdapter(windowInfoRepository())
        windowWasher = Consumer<WindowLayoutInfo> { windowLayoutInfo ->
            for (displayFeature in windowLayoutInfo.displayFeatures) {
                if (displayFeature is FoldingFeature) {
                    if (displayFeature.state == FoldingFeature.State.HALF_OPENED ||
                        displayFeature.state == FoldingFeature.State.FLAT
                    ) {
                        startService(Intent(applicationContext,
                            DisplayListenerService::class.java))

                        val windowIntent = Intent(Intent.ACTION_MAIN)
                        windowIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                        windowIntent.component = ComponentName(launchPackage!!, launchActivity!!)
                        val options = ActivityOptions.makeBasic().setLaunchDisplayId(0)
                        windowIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        windowIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        windowIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                        windowIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        startActivity(windowIntent, options.toBundle())

                        fakeOrientationLock(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)

                        wIRCA.removeWindowLayoutInfoListener(windowWasher)

                        finish()
                    }
                }
            }
        }
        wIRCA.addWindowLayoutInfoListener(runOnUiThreadExecutor(), windowWasher)

        val overlayLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(SamSprung.context))  {
                launchWidgetActivity(launchActivity, launchPackage)
                fakeOrientationLock(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT)
            }
        }
        if (Settings.canDrawOverlays(SamSprung.context)) {
            launchWidgetActivity(launchActivity, launchPackage)
            fakeOrientationLock(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT)
        } else {
            overlayLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
        }
    }

    private fun runOnUiThreadExecutor(): Executor {
        val handler = Handler(Looper.getMainLooper())
        return Executor { handler.post(it) }
    }

    private fun fakeOrientationLock(screenOrientation: Int) {
        val orientationChanger = LinearLayout(this)
        @Suppress("DEPRECATION")
        val orientationLayout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
            0, PixelFormat.RGBA_8888
        )
        orientationLayout.screenOrientation = screenOrientation
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).addView(
            orientationChanger, orientationLayout)
        orientationChanger.visibility = View.VISIBLE
    }

    private fun launchWidgetActivity(launchPackage: String?, launchActivity: String?) {
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
    }
}