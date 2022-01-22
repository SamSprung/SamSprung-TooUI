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

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.eightbit.samsprung.AppDrawerAdapater.AppViewHolder

class AppDrawerAdapater(
    private var packages: MutableList<ResolveInfo>,
    private val listener: OnAppClickListener,
    private val packageManager: PackageManager
) : RecyclerView.Adapter<AppViewHolder>() {
    fun setPackages(packages: MutableList<ResolveInfo>) {
        this.packages = packages
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        return if (SamSprung.prefs.getBoolean(SamSprung.prefLayout, true))
            SimpleGridHolder(parent, listener, packageManager)
        else
        return SimpleViewHolder(parent, listener, packageManager)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            if (null != holder.listener) holder.listener.onAppClicked(
                holder.appInfo!!,
                position
            )
        }
        holder.iconView.setOnClickListener {
            if (null != holder.listener) {
                holder.listener.onAppClicked(holder.appInfo!!, position)
            }
        }
        holder.bind(getItem(position))
    }

    abstract class AppViewHolder(
        itemView: View, val listener: OnAppClickListener?,
        private val packageManager: PackageManager
    ) : RecyclerView.ViewHolder(itemView) {
        val iconView: ImageView = itemView.findViewById(R.id.widgetItemImage)
        var appInfo: ResolveInfo? = null
        fun bind(appInfo: ResolveInfo) {
            this.appInfo = appInfo
            val icon = appInfo.loadIcon(packageManager)
            if (null != icon) {
                iconView.setImageDrawable(icon)
            }
            if (!SamSprung.prefs.getBoolean(SamSprung.prefLayout, true)) {
                val label: CharSequence? = try {
                    appInfo.loadLabel(packageManager)
                } catch (e: Exception) {
                    appInfo.nonLocalizedLabel
                }
                if (null != label) {
                    itemView.findViewById<TextView>(R.id.widgetItemText).text = label
                }
            }
        }
    }

    internal class SimpleViewHolder(
        parent: ViewGroup,
        listener: OnAppClickListener?,
        packageManager: PackageManager
    ) : AppViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.app_drawer_item,
            parent, false
        ), listener, packageManager
    )

    internal class SimpleGridHolder(
        parent: ViewGroup,
        listener: OnAppClickListener?,
        packageManager: PackageManager
    ) : AppViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.app_drawer_icon,
            parent, false
        ), listener, packageManager
    )

    interface OnAppClickListener {
        fun onAppClicked(appInfo: ResolveInfo, position: Int)
    }
}