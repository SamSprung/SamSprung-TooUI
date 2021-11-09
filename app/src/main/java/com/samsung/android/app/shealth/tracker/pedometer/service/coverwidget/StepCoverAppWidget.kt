package com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget

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
import android.app.KeyguardManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import com.sec.android.app.shealth.R
import com.sec.android.app.shealth.SamSprung
import com.sec.android.app.shealth.AppLauncherActivity
import com.sec.android.app.shealth.AppCollectionService
import com.sec.android.app.shealth.DisplayListenerService
import com.sec.android.app.shealth.OffBroadcastReceiver


class StepCoverAppWidget: AppWidgetProvider() {

    private val onClickTag = "OnClickTag"
    private val coverLock = "cover_lock"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val isGridView = SamSprung.prefs.getBoolean(SamSprung.prefLayout, true)
            val view = if (isGridView) R.id.widgetGridView else R.id.widgetListView
            val mgr = AppWidgetManager.getInstance(context.applicationContext)
            mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(ComponentName(
                context.applicationContext, StepCoverAppWidget::class.java)), view)
        }
        if (intent.action.equals(onClickTag)) {
            if (!intent.hasExtra("launchPackage")
                || !intent.hasExtra("launchActivity")) return
            val launchPackage = intent.getStringExtra("launchPackage")!!
            val launchActivity = intent.getStringExtra("launchActivity")!!

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

            val mKeyguardManager = (context.getSystemService(
                Context.KEYGUARD_SERVICE) as KeyguardManager)
            @Suppress("DEPRECATION")
            SamSprung.isKeyguardLocked = mKeyguardManager.inKeyguardRestrictedInputMode()

            if (SamSprung.isKeyguardLocked) {
                @Suppress("DEPRECATION")
                mKeyguardManager.newKeyguardLock(coverLock).disableKeyguard()
            }

            val serviceIntent = Intent(context, DisplayListenerService::class.java)
            val extras = Bundle()
            extras.putString("launchPackage", launchPackage)
            extras.putString("launchActivity", launchActivity)
            context.startForegroundService(serviceIntent.putExtras(extras))

            if (SamSprung.useAppLauncherActivity) {
                context.startActivity(Intent(context.applicationContext,
                    AppLauncherActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK).putExtras(extras))
            } else {
                if (SamSprung.prefs.getBoolean(SamSprung.prefScreen, false)) {
                    val mReceiver: BroadcastReceiver = OffBroadcastReceiver(
                        ComponentName(launchPackage, launchActivity)
                    )
                    IntentFilter(Intent.ACTION_SCREEN_OFF).also {
                        context.applicationContext.registerReceiver(mReceiver, it)
                    }
                }
                val coverIntent = Intent(Intent.ACTION_MAIN)
                coverIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                coverIntent.component = ComponentName(launchPackage, launchActivity)
                val options = ActivityOptions.makeBasic().setLaunchDisplayId(1)
                try {
                    val applicationInfo: ApplicationInfo =
                        context.packageManager.getApplicationInfo(
                            launchPackage, PackageManager.GET_META_DATA
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
                context.startActivity(coverIntent.putExtras(extras), options.toBundle())
            }
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val isGridView = SamSprung.prefs.getBoolean(SamSprung.prefLayout, true)
        val view = if (isGridView) R.id.widgetGridView else R.id.widgetListView

        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(
                context.packageName, R.layout.step_widget_view
            )

            views.setViewVisibility(R.id.widgetListView,
                if (isGridView) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.widgetGridView,
                if (isGridView) View.VISIBLE else View.GONE)

            val serviceIntent = Intent(context, AppCollectionService::class.java)
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            serviceIntent.data = Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))
            views.setRemoteAdapter(view, serviceIntent)

            val widgetIntent = Intent(context, StepCoverAppWidget::class.java)
            widgetIntent.action = onClickTag
            widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            val itemPendingIntent = PendingIntent.getBroadcast(
                context, 0, widgetIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                else PendingIntent.FLAG_UPDATE_CURRENT)
            views.setPendingIntentTemplate(view, itemPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        // TODO: Attempt to override configuration changes
    }
}
