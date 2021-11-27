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
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast
import com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget.StepCoverAppWidget


class OffBroadcastReceiver : BroadcastReceiver {
    private var componentName : ComponentName? = null

    constructor()
    constructor(componentName: ComponentName) {
        this.componentName = componentName
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_FULLY_REMOVED) {
            sendAppWidgetUpdateBroadcast()
        }
        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
            if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                sendAppWidgetUpdateBroadcast()
            }
        }
        if (intent.action == SamSprung.updating) {
            when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    val activityIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                    if (null != activityIntent)
                        context.startActivity(activityIntent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                PackageInstaller.STATUS_SUCCESS -> {
                    // Installation was successful
                } else -> {
                    Toast.makeText(
                        SamSprung.context,
                        intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        if (intent.action == Intent.ACTION_SCREEN_OFF && null != componentName) {
            context.startService(Intent(context, DisplayListenerService::class.java))

            val screenIntent = Intent(Intent.ACTION_MAIN)
            screenIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            screenIntent.component = componentName
            val options = ActivityOptions.makeBasic().setLaunchDisplayId(0)
            screenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            screenIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            screenIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            screenIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            context.startActivity(screenIntent, options.toBundle())

            componentName = null
            SamSprung.context.unregisterReceiver(this)
        }
    }

    private fun sendAppWidgetUpdateBroadcast() {
        val updateIntent = Intent(SamSprung.context, StepCoverAppWidget::class.java)
        updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
            AppWidgetManager.getInstance(SamSprung.context).getAppWidgetIds(
            ComponentName(SamSprung.context, StepCoverAppWidget::class.java))
        )
        SamSprung.context.sendBroadcast(updateIntent)
    }
}
