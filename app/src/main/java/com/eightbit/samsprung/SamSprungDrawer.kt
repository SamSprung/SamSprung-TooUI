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

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eightbitlab.blurview.BlurView
import com.eightbitlab.blurview.RenderScriptBlur
import java.util.*


class SamSprungDrawer : AppCompatActivity(), AppLauncherAdapter.OnAppClickListener {

    @SuppressLint("InflateParams", "CutPasteId", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        setShowWhenLocked(true)
        // setTurnScreenOn(true)

        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.apps_view_layout)

        val permission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        )
        if (permission == PackageManager.PERMISSION_GRANTED) {
            val wallpaperManager: WallpaperManager = WallpaperManager.getInstance(this)
            findViewById<LinearLayout>(R.id.rootLayout).background = wallpaperManager.drawable
            findViewById<BlurView>(R.id.blurContainer).setupWith(
                window.decorView.findViewById(R.id.rootLayout)
            )
                .setFrameClearDrawable(window.decorView.background)
                .setBlurRadius(1f)
                .setBlurAutoUpdate(true)
                .setHasFixedTransformationMatrix(true)
                .setBlurAlgorithm(RenderScriptBlur(this))
        }

        val launcherView = findViewById<RecyclerView>(R.id.appsList)

        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        mainIntent.removeCategory(Intent.CATEGORY_HOME)

        val packages: MutableList<ResolveInfo> = packageManager.queryIntentActivities(mainIntent, 0)
        packages.removeIf { item -> SamSprung.prefs.getStringSet(
            SamSprung.prefHidden, HashSet())!!.contains(item.activityInfo.packageName
        ) }
        Collections.sort(packages, ResolveInfo.DisplayNameComparator(packageManager))

        if (SamSprung.prefs.getBoolean(SamSprung.prefLayout, true))
            launcherView.layoutManager = GridLayoutManager(this, getColumnCount())
        else
            launcherView.layoutManager = LinearLayoutManager(this)
        launcherView.adapter = AppLauncherAdapter(packages, this, packageManager)

        val simpleItemTouchCallback: ItemTouchHelper.SimpleCallback = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.RIGHT) {
                    if (isNotificationListenerEnabled()) {
                        startActivity(
                            Intent(SamSprung.context, SamSprungNotices::class.java),
                            ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle()
                        )
                    }
                    finish()
                }
                if (direction == ItemTouchHelper.LEFT) {
                    finish()
                }
            }
        }
        ItemTouchHelper(simpleItemTouchCallback).attachToRecyclerView(launcherView)
    }

    override fun onAppClicked(appInfo: ResolveInfo, position: Int) {
        if (Settings.System.canWrite(SamSprung.context))  {
            try {
                with (SamSprung.prefs.edit()) {
                    putBoolean(SamSprung.autoRotate,  Settings.System.getInt(
                        SamSprung.context.contentResolver,
                        Settings.System.ACCELEROMETER_ROTATION
                    ) == 1)
                    apply()
                }
            } catch (e: Settings.SettingNotFoundException) {
                e.printStackTrace()
            }
        }

        val mKeyguardManager = (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
        @Suppress("DEPRECATION")
        SamSprung.isKeyguardLocked = mKeyguardManager.inKeyguardRestrictedInputMode()

        if (SamSprung.isKeyguardLocked) {
            @Suppress("DEPRECATION")
            mKeyguardManager.newKeyguardLock("cover_lock").disableKeyguard()
        }

        val serviceIntent = Intent(this, DisplayListenerService::class.java)
        val extras = Bundle()
        extras.putString("launchPackage", appInfo.activityInfo.packageName)
        extras.putString("launchActivity", appInfo.activityInfo.name)
        startForegroundService(serviceIntent.putExtras(extras))

        if (SamSprung.useAppLauncherActivity) {
            startActivity(Intent(applicationContext, AppLauncherActivity::class.java).putExtras(extras))
        } else {
            if (SamSprung.prefs.getBoolean(SamSprung.prefScreen, false)) {
                IntentFilter(Intent.ACTION_SCREEN_OFF).also {
                    applicationContext.registerReceiver(
                        OffBroadcastReceiver(
                            ComponentName(appInfo.activityInfo.packageName, appInfo.activityInfo.name)
                        ), it
                    )
                }
            }
            val coverIntent = Intent(Intent.ACTION_MAIN)
            coverIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            coverIntent.component = ComponentName(appInfo.activityInfo.packageName, appInfo.activityInfo.name)
            val options = ActivityOptions.makeBasic().setLaunchDisplayId(1)
            try {
                val applicationInfo: ApplicationInfo =
                    packageManager.getApplicationInfo(
                        appInfo.activityInfo.packageName, PackageManager.GET_META_DATA
                    )
                applicationInfo.metaData.putString(
                    "com.samsung.android.activity.showWhenLocked", "true"
                )
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            coverIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            coverIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            coverIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            coverIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(coverIntent.putExtras(extras), options.toBundle())
        }
    }

    private fun getColumnCount(): Int {
        return (windowManager.currentWindowMetrics.bounds.width() / 88 + 0.5).toInt()
    }

    /**
     * https://github.com/kpbird/NotificationListenerService-Example
     */
    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        )
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (i in names.indices) {
                val cn = ComponentName.unflattenFromString(names[i])
                if (null != cn && TextUtils.equals(packageName, cn.packageName)) return true
            }
        }
        return false
    }
}