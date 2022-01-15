package com.sec.android.app.shealth

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

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget.StepCoverAppWidget


class FilteredAppsAdapter(
    private val context: Context,
    private var packages: MutableList<ResolveInfo>,
    private var hide: HashSet<String>
) : BaseAdapter() {
    private var pacMan: PackageManager = context.packageManager

    override fun getCount(): Int {
        return packages.size //returns total of items in the list
    }

    override fun getItem(position: Int): Any {
        return packages[position] //returns list item at the specified position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var convertView = view
        if (null == convertView) {
            convertView = LayoutInflater.from(context)
                .inflate(R.layout.app_hidden_item, parent, false)
        }

        val application = packages[position]

        val appName = application.loadLabel(pacMan).toString()

        val detailView = convertView!!.findViewById<LinearLayout>(R.id.hiddenItemContainer)

        detailView.findViewById<ImageView>(R.id.hiddenItemImage).setImageBitmap(
            getBitmapFromDrawable(application.loadIcon(pacMan)))

        detailView.findViewById<TextView>(R.id.hiddenItemText).text = appName

        val hideSwitch = detailView.findViewById<SwitchCompat>(R.id.hiddenItemSwitch)
        hideSwitch.isChecked = !hide.contains(application.activityInfo.packageName)

        hideSwitch.setOnClickListener {
            val packageName = application.activityInfo.packageName
            if (hide.contains(packageName)) {
                hide.remove(packageName)
                with(SamSprung.prefs.edit()) {
                    putStringSet(SamSprung.prefHidden, hide)
                    apply()
                }
                Toast.makeText(
                    context, context.getString(
                        R.string.show_package, appName
                    ), Toast.LENGTH_SHORT
                ).show()
            } else {
                hide.add(packageName)
                with(SamSprung.prefs.edit()) {
                    putStringSet(SamSprung.prefHidden, hide)
                    apply()
                }
                Toast.makeText(
                    context, context.getString(
                        R.string.hide_package, appName
                    ), Toast.LENGTH_SHORT
                ).show()
            }
            sendAppWidgetUpdateBroadcast(context)
        }

        return convertView
    }

    private fun sendAppWidgetUpdateBroadcast(context: Context) {
        val updateIntent = Intent(context, StepCoverAppWidget::class.java)
        updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        updateIntent.putExtra(
            AppWidgetManager.EXTRA_APPWIDGET_IDS,
            AppWidgetManager.getInstance(SamSprung.context).getAppWidgetIds(
                ComponentName(SamSprung.context, StepCoverAppWidget::class.java)
            )
        )
        context.sendBroadcast(updateIntent)
    }

    private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
        val bitmapDrawable = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmapDrawable)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmapDrawable
    }
}