package com.eightbit.samsprung.settings

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

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.eightbit.samsprung.R
import com.eightbit.samsprung.SamSprung
import java.util.concurrent.Executors

class FilteredAppsAdapter(
    private val pacMan: PackageManager,
    private var packages: MutableList<ResolveInfo>,
    private var hide: HashSet<String>,
    private val prefs: SharedPreferences
) : RecyclerView.Adapter<FilteredAppsAdapter.HideViewHolder>() {

    fun setPackages(packages: MutableList<ResolveInfo>, hide: HashSet<String>) {
        this.packages = packages
        this.hide = hide
    }

    override fun getItemCount(): Int {
        return packages.size
    }

    override fun getItemId(i: Int): Long {
        return packages[i].labelRes.toLong()
    }

    private fun getItem(i: Int): ResolveInfo {
        return packages[i]
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HideViewHolder {
        return SimpleViewHolder(parent, pacMan, prefs, hide)
    }

    override fun onBindViewHolder(holder: HideViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    abstract class HideViewHolder(
        itemView: View,
        private val packageManager: PackageManager,
        private val prefs: SharedPreferences,
        private val hide: HashSet<String>
    ) : RecyclerView.ViewHolder(itemView) {
        lateinit var application: ResolveInfo
        fun bind(appInfo: ResolveInfo) {
            this.application = appInfo
            val detailView = itemView.findViewById<LinearLayout>(R.id.hiddenItemContainer)

            Executors.newSingleThreadExecutor().execute {
                val appName: CharSequence? = try {
                    application.loadLabel(packageManager)
                } catch (e: Exception) {
                    application.nonLocalizedLabel
                }
                val icon = application.loadIcon(packageManager)

                detailView.post {
                    detailView.findViewById<AppCompatImageView>(
                        R.id.hiddenItemImage).setImageDrawable(icon)
                    detailView.findViewById<TextView>(R.id.hiddenItemText).text = appName
                }
            }

            val hideSwitch = detailView.findViewById<SwitchCompat>(R.id.hiddenItemSwitch)
            hideSwitch.isChecked = !hide.contains(application.activityInfo.packageName)

            hideSwitch.setOnClickListener {
                val packageName = application.activityInfo.packageName
                if (hide.contains(packageName)) {
                    hide.remove(packageName)
                    with(prefs.edit()) {
                        putStringSet(SamSprung.prefHidden, hide)
                        apply()
                    }
                } else {
                    hide.add(packageName)
                    with(prefs.edit()) {
                        putStringSet(SamSprung.prefHidden, hide)
                        apply()
                    }
                }
            }
        }
    }

    internal class SimpleViewHolder(
        parent: ViewGroup,
        packageManager: PackageManager,
        prefs: SharedPreferences,
        hide: HashSet<String>
    ) : HideViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.filtered_apps_item, parent, false
        ), packageManager, prefs, hide
    )
}