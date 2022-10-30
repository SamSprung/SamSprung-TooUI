/*
 * ====================================================================
 * Copyright (c) 2021-2022 AbandonedCart.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * For the purpose of this license, the phrase "SamSprung labels" shall
 * be used to refer to the labels "8-bit Dream", "TwistedUmbrella",
 * "SamSprung" and "AbandonedCart" and these labels should be considered
 * the equivalent of any usage of the aforementioned phrase.
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All materials mentioning features or use of this software and
 *    redistributions of any form whatsoever must display the following
 *    acknowledgment unless made available by tagged, public "commits":
 *    "This product includes software developed for SamSprung by AbandonedCart"
 *
 * 4. The SamSprung labels must not be used in any form to endorse or promote
 *    products derived from this software without prior written permission.
 *    For written permission, please contact enderinexiledc@gmail.com
 *
 * 5. Products derived from this software may not be called by the SamSprung
 *    labels nor may these labels appear in their names or product information
 *    without prior written permission of AbandonedCart.
 *
 * THIS SOFTWARE IS PROVIDED BY AbandonedCart AND SamSprung ``AS IS'' AND ANY
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

package com.eightbit.samsprung.drawer

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.eightbit.samsprung.R
import com.eightbit.samsprung.SamSprung
import java.util.concurrent.Executors

class DrawerAppAdapter(
    private var packages: MutableList<ResolveInfo>,
    private val listener: OnAppClickListener,
    private val packageManager: PackageManager,
    private val prefs: SharedPreferences
) : RecyclerView.Adapter<DrawerAppAdapter.AppViewHolder>() {
    private var filter: PackageFilter? = null
    private var filteredData: MutableList<ResolveInfo> = packages

    fun setPackages(packages: MutableList<ResolveInfo>) {
        this.packages = packages
        filteredData = packages
    }

    override fun getItemCount(): Int {
        return filteredData.size
    }

    override fun getItemId(i: Int): Long {
        return filteredData[i].labelRes.toLong()
    }

    private fun getItem(i: Int): ResolveInfo {
        return filteredData[i]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        return if (prefs.getBoolean(SamSprung.prefLayout, true))
            SimpleGridHolder(parent, listener, packageManager, prefs)
        else
        return SimpleViewHolder(parent, listener, packageManager, prefs)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            if (null != holder.listener)
                holder.listener.onAppClicked(holder.resolveInfo, position)
        }
        holder.iconView.setOnClickListener {
            if (null != holder.listener)
                holder.listener.onAppClicked(holder.resolveInfo, position)
        }
        holder.bind(getItem(position))
    }

    fun setQuery(query: String) {
        getFilter()?.filter(query)
    }

    private fun getFilter(): PackageFilter? {
        if (null == this.filter) {
            this.filter = PackageFilter()
        }
        return this.filter
    }

    inner class PackageFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = constraint?.toString() ?: ""
            val filterResults = FilterResults()
            val queryText = query.trim { it <= ' ' }.lowercase()
            if (queryText.isBlank()) {
                filterResults.count = packages.size
                filterResults.values = packages
                return filterResults
            }
            val tempList: MutableList<ResolveInfo> = mutableListOf()
            Executors.newSingleThreadExecutor().execute {
                for (app in packages) {
                    if (app.loadLabel(packageManager).contains(queryText, ignoreCase = true))
                        tempList.add(app)
                }
            }
            filterResults.count = tempList.size
            filterResults.values = tempList
            return filterResults
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
            if (filteredData === filterResults.values) return
            val results: MutableList<ResolveInfo> = mutableListOf()
            results.addAll(filterResults.values as Collection<ResolveInfo>)
            filteredData = results
            notifyDataSetChanged()
        }
    }

    abstract class AppViewHolder(
        itemView: View, val listener: OnAppClickListener?,
        private val packageManager: PackageManager,
        private val prefs: SharedPreferences
    ) : RecyclerView.ViewHolder(itemView) {
        val iconView: AppCompatImageView = itemView.findViewById(R.id.widgetItemImage)
        lateinit var resolveInfo: ResolveInfo
        fun bind(resolveInfo: ResolveInfo) {
            this.resolveInfo = resolveInfo
            Executors.newSingleThreadExecutor().execute {
                val icon = resolveInfo.loadIcon(packageManager)
                if (null != icon) {
                    iconView.post {
                        iconView.setImageDrawable(icon)
                    }
                }
                if (!prefs.getBoolean(SamSprung.prefLayout, true)) {
                    val label: CharSequence? = try {
                        resolveInfo.loadLabel(packageManager)
                    } catch (e: Exception) {
                        resolveInfo.nonLocalizedLabel
                    }
                    if (null != label) {
                        itemView.post {
                            itemView.findViewById<TextView>(R.id.widgetItemText).text = label
                        }
                    }
                }
            }
        }
    }

    internal class SimpleViewHolder(
        parent: ViewGroup,
        listener: OnAppClickListener?,
        packageManager: PackageManager,
        prefs: SharedPreferences
    ) : AppViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.drawer_apps_card, parent, false
        ), listener, packageManager, prefs
    )

    internal class SimpleGridHolder(
        parent: ViewGroup,
        listener: OnAppClickListener?,
        packageManager: PackageManager,
        prefs: SharedPreferences
    ) : AppViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.drawer_apps_icon, parent, false
        ), listener, packageManager, prefs
    )

    interface OnAppClickListener {
        fun onAppClicked(resolveInfo: ResolveInfo, position: Int)
    }
}
